import api from './axios';
import type { ApiResponse } from '../types';
import type { CitaResponse } from '../types/cita';
import type { SesionResponse } from '../types/sesion';

export const docenteService = {
  misSesiones: () =>
    api.get<ApiResponse<SesionResponse[]>>('/docente/sesiones'),

  misCitas: () =>
    api.get<ApiResponse<CitaResponse[]>>('/docente/citas'),

  confirmarAsistencia: (idCita: number) =>
    api.patch<ApiResponse<CitaResponse>>(`/docente/citas/${idCita}/confirmar`),
};
