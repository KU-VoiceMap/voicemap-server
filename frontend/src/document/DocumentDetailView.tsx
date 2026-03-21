import type { DocumentDetail } from '../types';
import { formatDate } from '../utils/formatDate';

interface DocumentDetailViewProps {
  detail: DocumentDetail | null;
  isLoading: boolean;
  onNavigateToChat: (chatId: string) => void;
}

export default function DocumentDetailView({ detail, isLoading, onNavigateToChat }: Readonly<DocumentDetailViewProps>) {
  if (isLoading) {
    return (
      <div className="document-detail-view">
        <div className="empty-state is-loading">
          <p>문서 상세를 불러오는 중입니다.</p>
        </div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="document-detail-view">
        <div className="empty-state">
          <p>문서 상세를 불러오지 못했습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="document-detail-view">
      <article className="document-detail-content">
        <header className="document-detail-header">
          <h4 className="document-detail-title">{detail.title || '제목 없는 문서'}</h4>
          <div className="document-detail-meta">
            <p className="document-detail-date">
              {detail.createdAt ? `생성일 ${formatDate(detail.createdAt)}` : '생성일 정보 없음'}
            </p>
            {detail.chatId && (
              <button
                type="button"
                className="btn-link"
                onClick={() => onNavigateToChat(detail.chatId!)}
              >
                원본 채팅 보기
              </button>
            )}
          </div>
        </header>

        <section className="document-detail-section">
          <h5>요약</h5>
          <p className="document-detail-summary">{detail.summary || '요약 정보가 없습니다.'}</p>
        </section>

        <section className="document-detail-section">
          <h5>본문</h5>
          <div className="document-detail-body">{detail.content || '문서 본문이 없습니다.'}</div>
        </section>

        <section className="document-detail-section">
          <h5>키워드</h5>
          {detail.keywords.length > 0 ? (
            <div className="keyword-badge-row">
              {detail.keywords.map((kw) => (
                <span key={kw} className="keyword-badge">{kw}</span>
              ))}
            </div>
          ) : (
            <p className="document-detail-summary">연결된 키워드가 없습니다.</p>
          )}
        </section>
      </article>
    </div>
  );
}


