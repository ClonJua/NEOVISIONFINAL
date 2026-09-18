import { useEffect, useState } from 'react';
import {
  historiaClinicaService,
  type AtencionResponse,
  type DiagnosticoDto,
  type ProcedimientoDto,
  type GuardarAtencionRequest,
} from '../../api/historiaClinicaService';
import { Button, Modal, useToast, XIcon } from '../../components/ui';
import { AtencionFolios, type ClinicalData } from './AtencionFolios';
import { EstadoAtencionBadge } from './estadoBadge';

interface Props {
  atencion: AtencionResponse;
  onClose: () => void;
  onSaved: () => void;
}

const SECCIONES = [
  'datosGenerales', 'anamnesis', 'signosVitales', 'lensometria', 'agudezaVisual',
  'queratometria', 'refraccion', 'pruebasComplementarias', 'formulaFinal',
  'examenExterno', 'motilidad', 'fondoOjo', 'conducta',
];

function initForm(a: AtencionResponse): ClinicalData {
  const f: ClinicalData = { motivoConsulta: a.motivoConsulta ?? '' };
  for (const s of SECCIONES) f[s] = { ...((a as any)[s] ?? {}) };
  return f;
}

const GRACIA_DIAS = 2;

/** ¿Aún se puede editar? Hasta la fecha de la cita + ventana de gracia (oportunidad). */
function dentroDeVentana(fechaCita: string | null): boolean {
  if (!fechaCita) return true;
  const limite = new Date(fechaCita + 'T00:00:00');
  limite.setDate(limite.getDate() + GRACIA_DIAS);
  const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
  return hoy <= limite;
}

/** Formulario editable de una atención (estudiante). Espeja los 4 folios del papel. */
export function AtencionForm({ atencion, onClose, onSaved }: Props) {
  const toast = useToast();
  const editable = dentroDeVentana(atencion.fechaCita);
  const [form, setForm] = useState<ClinicalData>(() => initForm(atencion));
  const [diagnosticos, setDiagnosticos] = useState<DiagnosticoDto[]>(atencion.diagnosticos ?? []);
  const [procedimientos, setProcedimientos] = useState<ProcedimientoDto[]>(atencion.procedimientos ?? []);
  const [guardando, setGuardando] = useState(false);
  const [enviarOpen, setEnviarOpen] = useState(false);
  // Heurística de Nielsen #3 (control y libertad): sin esto, cerrar el
  // formulario por error perdía cambios sin ningún aviso.
  const [dirty, setDirty] = useState(false);

  const onField = (section: string, field: string, value: any) => {
    setForm((prev) => ({ ...prev, [section]: { ...(prev[section] ?? {}), [field]: value } }));
    setDirty(true);
  };

  const buildRequest = (): GuardarAtencionRequest => {
    const req: any = { motivoConsulta: form.motivoConsulta, diagnosticos, procedimientos };
    for (const s of SECCIONES) req[s] = form[s];
    return req;
  };

  const guardar = async (): Promise<boolean> => {
    setGuardando(true);
    try {
      await historiaClinicaService.guardarBorrador(atencion.idAtencion, buildRequest());
      toast.success('Borrador guardado');
      setDirty(false);
      onSaved();
      return true;
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo guardar');
      return false;
    } finally {
      setGuardando(false);
    }
  };

  const handleClose = () => {
    if (dirty && !confirm('Tienes cambios sin guardar. ¿Deseas salir sin guardarlos?')) return;
    onClose();
  };

  // Heurística #7 (flexibilidad para uso experto): Ctrl/Cmd+S guarda el
  // borrador sin tener que ir al mouse; Escape sale (con el mismo aviso de
  // cambios sin guardar que el botón de cerrar).
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        if (editable && !guardando) guardar();
      } else if (e.key === 'Escape' && !enviarOpen) {
        // Si el modal de "Enviar a revisión" está abierto, que sea ese el que
        // maneje su propio Escape (Modal.tsx) en vez de cerrar todo el formulario.
        handleClose();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editable, guardando, dirty, enviarOpen]);

  const enviar = async () => {
    setGuardando(true);
    try {
      // Si la ventana de edición cerró, no se reenvían cambios; solo se envía a revisión.
      if (editable) await historiaClinicaService.guardarBorrador(atencion.idAtencion, buildRequest());
      await historiaClinicaService.enviarRevision(atencion.idAtencion, {});
      toast.success('Atención enviada a revisión del docente');
      setEnviarOpen(false);
      onSaved();
      onClose();
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'No se pudo enviar');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-slate-900/50 backdrop-blur-sm">
      <div className="flex-1 overflow-y-auto p-4">
        <div className="max-w-5xl mx-auto">
          {/* Header */}
          <div className="sticky top-0 z-10 bg-white rounded-xl border border-slate-200 px-5 py-3 mb-4 flex items-center gap-3 shadow-sm">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-800 truncate">Historia clínica · {atencion.nombrePaciente}</h2>
                <EstadoAtencionBadge estado={atencion.estado} />
              </div>
              <p className="text-xs text-slate-500">Examen Funcional Optométrico</p>
            </div>
            {editable && <Button variant="secondary" size="sm" loading={guardando} onClick={guardar}>Guardar borrador</Button>}
            <Button size="sm" onClick={() => setEnviarOpen(true)}>Enviar a revisión</Button>
            <button onClick={handleClose} className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg" aria-label="Cerrar">
              <XIcon size={20} />
            </button>
          </div>

          {atencion.estado === 'DEVUELTA' && atencion.motivoDevolucion && (
            <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              <strong>Devuelta por el docente:</strong> {atencion.motivoDevolucion}
            </div>
          )}

          {!editable && (
            <div className="mb-4 bg-amber-50 border border-amber-200 text-amber-800 px-4 py-3 rounded-lg text-sm">
              La ventana para editar este registro (fecha de la cita + {GRACIA_DIAS} días) ya cerró.
              Ya no puedes modificar los datos; puedes <strong>enviarlo a revisión</strong> para finalizarlo,
              o agregar una <strong>adenda</strong> una vez finalizado.
            </div>
          )}

          <div className="pb-10">
            <AtencionFolios
              mode={editable ? 'edit' : 'view'}
              data={form}
              onField={onField}
              onMotivo={(v) => { setForm((prev) => ({ ...prev, motivoConsulta: v })); setDirty(true); }}
              diagnosticos={diagnosticos}
              onDiagnosticos={(l) => { setDiagnosticos(l); setDirty(true); }}
              procedimientos={procedimientos}
              onProcedimientos={(l) => { setProcedimientos(l); setDirty(true); }}
            />
          </div>
        </div>
      </div>

      {/* Confirmación + firma del estudiante */}
      <Modal
        open={enviarOpen}
        onClose={() => setEnviarOpen(false)}
        title="Enviar a revisión"
        description="El docente supervisor revisará y firmará la atención."
        footer={
          <>
            <Button variant="secondary" onClick={() => setEnviarOpen(false)}>Cancelar</Button>
            <Button loading={guardando} onClick={enviar}>Confirmar envío</Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          Se aplicará tu <strong>firma registrada</strong> en el perfil. Si no la has configurado,
          hazlo en “Editar perfil → Firma y sello”.
        </p>
      </Modal>
    </div>
  );
}
