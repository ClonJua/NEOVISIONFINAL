import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { citaService } from '../api/citaService';
import { estudianteService } from '../api/estudianteService';
import { qk } from '../lib/queryKeys';

export function useMisBloques() {
  return useQuery({
    queryKey: qk.estudiante.bloques,
    queryFn: async () => (await citaService.misBloques()).data.data,
  });
}

export function usePacientes() {
  return useQuery({
    queryKey: qk.estudiante.pacientes,
    queryFn: async () => (await estudianteService.listarPacientes()).data.data.filter(p => p.activo),
  });
}

export function useProgresoEstudiante() {
  return useQuery({
    queryKey: ['estudiante', 'progreso'],
    queryFn: async () => (await citaService.progresoEstudiante()).data.data,
  });
}

export function useAsignarPaciente() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ idCita, idPaciente }: { idCita: number; idPaciente: number }) =>
      citaService.asignarPaciente(idCita, idPaciente),
    onSuccess: () => { qc.invalidateQueries({ queryKey: qk.estudiante.bloques }); },
  });
}

