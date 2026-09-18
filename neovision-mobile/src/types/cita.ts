export type EstadoCita = 'DISPONIBLE' | 'RESERVADO' | 'COMPLETADO' | 'CANCELADO' | 'VENCIDA';

export interface EspecialidadInfo {
  idEspecialidad: number;
  nombreEspecialidad: string;
}

export interface CitaResponse {
  idCita: number;
  estado: EstadoCita;
  horaInicio: string;
  horaFin: string;
  observaciones: string | null;
  fechaReserva: string | null;

  idSesion: number;
  fechaSesion: string;
  nombreSalon: string | null;
  nombreEspacio: string | null;
  nombreDocente: string;

  idEstudiante: number;
  nombreEstudiante: string;
  /** Segundo estudiante del bloque (los pacientes se atienden normalmente en pareja). */
  idEstudiante2?: number | null;
  nombreEstudiante2?: string | null;

  idPaciente: number | null;
  nombrePaciente: string | null;

  idEspecialidad?: number | null;
  nombreEspecialidad?: string | null;
  observacionRecordatorio?: string | null;
  subtipoControl?: string | null;
  especialidadesDocente?: EspecialidadInfo[];
}

export interface PacienteResponse {
  idPaciente: number;
  nombre: string;
  apellido: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
  /** null si el paciente se registro sin correo (no tiene cuenta de acceso). */
  correo: string | null;
  activo: boolean;
}
