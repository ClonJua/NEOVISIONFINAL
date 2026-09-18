import api from './axios';
import type { ApiResponse } from './authService';

export interface EspecialidadInfo {
  idEspecialidad: number;
  nombre: string;
  observacionRecordatorio: string | null;
  requiereSubtipo: boolean;
  esDefault: boolean;
  activo: boolean;
}

export interface DocenteResponse {
  idDocente: number;
  nombre: string;
  apellido: string;
  correo: string;
  cedula: string | null;
  telefono: string | null;
  activo: boolean;
  especialidades?: EspecialidadInfo[];
}

export interface EstudianteResponse {
  idEstudiante: number;
  nombre: string;
  apellido: string;
  correo: string;
  cedula: string;
  semestre: number;
  telefono: string | null;
  activo: boolean;
}

export interface PacienteResponse {
  idPaciente: number;
  nombre: string;
  apellido: string;
  correo: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
  activo: boolean;
  autorizaTratamientoDatos?: boolean;
  representanteNombre?: string | null;
  representanteDocumento?: string | null;
  representanteParentesco?: string | null;
}

export interface CrearDocenteRequest {
  nombre: string;
  apellido: string;
  correo: string;
  cedula?: string;
  telefono?: string;
  idsEspecialidades?: number[];
}

export interface EditarDocenteAdminRequest {
  nombre: string;
  apellido: string;
  correo: string;
  cedula: string;
  telefono: string;
  idsEspecialidades?: number[];
}

export interface CrearEstudianteRequest {
  nombre: string;
  apellido: string;
  correo: string;
  cedula: string;
  semestre: number;
  telefono?: string;
}

export interface CrearPacienteRequest {
  nombre: string;
  apellido: string;
  correo: string;
  contrasena: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
  autorizaTratamientoDatos?: boolean;
  representanteNombre?: string;
  representanteDocumento?: string;
  representanteParentesco?: string;
}

export interface EditarPacienteRequest {
  nombre: string;
  apellido: string;
  correo: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
}

export interface CitaResponse {
  idCita: number;
  estado: string;
  horaInicio: string | null;
  horaFin: string | null;
  observaciones?: string;
  fechaReserva?: string;
  idSesion: number;
  fechaSesion: string;
  nombreSalon?: string;
  nombreEspacio?: string;
  nombreDocente?: string;
  idEstudiante: number;
  nombreEstudiante: string;
  /** Segundo estudiante del bloque (los pacientes se atienden normalmente en pareja). */
  idEstudiante2?: number | null;
  nombreEstudiante2?: string | null;
  idPaciente?: number;
  nombrePaciente?: string;
  idEspecialidad?: number;
  nombreEspecialidad?: string;
  observacionRecordatorio?: string;
  subtipoControl?: string;
  especialidadesDocente?: EspecialidadInfo[];
}

export const adminService = {
  // Docentes
  listarDocentes: () => api.get<ApiResponse<DocenteResponse[]>>('/admin/docentes'),
  crearDocente: (data: CrearDocenteRequest) => api.post<ApiResponse<DocenteResponse>>('/admin/docentes', data),
  editarDocente: (id: number, data: EditarDocenteAdminRequest) => api.put<ApiResponse<DocenteResponse>>(`/admin/docentes/${id}`, data),
  desactivarDocente: (id: number) => api.delete<ApiResponse<void>>(`/admin/docentes/${id}`),
  eliminarDocente: (id: number) => api.delete<ApiResponse<void>>(`/admin/docentes/${id}/permanente`),
  reactivarDocente: (id: number) => api.patch<ApiResponse<void>>(`/admin/docentes/${id}/reactivar`),

  // Estudiantes
  listarEstudiantes: () => api.get<ApiResponse<EstudianteResponse[]>>('/admin/estudiantes'),
  crearEstudiante: (data: CrearEstudianteRequest) => api.post<ApiResponse<EstudianteResponse>>('/admin/estudiantes', data),
  editarEstudiante: (id: number, data: CrearEstudianteRequest) => api.put<ApiResponse<EstudianteResponse>>(`/admin/estudiantes/${id}`, data),
  desactivarEstudiante: (id: number) => api.delete<ApiResponse<void>>(`/admin/estudiantes/${id}`),
  eliminarEstudiante: (id: number) => api.delete<ApiResponse<void>>(`/admin/estudiantes/${id}/permanente`),
  reactivarEstudiante: (id: number) => api.patch<ApiResponse<void>>(`/admin/estudiantes/${id}/reactivar`),

  // Pacientes
  listarPacientes: () => api.get<ApiResponse<PacienteResponse[]>>('/admin/pacientes'),
  crearPaciente: (data: CrearPacienteRequest) => api.post<ApiResponse<PacienteResponse>>('/admin/pacientes', data),
  editarPaciente: (id: number, data: EditarPacienteRequest) => api.put<ApiResponse<PacienteResponse>>(`/admin/pacientes/${id}`, data),
  desactivarPaciente: (id: number) => api.delete<ApiResponse<void>>(`/admin/pacientes/${id}`),
  eliminarPaciente: (id: number) => api.delete<ApiResponse<void>>(`/admin/pacientes/${id}/permanente`),
  reactivarPaciente: (id: number) => api.patch<ApiResponse<void>>(`/admin/pacientes/${id}/reactivar`),
  resetearContrasenaPaciente: (id: number, nuevaContrasena: string) =>
    api.patch<ApiResponse<void>>(`/admin/pacientes/${id}/contrasena`, { nuevaContrasena }),

  // Citas
  listarCitas: () => api.get<ApiResponse<CitaResponse[]>>('/admin/citas'),
  historialPaciente: (idPaciente: number) =>
    api.get<ApiResponse<CitaResponse[]>>(`/admin/citas/paciente/${idPaciente}`),

  // Reportes — agregados en el backend, no cargan todas las citas al navegador
  reporteIndicadores: () => api.get<ApiResponse<IndicadoresResponse>>('/admin/citas/reportes/indicadores'),
  reporteDistribucion: () => api.get<ApiResponse<DistribucionEstudianteResponse[]>>('/admin/citas/reportes/distribucion'),
  reporteOcupacion: () => api.get<ApiResponse<CitaResponse[]>>('/admin/citas/reportes/ocupacion'),
  reporteInasistencias: () => api.get<ApiResponse<CitaResponse[]>>('/admin/citas/reportes/inasistencias'),

  // Metas de pacientes por semestre (configurables)
  listarMetasSemestre: () => api.get<ApiResponse<MetaSemestreResponse[]>>('/admin/citas/reportes/metas-semestre'),
  actualizarMetaSemestre: (semestre: number, minimoPacientes: number) =>
    api.put<ApiResponse<MetaSemestreResponse>>(`/admin/citas/reportes/metas-semestre/${semestre}`, { minimoPacientes }),

  avanzarSemestre: (idsEstudiantes: number[]) =>
    api.post<ApiResponse<number>>('/admin/citas/reportes/avance-semestre', { idsEstudiantes }),
};

export interface IndicadoresResponse {
  porEstado: Record<string, number>;
  consultoriosUsados: number;
  estudiantesActivos: number;
  total: number;
}

export interface DistribucionEstudianteResponse {
  idEstudiante: number;
  nombre: string;
  semestre: number;
  completadas: number;
}

export interface MetaSemestreResponse {
  semestre: number;
  minimoPacientes: number;
}
