import { useEffect, useMemo, useState } from 'react';
import { solicitudService, type SolicitudResponse, type EstadoSolicitud } from '../../api/solicitudService';
import { adminService, type PacienteResponse } from '../../api/adminService';
import { Badge, Button, Modal, useToast } from '../../components/ui';

const TONO: Record<EstadoSolicitud, 'amber' | 'emerald' | 'red'> = {
  PENDIENTE: 'amber', APROBADA: 'emerald', RECHAZADA: 'red',
};

export default function SolicitudesTab() {
  const toast = useToast();
  const [solicitudes, setSolicitudes] = useState<SolicitudResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [accion, setAccion] = useState<number | null>(null);
  const [rechazoId, setRechazoId] = useState<number | null>(null);
  const [motivo, setMotivo] = useState('');

  // Solicitud presencial: para el paciente que la pide en la clinica y no tiene
  // cuenta ni correo. El derecho de acceso a la historia no depende de tenerlos.
  const [presencialAbierto, setPresencialAbierto] = useState(false);
  const [pacientes, setPacientes] = useState<PacienteResponse[]>([]);
  const [busquedaPac, setBusquedaPac] = useState('');
  const [pacienteSel, setPacienteSel] = useState<number | null>(null);
  const [motivoPresencial, setMotivoPresencial] = useState('');
  const [generando, setGenerando] = useState(false);

  const cargar = async () => {
    setLoading(true);
    try {
      const r = await solicitudService.listar();
      setSolicitudes(r.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudieron cargar las solicitudes');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { cargar(); }, []);

  const abrirPresencial = async () => {
    setPresencialAbierto(true);
    setPacienteSel(null); setBusquedaPac(''); setMotivoPresencial('');
    if (pacientes.length === 0) {
      try {
        const r = await adminService.listarPacientes();
        setPacientes(r.data.data);
      } catch {
        toast.error('No se pudieron cargar los pacientes');
      }
    }
  };

  const pacientesFiltrados = useMemo(() => {
    const q = busquedaPac.trim().toLowerCase();
    if (!q) return pacientes.slice(0, 20);
    return pacientes.filter(p =>
      `${p.nombre} ${p.apellido}`.toLowerCase().includes(q) || p.numeroDocumento.includes(q)
    ).slice(0, 20);
  }, [pacientes, busquedaPac]);

  const registrarPresencial = async () => {
    if (pacienteSel == null) { toast.error('Selecciona el paciente'); return; }
    setGenerando(true);
    try {
      const res = await solicitudService.crearPresencial(pacienteSel, motivoPresencial.trim() || undefined);
      // Se descarga para imprimirla y entregarla en mano.
      const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `historia-clinica-${pacienteSel}.pdf`;
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(url);

      toast.success('Solicitud registrada. Se descargó la copia para entregarla al paciente.');
      setPresencialAbierto(false);
      cargar();
    } catch (e: any) {
      // El backend responde el error como blob por el responseType: hay que leerlo.
      let msg = 'No se pudo registrar la solicitud';
      try {
        const texto = await e.response?.data?.text?.();
        if (texto) msg = JSON.parse(texto).message || msg;
      } catch { /* se queda el mensaje generico */ }
      toast.error(msg);
    } finally {
      setGenerando(false);
    }
  };

  const aprobar = async (id: number) => {
    setAccion(id);
    try {
      await solicitudService.aprobar(id);
      toast.success('Aprobada. La historia se envió al correo del paciente.');
      cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo aprobar');
    } finally {
      setAccion(null);
    }
  };

  const rechazar = async () => {
    if (rechazoId == null || !motivo.trim()) { toast.error('Indica el motivo'); return; }
    setAccion(rechazoId);
    try {
      await solicitudService.rechazar(rechazoId, motivo.trim());
      toast.success('Solicitud rechazada');
      setRechazoId(null); setMotivo('');
      cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo rechazar');
    } finally {
      setAccion(null);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-700">Solicitudes de historia clínica</h3>
        <Button size="sm" onClick={abrirPresencial}>+ Solicitud presencial</Button>
      </div>

      {loading ? (
        <p className="text-slate-400">Cargando…</p>
      ) : solicitudes.length === 0 ? (
        <p className="text-slate-400">No hay solicitudes.</p>
      ) : (
        <div className="grid gap-2">
          {solicitudes.map(s => (
            <div key={s.idSolicitud} className="bg-white border border-slate-200 rounded-xl px-4 py-3 flex items-center gap-3">
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-slate-800">{s.nombrePaciente}</span>
                  <Badge tone={TONO[s.estado]}>{s.estado}</Badge>
                  {s.canal === 'PRESENCIAL' && <Badge tone="slate">PRESENCIAL</Badge>}
                </div>
                <div className="text-xs text-slate-500">
                  {new Date(s.fechaSolicitud).toLocaleString()}
                  {s.correoPaciente ? ` · ${s.correoPaciente}` : ' · sin correo'}
                  {s.motivo ? ` · "${s.motivo}"` : ''}
                </div>
                {s.canal === 'PRESENCIAL' && s.creadaPorCorreo && (
                  <div className="text-xs text-slate-400">Identidad verificada por {s.creadaPorCorreo}</div>
                )}
                {s.estado === 'RECHAZADA' && s.motivoRechazo && (
                  <div className="text-xs text-red-600">Motivo rechazo: {s.motivoRechazo}</div>
                )}
              </div>
              {s.estado === 'PENDIENTE' && (
                <div className="flex gap-2">
                  <Button size="sm" variant="danger" onClick={() => { setRechazoId(s.idSolicitud); setMotivo(''); }}>Rechazar</Button>
                  <Button size="sm" loading={accion === s.idSolicitud} onClick={() => aprobar(s.idSolicitud)}>
                    {s.correoPaciente ? 'Aprobar y enviar' : 'Aprobar (entrega en mano)'}
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <Modal
        open={rechazoId !== null}
        onClose={() => setRechazoId(null)}
        title="Rechazar solicitud"
        footer={
          <>
            <Button variant="secondary" onClick={() => setRechazoId(null)}>Cancelar</Button>
            <Button variant="danger" loading={accion === rechazoId} onClick={rechazar}>Rechazar</Button>
          </>
        }
      >
        <label className="block text-sm font-medium text-slate-700 mb-1">Motivo del rechazo</label>
        <textarea rows={3} value={motivo} onChange={(e) => setMotivo(e.target.value)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
      </Modal>

      <Modal
        open={presencialAbierto}
        onClose={() => setPresencialAbierto(false)}
        title="Registrar solicitud presencial"
        footer={
          <>
            <Button variant="secondary" onClick={() => setPresencialAbierto(false)}>Cancelar</Button>
            <Button loading={generando} onClick={registrarPresencial}>Registrar y descargar copia</Button>
          </>
        }
      >
        <p className="text-xs text-slate-500 mb-3">
          Verifica el documento de identidad del paciente antes de continuar. Quedará registrado
          que la solicitud la atendiste tú, junto con el acceso a la historia clínica.
        </p>

        <label className="block text-sm font-medium text-slate-700 mb-1">Paciente</label>
        <input type="text" value={busquedaPac} onChange={e => setBusquedaPac(e.target.value)}
          placeholder="Buscar por nombre o documento…"
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none mb-2" />

        <div className="max-h-52 overflow-y-auto border border-slate-200 rounded-md divide-y divide-slate-100 mb-3">
          {pacientesFiltrados.length === 0 ? (
            <p className="text-sm text-slate-400 px-3 py-4 text-center">Sin resultados</p>
          ) : pacientesFiltrados.map(p => (
            <button key={p.idPaciente} type="button" onClick={() => setPacienteSel(p.idPaciente)}
              className={`w-full text-left px-3 py-2 text-sm hover:bg-slate-50 ${pacienteSel === p.idPaciente ? 'bg-bosque/10 font-medium' : ''}`}>
              {p.nombre} {p.apellido}
              <span className="text-xs text-slate-500"> · {p.tipoDocumento} {p.numeroDocumento}</span>
              {!p.correo && <span className="text-xs text-slate-400 italic"> · sin correo</span>}
            </button>
          ))}
        </div>

        <label className="block text-sm font-medium text-slate-700 mb-1">
          Motivo <span className="font-normal text-slate-400">(opcional)</span>
        </label>
        <textarea rows={2} value={motivoPresencial} onChange={e => setMotivoPresencial(e.target.value)}
          placeholder="Lo que indique el paciente"
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
      </Modal>
    </div>
  );
}
