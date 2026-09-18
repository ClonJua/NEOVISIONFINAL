import { useMemo, useState } from 'react';
import { nombresEstudiantes, type CitaResponse } from '../../api/citaService';
import { docenteService } from '../../api/docenteService';
import { EmptyState, SearchIcon, ClipboardListIcon, EstadoBadge, useToast } from '../../components/ui';

interface Props {
  citas: CitaResponse[];
  onRefrescar: () => void;
}

const ESTADOS = ['TODAS', 'DISPONIBLE', 'RESERVADO', 'COMPLETADO', 'CANCELADO', 'VENCIDA'] as const;

/** Citas de todas las sesiones del docente, con filtro por estado, búsqueda y confirmación de asistencia. */
export default function CitasTab({ citas, onRefrescar }: Props) {
  const toast = useToast();
  const [estado, setEstado] = useState<(typeof ESTADOS)[number]>('TODAS');
  const [query, setQuery] = useState('');
  const [confirmandoId, setConfirmandoId] = useState<number | null>(null);

  const handleConfirmarAsistencia = async (idCita: number) => {
    setConfirmandoId(idCita);
    try {
      await docenteService.confirmarAsistencia(idCita);
      toast.success('Asistencia confirmada');
      onRefrescar();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Error al confirmar asistencia');
    } finally {
      setConfirmandoId(null);
    }
  };

  const filtradas = useMemo(() => {
    let lista = citas;
    if (estado !== 'TODAS') lista = lista.filter((c) => c.estado === estado);
    const q = query.trim().toLowerCase();
    if (q) {
      lista = lista.filter((c) =>
        `${nombresEstudiantes(c)} ${c.nombrePaciente ?? ''} ${c.nombreEspecialidad ?? ''}`.toLowerCase().includes(q));
    }
    return [...lista].sort((a, b) => (a.fechaSesion + a.horaInicio).localeCompare(b.fechaSesion + b.horaInicio));
  }, [citas, estado, query]);

  const conteos = useMemo(() => {
    const m: Record<string, number> = {};
    for (const c of citas) m[c.estado] = (m[c.estado] ?? 0) + 1;
    return m;
  }, [citas]);

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
        <div className="relative max-w-md flex-1">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"><SearchIcon size={16} /></span>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar por estudiante, paciente o especialidad…"
            className="w-full pl-9 pr-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
          />
        </div>
        <div className="flex flex-wrap gap-1.5">
          {ESTADOS.map((e) => (
            <button
              key={e}
              onClick={() => setEstado(e)}
              className={`text-xs font-medium px-3 py-1.5 rounded-full border transition ${
                estado === e
                  ? 'bg-bosque border-bosque text-white'
                  : 'bg-white border-slate-200 text-slate-600 hover:border-blue-300'
              }`}
            >
              {e === 'TODAS' ? 'Todas' : e} {e !== 'TODAS' && conteos[e] ? `(${conteos[e]})` : ''}
            </button>
          ))}
        </div>
      </div>

      {filtradas.length === 0 ? (
        <EmptyState icon={<ClipboardListIcon size={28} />} title="No hay citas" description="No se encontraron citas con estos filtros." />
      ) : (
        <div className="space-y-3">
          {filtradas.map((c) => (
            <div key={c.idCita} className="bg-white border border-slate-200 rounded-xl p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center gap-3 flex-wrap">
                    <EstadoBadge estado={c.estado} />
                    <span className="font-medium text-slate-800">{c.fechaSesion} | {c.horaInicio} - {c.horaFin}</span>
                    {c.nombreEspecialidad && (
                      <span className="text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full">{c.nombreEspecialidad}</span>
                    )}
                  </div>
                  <div className="text-sm text-slate-500">{c.nombreEspacio ?? 'Sin espacio'} / {c.nombreSalon ?? 'Sin salón'}</div>
                  <div className="text-sm text-slate-600">{c.nombreEstudiante2 ? 'Estudiantes' : 'Estudiante'}: {nombresEstudiantes(c)}</div>
                  {c.nombrePaciente && <div className="text-sm text-slate-600">Paciente: {c.nombrePaciente}</div>}
                  {c.observaciones && <div className="text-sm text-slate-400 italic">"{c.observaciones}"</div>}
                </div>
                {c.estado === 'RESERVADO' && (
                  confirmandoId === c.idCita ? (
                    <span className="text-xs text-slate-400 shrink-0">Confirmando...</span>
                  ) : (
                    <button onClick={() => handleConfirmarAsistencia(c.idCita)}
                      className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-medium px-3 py-1.5 rounded-lg shrink-0">
                      Confirmar asistencia
                    </button>
                  )
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
