import type { AtencionResumenResponse } from '../../api/historiaClinicaService';
import { EstadoAtencionBadge } from './estadoBadge';

interface Props {
  atenciones: AtencionResumenResponse[];
  onOpen: (idAtencion: number) => void;
  emptyText?: string;
}

/** Línea de tiempo (secuencial) de las atenciones de una historia. */
export function AtencionesTimeline({ atenciones, onOpen, emptyText = 'Sin atenciones registradas.' }: Props) {
  if (!atenciones.length) {
    return <p className="text-sm text-slate-400 py-6 text-center">{emptyText}</p>;
  }
  return (
    <ul className="space-y-2">
      {atenciones.map((a) => (
        <li key={a.idAtencion}>
          <button
            onClick={() => onOpen(a.idAtencion)}
            className="w-full text-left bg-white border border-slate-200 rounded-xl px-4 py-3 hover:border-blue-300 hover:shadow-sm transition flex items-center gap-3"
          >
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-slate-800">
                  {new Date(a.fechaAtencion).toLocaleDateString()}
                </span>
                <EstadoAtencionBadge estado={a.estado} />
              </div>
              <p className="text-xs text-slate-500 truncate">
                {a.motivoConsulta || 'Sin motivo'} · {a.numDiagnosticos} diagnóstico(s)
              </p>
              <p className="text-[11px] text-slate-400 truncate">
                Estudiante: {a.nombreEstudiante ?? '—'} · Docente: {a.nombreDocente ?? '—'}
              </p>
            </div>
            <span className="text-slate-300 text-lg">›</span>
          </button>
        </li>
      ))}
    </ul>
  );
}
