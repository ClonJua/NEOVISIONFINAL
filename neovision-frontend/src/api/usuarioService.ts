import api from './axios';
import type { ApiResponse } from './authService';

export const usuarioService = {
  obtenerFirma: () =>
    api.get<ApiResponse<string | null>>('/usuario/firma'),
  guardarFirma: (firma: string) =>
    api.put<ApiResponse<void>>('/usuario/firma', { firma }),
};
