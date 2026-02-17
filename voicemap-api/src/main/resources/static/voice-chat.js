const STORAGE_KEYS = {
    accessToken: "voicemap.accessToken",
    refreshToken: "voicemap.refreshToken",
    pendingGoogleIdToken: "voicemap.pendingGoogleIdToken"
};

const GOOGLE_CLIENT_ID = "449433791049-276ln0ta9r768dddnjftjq1qleerq231.apps.googleusercontent.com";
const GIS_REDIRECT_URI = "http://localhost:8080/oauth/callback";
const APP_PAGE_PATH = "/voice-chat.html";

class VoiceMapApp {
    constructor() {
        this.ws = null;
        this.manualSocketClose = false;
        this.sessionReadyResolve = null;
        this.sessionReadyReject = null;
        this.sessionReadyTimeoutId = null;

        this.sessionId = null;
        this.isConnecting = false;
        this.isRecording = false;

        this.accessToken = "";
        this.refreshToken = "";
        this.activeChatId = null;
        this.chats = [];
        this.knownChatIdsBeforeSession = new Set();

        this.micAudioContext = null;
        this.playbackAudioContext = null;
        this.mediaStream = null;
        this.audioWorkletNode = null;
        this.audioWorkletModuleUrl = null;
        this.audioQueue = [];
        this.audioSources = [];
        this.isPlayingAudio = false;
        this.nextStartTime = 0;

        this.liveMessageRole = null;
        this.liveMessageElement = null;

        this.authView = document.getElementById("authView");
        this.appView = document.getElementById("appView");
        this.authStatus = document.getElementById("authStatus");
        this.googleSignInButton = document.getElementById("googleSignInButton");

        this.memberInfo = document.getElementById("memberInfo");
        this.newSessionBtn = document.getElementById("newSessionBtn");
        this.logoutBtn = document.getElementById("logoutBtn");
        this.chatList = document.getElementById("chatList");
        this.chatTitle = document.getElementById("chatTitle");
        this.connectionState = document.getElementById("connectionState");
        this.sessionIdBadge = document.getElementById("sessionIdBadge");
        this.micStatusBadge = document.getElementById("micStatusBadge");
        this.aiStatusBadge = document.getElementById("aiStatusBadge");
        this.transcriptPanel = document.getElementById("transcriptPanel");
        this.recordBtn = document.getElementById("recordBtn");

        this.bindEvents();
    }

    bindEvents() {
        this.newSessionBtn.addEventListener("click", () => this.prepareNewSession());
        this.logoutBtn.addEventListener("click", () => this.logout());
        this.recordBtn.addEventListener("click", () => this.toggleRecording());
    }

    async init() {
        const tokens = this.loadTokens();
        this.accessToken = tokens.accessToken;
        this.refreshToken = tokens.refreshToken;

        if (this.isCallbackPath()) {
            await this.handleCallbackLogin();
            return;
        }

        if (this.accessToken && this.refreshToken) {
            await this.enterMainView();
            return;
        }

        this.enterAuthView("Google 로그인을 진행해주세요.");
    }

    isCallbackPath() {
        return window.location.pathname === "/oauth/callback" || window.location.pathname === "/oauth/callback/";
    }

    loadTokens() {
        return {
            accessToken: localStorage.getItem(STORAGE_KEYS.accessToken) ?? "",
            refreshToken: localStorage.getItem(STORAGE_KEYS.refreshToken) ?? ""
        };
    }

    saveTokens(accessToken, refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
        localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
    }

    clearTokens() {
        this.accessToken = "";
        this.refreshToken = "";
        localStorage.removeItem(STORAGE_KEYS.accessToken);
        localStorage.removeItem(STORAGE_KEYS.refreshToken);
    }

    enterAuthView(statusMessage) {
        this.authView.hidden = false;
        this.appView.hidden = true;
        this.setAuthStatus(statusMessage ?? "");
        this.renderGoogleSignInButton();
    }

    async enterMainView() {
        this.authView.hidden = true;
        this.appView.hidden = false;
        this.updateConnectionState("연결 안됨");
        this.updateSessionBadges();

        const memberNumber = this.extractMemberNumber(this.accessToken);
        this.memberInfo.textContent = memberNumber ? `member: ${memberNumber}` : "member: -";

        try {
            await this.loadChatList();
            if (this.chats.length > 0) {
                await this.selectChat(this.chats[0].chatId);
            } else {
                this.prepareNewSession(true);
            }
        } catch (error) {
            console.error("[VoiceMapApp] failed to load chat list", error);
            this.renderEmptyTranscript("채팅 목록을 불러오지 못했습니다. 다시 시도해주세요.");
        }
    }

    setAuthStatus(message, isError = false) {
        this.authStatus.textContent = message ?? "";
        this.authStatus.style.color = isError ? "#be3f31" : "";
    }

    renderGoogleSignInButton() {
        if (!window.google || !window.google.accounts || !window.google.accounts.id) {
            this.setAuthStatus("Google 스크립트를 불러오는 중입니다.");
            window.setTimeout(() => this.renderGoogleSignInButton(), 300);
            return;
        }

        this.googleSignInButton.innerHTML = "";

        window.google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: (response) => this.onGoogleCredential(response)
        });

        window.google.accounts.id.renderButton(this.googleSignInButton, {
            theme: "outline",
            size: "large",
            shape: "pill",
            text: "signin_with",
            width: 300
        });

        this.setAuthStatus("Google 로그인 버튼 준비 완료.");
    }

    onGoogleCredential(response) {
        if (!response || !response.credential) {
            this.setAuthStatus("Google 인증 토큰을 받지 못했습니다.", true);
            return;
        }

        sessionStorage.setItem(STORAGE_KEYS.pendingGoogleIdToken, response.credential);
        this.setAuthStatus("인증 완료. 콜백 페이지로 이동합니다.");
        window.location.assign(GIS_REDIRECT_URI);
    }

    async handleCallbackLogin() {
        this.enterAuthView("로그인 처리 중...");

        const pendingToken = sessionStorage.getItem(STORAGE_KEYS.pendingGoogleIdToken) || "";
        if (!pendingToken) {
            this.setAuthStatus("콜백 토큰이 존재하지 않습니다. 다시 로그인해주세요.", true);
            window.history.replaceState({}, "", APP_PAGE_PATH);
            return;
        }

        try {
            await this.loginOrRegisterWithGoogleToken(pendingToken);
            sessionStorage.removeItem(STORAGE_KEYS.pendingGoogleIdToken);
            window.history.replaceState({}, "", APP_PAGE_PATH);
            await this.enterMainView();
        } catch (error) {
            console.error("[VoiceMapApp] callback login failed", error);
            this.setAuthStatus(`로그인 실패: ${error.message}`, true);
        }
    }

    async loginOrRegisterWithGoogleToken(googleIdToken) {
        const loginResponse = await this.fetchJson("/auth/login", {
            method: "POST",
            auth: false,
            body: {
                provider: "GOOGLE",
                providerToken: googleIdToken
            }
        });

        if (loginResponse.status === 200) {
            this.saveTokens(loginResponse.json.accessToken, loginResponse.json.refreshToken);
            return;
        }

        if (loginResponse.status !== 404) {
            throw new Error(`/auth/login 실패 (${loginResponse.status})`);
        }

        const email = this.extractEmailFromGoogleIdToken(googleIdToken);
        if (!email) {
            throw new Error("Google ID 토큰에서 이메일을 추출할 수 없습니다.");
        }

        const registerResponse = await this.fetchJson("/v1/register", {
            method: "POST",
            auth: false,
            body: {
                provider: "GOOGLE",
                providerToken: googleIdToken,
                email
            }
        });

        if (registerResponse.status !== 200) {
            throw new Error(`/v1/register 실패 (${registerResponse.status})`);
        }

        this.saveTokens(registerResponse.json.accessToken, registerResponse.json.refreshToken);
    }

    extractEmailFromGoogleIdToken(googleIdToken) {
        try {
            const payload = this.decodeJwtPayload(googleIdToken);
            return payload.email ?? "";
        } catch (error) {
            console.error("[VoiceMapApp] failed to decode google id token", error);
            return "";
        }
    }

    extractMemberNumber(accessToken) {
        try {
            const payload = this.decodeJwtPayload(accessToken);
            return payload.memberNumber ?? "";
        } catch (error) {
            return "";
        }
    }

    decodeJwtPayload(token) {
        const tokenParts = token.split(".");
        if (tokenParts.length < 2) {
            throw new Error("Invalid JWT token");
        }
        const base64 = tokenParts[1].replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
        const jsonString = atob(padded);
        return JSON.parse(jsonString);
    }

    async loadChatList() {
        const response = await this.fetchJson("/chats");
        if (response.status !== 200) {
            throw new Error(`채팅 목록 조회 실패 (${response.status})`);
        }
        this.chats = Array.isArray(response.json.chats) ? response.json.chats : [];
        this.renderChatList();
    }

    renderChatList() {
        this.chatList.innerHTML = "";

        if (this.chats.length === 0) {
            const emptyItem = document.createElement("li");
            emptyItem.className = "chat-item-empty";
            emptyItem.textContent = "아직 채팅이 없습니다.";
            this.chatList.appendChild(emptyItem);
            return;
        }

        this.chats.forEach((chat) => {
            const item = document.createElement("li");
            const button = document.createElement("button");
            button.type = "button";
            button.className = "chat-item";
            if (chat.chatId === this.activeChatId) {
                button.classList.add("is-active");
            }
            button.textContent = chat.title || "제목 없음";
            button.addEventListener("click", async () => {
                await this.selectChat(chat.chatId);
            });
            item.appendChild(button);
            this.chatList.appendChild(item);
        });
    }

    async selectChat(chatId) {
        this.activeChatId = chatId;
        this.renderChatList();
        this.chatTitle.textContent = this.chats.find((chat) => chat.chatId === chatId)?.title || "채팅 상세";
        await this.loadChatDetails(chatId);
    }

    async loadChatDetails(chatId) {
        const response = await this.fetchJson(`/chats/${encodeURIComponent(chatId)}`);
        if (response.status !== 200) {
            this.renderEmptyTranscript("채팅 상세를 불러오지 못했습니다.");
            return;
        }
        const scripts = Array.isArray(response.json.scripts) ? response.json.scripts : [];
        this.renderScriptHistory(scripts);
    }

    renderScriptHistory(scripts) {
        this.transcriptPanel.innerHTML = "";
        this.liveMessageRole = null;
        this.liveMessageElement = null;

        if (scripts.length === 0) {
            this.renderEmptyTranscript("아직 스크립트가 없습니다. 녹음을 시작해보세요.");
            return;
        }

        scripts.forEach((script) => {
            this.appendMessage("USER", script.question ?? "");
            if (script.answer) {
                this.appendMessage("AGENT", script.answer);
            }
        });
        this.scrollTranscriptToBottom();
    }

    renderEmptyTranscript(text) {
        this.transcriptPanel.innerHTML = "";
        const empty = document.createElement("div");
        empty.className = "empty-state";
        empty.innerHTML = `<p>${text}</p>`;
        this.transcriptPanel.appendChild(empty);
    }

    prepareNewSession(skipRender = false) {
        this.activeChatId = null;
        this.chatTitle.textContent = "새 대화 준비 중";
        this.renderChatList();
        if (!skipRender) {
            this.renderEmptyTranscript("녹음 시작 버튼을 누르면 새 음성 세션이 시작됩니다.");
        }
    }

    async toggleRecording() {
        if (this.isConnecting) {
            return;
        }
        if (this.isRecording) {
            await this.stopVoiceSession();
            return;
        }
        await this.startVoiceSession();
    }

    async startVoiceSession() {
        if (!this.accessToken) {
            this.enterAuthView("로그인이 필요합니다.");
            return;
        }

        this.isConnecting = true;
        this.recordBtn.textContent = "연결 중...";
        this.recordBtn.disabled = true;
        this.updateConnectionState("WebSocket 연결 중...");
        this.knownChatIdsBeforeSession = new Set(this.chats.map((chat) => chat.chatId));

        try {
            await this.openSocketAndInitializeSession();
            await this.startMicrophone();
            this.isRecording = true;
            this.updateConnectionState("연결됨");
            this.recordBtn.textContent = "녹음 중지";
            this.recordBtn.disabled = false;
        } catch (error) {
            console.error("[VoiceMapApp] start voice session failed", error);
            await this.stopVoiceSession();
            this.updateConnectionState(`연결 실패: ${error.message}`);
        } finally {
            this.isConnecting = false;
            if (!this.isRecording) {
                this.recordBtn.textContent = "녹음 시작";
                this.recordBtn.disabled = false;
            }
        }
    }

    async stopVoiceSession() {
        await this.stopMicrophone();
        await this.stopPlayback();
        this.closeSocket();
        this.isRecording = false;
        this.sessionId = null;
        this.updateConnectionState("연결 안됨");
        this.updateSessionBadges();
        this.recordBtn.textContent = "녹음 시작";
        this.recordBtn.disabled = false;
    }

    openSocketAndInitializeSession() {
        this.closeSocket();

        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        const wsUrl = `${protocol}//${window.location.host}/ws/chat`;
        const socket = new WebSocket(wsUrl);
        this.ws = socket;

        return new Promise((resolve, reject) => {
            this.sessionReadyResolve = resolve;
            this.sessionReadyReject = reject;
            this.sessionReadyTimeoutId = window.setTimeout(() => {
                this.rejectSessionReady(new Error("SESSION_READY 응답 시간 초과"));
            }, 12000);

            socket.onopen = () => {
                if (this.ws !== socket) {
                    return;
                }
                const initMessage = {
                    type: "SESSION_INIT",
                    payload: {
                        token: this.accessToken,
                        chatId: this.activeChatId
                    }
                };
                socket.send(JSON.stringify(initMessage));
            };

            socket.onmessage = (event) => {
                if (this.ws !== socket) {
                    return;
                }
                this.handleSocketMessage(event.data);
            };

            socket.onerror = () => {
                if (this.ws !== socket) {
                    return;
                }
                this.rejectSessionReady(new Error("WebSocket 오류"));
            };

            socket.onclose = () => {
                if (this.ws !== socket) {
                    return;
                }
                this.ws = null;
                const wasManual = this.manualSocketClose;
                this.manualSocketClose = false;
                if (!wasManual) {
                    this.rejectSessionReady(new Error("WebSocket 연결이 종료되었습니다."));
                    if (this.isRecording || this.isConnecting) {
                        this.stopVoiceSession().catch((error) => console.error(error));
                    }
                }
            };
        });
    }

    rejectSessionReady(error) {
        if (this.sessionReadyTimeoutId) {
            window.clearTimeout(this.sessionReadyTimeoutId);
            this.sessionReadyTimeoutId = null;
        }
        if (this.sessionReadyReject) {
            this.sessionReadyReject(error);
            this.sessionReadyResolve = null;
            this.sessionReadyReject = null;
        }
    }

    resolveSessionReady() {
        if (this.sessionReadyTimeoutId) {
            window.clearTimeout(this.sessionReadyTimeoutId);
            this.sessionReadyTimeoutId = null;
        }
        if (this.sessionReadyResolve) {
            this.sessionReadyResolve();
            this.sessionReadyResolve = null;
            this.sessionReadyReject = null;
        }
    }

    closeSocket() {
        if (!this.ws) {
            return;
        }
        const socket = this.ws;
        this.manualSocketClose = true;
        this.ws = null;
        try {
            socket.close();
        } catch (error) {
            console.error("[VoiceMapApp] failed to close socket", error);
        }
        this.rejectSessionReady(new Error("WebSocket closed"));
    }

    handleSocketMessage(rawData) {
        let message;
        try {
            message = JSON.parse(rawData);
        } catch (error) {
            console.error("[VoiceMapApp] invalid ws payload", rawData);
            return;
        }

        const messageType = message.type;
        const payload = message.payload || {};

        switch (messageType) {
            case "SESSION_READY":
                this.sessionId = payload.sessionId || null;
                this.updateSessionBadges();
                this.resolveSessionReady();
                break;
            case "AUDIO_OUTPUT":
                this.onAudioOutput(payload).catch((error) => console.error(error));
                break;
            case "TRANSCRIPT":
                this.onTranscript(payload);
                break;
            case "INTERRUPTED":
                this.onInterrupted();
                break;
            case "TURN_COMPLETED":
                this.onTurnCompleted().catch((error) => console.error(error));
                break;
            default:
                console.warn("[VoiceMapApp] unsupported message type", messageType);
        }
    }

    onTranscript(payload) {
        const role = payload.role === "USER" ? "USER" : "AGENT";
        const text = typeof payload.text === "string" ? payload.text : "";
        if (!text) {
            return;
        }

        if (this.transcriptPanel.querySelector(".empty-state")) {
            this.transcriptPanel.innerHTML = "";
        }

        if (this.liveMessageRole === role && this.liveMessageElement) {
            const content = this.liveMessageElement.querySelector(".message-content");
            content.textContent += text;
        } else {
            this.liveMessageElement = this.appendMessage(role, text);
            this.liveMessageRole = role;
        }
        this.scrollTranscriptToBottom();
    }

    appendMessage(role, text) {
        const message = document.createElement("article");
        message.className = `message ${role === "USER" ? "message-user" : "message-agent"}`;

        const roleLabel = document.createElement("div");
        roleLabel.className = "message-role";
        roleLabel.textContent = role === "USER" ? "사용자" : "AI";

        const content = document.createElement("div");
        content.className = "message-content";
        content.textContent = text;

        message.appendChild(roleLabel);
        message.appendChild(content);
        this.transcriptPanel.appendChild(message);
        return message;
    }

    async onTurnCompleted() {
        this.aiStatusBadge.textContent = "ai: idle";
        this.liveMessageRole = null;
        this.liveMessageElement = null;

        await this.loadChatList();

        if (!this.activeChatId) {
            const createdChat = this.chats.find((chat) => !this.knownChatIdsBeforeSession.has(chat.chatId));
            if (createdChat) {
                await this.selectChat(createdChat.chatId);
                return;
            }
            if (this.chats.length > 0) {
                await this.selectChat(this.chats[0].chatId);
            }
            return;
        }
        await this.loadChatDetails(this.activeChatId);
    }

    onInterrupted() {
        this.audioQueue = [];
        this.audioSources.forEach((source) => {
            try {
                source.stop();
            } catch (error) {
                // no-op
            }
        });
        this.audioSources = [];
        this.isPlayingAudio = false;
        this.nextStartTime = 0;
        this.aiStatusBadge.textContent = "ai: interrupted";
    }

    async startMicrophone() {
        this.micStatusBadge.textContent = "mic: requesting";
        this.mediaStream = await navigator.mediaDevices.getUserMedia({
            audio: {
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true,
                channelCount: 1
            }
        });

        this.micAudioContext = new (window.AudioContext || window.webkitAudioContext)();
        const source = this.micAudioContext.createMediaStreamSource(this.mediaStream);
        const sourceSampleRate = this.micAudioContext.sampleRate;
        const downsampleRatio = Math.max(1, Math.round(sourceSampleRate / 16000));

        this.audioWorkletModuleUrl = this.createAudioProcessorBlobUrl();
        await this.micAudioContext.audioWorklet.addModule(this.audioWorkletModuleUrl);

        this.audioWorkletNode = new AudioWorkletNode(this.micAudioContext, "voicemap-audio-processor", {
            processorOptions: {
                sourceSampleRate,
                targetSampleRate: 16000,
                downsampleRatio
            }
        });

        this.audioWorkletNode.port.onmessage = (event) => {
            if (event.data.type !== "audio") {
                return;
            }
            this.sendAudioChunk(event.data.audio);
        };

        source.connect(this.audioWorkletNode);
        this.micStatusBadge.textContent = "mic: recording";
    }

    async stopMicrophone() {
        if (this.audioWorkletNode) {
            try {
                this.audioWorkletNode.disconnect();
            } catch (error) {
                // no-op
            }
            this.audioWorkletNode = null;
        }

        if (this.mediaStream) {
            this.mediaStream.getTracks().forEach((track) => track.stop());
            this.mediaStream = null;
        }

        if (this.micAudioContext) {
            await this.micAudioContext.close();
            this.micAudioContext = null;
        }

        if (this.audioWorkletModuleUrl) {
            URL.revokeObjectURL(this.audioWorkletModuleUrl);
            this.audioWorkletModuleUrl = null;
        }

        this.micStatusBadge.textContent = "mic: idle";
    }

    createAudioProcessorBlobUrl() {
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
                    if (!input || input.length === 0) {
                        return true;
                    }
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
        return URL.createObjectURL(new Blob([script], { type: "application/javascript" }));
    }

    sendAudioChunk(float32Array) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.sessionId) {
            return;
        }
        const pcm = this.floatToPcm16(float32Array);
        const base64 = this.arrayBufferToBase64(pcm.buffer);
        const message = {
            type: "AUDIO_INPUT",
            payload: {
                data: base64
            }
        };
        this.ws.send(JSON.stringify(message));
    }

    floatToPcm16(float32Array) {
        const output = new Int16Array(float32Array.length);
        for (let i = 0; i < float32Array.length; i++) {
            const value = Math.max(-1, Math.min(1, float32Array[i]));
            output[i] = value < 0 ? value * 0x8000 : value * 0x7fff;
        }
        return output;
    }

    arrayBufferToBase64(buffer) {
        let binary = "";
        const bytes = new Uint8Array(buffer);
        for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }

    base64ToArrayBuffer(base64) {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    }

    async onAudioOutput(payload) {
        if (!payload || !payload.base64Audio) {
            return;
        }
        this.aiStatusBadge.textContent = "ai: speaking";
        this.audioQueue.push(this.base64ToArrayBuffer(payload.base64Audio));
        // 청크 도착 타이밍이 느린 경우, isPlayingAudio는 true인데 실제 소스가 없을 수 있다.
        if (!this.isPlayingAudio || this.audioSources.length === 0) {
            await this.playAudioQueue();
        }
    }

    async playAudioQueue() {
        if (this.audioQueue.length === 0) {
            this.isPlayingAudio = false;
            this.nextStartTime = 0;
            return;
        }

        this.isPlayingAudio = true;

        if (!this.playbackAudioContext) {
            this.playbackAudioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (this.playbackAudioContext.state === "suspended") {
            await this.playbackAudioContext.resume();
        }

        const audioData = this.audioQueue.shift();
        const int16Array = new Int16Array(audioData);
        const float32Array = new Float32Array(int16Array.length);
        for (let i = 0; i < int16Array.length; i++) {
            float32Array[i] = int16Array[i] / (int16Array[i] < 0 ? 0x8000 : 0x7fff);
        }

        const sourceSampleRate = 24000;
        const targetSampleRate = this.playbackAudioContext.sampleRate;
        const pcm = this.resample(float32Array, sourceSampleRate, targetSampleRate);
        const audioBuffer = this.playbackAudioContext.createBuffer(1, pcm.length, targetSampleRate);
        audioBuffer.getChannelData(0).set(pcm);

        const source = this.playbackAudioContext.createBufferSource();
        const gain = this.playbackAudioContext.createGain();
        gain.gain.value = 1.0;
        source.buffer = audioBuffer;
        source.connect(gain);
        gain.connect(this.playbackAudioContext.destination);

        const now = this.playbackAudioContext.currentTime;
        if (this.nextStartTime < now) {
            this.nextStartTime = now;
        }
        source.start(this.nextStartTime);
        this.nextStartTime += audioBuffer.duration;
        this.audioSources.push(source);

        source.onended = () => {
            this.audioSources = this.audioSources.filter((audioSource) => audioSource !== source);
            if (this.audioQueue.length > 0) {
                window.setTimeout(() => this.playAudioQueue().catch((error) => console.error(error)), 0);
                return;
            }
            if (this.audioQueue.length === 0 && this.audioSources.length === 0) {
                this.aiStatusBadge.textContent = "ai: idle";
                this.isPlayingAudio = false;
                this.nextStartTime = 0;
            }
        };

        if (this.audioQueue.length > 0) {
            window.setTimeout(() => this.playAudioQueue().catch((error) => console.error(error)), 0);
        }
    }

    resample(source, sourceRate, targetRate) {
        if (sourceRate === targetRate) {
            return source;
        }
        const ratio = sourceRate / targetRate;
        const outputLength = Math.round(source.length / ratio);
        const output = new Float32Array(outputLength);
        for (let i = 0; i < outputLength; i++) {
            const srcIndex = i * ratio;
            const idx = Math.floor(srcIndex);
            const frac = srcIndex - idx;
            if (idx + 1 < source.length) {
                output[i] = source[idx] * (1 - frac) + source[idx + 1] * frac;
            } else {
                output[i] = source[idx];
            }
        }
        return output;
    }

    async stopPlayback() {
        this.audioQueue = [];
        this.audioSources.forEach((source) => {
            try {
                source.stop();
            } catch (error) {
                // no-op
            }
        });
        this.audioSources = [];
        this.isPlayingAudio = false;
        this.nextStartTime = 0;

        if (this.playbackAudioContext) {
            await this.playbackAudioContext.close();
            this.playbackAudioContext = null;
        }
        this.aiStatusBadge.textContent = "ai: idle";
    }

    updateConnectionState(text) {
        this.connectionState.textContent = text;
    }

    updateSessionBadges() {
        this.sessionIdBadge.textContent = `session: ${this.sessionId || "-"}`;
        if (!this.isRecording && !this.isConnecting) {
            this.micStatusBadge.textContent = "mic: idle";
            this.aiStatusBadge.textContent = "ai: idle";
        }
    }

    scrollTranscriptToBottom() {
        this.transcriptPanel.scrollTop = this.transcriptPanel.scrollHeight;
    }

    async logout() {
        try {
            if (this.refreshToken) {
                await this.fetchJson("/auth/logout", {
                    method: "POST",
                    auth: false,
                    body: { refreshToken: this.refreshToken }
                });
            }
        } catch (error) {
            console.error("[VoiceMapApp] logout request failed", error);
        } finally {
            await this.stopVoiceSession();
            this.clearTokens();
            this.chats = [];
            this.activeChatId = null;
            this.chatList.innerHTML = "";
            this.renderEmptyTranscript("로그아웃되었습니다.");
            this.enterAuthView("Google 로그인을 다시 진행해주세요.");
        }
    }

    async fetchJson(path, options = {}) {
        const response = await this.request(path, options);
        let json = null;
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            json = await response.json();
        }
        return { status: response.status, json };
    }

    async request(path, options = {}) {
        const {
            method = "GET",
            body = null,
            auth = true,
            retryOnUnauthorized = true
        } = options;

        const headers = {};
        if (body !== null) {
            headers["Content-Type"] = "application/json";
        }
        if (auth) {
            headers.Authorization = `Bearer ${this.accessToken}`;
        }

        const response = await fetch(path, {
            method,
            headers,
            body: body !== null ? JSON.stringify(body) : undefined
        });

        if (response.status === 401 && auth && retryOnUnauthorized) {
            const refreshed = await this.rotateAccessToken();
            if (refreshed) {
                return this.request(path, { ...options, retryOnUnauthorized: false });
            }
        }
        return response;
    }

    async rotateAccessToken() {
        if (!this.refreshToken) {
            return false;
        }
        try {
            const response = await fetch("/auth/access", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ refreshToken: this.refreshToken })
            });
            if (response.status !== 200) {
                return false;
            }
            const body = await response.json();
            if (!body.accessToken) {
                return false;
            }
            this.saveTokens(body.accessToken, body.refreshToken || this.refreshToken);
            return true;
        } catch (error) {
            console.error("[VoiceMapApp] token refresh failed", error);
            return false;
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const app = new VoiceMapApp();
    app.init().catch((error) => {
        console.error("[VoiceMapApp] init failed", error);
    });
});
