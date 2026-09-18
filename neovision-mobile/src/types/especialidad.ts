export interface EspecialidadResponse {
  idEspecialidad: number;
  nombre: string;
  observacionRecordatorio: string | null;
  requiereSubtipo: boolean;
  esDefault: boolean;
  activo: boolean;
}

export const SUBTIPOS_CONTROL = [
  'lentes de contacto',
  'funcional',
  'protesis',
  'prueba ambulatoria',
  'terapia',
] as const;

export type SubtipoControl = typeof SUBTIPOS_CONTROL[number];
