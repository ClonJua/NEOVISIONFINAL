import { useEffect, useState } from 'react';
import { solicitudService, type SolicitudResponse, type EstadoSolicitud } from '../../api/solicitudService';
import { Badge, Button, useToast } from '../../components/ui';

const TONO: Record<EstadoSolicitud, 'amber' | 'emerald' | 'red'> = {
  PENDIENTE: 'amber', APROBADA: 'emerald', RECHAZADA: 'red',
};
const LABEL: Record<EstadoSolicitud, string> = {
  PENDIENTE: 'Pendiente', APROBADA: 'Aprobada · enviada a tu correo', RECHAZADA: 'Rechazada',
};

/** Tarjeta para que el paciente solicite una copia de su historia clínica. */
export function SolicitudCopiaCard() {
  const toast = useToast();
  const [solicitudes, setSolicitudes] = useState<SolicitudResponse[]>([]);
  const [loading, setLoading] = useState(false);

  const cargar = async () => {
    try {
      const r = await solicitudService.mias();
      setSolicitudes(r.data.data);
    } catch { /* noop */ }
  };
  useEffect(() => { cargar(); }, []);

  const solicitar = async () => {
    setLoading(true);
    try {
      await solicitudService.crear();
      toast.success('Solicitud enviada. La secretaría la revisará.');
      cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo crear la solicitud');
    } finally {
      setLoading(false);
    }
  };

  const hayPendiente = solicitudes.some(s => s.estado === 'PENDIENTE');

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-5">
      <h3 className="text-base font-semibold text-slate-800">Copia de mi historia clínica</h3>
      <p className="text-sm text-slate-500 mt-1 mb-3">
        Puedes solicitar una copia de tu historia clínica. Si se aprueba, se enviará a tu correo registrado.
      </p>
      <Button onClick={solicitar} loading={loading} disabled={hayPendiente}>
        {hayPendiente ? 'Ya tienes una solicitud pendiente' : 'Solicitar copia'}
      </Button>

      {solicitudes.length > 0 && (
        <ul className="mt-4 space-y-2">
          {solicitudes.map(s => (
            <li key={s.idSolicitud} className="flex items-center gap-3 text-sm border-t border-slate-100 pt-2">
              <span className="text-slate-500">{new Date(s.fechaSolicitud).toLocaleDateString()}</span>
              <Badge tone={TONO[s.estado]}>{LABEL[s.estado]}</Badge>
              {s.estado === 'RECHAZADA' && s.motivoRechazo && (
                <span className="text-xs text-red-600">Motivo: {s.motivoRechazo}</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
