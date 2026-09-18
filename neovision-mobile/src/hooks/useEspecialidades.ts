import { useQuery } from '@tanstack/react-query';
import { especialidadService } from '../api/especialidadService';

export function useEspecialidades() {
  return useQuery({
    queryKey: ['especialidades'],
    queryFn: async () => (await especialidadService.listarActivas()).data.data,
    staleTime: 1000 * 60 * 10, // catalogo cambia poco
  });
}
