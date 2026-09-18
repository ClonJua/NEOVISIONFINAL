export type Rol = 'ROLE_ADMIN' | 'ROLE_DOCENTE' | 'ROLE_ESTUDIANTE' | 'ROLE_PACIENTE';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthData {
  token:  string;
  rol:    Rol;
  correo: string;
}
