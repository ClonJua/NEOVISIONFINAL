import { useEffect, useMemo, useState } from 'react';
import {
  historiaClinicaService,
  type AuditoriaResponse,
  type HistoriaClinicaResponse,
} from '../../api/historiaClinicaService';
import { AtencionDetailModal } from '../../components/historia/AtencionDetailModal';
import { AtencionesTimeline } from '../../components/historia/AtencionesTimeline';
import { useToast } from '../../components/ui';

export default function HistoriasClinicasTab() {
  const toast = useToast();
  const [historias, setHistorias] = useState<HistoriaClinicaResponse[]>([]);
  const [query, setQuery] = useState('');
  const [seleccion, setSeleccion] = useState<HistoriaClinicaResponse | null>(null);
  const [auditoria, setAuditoria] = useState<AuditoriaResponse[] | null>(null);
  const [atencionAbierta, setAtencionAbierta] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [verificandoCadena, setVerificandoCadena] = useState(false);

  const verificarCadenaAuditoria = async () => {
    setVerificandoCadena(true);
    try {
      const res = await historiaClinicaService.verificarCadenaAuditoria();
      const r = res.data.data;
      if (r.valida) toast.success(r.mensaje);
      else toast.error(r.mensaje);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo verificar la cadena de auditoría');
    } finally {
      setVerificandoCadena(false);
    }
  };

  const cargarLista = async () => {
    setCargando(true);
    try {
      const res = await historiaClinicaService.listarTodas();
      setHistorias(res.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudieron cargar las historias');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => { cargarLista(); /* eslint-disable-next-line */ }, []);

  const abrirHistoria = async (idPaciente: number) => {
    try {
      const res = await historiaClinicaService.obtenerPorPaciente(idPaciente);
      setSeleccion(res.data.data);
      setAuditoria(null);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo abrir la historia');
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

  const filtradas = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return historias;
    return historias.filter((h) =>
      `${h.nombrePaciente ?? ''} ${h.numeroDocumento ?? ''} ${h.numeroHistoria ?? ''}`.toLowerCase().includes(q));
  }, [historias, query]);

  // ---- Detalle de una historia ----
  if (seleccion) {
    return (
      <div>
        <button onClick={() => setSeleccion(null)} className="text-sm text-blue-600 hover:underline mb-4">‹ Volver al listado</button>

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
        <AtencionesTimeline atenciones={seleccion.atenciones} onOpen={setAtencionAbierta} />

        {atencionAbierta != null && (
          <AtencionDetailModal idAtencion={atencionAbierta} open onClose={() => setAtencionAbierta(null)} />
        )}
      </div>
    );
  }

  // ---- Listado ----
  return (
    <div>
      <div className="flex items-center gap-3 mb-5">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar por nombre, documento o N° de historia…"
          className="w-full max-w-md px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
        />
        <button
          onClick={verificarCadenaAuditoria}
          disabled={verificandoCadena}
          className="shrink-0 text-sm font-medium text-blue-600 hover:text-blue-700 border border-blue-200 hover:bg-blue-50 px-3 py-2.5 rounded-lg disabled:opacity-50 whitespace-nowrap"
        >
          {verificandoCadena ? 'Verificando…' : 'Verificar cadena de auditoría'}
        </button>
      </div>

      {cargando ? (
        <p className="text-slate-400">Cargando…</p>
      ) : filtradas.length === 0 ? (
        <p className="text-slate-400">No hay historias clínicas registradas.</p>
      ) : (
        <div className="grid gap-2">
          {filtradas.map((h) => (
            <button
              key={h.idHistoria ?? h.idPaciente}
              onClick={() => abrirHistoria(h.idPaciente!)}
              className="text-left bg-white border border-slate-200 rounded-xl px-4 py-3 hover:border-blue-300 hover:shadow-sm transition flex items-center gap-3"
            >
              <div className="flex-1">
                <div className="text-sm font-semibold text-slate-800">{h.nombrePaciente}</div>
                <div className="text-xs text-slate-500">
                  Doc: {h.numeroDocumento ?? '—'} · Historia N°: {h.numeroHistoria ?? '—'}
                </div>
              </div>
              <span className="text-slate-300 text-lg">›</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
