import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { citaService, type ReservarBloqueRequest } from '../api/citaService';
import { qk } from '../lib/queryKeys';

export function useBloquesDisponibles(idEspecialidad?: number) {
  return useQuery({
    queryKey: [...qk.paciente.disponibles, idEspecialidad ?? 'todas'],
    queryFn: async () => (await citaService.bloquesDisponibles(idEspecialidad)).data.data,
  });
}

export function useMisCitas() {
  return useQuery({
    queryKey: qk.paciente.misCitas,
    queryFn: async () => (await citaService.misCitas()).data.data,
  });
}

export function useReservar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (args: { idCita: number; body?: ReservarBloqueRequest }) =>
      citaService.reservarBloque(args.idCita, args.body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.paciente.disponibles });
      qc.invalidateQueries({ queryKey: qk.paciente.misCitas });
    },
  });
}

export function useCancelar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (idCita: number) => citaService.cancelarPaciente(idCita),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.paciente.disponibles });
      qc.invalidateQueries({ queryKey: qk.paciente.misCitas });
    },
  });
}
