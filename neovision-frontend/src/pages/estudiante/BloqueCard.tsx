import type { ReactNode } from 'react';
import type { CitaResponse } from '../../api/citaService';
import { EstadoBadge } from '../../components/ui';

interface Props {
  cita: CitaResponse;
  acciones?: ReactNode;
}

/** Tarjeta de un bloque/cita del estudiante. Mismo patrón visual que las tarjetas de citas del docente. */
export default function BloqueCard({ cita: c, acciones }: Props) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1 min-w-0">
          <div className="flex items-center gap-3 flex-wrap">
            <EstadoBadge estado={c.estado} />
            <span className="font-medium text-slate-800">{c.fechaSesion} | {c.horaInicio} - {c.horaFin}</span>
          </div>
          <div className="text-sm text-slate-500">
            {c.nombreEspacio ?? 'Sin espacio'} / {c.nombreSalon ?? 'Sin salón'} · Docente: {c.nombreDocente}
          </div>
          <div className="text-sm text-slate-600">
            Paciente: {c.nombrePaciente || <span className="italic text-slate-400">Sin asignar</span>}
          </div>
          {c.nombreEstudiante2 && (
            <div className="text-sm text-slate-600">
              Atienden: {c.nombreEstudiante} y {c.nombreEstudiante2}
            </div>
          )}
          {c.observaciones && <div className="text-sm text-slate-400 italic">"{c.observaciones}"</div>}
          {c.fechaReserva && (
            <div className="text-xs text-slate-400">Reservada: {c.fechaReserva.replace('T', ' ').substring(0, 16)}</div>
          )}
        </div>
        {acciones && <div className="flex flex-col items-end gap-2 shrink-0">{acciones}</div>}
      </div>
    </div>
  );
}
