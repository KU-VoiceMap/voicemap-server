import { useState, useCallback, useRef, useEffect } from 'react';
import { useAuth } from './auth/useAuth';
import AuthView from './auth/AuthView';
import ChatSidebar from './chat/ChatSidebar';
import TranscriptPanel from './chat/TranscriptPanel';
import RecorderFooter from './chat/RecorderFooter';
import DocumentList from './document/DocumentList';
import DocumentDetailView from './document/DocumentDetailView';
import DocumentDetailModal from './document/DocumentDetailModal';
import GraphView from './graph/GraphView';
import ToastContainer from './components/Toast';
import { createToast } from './components/toastUtils';
import type { ToastItem } from './components/toastUtils';
import { useVoiceSession } from './chat/useVoiceSession';
import type { WsTitleUpdatedPayload } from './api/websocket';
import { fetchJson } from './api/client';
import { getAccessToken } from './auth/tokenStorage';
import type {
  Chat,
  Message,
  DocumentSummary,
  DocumentDetail,
  GraphNode,
  GraphEdge,
  SidebarTab,
  MainView,
} from './types';
import { formatDate } from './utils/formatDate';

function normalizeKeywords(keywords: unknown): string[] {
  if (!Array.isArray(keywords)) return [];
  return keywords
    .map((kw) => {
      if (typeof kw === 'string') return kw.trim();
      if (kw && typeof kw === 'object') {
        const obj = kw as Record<string, unknown>;
        if (typeof obj.name === 'string') return obj.name.trim();
        if (typeof obj.keywordName === 'string') return obj.keywordName.trim();
        if (typeof obj.keyword === 'string') return obj.keyword.trim();
      }
      return '';
    })
    .filter(Boolean);
}

export default function App() {
  const auth = useAuth();
  const voiceSession = useVoiceSession();

  const [chats, setChats] = useState<Chat[]>([]);
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [emptyText, setEmptyText] = useState('대화를 선택하거나 새 대화를 시작하세요.');

  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [documentDetails, setDocumentDetails] = useState<Map<string, DocumentDetail>>(new Map());
  const [activeDocumentId, setActiveDocumentId] = useState<string | null>(null);
  const [documentDetailLoading, setDocumentDetailLoading] = useState(false);

  const [graphNodes, setGraphNodes] = useState<GraphNode[]>([]);
  const [graphEdges, setGraphEdges] = useState<GraphEdge[]>([]);
  const [graphLoading, setGraphLoading] = useState(false);
  const [graphError, setGraphError] = useState<string | null>(null);
  const [graphModalDocumentId, setGraphModalDocumentId] = useState<string | null>(null);
  const [graphModalLoading, setGraphModalLoading] = useState(false);

  const [activeSidebarTab, setActiveSidebarTab] = useState<SidebarTab>('chats');
  const [mainView, setMainView] = useState<MainView>('chat');

  const [isRecording, setIsRecording] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isCreatingDocument, setIsCreatingDocument] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [connectionState, setConnectionState] = useState('연결 안됨');
  const [micStatus, setMicStatus] = useState('idle');
  const [aiStatus, setAiStatus] = useState('idle');

  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const knownChatIdsRef = useRef<Set<string>>(new Set());
  const liveMessageRef = useRef<{ role: 'USER' | 'AGENT' } | null>(null);
  const messageIdRef = useRef(0);

  const showToast = useCallback((message: string, type: 'success' | 'error' = 'success') => {
    setToasts((prev) => [...prev, createToast(message, type)]);
  }, []);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const loadChatList = useCallback(async () => {
    const response = await fetchJson<{ chats: Chat[] }>('/chats');
    if (response.status === 200 && response.json) {
      const chatList = Array.isArray(response.json.chats) ? response.json.chats : [];
      setChats(chatList);
      return chatList;
    }
    throw new Error(`채팅 목록 조회 실패 (${response.status})`);
  }, []);

  const loadChatDetails = useCallback(async (chatId: string) => {
    const response = await fetchJson<{ scripts: { question: string; answer?: string }[] }>(
      `/chats/${encodeURIComponent(chatId)}`,
    );
    if (response.status === 200 && response.json) {
      const scripts = Array.isArray(response.json.scripts) ? response.json.scripts : [];
      const msgs: Message[] = [];
      scripts.forEach((s) => {
        msgs.push({ id: messageIdRef.current++, role: 'USER', text: s.question ?? '' });
        if (s.answer) msgs.push({ id: messageIdRef.current++, role: 'AGENT', text: s.answer });
      });
      setMessages(msgs);
    } else {
      setMessages([]);
      setEmptyText('채팅 상세를 불러오지 못했습니다.');
    }
  }, []);

  const loadDocuments = useCallback(async () => {
    const response = await fetchJson<{ documents: DocumentSummary[] }>('/documents');
    if (response.status !== 200) throw new Error(`문서 목록 조회 실패 (${response.status})`);
    const docs = (response.json?.documents ?? [])
      .filter((d: DocumentSummary) => d?.documentId)
      .map((d: DocumentSummary) => ({
        documentId: String(d.documentId),
        chatId: d.chatId ? String(d.chatId) : null,
        title: typeof d.title === 'string' ? d.title : '',
        summary: typeof d.summary === 'string' ? d.summary : '',
        createdAt: typeof d.createdAt === 'string' ? d.createdAt : '',
      }));
    setDocuments(docs);
    return docs;
  }, []);

  const fetchDocumentDetail = useCallback(async (documentId: string): Promise<DocumentDetail> => {
    const response = await fetchJson<Record<string, unknown>>(`/documents/${encodeURIComponent(documentId)}`);
    if (response.status !== 200) throw new Error(`문서 상세 조회 실패 (${response.status})`);
    const json = response.json ?? {};
    const detail: DocumentDetail = {
      documentId: typeof json.documentId === 'string' ? json.documentId : documentId,
      chatId: typeof json.chatId === 'string' ? json.chatId : null,
      title: typeof json.title === 'string' ? json.title : '',
      summary: typeof json.summary === 'string' ? json.summary : '',
      content: typeof json.content === 'string' ? json.content : '',
      keywords: normalizeKeywords(json.keywords),
      createdAt: typeof json.createdAt === 'string' ? json.createdAt : '',
    };
    setDocumentDetails((prev) => new Map(prev).set(detail.documentId, detail));
    return detail;
  }, []);

  const hydrateDocumentDetails = useCallback(async (docs: DocumentSummary[], current: Map<string, DocumentDetail>) => {
    const missing = docs
      .map((d) => d.documentId)
      .filter((id) => !current.has(id));
    if (missing.length === 0) return;
    await Promise.allSettled(missing.map((id) => fetchDocumentDetail(id)));
  }, [fetchDocumentDetail]);

  const selectChat = useCallback(async (chatId: string) => {
    setActiveChatId(chatId);
    setActiveDocumentId(null);
    if (activeSidebarTab === 'chats') {
      setMainView('chat');
    }
    await loadChatDetails(chatId);
  }, [activeSidebarTab, loadChatDetails]);

  const loadDocumentGraph = useCallback(async () => {
    setGraphLoading(true);
    setGraphError(null);
    try {
      const response = await fetchJson<{ nodes: GraphNode[]; edges: GraphEdge[] }>('/documents/graph');
      if (response.status !== 200) throw new Error(`문서 그래프 조회 실패 (${response.status})`);
      setGraphNodes(response.json?.nodes ?? []);
      setGraphEdges(response.json?.edges ?? []);
    } catch (error) {
      setGraphError('문서 그래프를 불러오지 못했습니다.');
      console.error('[app] graph load failed', error);
    } finally {
      setGraphLoading(false);
    }
  }, []);

  const switchTab = useCallback(async (tab: SidebarTab) => {
    if ((isRecording || isConnecting) && tab !== 'chats') {
      showToast('녹음 중에는 채팅 탭에서만 이동할 수 있습니다.', 'error');
      return;
    }

    setActiveSidebarTab(tab);

    if (tab === 'chats') {
      setMainView('chat');
      return;
    }
    if (tab === 'documents') {
      setMainView('documentList');
      try {
        const docs = await loadDocuments();
        hydrateDocumentDetails(docs, documentDetails).catch(console.error);
      } catch {
        showToast('문서 목록을 불러오지 못했습니다.', 'error');
      }
      return;
    }
    setMainView('graph');
    await loadDocumentGraph();
  }, [isRecording, isConnecting, showToast, loadDocuments, hydrateDocumentDetails, documentDetails, loadDocumentGraph]);

  const handleSelectDocument = useCallback(async (documentId: string) => {
    setActiveDocumentId(documentId);
    setDocumentDetailLoading(true);
    setMainView('documentDetail');
    try {
      await fetchDocumentDetail(documentId);
    } catch {
      showToast('문서 상세를 불러오지 못했습니다.', 'error');
    } finally {
      setDocumentDetailLoading(false);
    }
  }, [fetchDocumentDetail, showToast]);

  const handleGraphDocumentSelect = useCallback(async (documentId: string) => {
    setGraphModalDocumentId(documentId);
    setGraphModalLoading(true);
    try {
      await fetchDocumentDetail(documentId);
    } catch {
      showToast('문서 상세를 불러오지 못했습니다.', 'error');
    } finally {
      setGraphModalLoading(false);
    }
  }, [fetchDocumentDetail, showToast]);

  const handleGraphModalClose = useCallback(() => {
    setGraphModalDocumentId(null);
  }, []);

  const handleNewSession = useCallback(() => {
    setActiveChatId(null);
    setMessages([]);
    setEmptyText('녹음 시작 버튼을 누르면 새 음성 세션이 시작됩니다.');
  }, []);

  const handleTitleUpdated = useCallback((payload: WsTitleUpdatedPayload) => {
    setChats((prev) => prev.map((chat) => (
      chat.chatId === payload.chatId ? { ...chat, title: payload.title } : chat
    )));
  }, []);

  const handleTurnCompleted = useCallback(async () => {
    setAiStatus('idle');
    liveMessageRef.current = null;
    try {
      const chatList = await loadChatList();
      if (activeChatId) {
        await loadChatDetails(activeChatId);
      } else {
        const newChat = chatList.find((c) => !knownChatIdsRef.current.has(c.chatId));
        if (newChat) {
          setActiveChatId(newChat.chatId);
          await loadChatDetails(newChat.chatId);
        }
      }
    } catch (error) {
      console.error('[app] turn completed handler failed', error);
    }
  }, [activeChatId, loadChatList, loadChatDetails]);

  const handleConnectionLost = useCallback(async () => {
    setIsRecording(false);
    setSessionId(null);
    setConnectionState('연결 안됨');
    setMicStatus('idle');
    setAiStatus('idle');
    await voiceSession.stop();
  }, [voiceSession]);

  const handleToggleRecording = useCallback(async () => {
    if (isConnecting) return;

    if (isRecording) {
      await voiceSession.stop();
      setIsRecording(false);
      setSessionId(null);
      setConnectionState('연결 안됨');
      setMicStatus('idle');
      setAiStatus('idle');
      return;
    }

    const accessToken = getAccessToken();
    if (!accessToken) return;

    setIsConnecting(true);
    setConnectionState('WebSocket 연결 중...');
    knownChatIdsRef.current = new Set(chats.map((c) => c.chatId));

    try {
      const sid = await voiceSession.start(accessToken, activeChatId, {
        onSessionReady: (id) => {
          setSessionId(id);
        },
        onTranscript: (payload) => {
          setAiStatus(payload.role === 'AGENT' ? 'speaking' : 'idle');
          setMessages((prev) => {
            const role = payload.role === 'USER' ? 'USER' as const : 'AGENT' as const;
            const last = prev.at(-1);
            if (liveMessageRef.current?.role === role && last?.role === role) {
              return [...prev.slice(0, -1), { id: last.id, role, text: last.text + payload.text }];
            }
            liveMessageRef.current = { role };
            return [...prev, { id: messageIdRef.current++, role, text: payload.text }];
          });
        },
        onTitleUpdated: handleTitleUpdated,
        onTurnCompleted: () => void handleTurnCompleted(),
        onInterrupted: () => {
          setAiStatus('interrupted');
        },
        onConnectionLost: () => void handleConnectionLost(),
      });

      setSessionId(sid);
      setIsRecording(true);
      setConnectionState('연결됨');
      setMicStatus('recording');
    } catch (error) {
      console.error('[app] start voice session failed', error);
      await voiceSession.stop();
      setConnectionState(`연결 실패: ${(error as Error).message}`);
    } finally {
      setIsConnecting(false);
    }
  }, [isConnecting, isRecording, voiceSession, chats, activeChatId, handleTurnCompleted, handleConnectionLost, handleTitleUpdated]);

  const handleCreateDocument = useCallback(async () => {
    if (!activeChatId || isCreatingDocument) return;
    setIsCreatingDocument(true);
    try {
      const response = await fetchJson<{ documentId?: string; title?: string }>('/documents', {
        method: 'POST',
        body: { chatId: activeChatId },
      });
      if (response.status !== 200 && response.status !== 201) {
        throw new Error(`문서 생성 실패 (${response.status})`);
      }
      const title = response.json?.title || '새 문서';
      showToast(`문서가 생성되었습니다: ${title}`, 'success');
      try { await loadDocuments(); } catch { /* 문서 목록 갱신 실패는 무시 — 생성 자체는 성공 */ }
    } catch {
      showToast('문서 생성에 실패했습니다. 다시 시도해주세요.', 'error');
    } finally {
      setIsCreatingDocument(false);
    }
  }, [activeChatId, isCreatingDocument, showToast, loadDocuments]);

  const handleSendText = useCallback((text: string) => {
    if (!text.trim()) return;
    voiceSession.sendText(text);
    setMessages((prev) => [...prev, { id: messageIdRef.current++, role: 'USER', text }]);
    liveMessageRef.current = null;
  }, [voiceSession]);

  const handleLogout = useCallback(async () => {
    await voiceSession.stop();
    setIsRecording(false);
    setSessionId(null);
    setChats([]);
    setMessages([]);
    setDocuments([]);
    setDocumentDetails(new Map());
    setActiveChatId(null);
    setActiveDocumentId(null);
    setActiveSidebarTab('chats');
    setMainView('chat');
    setGraphNodes([]);
    setGraphEdges([]);
    setGraphModalDocumentId(null);
    setConnectionState('연결 안됨');
    setMicStatus('idle');
    setAiStatus('idle');
    await auth.logout();
  }, [voiceSession, auth]);

  useEffect(() => {
    if (!auth.isAuthenticated) return;

    const init = async () => {
      try {
        const [chatList] = await Promise.allSettled([
          loadChatList(),
          loadDocuments(),
        ]);

        if (chatList.status === 'fulfilled' && chatList.value.length > 0) {
          const firstChatId = chatList.value[0].chatId;
          setActiveChatId(firstChatId);
          await loadChatDetails(firstChatId);
        } else {
          setEmptyText('녹음 시작 버튼을 누르면 새 음성 세션이 시작됩니다.');
        }
      } catch (error) {
        console.error('[app] init failed', error);
      }
    };

    init();
  }, [auth.isAuthenticated, loadChatList, loadDocuments, loadChatDetails]);

  if (auth.isLoading) {
    return (
      <main className="page">
        <div className="auth-view">
          <div className="auth-card">
            <h1>VoiceMap</h1>
            <p className="auth-status">로그인 처리 중...</p>
          </div>
        </div>
      </main>
    );
  }

  if (!auth.isAuthenticated) {
    return (
      <main className="page">
        <AuthView statusMessage={auth.statusMessage} />
      </main>
    );
  }

  const chatTitle = (() => {
    if (mainView === 'documentList') return '문서';
    if (mainView === 'documentDetail') {
      const d = activeDocumentId ? documentDetails.get(activeDocumentId) : null;
      return d?.title || '문서 상세';
    }
    if (mainView === 'graph') return '그래프';
    if (activeChatId) return chats.find((c) => c.chatId === activeChatId)?.title || '채팅 상세';
    return '대화를 선택하거나 새 대화를 시작하세요.';
  })();

  const headerSubtext = (() => {
    if (mainView === 'documentList') {
      return documents.length > 0 ? `총 ${documents.length}개의 문서` : '생성된 문서를 확인하세요.';
    }
    if (mainView === 'documentDetail') {
      const d = activeDocumentId ? documentDetails.get(activeDocumentId) : null;
      return d?.createdAt ? `생성일 ${formatDate(d.createdAt)}` : '문서 상세 정보를 확인하세요.';
    }
    if (mainView === 'graph') return '문서와 키워드의 연결 관계를 확인하세요.';
    return connectionState;
  })();

  return (
    <main className="page">
      <section className="app-view">
        <ChatSidebar
          memberNumber={auth.memberNumber}
          chats={chats}
          activeChatId={activeChatId}
          documents={documents}
          activeDocumentId={activeDocumentId}
          activeSidebarTab={activeSidebarTab}
          isRecording={isRecording}
          isConnecting={isConnecting}
          onNewSession={handleNewSession}
          onLogout={handleLogout}
          onSelectChat={selectChat}
          onSwitchTab={switchTab}
          onSelectDocument={handleSelectDocument}
        />

        <section className="main-panel">
          <header className="main-header">
            <div>
              <h3>{chatTitle}</h3>
              <p className="connection-state">{headerSubtext}</p>
            </div>
            <div className="status-group">
              <span className="badge">session: {sessionId || '-'}</span>
              <span className="badge">mic: {micStatus}</span>
              <span className="badge">ai: {aiStatus}</span>
            </div>
          </header>

          {mainView === 'chat' && (
            <>
              <TranscriptPanel messages={messages} emptyText={emptyText} />
              <RecorderFooter
                isRecording={isRecording}
                isConnecting={isConnecting}
                activeChatId={activeChatId}
                isCreatingDocument={isCreatingDocument}
                isSessionActive={isRecording}
                onToggleRecording={handleToggleRecording}
                onCreateDocument={handleCreateDocument}
                onSendText={handleSendText}
              />
            </>
          )}

          {mainView === 'documentList' && (
            <DocumentList
              documents={documents}
              activeDocumentId={activeDocumentId}
              documentDetails={documentDetails}
              onSelectDocument={handleSelectDocument}
            />
          )}

          {mainView === 'documentDetail' && (
            <DocumentDetailView
              detail={activeDocumentId ? documentDetails.get(activeDocumentId) ?? null : null}
              isLoading={documentDetailLoading}
              onNavigateToChat={(chatId) => {
                selectChat(chatId);
                setActiveSidebarTab('chats');
                setMainView('chat');
              }}
            />
          )}

          {mainView === 'graph' && (
            <GraphView
              nodes={graphNodes}
              edges={graphEdges}
              isLoading={graphLoading}
              error={graphError}
              documentDetails={documentDetails}
              onSelectDocument={handleGraphDocumentSelect}
            >
              {graphModalDocumentId && (
                <DocumentDetailModal
                  detail={documentDetails.get(graphModalDocumentId) ?? null}
                  isLoading={graphModalLoading}
                  onClose={handleGraphModalClose}
                  onNavigateToChat={(chatId) => {
                    handleGraphModalClose();
                    selectChat(chatId);
                    setActiveSidebarTab('chats');
                    setMainView('chat');
                  }}
                  onNavigateToDocument={(documentId) => {
                    handleGraphModalClose();
                    handleSelectDocument(documentId);
                    setActiveSidebarTab('documents');
                  }}
                />
              )}
            </GraphView>
          )}
        </section>
      </section>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </main>
  );
}
