import api from './axios';
import type { ApiResponse } from './authService';

export interface CupsItem {
  codigo: string;
  descripcion: string;
  categoria: string | null;
}

export const cupsService = {
  buscar: (q: string) =>
    api.get<ApiResponse<CupsItem[]>>('/historias/cups', { params: { q } }),
};
