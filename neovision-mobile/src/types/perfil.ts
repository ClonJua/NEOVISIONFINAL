import type { EspecialidadResponse } from './especialidad';

export interface PerfilPaciente {
  idPaciente: number;
  nombre: string;
  apellido: string;
  correo: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
  activo: boolean;
}

export interface PerfilEstudiante {
  idEstudiante: number;
  nombre: string;
  apellido: string;
  correo: string;
  cedula: string;
  semestre: number;
  telefono: string;
  activo: boolean;
}

export interface PerfilDocente {
  idDocente: number;
  nombre: string;
  apellido: string;
  correo: string;
  cedula: string;
  telefono: string;
  activo: boolean;
  especialidades: EspecialidadResponse[];
}

export type Perfil = PerfilPaciente | PerfilEstudiante | PerfilDocente;

export interface EditarPerfilRequest {
  nombre: string;
  apellido: string;
  telefono: string;
}

export interface ProgresoEstudiante {
  semestre: number;
  atendidos: number;
  meta: number;
}
