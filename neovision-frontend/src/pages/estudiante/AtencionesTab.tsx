import type { AtencionResumenResponse } from '../../api/historiaClinicaService';
import { AtencionesTimeline } from '../../components/historia/AtencionesTimeline';

interface Props {
  atenciones: AtencionResumenResponse[];
  atencionesDevueltas: number;
  onOpen: (idAtencion: number) => void;
}

/** Vista general del estado de todas las historias clínicas diligenciadas por el estudiante. */
export default function AtencionesTab({ atenciones, atencionesDevueltas, onOpen }: Props) {
  return (
    <div>
      {atencionesDevueltas > 0 && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          Tienes {atencionesDevueltas} atenci{atencionesDevueltas === 1 ? 'ón devuelta' : 'ones devueltas'} por el docente.
          Ábrela desde la pestaña "Agendadas" para corregirla y reenviarla.
        </div>
      )}
      <AtencionesTimeline
        atenciones={atenciones}
        onOpen={onOpen}
        emptyText="Aún no has diligenciado ninguna atención."
      />
    </div>
  );
}
