import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { CheckCircleIcon, AlertCircleIcon, InfoIcon, XIcon } from './Icons';

type ToastType = 'success' | 'error' | 'info';

interface ToastItem {
  id: number;
  type: ToastType;
  title: string;
  description?: string;
}

interface ToastContextValue {
  show: (type: ToastType, title: string, description?: string) => void;
  success: (title: string, description?: string) => void;
  error: (title: string, description?: string) => void;
  info: (title: string, description?: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const show = useCallback((type: ToastType, title: string, description?: string) => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, type, title, description }]);
    setTimeout(() => dismiss(id), 4500);
  }, [dismiss]);

  const api: ToastContextValue = {
    show,
    success: (title, description) => show('success', title, description),
    error: (title, description) => show('error', title, description),
    info: (title, description) => show('info', title, description),
  };

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
        {toasts.map((t) => (
          <ToastCard key={t.id} item={t} onClose={() => dismiss(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastCard({ item, onClose }: { item: ToastItem; onClose: () => void }) {
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const id = requestAnimationFrame(() => setVisible(true));
    return () => cancelAnimationFrame(id);
  }, []);

  const palette = {
    success: { bar: 'bg-emerald-500', icon: <CheckCircleIcon size={20} className="text-emerald-500" /> },
    error:   { bar: 'bg-red-500',     icon: <AlertCircleIcon size={20} className="text-red-500" /> },
    info:    { bar: 'bg-blue-500',    icon: <InfoIcon size={20} className="text-blue-500" /> },
  }[item.type];

  return (
    <div
      className={`
        pointer-events-auto flex items-start gap-3 bg-white rounded-xl shadow-lg border border-slate-200
        min-w-[280px] max-w-sm overflow-hidden
        transform transition-all duration-200 ease-out
        ${visible ? 'translate-x-0 opacity-100' : 'translate-x-4 opacity-0'}
      `}
      role="alert"
    >
      <div className={`w-1 self-stretch ${palette.bar}`} />
      <div className="pt-3.5 pb-3 flex-1">
        <div className="flex items-center gap-2">
          {palette.icon}
          <p className="text-sm font-semibold text-slate-800">{item.title}</p>
        </div>
        {item.description && (
          <p className="text-xs text-slate-500 mt-1 ml-7">{item.description}</p>
        )}
      </div>
      <button
        onClick={onClose}
        className="p-2 text-slate-400 hover:text-slate-700 transition"
        aria-label="Cerrar"
      >
        <XIcon size={14} />
      </button>
    </div>
  );
}
