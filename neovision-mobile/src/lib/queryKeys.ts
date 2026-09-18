/** Claves de cache centralizadas para React Query. */
export const qk = {
  estudiante: {
    bloques:   ['estudiante', 'bloques'] as const,
    pacientes: ['estudiante', 'pacientes'] as const,
  },
  paciente: {
    disponibles: ['paciente', 'disponibles'] as const,
    misCitas:    ['paciente', 'misCitas'] as const,
  },
  docente: {
    sesiones: ['docente', 'sesiones'] as const,
    citas:    ['docente', 'citas'] as const,
  },
} as const;
