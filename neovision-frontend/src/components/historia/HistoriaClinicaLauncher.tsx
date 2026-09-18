import { useEffect, useState } from 'react';
import { historiaClinicaService, type AtencionResponse } from '../../api/historiaClinicaService';
import { useToast } from '../../components/ui';
import { AtencionDetailModal } from './AtencionDetailModal';
import { AtencionForm } from './AtencionForm';

interface Props {
  idCita: number;
  idPaciente: number;
  nombrePaciente: string;
  onClose: () => void;
}

type Phase = 'loading' | 'form' | 'view';

/**
 * Orquesta el flujo del estudiante desde una cita: abre la historia si no existe
 * (el consentimiento de datos ya se capturó al crear la cuenta del paciente) →
 * abre/retoma la atención → muestra el formulario editable (BORRADOR/DEVUELTA) o
 * la vista de solo lectura (en revisión/finalizada).
 */
export function HistoriaClinicaLauncher({ idCita, idPaciente, nombrePaciente, onClose }: Props) {
  const toast = useToast();
  const [phase, setPhase] = useState<Phase>('loading');
  const [atencion, setAtencion] = useState<AtencionResponse | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const res = await historiaClinicaService.obtenerPorPaciente(idPaciente);
        if (res.data.data.idHistoria == null) {
          await historiaClinicaService.abrirHistoria(idPaciente, {});
        }
        const at = (await historiaClinicaService.abrirAtencion({ idCita })).data.data;
        setAtencion(at);
        setPhase(at.estado === 'BORRADOR' || at.estado === 'DEVUELTA' ? 'form' : 'view');
      } catch (e: any) {
        toast.error(e.response?.data?.message || 'No se pudo abrir la historia clínica');
        onClose();
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (phase === 'loading') {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
        <div className="bg-white rounded-xl px-6 py-4 text-slate-500">Cargando historia clínica de {nombrePaciente}…</div>
      </div>
    );
  }

  if (phase === 'form' && atencion) {
    return <AtencionForm atencion={atencion} onClose={onClose} onSaved={() => { /* refresco implícito al reabrir */ }} />;
  }

  if (phase === 'view' && atencion) {
    return <AtencionDetailModal idAtencion={atencion.idAtencion} open onClose={onClose} />;
  }

  return null;
}
