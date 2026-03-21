import { useEffect, useRef } from 'react';
import type { Message } from '../types';

interface TranscriptPanelProps {
  messages: Message[];
  emptyText: string;
}

export default function TranscriptPanel({ messages, emptyText }: TranscriptPanelProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (panelRef.current) {
      panelRef.current.scrollTop = panelRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div ref={panelRef} className="transcript-panel">
      {messages.length === 0 ? (
        <div className="empty-state">
          <p>{emptyText}</p>
        </div>
      ) : (
        messages.map((msg, idx) => (
          <article key={idx} className={`message ${msg.role === 'USER' ? 'message-user' : 'message-agent'}`}>
            <div className="message-role">{msg.role === 'USER' ? '사용자' : 'AI'}</div>
            <div className="message-content">{msg.text}</div>
          </article>
        ))
      )}
    </div>
  );
}
