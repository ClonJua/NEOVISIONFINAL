import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { docenteService } from '../api/docenteService';
import { qk } from '../lib/queryKeys';

export function useMisSesiones() {
  return useQuery({
    queryKey: qk.docente.sesiones,
    queryFn: async () => (await docenteService.misSesiones()).data.data,
  });
}

export function useMisCitasDocente() {
  return useQuery({
    queryKey: qk.docente.citas,
    queryFn: async () => (await docenteService.misCitas()).data.data,
  });
}

export function useConfirmarAsistenciaDocente() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (idCita: number) => docenteService.confirmarAsistencia(idCita),
    onSuccess: () => { qc.invalidateQueries({ queryKey: qk.docente.citas }); },
  });
}
