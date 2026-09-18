import api from './axios';
import type { ApiResponse, AuthData } from '../types';

export interface LoginRequest {
  correo: string;
  contrasena: string;
}

export interface RegistroRequest {
  nombre: string;
  apellido: string;
  correo: string;
  contrasena: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string;
  telefono: string;
  autorizaTratamientoDatos: boolean;
  representanteNombre?: string;
  representanteDocumento?: string;
  representanteParentesco?: string;
}

export interface MicrosoftLoginRequest {
  accessToken: string;
}

export const authService = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthData>>('/auth/login', data),

  registro: (data: RegistroRequest) =>
    api.post<ApiResponse<AuthData>>('/auth/registro', data),

  loginMicrosoft: (data: MicrosoftLoginRequest) =>
    api.post<ApiResponse<AuthData>>('/auth/microsoft', data),

  logout: () => api.post<ApiResponse<null>>('/auth/logout'),
};
