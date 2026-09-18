import { create } from 'zustand';

export type ToastType = 'success' | 'error';

interface ToastState {
  message: string | null;
  type: ToastType;
  show: (message: string, type?: ToastType) => void;
  hide: () => void;
}

export const useToastStore = create<ToastState>((set) => ({
  message: null,
  type: 'success',
  show: (message, type = 'success') => set({ message, type }),
  hide: () => set({ message: null }),
}));

/** Helper para usar fuera de componentes (ej. callbacks de mutations). */
export const toast = {
  success: (m: string) => useToastStore.getState().show(m, 'success'),
  error:   (m: string) => useToastStore.getState().show(m, 'error'),
};
