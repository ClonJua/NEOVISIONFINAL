import api from './axios';
import type { ApiResponse } from './authService';
import type { PacienteResponse, CrearPacienteRequest, EstudianteResponse } from './adminService';

export interface EditarEstudianteRequest {
  nombre: string;
  apellido: string;
  telefono: string;
}

export interface EditarEstudianteAdminRequest {
  nombre: string;
  apellido: string;
  cedula: string;
  semestre: number;
  telefono?: string;
  correo: string;
}

export const estudianteService = {
  listarPacientes: () =>
    api.get<ApiResponse<PacienteResponse[]>>('/estudiante/pacientes'),
  crearPaciente: (data: CrearPacienteRequest) =>
    api.post<ApiResponse<PacienteResponse>>('/estudiante/pacientes', data),

  miPerfil: () =>
    api.get<ApiResponse<EstudianteResponse>>('/estudiante/perfil'),

  editarPerfil: (data: EditarEstudianteRequest) =>
    api.put<ApiResponse<EstudianteResponse>>('/estudiante/perfil', data),

  editarEstudianteAdmin: (idEstudiante: number, data: EditarEstudianteAdminRequest) =>
    api.put<ApiResponse<EstudianteResponse>>(`/admin/estudiantes/${idEstudiante}`, data),
};
