interface RecorderFooterProps {
  isRecording: boolean;
  isConnecting: boolean;
  activeChatId: string | null;
  isCreatingDocument: boolean;
  onToggleRecording: () => void;
  onCreateDocument: () => void;
}

export default function RecorderFooter({
  isRecording,
  isConnecting,
  activeChatId,
  isCreatingDocument,
  onToggleRecording,
  onCreateDocument,
}: RecorderFooterProps) {
  const recordLabel = isConnecting ? '연결 중...' : isRecording ? '녹음 중지' : '녹음 시작';

  return (
    <footer className="recorder-footer">
      <div className="recorder-actions">
        <button
          className="btn btn-primary"
          onClick={onToggleRecording}
          disabled={isConnecting}
        >
          {recordLabel}
        </button>
        {activeChatId && (
          <button
            className={`btn btn-secondary${isCreatingDocument ? ' is-loading' : ''}`}
            onClick={onCreateDocument}
            disabled={!activeChatId || isCreatingDocument}
            aria-busy={isCreatingDocument}
          >
            {isCreatingDocument ? '문서 생성 중...' : '문서 생성'}
          </button>
        )}
      </div>
      <p className="footer-hint">텍스트 입력 없이 녹음 버튼으로만 WebSocket 음성 세션을 진행합니다.</p>
    </footer>
  );
}
