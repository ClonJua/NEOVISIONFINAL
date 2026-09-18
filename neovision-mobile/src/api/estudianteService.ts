import api from './axios';
import type { ApiResponse } from '../types';
import type { PacienteResponse } from '../types/cita';

export const estudianteService = {
  listarPacientes: () =>
    api.get<ApiResponse<PacienteResponse[]>>('/estudiante/pacientes'),
};
