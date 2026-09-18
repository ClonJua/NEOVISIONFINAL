import { useEffect, useState } from 'react';
import { historiaClinicaService, type AtencionResumenResponse } from '../../api/historiaClinicaService';
import { Button, Modal, useToast } from '../../components/ui';
import { AtencionDetailModal } from './AtencionDetailModal';
import { AtencionesTimeline } from './AtencionesTimeline';
import { DiagramaCanvas } from './DiagramaCanvas';
import { EstadosAtencionInfo } from './EstadosAtencionInfo';

/** Bandeja de revisión del docente: atenciones pendientes → revisar, devolver o firmar. */
export function BandejaRevision() {
  const toast = useToast();
  const [pendientes, setPendientes] = useState<AtencionResumenResponse[]>([]);
  const [cargando, setCargando] = useState(true);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [accion, setAccion] = useState<'devolver' | 'finalizar' | null>(null);
  const [motivo, setMotivo] = useState('');
  const [firmaPaciente, setFirmaPaciente] = useState('');
  const [loading, setLoading] = useState(false);

  const cargar = async () => {
    setCargando(true);
    try {
      const res = await historiaClinicaService.bandejaRevision();
      setPendientes(res.data.data);
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo cargar la bandeja');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => { cargar(); /* eslint-disable-next-line */ }, []);

  const cerrarTodo = () => { setAccion(null); setSelectedId(null); setMotivo(''); setFirmaPaciente(''); };

  const devolver = async () => {
    if (!selectedId || !motivo.trim()) { toast.error('Indica el motivo de la devolución'); return; }
    setLoading(true);
    try {
      await historiaClinicaService.devolver(selectedId, { motivo: motivo.trim() });
      toast.success('Atención devuelta al estudiante');
      cerrarTodo();
      await cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo devolver');
    } finally {
      setLoading(false);
    }
  };

  const finalizar = async () => {
    if (!selectedId) return;
    if (!firmaPaciente && !confirm('No se capturó la firma del paciente. ¿Finalizar de todas formas?')) return;
    setLoading(true);
    try {
      await historiaClinicaService.finalizar(selectedId, {
        firmaPaciente: firmaPaciente || undefined,
      });
      toast.success('Atención finalizada y firmada');
      cerrarTodo();
      await cargar();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo finalizar');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="mb-8">
      <div className="flex items-center gap-2 mb-3">
        <h2 className="text-2xl font-semibold text-slate-800">Bandeja de revisión</h2>
        {pendientes.length > 0 && (
          <span className="bg-amber-100 text-amber-700 text-xs font-bold px-2 py-0.5 rounded-full">{pendientes.length}</span>
        )}
      </div>

      <EstadosAtencionInfo rol="docente" />

      {cargando ? (
        <p className="text-slate-400">Cargando…</p>
      ) : (
        <AtencionesTimeline atenciones={pendientes} onOpen={setSelectedId} emptyText="No hay atenciones pendientes de revisión." />
      )}

      {/* Detalle + acciones de revisión */}
      {selectedId != null && accion == null && (
        <AtencionDetailModal
          idAtencion={selectedId}
          open
          onClose={() => setSelectedId(null)}
          actions={(at) =>
            at.estado === 'PENDIENTE_REVISION' ? (
              <div className="flex gap-2">
                <Button variant="danger" size="sm" onClick={() => setAccion('devolver')}>Devolver</Button>
                <Button size="sm" onClick={() => setAccion('finalizar')}>Finalizar (firmar)</Button>
              </div>
            ) : null
          }
        />
      )}

      {/* Devolver */}
      <Modal
        open={accion === 'devolver'}
        onClose={() => setAccion(null)}
        title="Devolver atención"
        description="El estudiante podrá corregir y reenviar. La devolución le reabre el plazo de edición por 2 días, así que puedes devolver aunque la cita haya sido hace tiempo."
        footer={
          <>
            <Button variant="secondary" onClick={() => setAccion(null)}>Cancelar</Button>
            <Button variant="danger" loading={loading} onClick={devolver}>Devolver</Button>
          </>
        }
      >
        <label className="block text-sm font-medium text-slate-700 mb-1">Motivo de la devolución</label>
        <textarea rows={3} value={motivo} onChange={(e) => setMotivo(e.target.value)}
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
      </Modal>

      {/* Finalizar */}
      <Modal
        open={accion === 'finalizar'}
        onClose={() => setAccion(null)}
        title="Finalizar y firmar"
        description="Esto no se puede deshacer: la atención queda sellada e inmutable (Art. 18). Si después hay que corregir algo, se registra como adenda — el original nunca se modifica."
        size="lg"
        footer={
          <>
            <Button variant="secondary" onClick={() => setAccion(null)}>Cancelar</Button>
            <Button loading={loading} onClick={finalizar}>Firmar y finalizar</Button>
          </>
        }
      >
        <p className="text-sm text-slate-600 mb-3">
          Se aplicará tu <strong>firma registrada</strong> en tu perfil (Mi perfil → Firma y sello).
          Sin ella no se puede finalizar. Captura ahora la firma del paciente:
        </p>
        <div className="max-w-sm">
          <DiagramaCanvas label="Firma del paciente" value={firmaPaciente} onChange={setFirmaPaciente} />
        </div>
        {!firmaPaciente && (
          <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-md px-3 py-2 mt-3">
            Sin firma del paciente capturada. Se pedirá confirmación antes de finalizar.
          </p>
        )}
      </Modal>
    </section>
  );
}
