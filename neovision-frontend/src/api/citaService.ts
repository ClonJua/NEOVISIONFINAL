import api from './axios';
import type { ApiResponse } from './authService';
import type { EspecialidadInfo } from './adminService';

export interface CitaRequest {
  idSesion: number;
  idEstudiante: number;
  /** Segundo estudiante del bloque (los pacientes se atienden normalmente en pareja). */
  idEstudiante2?: number | null;
  idPaciente?: number | null;
  observaciones?: string;
}

export interface ReservarBloqueRequest {
  idEspecialidad?: number;
  subtipoControl?: string;
}

export interface EliminarVencidasResponse {
  eliminadas: number;
  omitidas: number;
}

export interface CitaResponse {
  idCita: number;
  estado: string;
  horaInicio: string;
  horaFin: string;
  observaciones: string | null;
  fechaReserva: string | null;
  // Sesion
  idSesion: number;
  fechaSesion: string;
  nombreSalon: string | null;
  nombreEspacio: string | null;
  nombreDocente: string;
  // Estudiantes (el segundo es opcional: los pacientes se atienden normalmente en pareja)
  idEstudiante: number;
  nombreEstudiante: string;
  idEstudiante2?: number | null;
  nombreEstudiante2?: string | null;
  // Paciente (null si DISPONIBLE)
  idPaciente: number | null;
  nombrePaciente: string | null;
  // Especialidad de la cita
  idEspecialidad?: number | null;
  nombreEspecialidad?: string | null;
  observacionRecordatorio?: string | null;
  subtipoControl?: string | null;
  // Especialidades del docente que supervisa la sesion
  especialidadesDocente?: EspecialidadInfo[];
}

/** "Ana Pérez" o "Ana Pérez y Luis Gómez" cuando el bloque lo atienden dos estudiantes. */
export const nombresEstudiantes = (c: Pick<CitaResponse, 'nombreEstudiante' | 'nombreEstudiante2'>) =>
  c.nombreEstudiante2 ? `${c.nombreEstudiante} y ${c.nombreEstudiante2}` : c.nombreEstudiante;

export const citaService = {
  // ---- Admin ----
  crearBloque: (data: CitaRequest) =>
    api.post<ApiResponse<CitaResponse>>('/admin/citas', data),
  listarCitas: (estado?: string) =>
    api.get<ApiResponse<CitaResponse[]>>('/admin/citas', { params: estado ? { estado } : {} }),
  editarCita: (idCita: number, data: CitaRequest) =>
    api.put<ApiResponse<CitaResponse>>(`/admin/citas/${idCita}`, data),
  eliminarCita: (idCita: number) =>
    api.delete<ApiResponse<void>>(`/admin/citas/${idCita}/permanente`),
  eliminarVencidas: () =>
    api.delete<ApiResponse<EliminarVencidasResponse>>('/admin/citas/vencidas'),
  cambiarEstado: (idCita: number, estado: string, observaciones?: string) =>
    api.patch<ApiResponse<CitaResponse>>(`/admin/citas/${idCita}/estado`, { estado, observaciones }),
  reprogramarAdmin: (idCitaActual: number, idCitaNueva: number) =>
    api.patch<ApiResponse<CitaResponse>>(`/admin/citas/${idCitaActual}/reprogramar`, { idCitaNueva }),

  // ---- Paciente ----
  bloquesDisponibles: (idEspecialidad?: number) =>
    api.get<ApiResponse<CitaResponse[]>>('/paciente/citas/disponibles', {
      params: idEspecialidad != null ? { idEspecialidad } : {},
    }),
  reservarBloque: (idCita: number, data?: ReservarBloqueRequest) =>
    api.post<ApiResponse<CitaResponse>>(`/paciente/citas/${idCita}/reservar`, data ?? {}),
  cancelarPaciente: (idCita: number, observaciones?: string) =>
    api.patch<ApiResponse<CitaResponse>>(`/paciente/citas/${idCita}/cancelar`, { observaciones }),
  misCitas: () =>
    api.get<ApiResponse<CitaResponse[]>>('/paciente/citas'),

  // ---- Export ----
  exportarCitas: (formato: 'csv' | 'excel' | 'pdf', estado?: string) =>
    api.get<Blob>('/admin/citas/exportar', {
      params: { formato, ...(estado && estado !== 'TODAS' ? { estado } : {}) },
      responseType: 'blob',
    }),
  enviarExportPorCorreo: (formato: 'csv' | 'excel' | 'pdf', correoDestino: string, estado?: string) =>
    api.post<ApiResponse<null>>('/admin/citas/exportar/enviar', {
      formato,
      correoDestino,
      ...(estado && estado !== 'TODAS' ? { estado } : {}),
    }),

  // ---- Estudiante ----
  misBloques: () =>
    api.get<ApiResponse<CitaResponse[]>>('/estudiante/citas'),
  progresoEstudiante: () =>
    api.get<ApiResponse<{ semestre: number; atendidos: number; meta: number }>>('/estudiante/citas/progreso'),
  asignarPaciente: (idCita: number, idPaciente: number) =>
    api.post<ApiResponse<CitaResponse>>(`/estudiante/citas/${idCita}/asignar-paciente`, { idPaciente }),
};
