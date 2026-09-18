import api from './axios';
import type { ApiResponse } from './authService';
import type { EspecialidadInfo } from './adminService';

export interface SesionResponse {
  idSesion: number;
  fecha: string;
  horaInicio: string | null;
  horaFin: string | null;
  cuposTotales: number;
  cuposDisponibles: number;
  activo: boolean;
  idSalon: number | null;
  nombreSalon: string | null;
  nombreEspacio: string | null;
  idDocente: number;
  nombreDocente: string;
  estudiantes: { idEstudiante: number; nombre: string }[];
  especialidadesDocente?: EspecialidadInfo[];
}

export interface SesionRequest {
  idSalon: number | null;
  idDocente: number;
  fecha: string;
  horaInicio?: string;
  horaFin?: string;
  cuposTotales: number;
  idsEstudiantes: number[];
}

export interface CrearSesionRecurrenteRequest {
  diaSemana: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  fechaInicio: string;
  fechaFin: string;
  horaInicio: string;
  horaFin: string;
  cuposTotales: number;
  idDocente?: number;
  idSalon?: number | null;
  idsEstudiantes?: number[];
}

export interface SesionRecurrenteResponse {
  sesionesCreadas: number;
  mensaje: string;
}

export const sesionService = {
  listar: (fecha?: string) =>
    api.get<ApiResponse<SesionResponse[]>>('/admin/sesiones', { params: fecha ? { fecha } : {} }),
  crear: (data: SesionRequest) =>
    api.post<ApiResponse<SesionResponse>>('/admin/sesiones', data),
  crearRecurrente: (data: CrearSesionRecurrenteRequest) =>
    api.post<ApiResponse<SesionRecurrenteResponse>>('/admin/sesiones/recurrente', data),
  editar: (id: number, data: SesionRequest) =>
    api.put<ApiResponse<SesionResponse>>(`/admin/sesiones/${id}`, data),
  desactivar: (id: number) =>
    api.delete<ApiResponse<void>>(`/admin/sesiones/${id}`),
  eliminar: (id: number) =>
    api.delete<ApiResponse<void>>(`/admin/sesiones/${id}/permanente`),
};
