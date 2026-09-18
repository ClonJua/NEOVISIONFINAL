import api from './axios';
import type { ApiResponse } from './authService';

export interface EspecialidadResponse {
  idEspecialidad: number;
  nombre: string;
  observacionRecordatorio: string | null;
  requiereSubtipo: boolean;
  esDefault: boolean;
  activo: boolean;
}

export interface EspecialidadRequest {
  nombre: string;
  observacionRecordatorio?: string;
  requiereSubtipo?: boolean;
  esDefault?: boolean;
}

export const SUBTIPOS_CONTROL = [
  'lentes de contacto',
  'funcional',
  'protesis',
  'prueba ambulatoria',
  'terapia',
] as const;

export type SubtipoControl = typeof SUBTIPOS_CONTROL[number];

export const especialidadService = {
  // Lectura abierta a usuarios autenticados
  listarActivas: () =>
    api.get<ApiResponse<EspecialidadResponse[]>>('/especialidades'),

  // Admin
  listarTodas: () =>
    api.get<ApiResponse<EspecialidadResponse[]>>('/admin/especialidades'),
  crear: (data: EspecialidadRequest) =>
    api.post<ApiResponse<EspecialidadResponse>>('/admin/especialidades', data),
  actualizar: (id: number, data: EspecialidadRequest) =>
    api.put<ApiResponse<EspecialidadResponse>>(`/admin/especialidades/${id}`, data),
  desactivar: (id: number) =>
    api.delete<ApiResponse<void>>(`/admin/especialidades/${id}`),
  reactivar: (id: number) =>
    api.patch<ApiResponse<void>>(`/admin/especialidades/${id}/reactivar`),
};
