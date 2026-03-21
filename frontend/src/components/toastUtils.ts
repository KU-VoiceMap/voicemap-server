export interface ToastItem {
  id: number;
  message: string;
  type: 'success' | 'error';
}

let nextId = 0;

export function createToast(message: string, type: 'success' | 'error' = 'success'): ToastItem {
  return { id: nextId++, message, type };
}
