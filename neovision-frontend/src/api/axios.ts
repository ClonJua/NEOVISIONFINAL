import axios from 'axios';

// En local, '/api' funciona gracias al proxy de desarrollo de Vite (vite.config.ts).
// Ese proxy no existe en producción (Azure Static Web Apps sirve solo archivos
// estáticos), así que ahí se necesita la URL absoluta del backend desplegado
// —se fija en build time con VITE_API_BASE_URL, ver el workflow de despliegue.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
