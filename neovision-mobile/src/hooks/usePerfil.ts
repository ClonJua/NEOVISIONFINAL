import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { perfilService } from '../api/perfilService';
import type { Rol } from '../types';
import type { EditarPerfilRequest, Perfil } from '../types/perfil';

type RolNoAdmin = Exclude<Rol, 'ROLE_ADMIN'>;

const key = (rol: string) => ['perfil', rol] as const;

export function usePerfil(rol: RolNoAdmin) {
  return useQuery({
    queryKey: key(rol),
    queryFn: async () => (await perfilService.obtener(rol)).data.data,
  });
}

export function useEditarPerfil(rol: RolNoAdmin) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: EditarPerfilRequest) => perfilService.editar(rol, data),
    onSuccess: (res) => { qc.setQueryData<Perfil>(key(rol), res.data.data); },
  });
}
