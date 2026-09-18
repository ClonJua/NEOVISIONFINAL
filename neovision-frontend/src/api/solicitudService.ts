import api from './axios';
import type { ApiResponse } from './authService';

export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

/**
 * DIGITAL: la creo el paciente desde su sesion.
 * PRESENCIAL: la registro la secretaria en su nombre, tras verificar su documento
 * (es el canal de quien no tiene cuenta ni correo).
 */
export type CanalSolicitud = 'DIGITAL' | 'PRESENCIAL';

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
  canal: CanalSolicitud;
  /** En PRESENCIAL, quien verifico la identidad del paciente. */
  creadaPorCorreo: string | null;
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

  /**
   * Registra una solicitud presencial y devuelve el PDF para entregarlo en mano.
   * Para el paciente que la pide en la clinica y no tiene cuenta ni correo.
   */
  crearPresencial: (idPaciente: number, motivo?: string) =>
    api.post(`/admin/solicitudes/presencial`, { idPaciente, motivo }, { responseType: 'blob' }),
};
