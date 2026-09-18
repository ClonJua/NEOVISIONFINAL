import { useEffect, useMemo, useState } from 'react';
import type { CitaResponse } from '../../api/citaService';
import {
  historiaClinicaService,
  type AuditoriaResponse,
  type HistoriaClinicaResponse,
} from '../../api/historiaClinicaService';
import { AtencionDetailModal } from '../../components/historia/AtencionDetailModal';
import { AtencionesTimeline } from '../../components/historia/AtencionesTimeline';
import { EmptyState, SearchIcon, UsersIcon, useToast } from '../../components/ui';

interface Props {
  citas: CitaResponse[];
}

interface PacienteResumen {
  idPaciente: number;
  nombrePaciente: string;
  numAtenciones: number;
}

/**
 * Historias clínicas de los pacientes atendidos en sesiones del docente.
 * Reutiliza los mismos endpoints de consulta que ya autoriza el backend
 * para ROLE_DOCENTE (HistoriaAccessGuard: solo pacientes con vínculo asistencial
 * real, es decir, atendidos en alguna sesión que el docente supervisa).
 */
export default function PacientesTab({ citas }: Props) {
  const toast = useToast();
  const [query, setQuery] = useState('');
  const [seleccion, setSeleccion] = useState<HistoriaClinicaResponse | null>(null);
  const [auditoria, setAuditoria] = useState<AuditoriaResponse[] | null>(null);
  const [atencionAbierta, setAtencionAbierta] = useState<number | null>(null);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);

  const pacientes = useMemo<PacienteResumen[]>(() => {
    const porId = new Map<number, PacienteResumen>();
    for (const c of citas) {
      if (c.idPaciente == null || !c.nombrePaciente) continue;
      const actual = porId.get(c.idPaciente);
      if (actual) actual.numAtenciones += 1;
      else porId.set(c.idPaciente, { idPaciente: c.idPaciente, nombrePaciente: c.nombrePaciente, numAtenciones: 1 });
    }
    return [...porId.values()].sort((a, b) => a.nombrePaciente.localeCompare(b.nombrePaciente));
  }, [citas]);

  const filtrados = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return pacientes;
    return pacientes.filter((p) => p.nombrePaciente.toLowerCase().includes(q));
  }, [pacientes, query]);

  const abrirHistoria = async (idPaciente: number) => {
    setCargandoDetalle(true);
    try {
      const res = await historiaClinicaService.obtenerPorPaciente(idPaciente);
      setSeleccion(res.data.data);
      setAuditoria(null);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo abrir la historia clínica');
    } finally {
      setCargandoDetalle(false);
    }
  };

  const verAuditoria = async (idPaciente: number) => {
    try {
      const res = await historiaClinicaService.auditoriaPaciente(idPaciente);
      setAuditoria(res.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo cargar la auditoría');
    }
  };

  useEffect(() => { setAuditoria(null); }, [seleccion?.idPaciente]);

  // ---- Detalle de un paciente ----
  if (seleccion) {
    return (
      <div>
        <button onClick={() => setSeleccion(null)} className="text-sm text-blue-600 hover:underline mb-4">
          ‹ Volver al listado de pacientes
        </button>

        <div className="bg-white rounded-2xl border border-slate-200 p-5 mb-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-xl font-bold text-slate-800">{seleccion.nombrePaciente}</h2>
              <p className="text-sm text-slate-500">
                Documento: {seleccion.numeroDocumento ?? '—'} · Historia N°: {seleccion.numeroHistoria ?? '—'}
              </p>
              {seleccion.fechaApertura && (
                <p className="text-xs text-slate-400">Apertura: {new Date(seleccion.fechaApertura).toLocaleString()}</p>
              )}
            </div>
            <button onClick={() => verAuditoria(seleccion.idPaciente!)} className="text-sm text-blue-600 hover:underline whitespace-nowrap">
              Ver auditoría
            </button>
          </div>
        </div>

        {auditoria && (
          <div className="bg-white rounded-2xl border border-slate-200 p-5 mb-5">
            <h3 className="text-sm font-bold text-slate-700 mb-3">Auditoría (append-only)</h3>
            {auditoria.length ? (
              <ul className="space-y-1 max-h-64 overflow-y-auto text-xs">
                {auditoria.map((a) => (
                  <li key={a.idAuditoria} className="flex gap-2 text-slate-600">
                    <span className="text-slate-400 whitespace-nowrap">{new Date(a.fechaHora).toLocaleString()}</span>
                    <span className="font-medium text-slate-700">{a.accion}</span>
                    <span className="text-slate-400">·</span>
                    <span className="truncate">{a.usuarioCorreo}</span>
                    {a.detalle && <span className="text-slate-400 truncate">— {a.detalle}</span>}
                  </li>
                ))}
              </ul>
            ) : <p className="text-sm text-slate-400">Sin registros.</p>}
          </div>
        )}

        <h3 className="text-sm font-bold text-slate-700 mb-3">Atenciones</h3>
        <AtencionesTimeline atenciones={seleccion.atenciones} onOpen={setAtencionAbierta} emptyText="Este paciente aún no tiene atenciones registradas." />

        {atencionAbierta != null && (
          <AtencionDetailModal idAtencion={atencionAbierta} open onClose={() => setAtencionAbierta(null)} onChanged={() => abrirHistoria(seleccion.idPaciente!)} />
        )}
      </div>
    );
  }

  // ---- Listado ----
  return (
    <div>
      <div className="relative max-w-md mb-5">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"><SearchIcon size={16} /></span>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar paciente por nombre…"
          className="w-full pl-9 pr-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
        />
      </div>

      {cargandoDetalle ? (
        <p className="text-slate-400">Cargando…</p>
      ) : filtrados.length === 0 ? (
        <EmptyState
          icon={<UsersIcon size={28} />}
          title="Sin pacientes"
          description="Aún no hay pacientes atendidos en tus sesiones de práctica."
        />
      ) : (
        <div className="grid gap-2">
          {filtrados.map((p) => (
            <button
              key={p.idPaciente}
              onClick={() => abrirHistoria(p.idPaciente)}
              className="text-left bg-white border border-slate-200 rounded-xl px-4 py-3 hover:border-blue-300 hover:shadow-sm transition flex items-center gap-3"
            >
              <div className="flex-1">
                <div className="text-sm font-semibold text-slate-800">{p.nombrePaciente}</div>
                <div className="text-xs text-slate-500">{p.numAtenciones} cita(s) en tus sesiones</div>
              </div>
              <span className="text-slate-300 text-lg">›</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
