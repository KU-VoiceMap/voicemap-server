// Voice Chat Client
class VoiceChatClient {
    constructor() {
        this.ws = null;
        this.sessionId = null;
        this.isConnected = false;
        this.micAudioContext = null; // 마이크 전용 AudioContext
        this.playbackAudioContext = null; // 재생 전용 AudioContext
        this.mediaStream = null;
        this.audioWorkletNode = null;
        this.audioQueue = [];
        this.isPlayingAudio = false;
        this.currentAudioSource = null; // 현재 재생 중인 소스
        this.currentMessageElement = null; // 현재 업데이트 중인 메시지 요소
        this.currentMessageRole = null; // 현재 메시지의 role
        this.nextStartTime = 0; // 다음 오디오 청크 재생 시작 시간
        this.audioSources = []; // 재생 중인 모든 소스 추적

        // UI Elements
        this.connectBtn = document.getElementById('connectBtn');
        this.disconnectBtn = document.getElementById('disconnectBtn');
        this.statusIndicator = document.getElementById('statusIndicator');
        this.statusText = document.querySelector('.status-text');
        this.chatMessages = document.getElementById('chatMessages');
        this.sessionIdElement = document.getElementById('sessionId');
        this.micStatus = document.getElementById('micStatus');
        this.aiStatus = document.getElementById('aiStatus');

        this.initEventListeners();
    }

    initEventListeners() {
        this.connectBtn.addEventListener('click', () => this.connect());
        this.disconnectBtn.addEventListener('click', () => this.disconnect());
    }

    async connect() {
        try {
            this.updateStatus('연결 중...', false);
            this.connectBtn.disabled = true;

            // WebSocket 연결
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${protocol}//${window.location.host}/ws/chat`;
            this.ws = new WebSocket(wsUrl);

            this.ws.onopen = () => this.onWebSocketOpen();
            this.ws.onmessage = (event) => this.onWebSocketMessage(event);
            this.ws.onerror = (error) => this.onWebSocketError(error);
            this.ws.onclose = () => this.onWebSocketClose();

        } catch (error) {
            console.error('Connection error:', error);
            this.updateStatus('연결 실패', false);
            this.connectBtn.disabled = false;
        }
    }

    onWebSocketOpen() {
        console.log('WebSocket connected');

        // SESSION_INIT 메시지 전송
        const initMessage = {
            type: 'SESSION_INIT',
            payload: {
                token: 'demo-access-token' // 실제로는 인증 토큰 사용
            }
        };

        this.ws.send(JSON.stringify(initMessage));
        this.updateStatus('세션 초기화 중...', false);
    }

    async onWebSocketMessage(event) {
        try {
            const message = JSON.parse(event.data);
            console.log('Received message:', message.type);

            switch (message.type) {
                case 'SESSION_READY':
                    await this.onSessionReady(message.payload);
                    break;
                case 'AUDIO_OUTPUT':
                    await this.onAudioOutput(message.payload);
                    break;
                case 'TRANSCRIPT':
                    this.onTranscript(message.payload);
                    break;
                case 'INTERRUPTED':
                    this.onInterrupted();
                    break;
                case 'TURN_COMPLETED':
                    this.onTurnComplete();
                    break;
                default:
                    console.warn('Unknown message type:', message.type);
            }
        } catch (error) {
            console.error('Error processing message:', error);
        }
    }

    async onSessionReady(payload) {
        console.log('Session ready:', payload.sessionId);
        this.sessionId = payload.sessionId;
        this.sessionIdElement.textContent = payload.sessionId;
        this.isConnected = true;

        this.updateStatus('연결됨', true);
        this.connectBtn.disabled = true;
        this.disconnectBtn.disabled = false;

        // 마이크 시작
        await this.startMicrophone();
    }

    async startMicrophone() {
        try {
            this.micStatus.textContent = '마이크 준비 중...';

            // 마이크 권한 체크
            try {
                const permissionStatus = await navigator.permissions.query({ name: 'microphone' });
                console.log('Microphone permission status:', permissionStatus.state);

                if (permissionStatus.state === 'denied') {
                    throw new Error('마이크 권한이 거부되었습니다. 브라우저 설정에서 권한을 허용해주세요.');
                }
            } catch (permError) {
                // Safari 등 permissions API를 지원하지 않는 브라우저에서는 무시
                console.log('Permissions API not supported, proceeding with getUserMedia');
            }

            // AudioContext를 먼저 생성 (브라우저 기본 샘플레이트 사용)
            // 마이크 전용 AudioContext
            this.micAudioContext = new (window.AudioContext || window.webkitAudioContext)();
            const contextSampleRate = this.micAudioContext.sampleRate;
            console.log('Microphone AudioContext sample rate:', contextSampleRate);

            // 마이크 스트림 가져오기
            this.mediaStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                    channelCount: 1 // 모노
                }
            });

            console.log('Media stream obtained:', this.mediaStream.getAudioTracks()[0].getSettings());

            const source = this.micAudioContext.createMediaStreamSource(this.mediaStream);

            // 다운샘플링 비율 계산 (예: 48000 -> 16000 = 3:1)
            const downsampleRatio = Math.round(contextSampleRate / 16000);
            console.log('Downsample ratio:', contextSampleRate, '->', 16000, '(ratio:', downsampleRatio, ')');

            // AudioWorklet을 사용하여 오디오 처리
            const processorUrl = this.createAudioProcessorBlob();
            await this.micAudioContext.audioWorklet.addModule(processorUrl);

            this.audioWorkletNode = new AudioWorkletNode(this.micAudioContext, 'audio-processor', {
                processorOptions: {
                    sampleRate: contextSampleRate,
                    targetSampleRate: 16000,
                    downsampleRatio: downsampleRatio
                }
            });

            this.audioWorkletNode.port.onmessage = (event) => {
                if (event.data.type === 'audio') {
                    this.sendAudioData(event.data.audio);
                }
            };

            source.connect(this.audioWorkletNode);
            // 주의: destination에 연결하면 에코가 발생할 수 있으므로 제거
            // this.audioWorkletNode.connect(this.micAudioContext.destination);

            this.micStatus.textContent = '녹음 중 🎙️';
            console.log('Microphone started successfully');

        } catch (error) {
            console.error('Microphone error:', error);
            this.micStatus.textContent = '마이크 오류';

            let errorMessage = '마이크를 시작할 수 없습니다.\n\n상세 정보: ' + error.message;

            if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
                errorMessage = '마이크 접근 권한이 거부되었습니다. 브라우저 설정에서 권한을 허용해주세요.';
            } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
                errorMessage = '마이크를 찾을 수 없습니다. 마이크가 연결되어 있는지 확인해주세요.';
            } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
                errorMessage = '마이크에 접근할 수 없습니다. 다른 프로그램이 마이크를 사용 중일 수 있습니다.';
            }

            alert(errorMessage);
            this.disconnect();
        }
    }

    createAudioProcessorBlob() {
        const processorCode = `
        class AudioProcessor extends AudioWorkletProcessor {
            constructor(options) {
                super();
                const processorOptions = options.processorOptions || {};
                this.sampleRate = processorOptions.sampleRate || 48000;
                this.targetSampleRate = processorOptions.targetSampleRate || 16000;
                this.downsampleRatio = processorOptions.downsampleRatio || 3;
                
                // 버퍼 크기: 100ms 분량
                this.bufferSize = Math.floor(this.sampleRate * 0.1);
                this.buffer = [];
                
                console.log('AudioProcessor initialized:', {
                    sampleRate: this.sampleRate,
                    targetSampleRate: this.targetSampleRate,
                    downsampleRatio: this.downsampleRatio,
                    bufferSize: this.bufferSize
                });
            }
            
            process(inputs, outputs, parameters) {
                const input = inputs[0];
                if (input.length > 0) {
                    const channelData = input[0]; // 모노 채널
                    
                    for (let i = 0; i < channelData.length; i++) {
                        this.buffer.push(channelData[i]);
                    }
                    
                    // 버퍼가 충분히 차면 다운샘플링하여 전송
                    if (this.buffer.length >= this.bufferSize) {
                        const downsampled = this.downsample(this.buffer, this.downsampleRatio);
                        this.port.postMessage({
                            type: 'audio',
                            audio: downsampled
                        });
                        this.buffer = [];
                    }
                }
                
                return true;
            }
            
            downsample(buffer, ratio) {
                if (ratio === 1) {
                    return new Float32Array(buffer);
                }
                
                const length = Math.floor(buffer.length / ratio);
                const result = new Float32Array(length);
                
                // 고품질 다운샘플링을 위한 평균 필터 사용
                for (let i = 0; i < length; i++) {
                    const start = i * ratio;
                    let sum = 0;
                    for (let j = 0; j < ratio; j++) {
                        sum += buffer[start + j];
                    }
                    result[i] = sum / ratio;
                }
                
                return result;
            }
        }
        
        registerProcessor('audio-processor', AudioProcessor);
        `;

        const blob = new Blob([processorCode], { type: 'application/javascript' });
        return URL.createObjectURL(blob);
    }

    sendAudioData(audioData) {
        if (!this.isConnected || !this.sessionId) {
            return;
        }

        // Float32Array를 Int16 PCM으로 변환
        const pcmData = this.floatToPCM(audioData);

        // Base64로 인코딩
        const base64Audio = this.arrayBufferToBase64(pcmData.buffer);

        // AUDIO_INPUT 메시지 전송
        const message = {
            type: 'AUDIO_INPUT',
            payload: {
                sessionId: this.sessionId,
                data: base64Audio
            }
        };

        this.ws.send(JSON.stringify(message));
    }

    floatToPCM(float32Array) {
        const int16Array = new Int16Array(float32Array.length);
        for (let i = 0; i < float32Array.length; i++) {
            // Float32 [-1.0, 1.0]을 Int16 [-32768, 32767]로 변환
            const s = Math.max(-1, Math.min(1, float32Array[i]));
            int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
        }
        return int16Array;
    }

    arrayBufferToBase64(buffer) {
        let binary = '';
        const bytes = new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }

    async onAudioOutput(payload) {
        try {
            // AI 응답이 시작되면 사용자 메시지 턴 종료 (첫 번째 AUDIO_OUTPUT에만 적용)
            if (this.currentMessageRole === 'USER') {
                this.currentMessageElement = null;
                this.currentMessageRole = null;
            }

            // payload 확인
            if (!payload || !payload.base64Audio) {
                console.error('Invalid payload:', payload);
                return;
            }

            // Base64 디코딩
            const audioData = this.base64ToArrayBuffer(payload.base64Audio);

            console.log('Received audio output, size:', audioData.byteLength);

            // 오디오 큐에 추가
            this.audioQueue.push(audioData);

            console.log('Audio queue length:', this.audioQueue.length, 'isPlayingAudio:', this.isPlayingAudio);

            // 재생 중이 아니면 재생 시작
            if (!this.isPlayingAudio) {
                await this.playAudioQueue();
            }
        } catch (error) {
            console.error('Error handling audio output:', error);
        }
    }

    base64ToArrayBuffer(base64) {
        try {
            const binaryString = atob(base64);
            const len = binaryString.length;
            const bytes = new Uint8Array(len);
            for (let i = 0; i < len; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }
            return bytes.buffer;
        } catch (error) {
            console.error('Error decoding base64:', error);
            throw error;
        }
    }

    onTurnComplete() {
        console.log('Turn complete');
        this.aiStatus.textContent = '대기 중';

        // AI 응답 턴 종료
        // 다음 TRANSCRIPT는 새로운 말풍선으로 시작
        this.currentMessageElement = null;
        this.currentMessageRole = null;
    }

    async playAudioQueue() {
        if (this.audioQueue.length === 0) {
            console.log('playAudioQueue: queue is empty');
            return;
        }

        // 첫 재생이면 상태 초기화
        if (!this.isPlayingAudio) {
            this.isPlayingAudio = true;
            this.aiStatus.textContent = '응답 중 🔊';
            this.nextStartTime = 0;
            console.log('Starting new audio playback session');
        }

        const audioData = this.audioQueue.shift();
        console.log('Scheduling audio chunk, size:', audioData.byteLength, 'queue remaining:', this.audioQueue.length);

        try {
            // 재생 전용 AudioContext가 없으면 생성 (브라우저 기본 샘플레이트 사용)
            if (!this.playbackAudioContext) {
                this.playbackAudioContext = new (window.AudioContext || window.webkitAudioContext)();
                console.log('Created playback AudioContext with sample rate:', this.playbackAudioContext.sampleRate);
            }

            // AudioContext 상태 확인
            if (this.playbackAudioContext.state === 'suspended') {
                console.log('AudioContext suspended, resuming...');
                await this.playbackAudioContext.resume();
            }
            console.log('AudioContext state:', this.playbackAudioContext.state);

            // PCM Int16을 Float32로 변환
            const int16Array = new Int16Array(audioData);
            const float32Array = new Float32Array(int16Array.length);
            for (let i = 0; i < int16Array.length; i++) {
                float32Array[i] = int16Array[i] / (int16Array[i] < 0 ? 0x8000 : 0x7FFF);
            }

            console.log('Converted to Float32, length:', float32Array.length);

            // 24kHz 오디오를 브라우저 샘플레이트로 리샘플링
            const sourceSampleRate = 24000;
            const targetSampleRate = this.playbackAudioContext.sampleRate;
            const resampledData = this.resampleAudio(float32Array, sourceSampleRate, targetSampleRate);

            console.log('Resampled from', sourceSampleRate, 'to', targetSampleRate, ', new length:', resampledData.length);

            // AudioBuffer 생성 (브라우저의 실제 샘플레이트 사용)
            const audioBuffer = this.playbackAudioContext.createBuffer(1, resampledData.length, targetSampleRate);
            audioBuffer.getChannelData(0).set(resampledData);

            console.log('AudioBuffer created, duration:', audioBuffer.duration.toFixed(3), 'seconds');

            // 재생 소스 생성 및 연결
            const source = this.playbackAudioContext.createBufferSource();
            source.buffer = audioBuffer;

            // GainNode를 통해 볼륨 조절 가능하도록
            const gainNode = this.playbackAudioContext.createGain();
            gainNode.gain.value = 1.0; // 볼륨 1.0 (최대)

            source.connect(gainNode);
            gainNode.connect(this.playbackAudioContext.destination);

            // 현재 시간과 다음 시작 시간 계산
            const currentTime = this.playbackAudioContext.currentTime;

            // nextStartTime이 0이거나 현재 시간보다 이전이면 현재 시간으로 리셋
            if (this.nextStartTime === 0 || this.nextStartTime < currentTime) {
                this.nextStartTime = currentTime;
                console.log('Reset start time to current time:', currentTime.toFixed(3));
            }

            // 스케줄링된 시간에 재생 시작
            console.log('Calling source.start at:', this.nextStartTime.toFixed(3), 'current:', currentTime.toFixed(3), 'delay:', (this.nextStartTime - currentTime).toFixed(3));
            source.start(this.nextStartTime);
            console.log('source.start() called successfully');

            // 다음 청크의 시작 시간 설정 (현재 청크가 끝나는 시간)
            this.nextStartTime += audioBuffer.duration;

            this.audioSources.push(source);
            console.log('Added to audioSources, total sources:', this.audioSources.length);

            source.onended = () => {
                console.log('Audio chunk ended');
                // 재생 완료된 소스 제거
                const index = this.audioSources.indexOf(source);
                if (index > -1) {
                    this.audioSources.splice(index, 1);
                }

                // 큐가 비어있고 모든 소스 재생 완료 시
                if (this.audioQueue.length === 0 && this.audioSources.length === 0) {
                    this.isPlayingAudio = false;
                    this.nextStartTime = 0;
                    this.aiStatus.textContent = '대기 중';
                    console.log('All audio playback completed');
                }
            };

            // 다음 청크 즉시 처리
            if (this.audioQueue.length > 0) {
                console.log('Processing next chunk immediately');
                setTimeout(() => this.playAudioQueue(), 0);
            }

        } catch (error) {
            console.error('Error scheduling audio:', error);
            console.error('Error stack:', error.stack);
            // 에러 발생 시 다음 청크 시도
            if (this.audioQueue.length > 0) {
                setTimeout(() => this.playAudioQueue(), 0);
            } else {
                this.isPlayingAudio = false;
                this.nextStartTime = 0;
                this.aiStatus.textContent = '대기 중';
            }
        }
    }

    resampleAudio(sourceBuffer, sourceSampleRate, targetSampleRate) {
        if (sourceSampleRate === targetSampleRate) {
            return sourceBuffer;
        }

        const ratio = sourceSampleRate / targetSampleRate;
        const newLength = Math.round(sourceBuffer.length / ratio);
        const result = new Float32Array(newLength);

        for (let i = 0; i < newLength; i++) {
            const srcIndex = i * ratio;
            const srcIndexInt = Math.floor(srcIndex);
            const fraction = srcIndex - srcIndexInt;

            // 선형 보간
            if (srcIndexInt + 1 < sourceBuffer.length) {
                result[i] = sourceBuffer[srcIndexInt] * (1 - fraction) + sourceBuffer[srcIndexInt + 1] * fraction;
            } else {
                result[i] = sourceBuffer[srcIndexInt];
            }
        }

        return result;
    }

    onTranscript(payload) {
        console.log('Transcript:', payload.role, payload.text);

        // 첫 번째 메시지인 경우 환영 메시지 제거
        if (!this.currentMessageElement) {
            const welcomeMsg = this.chatMessages.querySelector('.welcome-message');
            if (welcomeMsg) {
                welcomeMsg.remove();
            }
        }

        // 같은 role의 메시지가 연속되는 경우, 이전 메시지에 추가
        if (this.currentMessageRole === payload.role && this.currentMessageElement) {
            const contentDiv = this.currentMessageElement.querySelector('.message-content');
            // 줄바꿈 대신 공백으로 구분하여 자연스럽게 이어지도록
            contentDiv.textContent += payload.text;

            // 스크롤을 아래로
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        } else {
            // 새로운 메시지인 경우, 새로 생성
            this.addMessage(payload.role, payload.text);
        }

        // 현재 메시지 정보 업데이트
        this.currentMessageRole = payload.role;
    }

    onInterrupted() {
        console.log('Interrupted');

        // 현재 재생 중인 모든 오디오 소스 중단
        for (const source of this.audioSources) {
            try {
                source.stop();
            } catch (e) {
                console.log('Audio source already stopped');
            }
        }
        this.audioSources = [];
        this.currentAudioSource = null;

        // 오디오 큐 비우기
        this.audioQueue = [];
        this.isPlayingAudio = false;
        this.nextStartTime = 0;
        this.aiStatus.textContent = '중단됨';

        // 재생 컨텍스트는 유지 (다음 재생을 위해)
    }

    addMessage(role, text) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role.toLowerCase()}`;

        const roleLabel = document.createElement('div');
        roleLabel.className = 'message-role';
        roleLabel.textContent = role === 'USER' ? '사용자' : 'AI';

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        contentDiv.textContent = text;

        messageDiv.appendChild(roleLabel);
        messageDiv.appendChild(contentDiv);

        this.chatMessages.appendChild(messageDiv);

        // 스크롤을 아래로
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;

        // 현재 메시지 요소 업데이트
        this.currentMessageElement = messageDiv;
    }

    onWebSocketError(error) {
        console.error('WebSocket error:', error);
        this.updateStatus('연결 오류', false);
    }

    onWebSocketClose() {
        console.log('WebSocket closed');
        this.disconnect();
    }

    disconnect() {
        console.log('Disconnecting...');

        // WebSocket 닫기
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }

        // 마이크 중지
        if (this.mediaStream) {
            this.mediaStream.getTracks().forEach(track => track.stop());
            this.mediaStream = null;
        }

        // 마이크 AudioWorklet 정리
        if (this.audioWorkletNode) {
            this.audioWorkletNode.disconnect();
            this.audioWorkletNode = null;
        }

        // 마이크 AudioContext 정리
        if (this.micAudioContext) {
            this.micAudioContext.close();
            this.micAudioContext = null;
        }

        // 현재 재생 중인 모든 오디오 소스 중단
        for (const source of this.audioSources) {
            try {
                source.stop();
            } catch (e) {
                console.log('Audio source already stopped');
            }
        }
        this.audioSources = [];
        this.currentAudioSource = null;

        // 재생 전용 AudioContext 정리
        if (this.playbackAudioContext) {
            this.playbackAudioContext.close();
            this.playbackAudioContext = null;
        }

        // 상태 초기화
        this.isConnected = false;
        this.sessionId = null;
        this.audioQueue = [];
        this.isPlayingAudio = false;
        this.nextStartTime = 0;

        // UI 업데이트
        this.updateStatus('연결 안됨', false);
        this.connectBtn.disabled = false;
        this.disconnectBtn.disabled = true;
        this.sessionIdElement.textContent = '-';
        this.micStatus.textContent = '대기 중';
        this.aiStatus.textContent = '대기 중';
    }

    updateStatus(text, isConnected) {
        this.statusText.textContent = text;
        if (isConnected) {
            this.statusIndicator.classList.add('connected');
        } else {
            this.statusIndicator.classList.remove('connected');
        }
    }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    const client = new VoiceChatClient();
    console.log('Voice Chat Client initialized');
});
