import { useState } from 'react';
import { estudianteService, type EditarEstudianteRequest } from '../../api/estudianteService';
import type { EstudianteResponse } from '../../api/adminService';
import { FirmaPerfilCard } from '../../components/historia/FirmaPerfilCard';
import { Button, useToast } from '../../components/ui';

interface Props {
  perfil: EstudianteResponse | null;
  onActualizado: () => void;
}

/** Datos de perfil del estudiante y su firma/sello registrados. */
export default function PerfilTab({ perfil, onActualizado }: Props) {
  const toast = useToast();
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState<EditarEstudianteRequest>({
    nombre: perfil?.nombre ?? '',
    apellido: perfil?.apellido ?? '',
    telefono: perfil?.telefono ?? '',
  });

  const guardar = async () => {
    setLoading(true);
    try {
      await estudianteService.editarPerfil(form);
      toast.success('Perfil actualizado exitosamente');
      onActualizado();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Error al actualizar perfil');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-2xl">
      <div className="bg-white border border-slate-200 rounded-xl p-5">
        <h3 className="text-sm font-semibold text-slate-700 mb-4">Datos personales</h3>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Nombre</label>
            <input type="text" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Apellido</label>
            <input type="text" value={form.apellido} onChange={(e) => setForm({ ...form, apellido: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
        </div>
        <div className="mb-4">
          <label className="block text-xs font-medium text-slate-600 mb-1">Teléfono</label>
          <input type="text" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        {perfil?.correo && (
          <p className="text-xs text-slate-400 mb-4">Correo: {perfil.correo} (no editable)</p>
        )}
        {perfil?.semestre != null && (
          <p className="text-xs text-slate-400 mb-4">Semestre: {perfil.semestre}</p>
        )}
        <Button loading={loading} onClick={guardar}>Guardar cambios</Button>
      </div>

      <FirmaPerfilCard />
    </div>
  );
}
