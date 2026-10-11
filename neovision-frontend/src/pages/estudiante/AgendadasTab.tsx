import { useMemo, useState } from 'react';
import { citaService, type CitaResponse } from '../../api/citaService';
import { Button, EmptyState, Modal, Textarea, SearchIcon, ClipboardListIcon, CalendarIcon, XIcon, useToast } from '../../components/ui';
import BloqueCard from './BloqueCard';

interface Props {
  bloques: CitaResponse[];
  /** Bloques propios sin paciente: destinos posibles al reprogramar. */
  disponibles: CitaResponse[];
  onRefrescar: () => void;
  onVerHistoria: (c: CitaResponse) => void;
}

const hoyISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

/** Citas ya reservadas: abrir la historia clínica, reprogramar o cancelar. La asistencia la confirma el docente. */
export default function AgendadasTab({ bloques, disponibles, onRefrescar, onVerHistoria }: Props) {
  const toast = useToast();
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);

  const [citaACancelar, setCitaACancelar] = useState<CitaResponse | null>(null);
  const [motivo, setMotivo] = useState('');

  const [citaAReprogramar, setCitaAReprogramar] = useState<CitaResponse | null>(null);
  const [idDestino, setIdDestino] = useState<number | ''>('');

  const filtrados = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return bloques;
    return bloques.filter((b) =>
      `${b.fechaSesion} ${b.nombreEspacio ?? ''} ${b.nombreSalon ?? ''} ${b.nombreDocente} ${b.nombrePaciente ?? ''}`.toLowerCase().includes(q));
  }, [bloques, query]);

  const destinos = useMemo(() => {
    const hoy = hoyISO();
    return disponibles
      .filter((b) => b.fechaSesion >= hoy)
      .sort((a, b) => `${a.fechaSesion} ${a.horaInicio}`.localeCompare(`${b.fechaSesion} ${b.horaInicio}`));
  }, [disponibles]);

  const cerrarCancelar = () => { setCitaACancelar(null); setMotivo(''); };
  const cerrarReprogramar = () => { setCitaAReprogramar(null); setIdDestino(''); };

  const confirmarCancelacion = async () => {
    if (!citaACancelar) return;
    if (!motivo.trim()) {
      toast.error('Motivo requerido', 'Indica por qué cancelas la cita; se le enviará al paciente.');
      return;
    }
    setLoading(true);
    try {
      await citaService.cancelarEstudiante(citaACancelar.idCita, motivo.trim());
      toast.success('Cita cancelada', 'Se notificó al paciente y el bloque quedó disponible.');
      cerrarCancelar();
      onRefrescar();
    } catch (err: any) {
      toast.error('No se pudo cancelar', err.response?.data?.message || 'Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const confirmarReprogramacion = async () => {
    if (!citaAReprogramar || idDestino === '') return;
    setLoading(true);
    try {
      await citaService.reprogramarEstudiante(citaAReprogramar.idCita, idDestino);
      toast.success('Cita reprogramada', 'Se notificó al paciente del nuevo horario.');
      cerrarReprogramar();
      onRefrescar();
    } catch (err: any) {
      toast.error('No se pudo reprogramar', err.response?.data?.message || 'Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

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
                <>
                  <button onClick={() => onVerHistoria(c)}
                    className="bg-bosque hover:bg-bosque-dark text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                    Historia clínica
                  </button>
                  <div className="flex gap-2">
                    <button onClick={() => setCitaAReprogramar(c)}
                      className="border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-medium px-3 py-1.5 rounded-lg">
                      Reprogramar
                    </button>
                    <button onClick={() => setCitaACancelar(c)}
                      className="border border-red-200 hover:bg-red-50 text-red-600 text-xs font-medium px-3 py-1.5 rounded-lg">
                      Cancelar
                    </button>
                  </div>
                </>
              )
            } />
          ))}
        </div>
      )}

      {/* Modal reprogramar */}
      <Modal
        open={!!citaAReprogramar}
        onClose={cerrarReprogramar}
        title="Reprogramar cita"
        description="Mueve al paciente a otro de tus bloques libres. El bloque actual quedará disponible."
        footer={
          <>
            <Button variant="ghost" onClick={cerrarReprogramar}>Volver</Button>
            <Button loading={loading} disabled={idDestino === ''} leftIcon={<CalendarIcon size={15} />} onClick={confirmarReprogramacion}>
              Reprogramar
            </Button>
          </>
        }
      >
        {citaAReprogramar && (
          <div className="space-y-4">
            <BloqueCard cita={citaAReprogramar} />
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Nuevo bloque</label>
              {destinos.length === 0 ? (
                <p className="text-sm text-slate-500">No tienes otros bloques libres. Pide a la administración que te asigne uno.</p>
              ) : (
                <select
                  value={idDestino}
                  onChange={(e) => setIdDestino(e.target.value === '' ? '' : Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
                  <option value="">Selecciona...</option>
                  {destinos.map((d) => (
                    <option key={d.idCita} value={d.idCita}>
                      {d.fechaSesion} · {d.horaInicio}–{d.horaFin} · {d.nombreSalon ?? 'Sin salón'} · {d.nombreDocente}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Modal cancelar */}
      <Modal
        open={!!citaACancelar}
        onClose={cerrarCancelar}
        title="Cancelar cita"
        description="Se notificará al paciente y al docente. El bloque quedará disponible."
        footer={
          <>
            <Button variant="ghost" onClick={cerrarCancelar}>Volver</Button>
            <Button variant="danger" loading={loading} leftIcon={<XIcon size={15} />} onClick={confirmarCancelacion}>
              Sí, cancelar
            </Button>
          </>
        }
      >
        {citaACancelar && (
          <div className="space-y-4">
            <BloqueCard cita={citaACancelar} />
            <Textarea
              label="Motivo de la cancelación"
              placeholder="Ej: incapacidad médica"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              maxLength={255}
              rows={3}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
