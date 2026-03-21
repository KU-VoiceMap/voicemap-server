import { useRef, useCallback } from 'react';
import { VoiceWebSocket } from '../api/websocket';
import type { WsMessageHandler, WsTranscriptPayload, WsAudioOutputPayload } from '../api/websocket';

function floatToPcm16(float32: Float32Array): Int16Array {
  const output = new Int16Array(float32.length);
  for (let i = 0; i < float32.length; i++) {
    const v = Math.max(-1, Math.min(1, float32[i]));
    output[i] = v < 0 ? v * 0x8000 : v * 0x7fff;
  }
  return output;
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  let binary = '';
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function resample(source: Float32Array, sourceRate: number, targetRate: number): Float32Array {
  if (sourceRate === targetRate) return source;
  const ratio = sourceRate / targetRate;
  const outputLength = Math.round(source.length / ratio);
  const output = new Float32Array(outputLength);
  for (let i = 0; i < outputLength; i++) {
    const srcIndex = i * ratio;
    const idx = Math.floor(srcIndex);
    const frac = srcIndex - idx;
    output[i] = idx + 1 < source.length
      ? source[idx] * (1 - frac) + source[idx + 1] * frac
      : source[idx];
  }
  return output;
}

function createAudioProcessorBlobUrl(): string {
  const script = `
    class VoiceMapAudioProcessor extends AudioWorkletProcessor {
      constructor(options) {
        super();
        const cfg = options.processorOptions || {};
        this.sourceSampleRate = cfg.sourceSampleRate || 48000;
        this.targetSampleRate = cfg.targetSampleRate || 16000;
        this.downsampleRatio = cfg.downsampleRatio || 3;
        this.chunkSize = Math.floor(this.sourceSampleRate * 0.1);
        this.buffer = [];
      }
      process(inputs) {
        const input = inputs[0];
        if (!input || input.length === 0) return true;
        const channelData = input[0];
        for (let i = 0; i < channelData.length; i++) {
          this.buffer.push(channelData[i]);
        }
        if (this.buffer.length >= this.chunkSize) {
          const outputLength = Math.floor(this.buffer.length / this.downsampleRatio);
          const output = new Float32Array(outputLength);
          for (let i = 0; i < outputLength; i++) {
            const start = i * this.downsampleRatio;
            let sum = 0;
            for (let j = 0; j < this.downsampleRatio; j++) {
              sum += this.buffer[start + j];
            }
            output[i] = sum / this.downsampleRatio;
          }
          this.port.postMessage({ type: "audio", audio: output });
          this.buffer = [];
        }
        return true;
      }
    }
    registerProcessor("voicemap-audio-processor", VoiceMapAudioProcessor);
  `;
  return URL.createObjectURL(new Blob([script], { type: 'application/javascript' }));
}

export interface VoiceSessionCallbacks {
  onTranscript: (payload: WsTranscriptPayload) => void;
  onTurnCompleted: () => void;
  onInterrupted: () => void;
  onSessionReady: (sessionId: string) => void;
  onConnectionLost: () => void;
}

export function useVoiceSession() {
  const wsRef = useRef(new VoiceWebSocket());
  const micContextRef = useRef<AudioContext | null>(null);
  const playbackContextRef = useRef<AudioContext | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const workletNodeRef = useRef<AudioWorkletNode | null>(null);
  const workletUrlRef = useRef<string | null>(null);
  const audioQueueRef = useRef<ArrayBuffer[]>([]);
  const audioSourcesRef = useRef<AudioBufferSourceNode[]>([]);
  const isPlayingRef = useRef(false);
  const nextStartTimeRef = useRef(0);
  const callbacksRef = useRef<VoiceSessionCallbacks | null>(null);

  const stopPlayback = useCallback(async () => {
    audioQueueRef.current = [];
    audioSourcesRef.current.forEach((s) => { try { s.stop(); } catch { } });
    audioSourcesRef.current = [];
    isPlayingRef.current = false;
    nextStartTimeRef.current = 0;
    if (playbackContextRef.current) {
      await playbackContextRef.current.close();
      playbackContextRef.current = null;
    }
  }, []);

  const playAudioQueue = useCallback(async () => {
    if (audioQueueRef.current.length === 0) {
      isPlayingRef.current = false;
      nextStartTimeRef.current = 0;
      return;
    }

    isPlayingRef.current = true;

    if (!playbackContextRef.current) {
      playbackContextRef.current = new AudioContext();
    }
    if (playbackContextRef.current.state === 'suspended') {
      await playbackContextRef.current.resume();
    }

    const ctx = playbackContextRef.current;
    const audioData = audioQueueRef.current.shift()!;
    const int16 = new Int16Array(audioData);
    const float32 = new Float32Array(int16.length);
    for (let i = 0; i < int16.length; i++) {
      float32[i] = int16[i] / (int16[i] < 0 ? 0x8000 : 0x7fff);
    }

    const sourceSampleRate = 24000;
    const pcm = resample(float32, sourceSampleRate, ctx.sampleRate);
    const audioBuffer = ctx.createBuffer(1, pcm.length, ctx.sampleRate);
    audioBuffer.getChannelData(0).set(pcm);

    const source = ctx.createBufferSource();
    const gain = ctx.createGain();
    gain.gain.value = 1.0;
    source.buffer = audioBuffer;
    source.connect(gain);
    gain.connect(ctx.destination);

    const now = ctx.currentTime;
    if (nextStartTimeRef.current < now) {
      nextStartTimeRef.current = now;
    }
    source.start(nextStartTimeRef.current);
    nextStartTimeRef.current += audioBuffer.duration;
    audioSourcesRef.current.push(source);

    source.onended = () => {
      audioSourcesRef.current = audioSourcesRef.current.filter((s) => s !== source);
      if (audioQueueRef.current.length > 0) {
        setTimeout(() => { playAudioQueue().catch(console.error); }, 0);
        return;
      }
      if (audioQueueRef.current.length === 0 && audioSourcesRef.current.length === 0) {
        isPlayingRef.current = false;
        nextStartTimeRef.current = 0;
      }
    };

    if (audioQueueRef.current.length > 0) {
      setTimeout(() => { playAudioQueue().catch(console.error); }, 0);
    }
  }, []);

  const handleAudioOutput = useCallback(async (payload: WsAudioOutputPayload) => {
    if (!payload.base64Audio) return;
    audioQueueRef.current.push(base64ToArrayBuffer(payload.base64Audio));
    if (!isPlayingRef.current || audioSourcesRef.current.length === 0) {
      await playAudioQueue();
    }
  }, [playAudioQueue]);

  const handleInterrupted = useCallback(() => {
    audioQueueRef.current = [];
    audioSourcesRef.current.forEach((s) => { try { s.stop(); } catch { } });
    audioSourcesRef.current = [];
    isPlayingRef.current = false;
    nextStartTimeRef.current = 0;
    callbacksRef.current?.onInterrupted();
  }, []);

  const stopMicrophone = useCallback(async () => {
    if (workletNodeRef.current) {
      try { workletNodeRef.current.disconnect(); } catch { }
      workletNodeRef.current = null;
    }
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((t) => t.stop());
      mediaStreamRef.current = null;
    }
    if (micContextRef.current) {
      await micContextRef.current.close();
      micContextRef.current = null;
    }
    if (workletUrlRef.current) {
      URL.revokeObjectURL(workletUrlRef.current);
      workletUrlRef.current = null;
    }
  }, []);

  const startMicrophone = useCallback(async (sessionId: string) => {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
        channelCount: 1,
      },
    });
    mediaStreamRef.current = stream;

    const ctx = new AudioContext();
    micContextRef.current = ctx;
    const source = ctx.createMediaStreamSource(stream);
    const sourceSampleRate = ctx.sampleRate;
    const downsampleRatio = Math.max(1, Math.round(sourceSampleRate / 16000));

    workletUrlRef.current = createAudioProcessorBlobUrl();
    await ctx.audioWorklet.addModule(workletUrlRef.current);

    const workletNode = new AudioWorkletNode(ctx, 'voicemap-audio-processor', {
      processorOptions: { sourceSampleRate, targetSampleRate: 16000, downsampleRatio },
    });
    workletNodeRef.current = workletNode;

    workletNode.port.onmessage = (event: MessageEvent) => {
      if (event.data.type !== 'audio') return;
      if (!wsRef.current.isOpen || !sessionId) return;
      const pcm = floatToPcm16(event.data.audio as Float32Array);
      const base64 = arrayBufferToBase64(pcm.buffer as ArrayBuffer);
      wsRef.current.send({
        type: 'AUDIO_INPUT',
        payload: { data: base64 },
      });
    };

    source.connect(workletNode);
  }, []);

  const start = useCallback(async (
    accessToken: string,
    chatId: string | null,
    callbacks: VoiceSessionCallbacks,
  ): Promise<string> => {
    callbacksRef.current = callbacks;

    const handler: WsMessageHandler = {
      onSessionReady: (payload) => callbacks.onSessionReady(payload.sessionId),
      onTranscript: (payload) => callbacks.onTranscript(payload),
      onAudioOutput: (payload) => { handleAudioOutput(payload).catch(console.error); },
      onInterrupted: handleInterrupted,
      onTurnCompleted: () => callbacks.onTurnCompleted(),
      onClose: (wasManual) => { if (!wasManual) callbacks.onConnectionLost(); },
      onError: () => { },
    };

    const sessionId = await wsRef.current.connect(accessToken, chatId, handler);
    await startMicrophone(sessionId);
    return sessionId;
  }, [startMicrophone, handleAudioOutput, handleInterrupted]);

  const stop = useCallback(async () => {
    await stopMicrophone();
    await stopPlayback();
    wsRef.current.close();
  }, [stopMicrophone, stopPlayback]);

  return { start, stop };
}
