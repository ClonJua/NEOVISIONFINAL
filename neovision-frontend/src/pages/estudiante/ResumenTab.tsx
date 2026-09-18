import { useMemo } from 'react';
import type { CitaResponse } from '../../api/citaService';
import { StatCard, CalendarIcon, ClipboardListIcon, HistoryIcon, AlertCircleIcon } from '../../components/ui';
import ProgresoWidget from '../../components/ProgresoWidget';

interface Props {
  bloques: CitaResponse[];
  progreso: { semestre: number; atendidos: number; meta: number } | null;
  atencionesDevueltas: number;
  onIrAAtenciones: () => void;
}

/** Resumen del estudiante: progreso del semestre, KPIs de sus bloques y próxima cita. */
export default function ResumenTab({ bloques, progreso, atencionesDevueltas, onIrAAtenciones }: Props) {
  const stats = useMemo(() => {
    const disponibles = bloques.filter((b) => b.estado === 'DISPONIBLE').length;
    const agendadas = bloques.filter((b) => b.estado === 'RESERVADO').length;
    const historial = bloques.filter((b) => ['COMPLETADO', 'CANCELADO', 'VENCIDA'].includes(b.estado)).length;
    return { disponibles, agendadas, historial };
  }, [bloques]);

  const proximaCita = useMemo(() => {
    return bloques
      .filter((b) => b.estado === 'RESERVADO')
      .sort((a, b) => (a.fechaSesion + a.horaInicio).localeCompare(b.fechaSesion + b.horaInicio))[0] ?? null;
  }, [bloques]);

  return (
    <div className="space-y-6">
      {progreso && <ProgresoWidget semestre={progreso.semestre} atendidos={progreso.atendidos} meta={progreso.meta} />}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard label="Bloques disponibles" value={stats.disponibles} icon={<CalendarIcon size={20} />} tone="emerald" />
        <StatCard label="Citas agendadas" value={stats.agendadas} icon={<ClipboardListIcon size={20} />} tone="blue" />
        <StatCard label="En historial" value={stats.historial} icon={<HistoryIcon size={20} />} tone="slate" />
      </div>

      {atencionesDevueltas > 0 && (
        <button
          onClick={onIrAAtenciones}
          className="w-full text-left bg-red-50 border border-red-200 rounded-xl p-5 flex items-center gap-4 hover:bg-red-100 transition"
        >
          <div className="w-11 h-11 rounded-lg bg-red-100 text-red-600 flex items-center justify-center shrink-0">
            <AlertCircleIcon size={20} />
          </div>
          <div className="flex-1">
            <div className="font-semibold text-red-800">
              Tienes {atencionesDevueltas} atenci{atencionesDevueltas === 1 ? 'ón devuelta' : 'ones devueltas'} por el docente
            </div>
            <div className="text-sm text-red-700">Corrígela y reenvíala desde la pestaña "Mis atenciones".</div>
          </div>
          <span className="text-red-400 text-lg">›</span>
        </button>
      )}

      <div className="bg-white border border-slate-200 rounded-xl p-5">
        <h3 className="text-sm font-bold text-slate-700 mb-3">Próxima cita</h3>
        {proximaCita ? (
          <div className="flex items-start justify-between">
            <div>
              <div className="font-semibold text-slate-800">{proximaCita.fechaSesion}</div>
              <div className="text-sm text-slate-500">
                {proximaCita.horaInicio ?? '—'} - {proximaCita.horaFin ?? '—'} · {proximaCita.nombreEspacio ?? 'Sin espacio'} / {proximaCita.nombreSalon ?? 'Sin salón'}
              </div>
            </div>
            <div className="text-right text-xs text-slate-400">
              {proximaCita.nombrePaciente ? `Paciente: ${proximaCita.nombrePaciente}` : 'Sin paciente asignado'}
            </div>
          </div>
        ) : (
          <p className="text-sm text-slate-400">No tienes citas agendadas próximamente.</p>
        )}
      </div>
    </div>
  );
}
