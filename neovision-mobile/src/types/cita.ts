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
  // Cancelación: quién y cuándo (null si no está cancelada o es anterior al registro)
  canceladaPor?: 'PACIENTE' | 'ESTUDIANTE' | 'ADMIN' | null;
  fechaCancelacion?: string | null;
  /** Reprogramaciones que le quedan al paciente sobre esta reserva. */
  reprogramacionesRestantes?: number;
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

const AUTOR_CANCELACION: Record<string, string> = {
  PACIENTE: 'el paciente',
  ESTUDIANTE: 'el estudiante',
  ADMIN: 'la administración',
};

/** "Cancelada por el paciente el 2026-10-08 14:30", o null si no aplica. */
export const textoCancelacion = (c: Pick<CitaResponse, 'estado' | 'canceladaPor' | 'fechaCancelacion'>) => {
  if (c.estado !== 'CANCELADO' || !c.canceladaPor) return null;
  const quien = AUTOR_CANCELACION[c.canceladaPor] ?? c.canceladaPor;
  const cuando = c.fechaCancelacion ? ` el ${c.fechaCancelacion.replace('T', ' ').substring(0, 16)}` : '';
  return `Cancelada por ${quien}${cuando}`;
};
