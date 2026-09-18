import api from './axios';
import type { ApiResponse } from './authService';

export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

export interface SolicitudResponse {
  idSolicitud: number;
  idPaciente: number | null;
  nombrePaciente: string | null;
  correoPaciente: string | null;
  fechaSolicitud: string;
  estado: EstadoSolicitud;
  motivo: string | null;
  motivoRechazo: string | null;
  resueltaPorCorreo: string | null;
  fechaResolucion: string | null;
}

export const solicitudService = {
  // Paciente
  crear: (motivo?: string) =>
    api.post<ApiResponse<SolicitudResponse>>('/paciente/solicitudes', { motivo }),
  mias: () =>
    api.get<ApiResponse<SolicitudResponse[]>>('/paciente/solicitudes'),

  // Admin
  listar: () =>
    api.get<ApiResponse<SolicitudResponse[]>>('/admin/solicitudes'),
  aprobar: (id: number) =>
    api.post<ApiResponse<SolicitudResponse>>(`/admin/solicitudes/${id}/aprobar`),
  rechazar: (id: number, motivo: string) =>
    api.post<ApiResponse<SolicitudResponse>>(`/admin/solicitudes/${id}/rechazar`, { motivo }),
};
