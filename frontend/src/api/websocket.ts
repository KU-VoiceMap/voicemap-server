export interface WsSessionReadyPayload {
  sessionId: string;
}

export interface WsTranscriptPayload {
  role: 'USER' | 'AGENT';
  text: string;
}

export interface WsAudioOutputPayload {
  base64Audio: string;
}

export type WsMessageHandler = {
  onSessionReady: (payload: WsSessionReadyPayload) => void;
  onTranscript: (payload: WsTranscriptPayload) => void;
  onAudioOutput: (payload: WsAudioOutputPayload) => void;
  onInterrupted: () => void;
  onTurnCompleted: () => void;
  onClose: (wasManual: boolean) => void;
  onError: () => void;
};

export class VoiceWebSocket {
  private ws: WebSocket | null = null;
  private manualClose = false;

  connect(accessToken: string, chatId: string | null, handler: WsMessageHandler): Promise<string> {
    this.close();

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/chat`;
    const socket = new WebSocket(wsUrl);
    this.ws = socket;

    return new Promise<string>((resolve, reject) => {
      let timeoutId: ReturnType<typeof setTimeout> | null = setTimeout(() => {
        timeoutId = null;
        reject(new Error('SESSION_READY 응답 시간 초과'));
      }, 12000);

      const cleanup = () => {
        if (timeoutId) {
          clearTimeout(timeoutId);
          timeoutId = null;
        }
      };

      socket.onopen = () => {
        if (this.ws !== socket) return;
        socket.send(JSON.stringify({
          type: 'SESSION_INIT',
          payload: { token: accessToken, chatId },
        }));
      };

      socket.onmessage = (event) => {
        if (this.ws !== socket) return;

        let message: { type: string; payload?: unknown };
        try {
          message = JSON.parse(event.data as string);
        } catch {
          console.error('[ws] invalid payload', event.data);
          return;
        }

        const payload = (message.payload ?? {}) as Record<string, unknown>;

        switch (message.type) {
          case 'SESSION_READY': {
            const sessionPayload: WsSessionReadyPayload = {
              sessionId: String(payload.sessionId ?? ''),
            };
            cleanup();
            resolve(sessionPayload.sessionId);
            handler.onSessionReady(sessionPayload);
            break;
          }
          case 'AUDIO_OUTPUT': {
            const audioPayload: WsAudioOutputPayload = {
              base64Audio: String(payload.base64Audio ?? ''),
            };
            handler.onAudioOutput(audioPayload);
            break;
          }
          case 'TRANSCRIPT': {
            const transcriptPayload: WsTranscriptPayload = {
              role: payload.role === 'USER' ? 'USER' : 'AGENT',
              text: String(payload.text ?? ''),
            };
            handler.onTranscript(transcriptPayload);
            break;
          }
          case 'INTERRUPTED':
            handler.onInterrupted();
            break;
          case 'TURN_COMPLETED':
            handler.onTurnCompleted();
            break;
          default:
            console.warn('[ws] unsupported message type', message.type);
        }
      };

      socket.onerror = () => {
        if (this.ws !== socket) return;
        cleanup();
        reject(new Error('WebSocket 오류'));
        handler.onError();
      };

      socket.onclose = () => {
        if (this.ws !== socket) return;
        this.ws = null;
        const wasManual = this.manualClose;
        this.manualClose = false;
        if (!wasManual) {
          cleanup();
          reject(new Error('WebSocket 연결이 종료되었습니다.'));
        }
        handler.onClose(wasManual);
      };
    });
  }

  send(data: unknown): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    }
  }

  close(): void {
    if (!this.ws) return;
    const socket = this.ws;
    this.manualClose = true;
    this.ws = null;
    try {
      socket.close();
    } catch (error) {
      console.error('[ws] failed to close', error);
    }
  }

  get isOpen(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }
}
