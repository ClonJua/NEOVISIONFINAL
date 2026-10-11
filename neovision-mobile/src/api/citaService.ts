import api from './axios';
import type { ApiResponse } from '../types';
import type { CitaResponse } from '../types/cita';
import type { ProgresoEstudiante } from '../types/perfil';

export interface ReservarBloqueRequest {
  idEspecialidad?: number;
  subtipoControl?: string;
}

export const citaService = {
  // ---- Estudiante ----
  misBloques: () =>
    api.get<ApiResponse<CitaResponse[]>>('/estudiante/citas'),

  progresoEstudiante: () =>
    api.get<ApiResponse<ProgresoEstudiante>>('/estudiante/citas/progreso'),

  asignarPaciente: (idCita: number, idPaciente: number) =>
    api.post<ApiResponse<CitaResponse>>(`/estudiante/citas/${idCita}/asignar-paciente`, { idPaciente }),

  cancelarEstudiante: (idCita: number, observaciones: string) =>
    api.patch<ApiResponse<CitaResponse>>(`/estudiante/citas/${idCita}/cancelar`, { observaciones }),

  reprogramarEstudiante: (idCitaActual: number, idCitaNueva: number) =>
    api.patch<ApiResponse<CitaResponse>>(`/estudiante/citas/${idCitaActual}/reprogramar`, { idCitaNueva }),

  // ---- Paciente ----
  bloquesDisponibles: (idEspecialidad?: number) =>
    api.get<ApiResponse<CitaResponse[]>>('/paciente/citas/disponibles', {
      params: idEspecialidad != null ? { idEspecialidad } : {},
    }),

  reservarBloque: (idCita: number, data?: ReservarBloqueRequest) =>
    api.post<ApiResponse<CitaResponse>>(`/paciente/citas/${idCita}/reservar`, data ?? {}),

  cancelarPaciente: (idCita: number, observaciones?: string) =>
    api.patch<ApiResponse<CitaResponse>>(`/paciente/citas/${idCita}/cancelar`, { observaciones }),

  reprogramarPaciente: (idCitaActual: number, idCitaNueva: number) =>
    api.patch<ApiResponse<CitaResponse>>(`/paciente/citas/${idCitaActual}/reprogramar`, { idCitaNueva }),

  misCitas: () =>
    api.get<ApiResponse<CitaResponse[]>>('/paciente/citas'),
};
