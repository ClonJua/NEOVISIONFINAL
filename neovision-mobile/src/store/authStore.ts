import { create } from 'zustand';
import { authService, type LoginRequest, type RegistroRequest } from '../api/authService';
import { usuarioService } from '../api/usuarioService';
import { getMicrosoftAccessToken } from '../auth/microsoftAuth';
import { isJwtExpired, tokenStorage } from '../auth/tokenStorage';
import { queryClient } from '../lib/queryClient';
import { obtenerExpoPushToken } from '../notifications/push';
import type { AuthData } from '../types';

interface AuthState {
  status: 'loading' | 'authenticated' | 'unauthenticated';
  auth:   AuthData | null;
  error:  string | null;

  bootstrap: () => Promise<void>;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegistroRequest) => Promise<void>;
  loginWithMicrosoft: () => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
}

function errorMessage(e: any, fallback: string): string {
  return e?.response?.data?.message ?? e?.message ?? fallback;
}

export const useAuthStore = create<AuthState>((set) => {
  // Registra el dispositivo para push. Best-effort: nunca rompe el login.
  const sincronizarPushToken = async () => {
    try {
      const token = await obtenerExpoPushToken();
      if (token) await usuarioService.registrarPushToken(token);
    } catch {
      // silencioso: la app funciona sin push
    }
  };

  // Persiste la sesion y marca autenticado. Centraliza la logica comun
  // de login / registro / Microsoft.
  const establecerSesion = async (auth: AuthData) => {
    await tokenStorage.save(auth);
    set({ status: 'authenticated', auth, error: null });
    sincronizarPushToken();
  };

  return {
    status: 'loading',
    auth:   null,
    error:  null,

    bootstrap: async () => {
      const stored = await tokenStorage.load();
      if (stored && !isJwtExpired(stored.token)) {
        set({ status: 'authenticated', auth: stored, error: null });
      } else {
        if (stored) await tokenStorage.clear();
        set({ status: 'unauthenticated', auth: null, error: null });
      }
    },

    login: async (data) => {
      set({ error: null });
      try {
        const res = await authService.login(data);
        await establecerSesion(res.data.data);
      } catch (e: any) {
        const msg = errorMessage(e, 'Credenciales incorrectas');
        set({ error: msg });
        throw e;
      }
    },

    register: async (data) => {
      set({ error: null });
      try {
        const res = await authService.registro(data);
        await establecerSesion(res.data.data);
      } catch (e: any) {
        const msg = errorMessage(e, 'Error en el registro');
        set({ error: msg });
        throw e;
      }
    },

    loginWithMicrosoft: async () => {
      set({ error: null });
      try {
        const accessToken = await getMicrosoftAccessToken();
        const res = await authService.loginMicrosoft({ accessToken });
        await establecerSesion(res.data.data);
      } catch (e: any) {
        const msg = errorMessage(e, 'Error al iniciar sesión');
        set({ error: msg });
        throw e;
      }
    },

    logout: async () => {
      // Ambas llamadas son best-effort mientras el JWT actual aun es valido:
      // desvincula el dispositivo de push y revoca el token en el servidor,
      // para que una copia robada antes de este momento deje de servir.
      try { await usuarioService.eliminarPushToken(); } catch { /* best-effort */ }
      try { await authService.logout(); } catch { /* best-effort */ }
      await tokenStorage.clear();
      // Evita que el siguiente usuario vea datos cacheados del anterior.
      queryClient.clear();
      set({ status: 'unauthenticated', auth: null, error: null });
    },

    clearError: () => set({ error: null }),
  };
});
