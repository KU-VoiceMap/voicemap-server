import type { DocumentSummary, DocumentDetail } from '../types';
import { formatDate } from '../utils/formatDate';

interface DocumentListProps {
  documents: DocumentSummary[];
  activeDocumentId: string | null;
  documentDetails: Map<string, DocumentDetail>;
  onSelectDocument: (documentId: string) => void;
}

export default function DocumentList({
  documents,
  activeDocumentId,
  documentDetails,
  onSelectDocument,
}: DocumentListProps) {
  if (documents.length === 0) {
    return (
      <div className="document-list-view">
        <div className="empty-state">
          <p>생성된 문서가 없습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="document-list-view">
      <div className="document-card-grid">
        {documents.map((doc) => {
          const detail = documentDetails.get(doc.documentId);
          const keywords = detail?.keywords ?? [];

          return (
            <button
              key={doc.documentId}
              type="button"
              className={`document-card${doc.documentId === activeDocumentId ? ' is-active' : ''}`}
              onClick={() => onSelectDocument(doc.documentId)}
            >
              <div className="document-card-header">
                <h4 className="document-card-title">{doc.title || '제목 없는 문서'}</h4>
                <span className="document-card-date">{formatDate(doc.createdAt)}</span>
              </div>
              <p className="document-card-summary">{doc.summary || '요약 정보가 없습니다.'}</p>
              {keywords.length > 0 && (
                <div className="keyword-badge-row">
                  {keywords.slice(0, 8).map((kw, i) => (
                    <span key={i} className="keyword-badge">{kw}</span>
                  ))}
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}


