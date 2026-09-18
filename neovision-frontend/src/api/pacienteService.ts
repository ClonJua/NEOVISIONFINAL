import api from './axios';
import type { ApiResponse } from './authService';
import type { PacienteResponse } from './adminService';

export interface EditarPacienteRequest {
  nombre: string;
  apellido: string;
  telefono: string;
}

export interface EditarPacienteAdminRequest {
  nombre: string;
  apellido: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
  correo: string;
  representanteNombre?: string;
  representanteDocumento?: string;
  representanteParentesco?: string;
}

export const pacienteService = {
  miPerfil: () =>
    api.get<ApiResponse<PacienteResponse>>('/paciente/perfil'),

  editarPerfil: (data: EditarPacienteRequest) =>
    api.put<ApiResponse<PacienteResponse>>('/paciente/perfil', data),

  misCitas: () =>
    api.get<ApiResponse<any>>('/paciente/citas'),

  bloquesDisponibles: () =>
    api.get<ApiResponse<any>>('/paciente/citas/disponibles'),

  reservarBloque: (idCita: number) =>
    api.post<ApiResponse<any>>(`/paciente/citas/${idCita}/reservar`),

  cancelarCita: (idCita: number, observaciones?: string) =>
    api.patch<ApiResponse<any>>(`/paciente/citas/${idCita}/cancelar`, { observaciones }),

  editarPacienteAdmin: (idPaciente: number, data: EditarPacienteAdminRequest) =>
    api.put<ApiResponse<PacienteResponse>>(`/admin/pacientes/${idPaciente}`, data),
};
