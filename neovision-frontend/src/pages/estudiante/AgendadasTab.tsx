import { useMemo, useState } from 'react';
import { type CitaResponse } from '../../api/citaService';
import { EmptyState, SearchIcon, ClipboardListIcon } from '../../components/ui';
import BloqueCard from './BloqueCard';

interface Props {
  bloques: CitaResponse[];
  onRefrescar: () => void;
  onVerHistoria: (c: CitaResponse) => void;
}

/** Citas ya reservadas: abrir la historia clínica del paciente. La asistencia la confirma el docente. */
export default function AgendadasTab({ bloques, onVerHistoria }: Props) {
  const [query, setQuery] = useState('');

  const filtrados = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return bloques;
    return bloques.filter((b) =>
      `${b.fechaSesion} ${b.nombreEspacio ?? ''} ${b.nombreSalon ?? ''} ${b.nombreDocente} ${b.nombrePaciente ?? ''}`.toLowerCase().includes(q));
  }, [bloques, query]);

  return (
    <div className="space-y-5">
      <div className="relative max-w-md">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"><SearchIcon size={16} /></span>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar por fecha, docente o paciente…"
          className="w-full pl-9 pr-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
        />
      </div>

      {filtrados.length === 0 ? (
        <EmptyState icon={<ClipboardListIcon size={28} />} title="Sin citas agendadas" description="No tienes citas agendadas en este momento." />
      ) : (
        <div className="space-y-3">
          {filtrados.map((c) => (
            <BloqueCard key={c.idCita} cita={c} acciones={
              c.idPaciente && (
                <button onClick={() => onVerHistoria(c)}
                  className="bg-bosque hover:bg-bosque-dark text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                  Historia clínica
                </button>
              )
            } />
          ))}
        </div>
      )}
    </div>
  );
}
