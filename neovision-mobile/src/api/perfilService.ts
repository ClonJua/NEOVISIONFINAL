import api from './axios';
import type { ApiResponse, Rol } from '../types';
import type { EditarPerfilRequest, Perfil } from '../types/perfil';

const PATH: Record<Exclude<Rol, 'ROLE_ADMIN'>, string> = {
  ROLE_PACIENTE:   '/paciente/perfil',
  ROLE_ESTUDIANTE: '/estudiante/perfil',
  ROLE_DOCENTE:    '/docente/perfil',
};

export const perfilService = {
  obtener: (rol: Exclude<Rol, 'ROLE_ADMIN'>) =>
    api.get<ApiResponse<Perfil>>(PATH[rol]),

  editar: (rol: Exclude<Rol, 'ROLE_ADMIN'>, data: EditarPerfilRequest) =>
    api.put<ApiResponse<Perfil>>(PATH[rol], data),
};
