import axios from 'axios';
import { ENV } from '../config/env';
import { tokenStorage } from '../auth/tokenStorage';

const api = axios.create({
  baseURL: ENV.BACKEND_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

api.interceptors.request.use(async (config) => {
  const auth = await tokenStorage.load();
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

// El 401 de los endpoints de autenticacion (login/registro/microsoft) es
// "credenciales invalidas", no "sesion expirada": no debe forzar logout.
const AUTH_PATHS = ['/auth/login', '/auth/registro', '/auth/microsoft'];

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url: string = error.config?.url ?? '';
    const esEndpointAuth = AUTH_PATHS.some((p) => url.includes(p));
    if (error.response?.status === 401 && !esEndpointAuth && onUnauthorized) {
      onUnauthorized();
    }
    return Promise.reject(error);
  }
);

export default api;
