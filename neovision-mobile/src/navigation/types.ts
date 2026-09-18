import type { CitaResponse } from '../types/cita';

export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
};

export type MainStackParamList = {
  Home: undefined;
  Perfil: undefined;
  Estadisticas: undefined;
  CitaDetail: { cita: CitaResponse };
};
