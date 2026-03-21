import { useEffect, useState } from 'react';
import type { ToastItem } from './toastUtils';

interface ToastContainerProps {
  toasts: ToastItem[];
  onRemove: (id: number) => void;
}

export default function ToastContainer({ toasts, onRemove }: Readonly<ToastContainerProps>) {
  return (
    <div className="toast-container" aria-live="polite" aria-atomic="true">
      {toasts.map((toast) => (
        <ToastElement key={toast.id} toast={toast} onRemove={onRemove} />
      ))}
    </div>
  );
}

function ToastElement({ toast, onRemove }: Readonly<{ toast: ToastItem; onRemove: (id: number) => void }>) {
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setLeaving(true);
      setTimeout(() => onRemove(toast.id), 220);
    }, 3200);
    return () => clearTimeout(timer);
  }, [toast.id, onRemove]);

  if (toast.type === 'error') {
    return (
      <div
        className={`toast toast-error${leaving ? ' is-leaving' : ''}`}
        role="alert"
      >
        <span>{toast.message}</span>
      </div>
    );
  }

  return (
    <output
      className={`toast toast-${toast.type}${leaving ? ' is-leaving' : ''}`}
    >
      <span>{toast.message}</span>
    </output>
  );
}
