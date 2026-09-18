import api from './axios';
import type { ApiResponse } from '../types';

export const usuarioService = {
  registrarPushToken: (token: string) =>
    api.post<ApiResponse<void>>('/usuario/push-token', { token }),

  eliminarPushToken: () =>
    api.delete<ApiResponse<void>>('/usuario/push-token'),
};
