import type { Chat, DocumentSummary, SidebarTab } from '../types';
import { formatDate } from '../utils/formatDate';

interface ChatSidebarProps {
  memberNumber: string;
  chats: Chat[];
  activeChatId: string | null;
  documents: DocumentSummary[];
  activeDocumentId: string | null;
  activeSidebarTab: SidebarTab;
  isRecording: boolean;
  isConnecting: boolean;
  onNewSession: () => void;
  onLogout: () => void;
  onSelectChat: (chatId: string) => void;
  onSwitchTab: (tab: SidebarTab) => void;
  onSelectDocument: (documentId: string) => void;
}

export default function ChatSidebar({
  memberNumber,
  chats,
  activeChatId,
  documents,
  activeDocumentId,
  activeSidebarTab,
  isRecording,
  isConnecting,
  onNewSession,
  onLogout,
  onSelectChat,
  onSwitchTab,
  onSelectDocument,
}: ChatSidebarProps) {
  const handleTabSwitch = (tab: SidebarTab) => {
    if ((isRecording || isConnecting) && tab !== 'chats') return;
    onSwitchTab(tab);
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <h2>VoiceMap</h2>
        <p className="member-info">{memberNumber ? `member: ${memberNumber}` : 'member: -'}</p>
      </div>

      <div className="sidebar-controls">
        <button className="btn btn-tertiary" onClick={onNewSession}>새 대화</button>
        <button className="btn btn-ghost" onClick={onLogout}>로그아웃</button>
      </div>

      <div className="sidebar-tabs" role="tablist" aria-label="사이드바 보기 전환">
        <button
          className={`sidebar-tab${activeSidebarTab === 'chats' ? ' is-active' : ''}`}
          type="button"
          aria-selected={activeSidebarTab === 'chats'}
          onClick={() => handleTabSwitch('chats')}
        >
          채팅
        </button>
        <button
          className={`sidebar-tab${activeSidebarTab === 'documents' ? ' is-active' : ''}`}
          type="button"
          aria-selected={activeSidebarTab === 'documents'}
          onClick={() => handleTabSwitch('documents')}
        >
          문서
        </button>
        <button
          className={`sidebar-tab${activeSidebarTab === 'graph' ? ' is-active' : ''}`}
          type="button"
          aria-selected={activeSidebarTab === 'graph'}
          onClick={() => handleTabSwitch('graph')}
        >
          그래프
        </button>
      </div>

      {activeSidebarTab === 'chats' && (
        <div className="chat-list-wrap">
          <h3>채팅 목록</h3>
          <ul className="chat-list">
            {chats.length === 0 ? (
              <li className="chat-item-empty">아직 채팅이 없습니다.</li>
            ) : (
              chats.map((chat) => (
                <li key={chat.chatId}>
                    <button
                    type="button"
                    className={`chat-item${chat.chatId === activeChatId ? ' is-active' : ''}`}
                    onClick={() => onSelectChat(chat.chatId)}
                  >
                    <span className="chat-item-title">{chat.title || '제목 없음'}</span>
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>
      )}

      {activeSidebarTab === 'documents' && (
        <div className="document-list-wrap">
          <h3>문서 목록</h3>
          <ul className="document-list">
            {documents.length === 0 ? (
              <li className="chat-item-empty">생성된 문서가 없습니다.</li>
            ) : (
              documents.map((doc) => (
                <li key={doc.documentId}>
                  <button
                    type="button"
                    className={`document-item${doc.documentId === activeDocumentId ? ' is-active' : ''}`}
                    onClick={() => onSelectDocument(doc.documentId)}
                  >
                    <span className="document-item-title">{doc.title || '제목 없는 문서'}</span>
                    <span className="document-item-summary">{doc.summary || '요약 정보가 없습니다.'}</span>
                    <span className="document-item-meta">{formatDate(doc.createdAt)}</span>
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>
      )}
    </aside>
  );
}
