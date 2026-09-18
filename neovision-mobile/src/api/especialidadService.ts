import api from './axios';
import type { ApiResponse } from '../types';
import type { EspecialidadResponse } from '../types/especialidad';

export const especialidadService = {
  listarActivas: () =>
    api.get<ApiResponse<EspecialidadResponse[]>>('/especialidades'),
};
