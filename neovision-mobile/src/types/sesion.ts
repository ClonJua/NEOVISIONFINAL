export interface EstudianteInfo {
  idEstudiante: number;
  nombre: string;
}

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
  estudiantes: EstudianteInfo[];
}
