import { useEffect, useState } from 'react';
import { usuarioService } from '../../api/usuarioService';
import { Button, useToast } from '../../components/ui';
import { DiagramaCanvas } from './DiagramaCanvas';

/**
 * Tarjeta de perfil para registrar la firma y sello del usuario (estudiante/docente)
 * una sola vez. El sistema la aplica automáticamente al firmar atenciones.
 */
export function FirmaPerfilCard() {
  const toast = useToast();
  const [firma, setFirma] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const r = await usuarioService.obtenerFirma();
        setFirma(r.data.data || '');
      } catch { /* sin firma */ } finally { setLoading(false); }
    })();
  }, []);

  const guardar = async () => {
    setSaving(true);
    try {
      await usuarioService.guardarFirma(firma);
      toast.success('Firma guardada');
    } catch {
      toast.error('No se pudo guardar la firma');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-5">
      <h3 className="text-sm font-semibold text-slate-700 mb-1">Firma y sello</h3>
      <p className="text-xs text-slate-500 mb-3">
        Se aplicará automáticamente cuando firmes una historia clínica. Dibújala una vez aquí.
      </p>
      {loading ? (
        <p className="text-sm text-slate-400">Cargando…</p>
      ) : (
        <div className="max-w-sm">
          <DiagramaCanvas label="Tu firma y sello" value={firma} onChange={setFirma} />
          <div className="mt-2">
            <Button size="sm" loading={saving} onClick={guardar}>Guardar firma</Button>
          </div>
        </div>
      )}
    </div>
  );
}
