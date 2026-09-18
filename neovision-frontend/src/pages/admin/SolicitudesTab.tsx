import { useEffect, useState } from 'react';
import { solicitudService, type SolicitudResponse, type EstadoSolicitud } from '../../api/solicitudService';
import { Badge, Button, Modal, useToast } from '../../components/ui';

const TONO: Record<EstadoSolicitud, 'amber' | 'emerald' | 'red'> = {
  PENDIENTE: 'amber', APROBADA: 'emerald', RECHAZADA: 'red',
};

export default function SolicitudesTab() {
  const toast = useToast();
  const [solicitudes, setSolicitudes] = useState<SolicitudResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [accion, setAccion] = useState<number | null>(null);
  const [rechazoId, setRechazoId] = useState<number | null>(null);
  const [motivo, setMotivo] = useState('');

  const cargar = async () => {
    setLoading(true);
    try {
      const r = await solicitudService.listar();
      setSolicitudes(r.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudieron cargar las solicitudes');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { cargar(); }, []);

  const aprobar = async (id: number) => {
    setAccion(id);
    try {
      await solicitudService.aprobar(id);
      toast.success('Aprobada. La historia se envió al correo del paciente.');
      cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo aprobar');
    } finally {
      setAccion(null);
    }
  };

  const rechazar = async () => {
    if (rechazoId == null || !motivo.trim()) { toast.error('Indica el motivo'); return; }
    setAccion(rechazoId);
    try {
      await solicitudService.rechazar(rechazoId, motivo.trim());
      toast.success('Solicitud rechazada');
      setRechazoId(null); setMotivo('');
      cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo rechazar');
    } finally {
      setAccion(null);
    }
  };

  if (loading) return <p className="text-slate-400">Cargando…</p>;
  if (solicitudes.length === 0) return <p className="text-slate-400">No hay solicitudes.</p>;

  return (
    <div>
      <div className="grid gap-2">
        {solicitudes.map(s => (
          <div key={s.idSolicitud} className="bg-white border border-slate-200 rounded-xl px-4 py-3 flex items-center gap-3">
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-slate-800">{s.nombrePaciente}</span>
                <Badge tone={TONO[s.estado]}>{s.estado}</Badge>
              </div>
              <div className="text-xs text-slate-500">
                {new Date(s.fechaSolicitud).toLocaleString()} · {s.correoPaciente}
                {s.motivo ? ` · "${s.motivo}"` : ''}
              </div>
              {s.estado === 'RECHAZADA' && s.motivoRechazo && (
                <div className="text-xs text-red-600">Motivo rechazo: {s.motivoRechazo}</div>
              )}
            </div>
            {s.estado === 'PENDIENTE' && (
              <div className="flex gap-2">
                <Button size="sm" variant="danger" onClick={() => { setRechazoId(s.idSolicitud); setMotivo(''); }}>Rechazar</Button>
                <Button size="sm" loading={accion === s.idSolicitud} onClick={() => aprobar(s.idSolicitud)}>Aprobar y enviar</Button>
              </div>
            )}
          </div>
        ))}
      </div>

      <Modal
        open={rechazoId !== null}
        onClose={() => setRechazoId(null)}
        title="Rechazar solicitud"
        footer={
          <>
            <Button variant="secondary" onClick={() => setRechazoId(null)}>Cancelar</Button>
            <Button variant="danger" loading={accion === rechazoId} onClick={rechazar}>Rechazar</Button>
          </>
        }
      >
        <label className="block text-sm font-medium text-slate-700 mb-1">Motivo del rechazo</label>
        <textarea rows={3} value={motivo} onChange={(e) => setMotivo(e.target.value)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
      </Modal>
    </div>
  );
}
