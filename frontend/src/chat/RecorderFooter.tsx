import { useState, type ComponentProps } from 'react';

type RecorderFooterSubmitEvent = Parameters<NonNullable<ComponentProps<'form'>['onSubmit']>>[0];

interface RecorderFooterProps {
  isRecording: boolean;
  isConnecting: boolean;
  activeChatId: string | null;
  isCreatingDocument: boolean;
  isSessionActive: boolean;
  onToggleRecording: () => void;
  onCreateDocument: () => void;
  onSendText: (text: string) => void;
}

export default function RecorderFooter({
  isRecording,
  isConnecting,
  activeChatId,
  isCreatingDocument,
  isSessionActive,
  onToggleRecording,
  onCreateDocument,
  onSendText,
}: Readonly<RecorderFooterProps>) {
  const [textValue, setTextValue] = useState('');

  let recordLabel = '녹음 시작';
  if (isConnecting) {
    recordLabel = '연결 중...';
  } else if (isRecording) {
    recordLabel = '녹음 중지';
  }

  const handleSubmit = (event: RecorderFooterSubmitEvent) => {
    event.preventDefault();
    const text = textValue.trim();
    if (!text) return;
    onSendText(text);
    setTextValue('');
  };

  return (
    <footer className="recorder-footer">
      {isSessionActive && (
        <form className="text-input-form" onSubmit={handleSubmit}>
          <input
            type="text"
            className="text-input-field"
            value={textValue}
            onChange={(e) => setTextValue(e.target.value)}
            placeholder="메시지를 입력하세요..."
          />
          <button
            type="submit"
            className="btn btn-primary text-send-btn"
            disabled={!textValue.trim() || !isSessionActive}
          >
            전송
          </button>
        </form>
      )}
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
      <p className="footer-hint">음성 또는 텍스트로 대화할 수 있습니다.</p>
    </footer>
  );
}
