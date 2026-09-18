import { useMemo } from 'react';
import type { SesionResponse } from '../../api/sesionService';
import type { CitaResponse } from '../../api/citaService';
import { StatCard } from '../../components/ui';
import { CalendarIcon, ClockIcon, ClipboardListIcon, UsersIcon, AlertCircleIcon } from '../../components/ui';

interface Props {
  sesiones: SesionResponse[];
  citas: CitaResponse[];
  pendientesRevision: number;
  onIrABandeja: () => void;
}

/** Resumen del docente: KPIs rápidos de su carga de trabajo y accesos directos. */
export default function ResumenTab({ sesiones, citas, pendientesRevision, onIrABandeja }: Props) {
  const hoy = new Date().toISOString().slice(0, 10);

  const stats = useMemo(() => {
    const sesionesActivas = sesiones.filter((s) => s.activo).length;
    const citasHoy = citas.filter((c) => c.fechaSesion === hoy).length;
    const pacientesDistintos = new Set(citas.filter((c) => c.idPaciente != null).map((c) => c.idPaciente)).size;
    const estudiantesDistintos = new Set(sesiones.flatMap((s) => s.estudiantes.map((e) => e.idEstudiante))).size;
    return { sesionesActivas, citasHoy, pacientesDistintos, estudiantesDistintos };
  }, [sesiones, citas, hoy]);

  const proximaSesion = useMemo(() => {
    return sesiones
      .filter((s) => s.activo && s.fecha >= hoy)
      .sort((a, b) => a.fecha.localeCompare(b.fecha))[0] ?? null;
  }, [sesiones, hoy]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Sesiones activas" value={stats.sesionesActivas} icon={<ClockIcon size={20} />} tone="blue" />
        <StatCard label="Citas hoy" value={stats.citasHoy} icon={<CalendarIcon size={20} />} tone="indigo" />
        <StatCard label="Estudiantes a cargo" value={stats.estudiantesDistintos} icon={<UsersIcon size={20} />} tone="slate" />
        <StatCard label="Pacientes atendidos" value={stats.pacientesDistintos} icon={<ClipboardListIcon size={20} />} tone="emerald" />
      </div>

      {pendientesRevision > 0 && (
        <button
          onClick={onIrABandeja}
          className="w-full text-left bg-amber-50 border border-amber-200 rounded-xl p-5 flex items-center gap-4 hover:bg-amber-100 transition"
        >
          <div className="w-11 h-11 rounded-lg bg-amber-100 text-amber-600 flex items-center justify-center shrink-0">
            <AlertCircleIcon size={20} />
          </div>
          <div className="flex-1">
            <div className="font-semibold text-amber-800">
              Tienes {pendientesRevision} atenci{pendientesRevision === 1 ? 'ón' : 'ones'} pendiente{pendientesRevision === 1 ? '' : 's'} de revisión
            </div>
            <div className="text-sm text-amber-700">Revisa, devuelve o firma las historias clínicas enviadas por tus estudiantes.</div>
          </div>
          <span className="text-amber-400 text-lg">›</span>
        </button>
      )}

      <div className="bg-white border border-slate-200 rounded-xl p-5">
        <h3 className="text-sm font-bold text-slate-700 mb-3">Próxima sesión</h3>
        {proximaSesion ? (
          <div className="flex items-start justify-between">
            <div>
              <div className="font-semibold text-slate-800">{proximaSesion.fecha}</div>
              <div className="text-sm text-slate-500">
                {proximaSesion.horaInicio ?? '—'} - {proximaSesion.horaFin ?? '—'} · {proximaSesion.nombreEspacio ?? 'Sin espacio'} / {proximaSesion.nombreSalon ?? 'Sin salón'}
              </div>
            </div>
            <div className="text-right text-xs text-slate-400">
              {proximaSesion.estudiantes.length} estudiante(s) asignado(s)
            </div>
          </div>
        ) : (
          <p className="text-sm text-slate-400">No tienes sesiones próximas programadas.</p>
        )}
      </div>
    </div>
  );
}
