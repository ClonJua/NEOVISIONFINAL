import api from './axios';
import type { ApiResponse } from './authService';
import type { SesionResponse } from './sesionService';
import type { CitaResponse } from './citaService';
import type { EspecialidadInfo } from './adminService';

export interface DocenteResponse {
  idDocente: number;
  nombre: string;
  apellido: string;
  cedula: string;
  telefono: string;
  correo: string;
  activo: boolean;
  especialidades?: EspecialidadInfo[];
}

export interface EditarDocenteRequest {
  nombre: string;
  apellido: string;
  telefono: string;
}

export interface EditarDocenteAdminRequest {
  nombre: string;
  apellido: string;
  cedula: string;
  telefono: string;
  correo: string;
  idsEspecialidades?: number[];
}

export const docenteService = {
  misSesiones: () =>
    api.get<ApiResponse<SesionResponse[]>>('/docente/sesiones'),

  misCitas: () =>
    api.get<ApiResponse<CitaResponse[]>>('/docente/citas'),

  miPerfil: () =>
    api.get<ApiResponse<DocenteResponse>>('/docente/perfil'),

  editarPerfil: (data: EditarDocenteRequest) =>
    api.put<ApiResponse<DocenteResponse>>('/docente/perfil', data),

  editarDocenteAdmin: (idDocente: number, data: EditarDocenteAdminRequest) =>
    api.put<ApiResponse<DocenteResponse>>(`/admin/docentes/${idDocente}`, data),

  confirmarAsistencia: (idCita: number) =>
    api.patch<ApiResponse<CitaResponse>>(`/docente/citas/${idCita}/confirmar`),
};
