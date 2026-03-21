import { useEffect, useCallback } from 'react';
import DocumentDetailView from './DocumentDetailView';
import type { DocumentDetail } from '../types';

interface DocumentDetailModalProps {
  detail: DocumentDetail | null;
  isLoading: boolean;
  onClose: () => void;
  onNavigateToChat: (chatId: string) => void;
  onNavigateToDocument: (documentId: string) => void;
}

export default function DocumentDetailModal({
  detail,
  isLoading,
  onClose,
  onNavigateToChat,
  onNavigateToDocument,
}: Readonly<DocumentDetailModalProps>) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const handleBackdropClick = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (e.target === e.currentTarget) onClose();
    },
    [onClose],
  );

  return (
    <div className="modal-overlay" onClick={handleBackdropClick}>
      <div className="modal-content">
        <button type="button" className="modal-close-btn" onClick={onClose} aria-label="닫기">
          &times;
        </button>
        <DocumentDetailView
          detail={detail}
          isLoading={isLoading}
          onNavigateToChat={onNavigateToChat}
        />
        {detail && (
          <footer className="modal-footer">
            <button
              type="button"
              className="btn-link"
              onClick={() => onNavigateToDocument(detail.documentId)}
            >
              문서 탭에서 보기
            </button>
          </footer>
        )}
      </div>
    </div>
  );
}
