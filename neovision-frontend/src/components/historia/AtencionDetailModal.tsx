import { useEffect, useState, type ReactNode } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import { historiaClinicaService, type AtencionResponse, type VerificacionIntegridadResponse } from '../../api/historiaClinicaService';
import { Button, useToast, XIcon } from '../../components/ui';
import { AtencionFolios } from './AtencionFolios';
import { EstadoAtencionBadge } from './estadoBadge';

interface Props {
  idAtencion: number;
  open: boolean;
  onClose: () => void;
  onChanged?: () => void;
  /** Acciones extra en el encabezado (p. ej. devolver/finalizar del docente). */
  actions?: (atencion: AtencionResponse, reload: () => void) => ReactNode;
}

/** Vista de solo lectura de una atención (todos los folios) + firmas, integridad y adendas. */
export function AtencionDetailModal({ idAtencion, open, onClose, onChanged, actions }: Props) {
  const { rol } = useAuth();
  const toast = useToast();
  const [atencion, setAtencion] = useState<AtencionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [textoAdenda, setTextoAdenda] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [exportando, setExportando] = useState(false);
  const [verificando, setVerificando] = useState(false);
  const [integridad, setIntegridad] = useState<VerificacionIntegridadResponse | null>(null);

  const verificarIntegridad = async () => {
    setVerificando(true);
    try {
      const res = await historiaClinicaService.verificarIntegridad(idAtencion);
      setIntegridad(res.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo verificar la integridad');
    } finally {
      setVerificando(false);
    }
  };

  const cargar = async () => {
    setLoading(true);
    setIntegridad(null);
    try {
      const res = await historiaClinicaService.detalleAtencion(idAtencion);
      setAtencion(res.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo cargar la atención');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { if (open) cargar(); /* eslint-disable-next-line */ }, [open, idAtencion]);

  if (!open) return null;

  const puedeAdenda = atencion?.estado === 'FINALIZADA' && (rol === 'ROLE_ESTUDIANTE' || rol === 'ROLE_DOCENTE');

  const exportarPdf = async () => {
    setExportando(true);
    try {
      const res = await historiaClinicaService.exportarPdf(idAtencion);
      const url = URL.createObjectURL(res.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `historia-clinica-${idAtencion}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error('No se pudo generar el PDF');
    } finally {
      setExportando(false);
    }
  };

  const agregarAdenda = async () => {
    if (!textoAdenda.trim()) return;
    setGuardando(true);
    try {
      await historiaClinicaService.agregarAdenda(idAtencion, { texto: textoAdenda.trim() });
      setTextoAdenda('');
      toast.success('Adenda agregada');
      await cargar();
      onChanged?.();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo agregar la adenda');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-slate-900/50 backdrop-blur-sm" onClick={onClose}>
      <div className="flex-1 overflow-y-auto p-4" onClick={(e) => e.stopPropagation()}>
        <div className="max-w-5xl mx-auto">
          {/* Header */}
          <div className="sticky top-0 z-10 bg-white rounded-xl border border-slate-200 px-5 py-3 mb-4 flex items-center gap-3 shadow-sm">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-800 truncate">
                  {atencion?.nombrePaciente ?? 'Atención clínica'}
                </h2>
                {atencion && <EstadoAtencionBadge estado={atencion.estado} />}
              </div>
              {atencion && (
                <p className="text-xs text-slate-500">
                  {new Date(atencion.fechaAtencion).toLocaleString()} · Estudiante: {atencion.nombreEstudiante ?? '—'} · Docente: {atencion.nombreDocente ?? '—'}
                </p>
              )}
            </div>
            {atencion && rol !== 'ROLE_ESTUDIANTE' && (
              <button onClick={exportarPdf} disabled={exportando}
                className="text-sm font-medium text-blue-600 hover:text-blue-700 border border-blue-200 hover:bg-blue-50 px-3 py-1.5 rounded-lg disabled:opacity-50 whitespace-nowrap">
                {exportando ? 'Generando…' : 'Exportar PDF'}
              </button>
            )}
            {atencion && actions?.(atencion, cargar)}
            <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg" aria-label="Cerrar">
              <XIcon size={20} />
            </button>
          </div>

          {loading || !atencion ? (
            <div className="bg-white rounded-xl p-10 text-center text-slate-400">Cargando…</div>
          ) : (
            <>
              {atencion.estado === 'DEVUELTA' && atencion.motivoDevolucion && (
                <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                  <strong>Devuelta por el docente:</strong> {atencion.motivoDevolucion}
                </div>
              )}

              <AtencionFolios mode="view" data={atencion} diagnosticos={atencion.diagnosticos ?? []} procedimientos={atencion.procedimientos ?? []} />

              {/* Firmas e integridad */}
              <section className="bg-white rounded-2xl border border-slate-200 p-5 mt-6">
                <div className="text-xs font-bold uppercase tracking-wide text-blue-600 mb-3">Firmas e integridad</div>
                <div className="grid grid-cols-3 gap-4">
                  <Firma label="Estudiante" data={atencion.firmaEstudiante} />
                  <Firma label="Docente" data={atencion.firmaDocente} />
                  <Firma label="Paciente" data={atencion.firmaPaciente} />
                </div>
                {atencion.hashIntegridad && (
                  <>
                    <p className="mt-3 text-[11px] text-slate-500 break-all">
                      <strong>Sello de integridad (SHA-256):</strong> <span className="font-mono">{atencion.hashIntegridad}</span>
                    </p>
                    <div className="mt-2 flex items-center gap-3">
                      <Button variant="secondary" size="sm" loading={verificando} onClick={verificarIntegridad}>
                        Verificar integridad
                      </Button>
                      {integridad && (
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                            integridad.valido
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-red-50 text-red-700 border border-red-200'
                          }`}
                          title={integridad.mensaje}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${integridad.valido ? 'bg-emerald-500' : 'bg-red-500'}`} />
                          {integridad.valido ? 'Íntegra · sin alteraciones' : 'ALTERADA · no coincide con el sello'}
                        </span>
                      )}
                    </div>
                  </>
                )}
                {atencion.fechaFinalizacion && (
                  <p className="mt-2 text-[11px] text-slate-500">Finalizada: {new Date(atencion.fechaFinalizacion).toLocaleString()}</p>
                )}
              </section>

              {/* Adendas */}
              <section className="bg-white rounded-2xl border border-slate-200 p-5 mt-6 mb-10">
                <div className="text-xs font-bold uppercase tracking-wide text-blue-600 mb-3">Adendas</div>
                {atencion.adendas?.length ? (
                  <ul className="space-y-3 mb-4">
                    {atencion.adendas.map((a) => (
                      <li key={a.idAdenda} className="border-l-2 border-amber-400 pl-3">
                        <p className="text-sm text-slate-800 whitespace-pre-wrap">{a.texto}</p>
                        <p className="text-[11px] text-slate-400">{a.autorCorreo} · {new Date(a.fecha).toLocaleString()}</p>
                      </li>
                    ))}
                  </ul>
                ) : <p className="text-sm text-slate-400 mb-3">Sin adendas</p>}

                {puedeAdenda && (
                  <div className="border-t border-slate-100 pt-3">
                    <textarea rows={2} value={textoAdenda} onChange={(e) => setTextoAdenda(e.target.value)}
                      placeholder="Agregar una adenda (corrección posterior)…"
                      className="w-full px-3 py-2 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
                    <div className="mt-2 flex justify-end">
                      <Button size="sm" loading={guardando} onClick={agregarAdenda}>Agregar adenda</Button>
                    </div>
                  </div>
                )}
              </section>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function Firma({ label, data }: { label: string; data?: string }) {
  return (
    <div>
      <div className="text-[11px] font-medium text-slate-500 mb-1">{label}</div>
      {data ? (
        <img src={data} alt={`Firma ${label}`} className="h-16 w-full object-contain border border-slate-200 rounded bg-white" />
      ) : (
        <div className="h-16 border border-dashed border-slate-200 rounded flex items-center justify-center text-[11px] text-slate-400">Sin firma</div>
      )}
    </div>
  );
}
