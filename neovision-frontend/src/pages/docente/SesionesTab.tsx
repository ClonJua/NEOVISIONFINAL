import { useMemo, useState } from 'react';
import type { SesionResponse } from '../../api/sesionService';
import { EmptyState, SearchIcon, ClockIcon } from '../../components/ui';

interface Props {
  sesiones: SesionResponse[];
}

/** Sesiones de práctica asignadas al docente, separadas en próximas y pasadas. */
export default function SesionesTab({ sesiones }: Props) {
  const [query, setQuery] = useState('');
  const hoy = new Date().toISOString().slice(0, 10);

  const filtradas = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return sesiones;
    return sesiones.filter((s) =>
      `${s.fecha} ${s.nombreEspacio ?? ''} ${s.nombreSalon ?? ''} ${s.estudiantes.map((e) => e.nombre).join(' ')}`
        .toLowerCase().includes(q));
  }, [sesiones, query]);

  const proximas = filtradas.filter((s) => s.fecha >= hoy).sort((a, b) => a.fecha.localeCompare(b.fecha));
  const pasadas = filtradas.filter((s) => s.fecha < hoy).sort((a, b) => b.fecha.localeCompare(a.fecha));

  return (
    <div className="space-y-8">
      <div className="relative max-w-md">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"><SearchIcon size={16} /></span>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar por fecha, salón o estudiante…"
          className="w-full pl-9 pr-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
        />
      </div>

      <div>
        <h3 className="text-sm font-bold uppercase tracking-wide text-slate-500 mb-3">Próximas ({proximas.length})</h3>
        {proximas.length === 0 ? (
          <EmptyState icon={<ClockIcon size={28} />} title="Sin sesiones próximas" description="No tienes sesiones de práctica programadas a futuro." />
        ) : (
          <div className="grid gap-3">{proximas.map((s) => <SesionCard key={s.idSesion} s={s} />)}</div>
        )}
      </div>

      <div>
        <h3 className="text-sm font-bold uppercase tracking-wide text-slate-500 mb-3">Pasadas ({pasadas.length})</h3>
        {pasadas.length === 0 ? (
          <p className="text-sm text-slate-400">Sin historial de sesiones.</p>
        ) : (
          <div className="grid gap-3 opacity-80">{pasadas.map((s) => <SesionCard key={s.idSesion} s={s} />)}</div>
        )}
      </div>
    </div>
  );
}

function SesionCard({ s }: { s: SesionResponse }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5">
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="font-semibold text-slate-800 text-lg">{s.fecha}</div>
          <div className="text-sm text-slate-500">{s.horaInicio ?? '—'} - {s.horaFin ?? '—'}</div>
        </div>
        <div className="text-right">
          <div className="text-sm font-medium text-slate-700">{s.nombreEspacio ?? 'Sin espacio'} / {s.nombreSalon ?? 'Sin salón'}</div>
          <div className="text-xs text-slate-400">Cupos: {s.cuposTotales}</div>
        </div>
      </div>

      {s.estudiantes.length > 0 ? (
        <div>
          <div className="text-sm font-medium text-slate-600 mb-2">Estudiantes asignados:</div>
          <div className="flex flex-wrap gap-2">
            {s.estudiantes.map((e) => (
              <span key={e.idEstudiante} className="bg-blue-50 text-blue-700 text-xs px-3 py-1 rounded-full border border-blue-200">
                {e.nombre}
              </span>
            ))}
          </div>
        </div>
      ) : (
        <p className="text-xs text-slate-400">Sin estudiantes asignados.</p>
      )}
    </div>
  );
}
