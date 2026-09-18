import * as SecureStore from 'expo-secure-store';
import type { AuthData } from '../types';

const KEY = 'neovision.auth';

export const tokenStorage = {
  async save(data: AuthData): Promise<void> {
    await SecureStore.setItemAsync(KEY, JSON.stringify(data));
  },

  async load(): Promise<AuthData | null> {
    const raw = await SecureStore.getItemAsync(KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthData;
    } catch {
      return null;
    }
  },

  async clear(): Promise<void> {
    await SecureStore.deleteItemAsync(KEY);
  },
};

export function isJwtExpired(token: string): boolean {
  try {
    const [, payload] = token.split('.');
    const decoded = JSON.parse(
      atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    ) as { exp?: number };
    if (!decoded.exp) return true;
    return Date.now() >= decoded.exp * 1000;
  } catch {
    return true;
  }
}
