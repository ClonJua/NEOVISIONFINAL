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
    mutationFn: ({ idCita, motivo }: { idCita: number; motivo: string }) =>
      citaService.cancelarPaciente(idCita, motivo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.paciente.disponibles });
      qc.invalidateQueries({ queryKey: qk.paciente.misCitas });
    },
  });
}

export function useReprogramar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ idCita, idCitaNueva }: { idCita: number; idCitaNueva: number }) =>
      citaService.reprogramarPaciente(idCita, idCitaNueva),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.paciente.disponibles });
      qc.invalidateQueries({ queryKey: qk.paciente.misCitas });
    },
  });
}
