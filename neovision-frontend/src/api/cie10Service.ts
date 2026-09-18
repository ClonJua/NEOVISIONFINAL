import api from './axios';
import type { ApiResponse } from './authService';

export interface Cie10Item {
  codigo: string;
  descripcion: string;
  categoria: string | null;
}

export const cie10Service = {
  buscar: (q: string) =>
    api.get<ApiResponse<Cie10Item[]>>('/historias/cie10', { params: { q } }),
};
