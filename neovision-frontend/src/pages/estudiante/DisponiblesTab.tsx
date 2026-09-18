import { useMemo, useState } from 'react';
import { citaService, type CitaResponse } from '../../api/citaService';
import { estudianteService } from '../../api/estudianteService';
import type { PacienteResponse, CrearPacienteRequest } from '../../api/adminService';
import DateScroller from '../../components/DateScroller';
import { EmptyState, SearchIcon, CalendarIcon, useToast } from '../../components/ui';
import BloqueCard from './BloqueCard';

interface Props {
  bloques: CitaResponse[];
  pacientes: PacienteResponse[];
  onRefrescar: () => void;
}

const TIPOS_DOCUMENTO = ['CC', 'TI', 'RC', 'CE', 'PA'];

const PACIENTE_VACIO: CrearPacienteRequest = {
  nombre: '', apellido: '', correo: '', contrasena: '',
  tipoDocumento: 'CC', numeroDocumento: '', fechaNacimiento: '', telefono: '',
  autorizaTratamientoDatos: false,
  representanteNombre: '', representanteDocumento: '', representanteParentesco: '',
};

/** Bloques sin paciente asignado: el estudiante elige o crea el paciente para reservarlo. */
export default function DisponiblesTab({ bloques, pacientes, onRefrescar }: Props) {
  const toast = useToast();
  const [query, setQuery] = useState('');
  const [asignandoId, setAsignandoId] = useState<number | null>(null);
  const [idPacienteSeleccionado, setIdPacienteSeleccionado] = useState<number | ''>('');
  const [showNuevoPac, setShowNuevoPac] = useState(false);
  const [creandoPac, setCreandoPac] = useState(false);
  const [nuevoPac, setNuevoPac] = useState<CrearPacienteRequest>(PACIENTE_VACIO);

  const filtrados = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return bloques;
    return bloques.filter((b) =>
      `${b.fechaSesion} ${b.nombreEspacio ?? ''} ${b.nombreSalon ?? ''} ${b.nombreDocente}`.toLowerCase().includes(q));
  }, [bloques, query]);

  const handleAsignar = async (idCita: number) => {
    if (!idPacienteSeleccionado) {
      toast.error('Seleccione un paciente');
      return;
    }
    try {
      await citaService.asignarPaciente(idCita, idPacienteSeleccionado as number);
      toast.success('Paciente asignado exitosamente');
      setAsignandoId(null);
      setIdPacienteSeleccionado('');
      onRefrescar();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Error al asignar paciente');
    }
  };

  const handleCrearPaciente = async () => {
    const { nombre, apellido, correo, contrasena, numeroDocumento, fechaNacimiento, telefono, tipoDocumento } = nuevoPac;
    if (!nombre || !apellido || !correo || !contrasena || !numeroDocumento || !fechaNacimiento || !telefono) {
      toast.error('Todos los campos del paciente son obligatorios');
      return;
    }
    const esMenor = tipoDocumento === 'TI' || tipoDocumento === 'RC';
    if (esMenor && (!nuevoPac.representanteNombre || !nuevoPac.representanteDocumento || !nuevoPac.representanteParentesco)) {
      toast.error('Paciente menor de edad: se requieren los datos del representante legal');
      return;
    }
    if (!nuevoPac.autorizaTratamientoDatos) {
      toast.error('El paciente debe autorizar el tratamiento de sus datos');
      return;
    }
    setCreandoPac(true);
    try {
      const res = await estudianteService.crearPaciente(nuevoPac);
      setIdPacienteSeleccionado(res.data.data.idPaciente);
      setShowNuevoPac(false);
      setNuevoPac(PACIENTE_VACIO);
      toast.success('Paciente creado y seleccionado');
      onRefrescar();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Error al crear paciente');
    } finally {
      setCreandoPac(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="relative max-w-md">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"><SearchIcon size={16} /></span>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar por fecha, espacio, salón o docente…"
          className="w-full pl-9 pr-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
        />
      </div>

      {filtrados.length === 0 ? (
        <EmptyState icon={<CalendarIcon size={28} />} title="Sin bloques disponibles" description="No tienes bloques disponibles para asignar un paciente." />
      ) : (
        <div className="space-y-3">
          {filtrados.map((c) => (
            <BloqueCard key={c.idCita} cita={c} acciones={
              asignandoId === c.idCita ? (
                <div className="space-y-2 w-72">
                  <select value={idPacienteSeleccionado}
                    onChange={(e) => setIdPacienteSeleccionado(parseInt(e.target.value) || '')}
                    className="w-full px-2 py-1.5 border border-slate-300 rounded-lg text-xs outline-none">
                    <option value="">Seleccionar paciente...</option>
                    {pacientes.map((p) => (
                      <option key={p.idPaciente} value={p.idPaciente}>{p.nombre} {p.apellido} — {p.numeroDocumento}</option>
                    ))}
                  </select>

                  {!showNuevoPac && (
                    <button onClick={() => setShowNuevoPac(true)} className="text-xs text-blue-600 hover:text-blue-700 font-medium">
                      + Nuevo paciente
                    </button>
                  )}

                  {showNuevoPac && (
                    <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 space-y-2">
                      <div className="grid grid-cols-2 gap-2">
                        <input placeholder="Nombre *" value={nuevoPac.nombre}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, nombre: e.target.value })}
                          className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                        <input placeholder="Apellido *" value={nuevoPac.apellido}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, apellido: e.target.value })}
                          className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <input placeholder="Correo *" type="email" value={nuevoPac.correo}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, correo: e.target.value })}
                          className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                        <input placeholder="Contraseña *" type="password" value={nuevoPac.contrasena}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, contrasena: e.target.value })}
                          className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                      </div>
                      <div className="grid grid-cols-3 gap-2">
                        <select value={nuevoPac.tipoDocumento}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, tipoDocumento: e.target.value })}
                          className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none">
                          {TIPOS_DOCUMENTO.map((t) => <option key={t} value={t}>{t}</option>)}
                        </select>
                        <input placeholder="No. documento *" value={nuevoPac.numeroDocumento}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, numeroDocumento: e.target.value })}
                          className="col-span-2 px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <DateScroller value={nuevoPac.fechaNacimiento} onChange={(fecha) => setNuevoPac({ ...nuevoPac, fechaNacimiento: fecha })} maxDate={new Date().toISOString().split('T')[0]} />
                        <input placeholder="Teléfono *" value={nuevoPac.telefono}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, telefono: e.target.value })}
                          className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                      </div>
                      {(nuevoPac.tipoDocumento === 'TI' || nuevoPac.tipoDocumento === 'RC') && (
                        <div className="bg-amber-50 border border-amber-200 rounded p-2 space-y-2">
                          <p className="text-[11px] text-amber-800">
                            Menor de edad: la autorización de datos debe darla el representante legal.
                          </p>
                          <input placeholder="Nombre representante *" value={nuevoPac.representanteNombre}
                            onChange={(e) => setNuevoPac({ ...nuevoPac, representanteNombre: e.target.value })}
                            className="w-full px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                          <div className="grid grid-cols-2 gap-2">
                            <input placeholder="Documento representante *" value={nuevoPac.representanteDocumento}
                              onChange={(e) => setNuevoPac({ ...nuevoPac, representanteDocumento: e.target.value })}
                              className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                            <input placeholder="Parentesco *" value={nuevoPac.representanteParentesco}
                              onChange={(e) => setNuevoPac({ ...nuevoPac, representanteParentesco: e.target.value })}
                              className="px-2 py-1.5 border border-slate-300 rounded text-xs outline-none" />
                          </div>
                        </div>
                      )}
                      <label className="flex items-start gap-2 text-xs text-slate-600">
                        <input type="checkbox" checked={!!nuevoPac.autorizaTratamientoDatos}
                          onChange={(e) => setNuevoPac({ ...nuevoPac, autorizaTratamientoDatos: e.target.checked })}
                          className="mt-0.5 rounded" />
                        <span>El paciente autoriza el tratamiento de sus datos (Ley 1581).</span>
                      </label>
                      <div className="flex gap-2">
                        <button onClick={handleCrearPaciente} disabled={creandoPac}
                          className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs px-3 py-1.5 rounded-lg disabled:opacity-50">
                          {creandoPac ? 'Creando...' : 'Crear y seleccionar'}
                        </button>
                        <button onClick={() => setShowNuevoPac(false)} className="text-xs text-slate-500">Cancelar</button>
                      </div>
                    </div>
                  )}

                  <div className="flex gap-2">
                    <button onClick={() => handleAsignar(c.idCita)}
                      className="bg-bosque hover:bg-bosque-dark text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                      Asignar
                    </button>
                    <button onClick={() => { setAsignandoId(null); setShowNuevoPac(false); }}
                      className="text-xs text-slate-500 hover:text-slate-700">Cancelar</button>
                  </div>
                </div>
              ) : (
                <button onClick={() => setAsignandoId(c.idCita)}
                  className="bg-bosque hover:bg-bosque-dark text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                  Asignar paciente
                </button>
              )
            } />
          ))}
        </div>
      )}
    </div>
  );
}
