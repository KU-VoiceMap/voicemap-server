import { useEffect, useState } from 'react';

export interface ToastItem {
  id: number;
  message: string;
  type: 'success' | 'error';
}

let nextId = 0;

export function createToast(message: string, type: 'success' | 'error' = 'success'): ToastItem {
  return { id: nextId++, message, type };
}

interface ToastContainerProps {
  toasts: ToastItem[];
  onRemove: (id: number) => void;
}

export default function ToastContainer({ toasts, onRemove }: ToastContainerProps) {
  return (
    <div className="toast-container" aria-live="polite" aria-atomic="true">
      {toasts.map((toast) => (
        <ToastElement key={toast.id} toast={toast} onRemove={onRemove} />
      ))}
    </div>
  );
}

function ToastElement({ toast, onRemove }: { toast: ToastItem; onRemove: (id: number) => void }) {
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setLeaving(true);
      setTimeout(() => onRemove(toast.id), 220);
    }, 3200);
    return () => clearTimeout(timer);
  }, [toast.id, onRemove]);

  return (
    <div
      className={`toast toast-${toast.type}${leaving ? ' is-leaving' : ''}`}
      role={toast.type === 'error' ? 'alert' : 'status'}
    >
      <span>{toast.message}</span>
    </div>
  );
}
