/** Extrae un mensaje legible de un error de axios / red / React Query. */
export function errorMessage(e: unknown, fallback = 'Error al cargar datos'): string | null {
  if (!e) return null;
  const anyE = e as any;
  return anyE?.response?.data?.message ?? anyE?.message ?? fallback;
}
