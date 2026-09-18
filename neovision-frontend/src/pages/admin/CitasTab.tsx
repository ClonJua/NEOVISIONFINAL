import { useState, useEffect, useRef } from 'react';
import { useSaveShortcut } from '../../hooks/useSaveShortcut';
import { citaService, nombresEstudiantes, type CitaResponse, type CitaRequest } from '../../api/citaService';
import { sesionService, type SesionResponse } from '../../api/sesionService';
import { adminService, type PacienteResponse, type CrearPacienteRequest } from '../../api/adminService';
import DateScroller from '../../components/DateScroller';
import { useToast } from '../../components/ui/Toast';

const ESTADOS = ['TODAS', 'DISPONIBLE', 'RESERVADO', 'COMPLETADO', 'CANCELADO', 'VENCIDA'];
const estadoColor: Record<string, string> = {
  DISPONIBLE: 'bg-emerald-100 text-emerald-700',
  RESERVADO: 'bg-blue-100 text-blue-700',
  COMPLETADO: 'bg-slate-100 text-slate-500',
  CANCELADO: 'bg-red-100 text-red-700',
  VENCIDA: 'bg-amber-100 text-amber-700',
};

const TIPOS_DOCUMENTO = ['CC', 'TI', 'RC', 'CE', 'PA'];

export default function CitasTab() {
  const toast = useToast();
  const [citas, setCitas] = useState<CitaResponse[]>([]);
  const [sesiones, setSesiones] = useState<SesionResponse[]>([]);
  const [pacientes, setPacientes] = useState<PacienteResponse[]>([]);
  const [filtro, setFiltro] = useState('TODAS');
  const [busqueda, setBusqueda] = useState('');
  const [filtroEspacio, setFiltroEspacio] = useState('');
  const [filtroDocente, setFiltroDocente] = useState('');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const formRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (editandoId != null) formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [editandoId]);

  // Form state
  const [idSesion, setIdSesion] = useState<number | ''>('');
  const [idEstudiante, setIdEstudiante] = useState<number | ''>('');
  // Segundo estudiante del bloque: los pacientes se atienden normalmente en pareja.
  const [idEstudiante2, setIdEstudiante2] = useState<number | ''>('');
  const [idPaciente, setIdPaciente] = useState<number | ''>('');
  const [observaciones, setObservaciones] = useState('');

  // Nuevo paciente inline
  const [showNuevoPaciente, setShowNuevoPaciente] = useState(false);
  const [creandoPaciente, setCreandoPaciente] = useState(false);
  const NUEVO_PAC_VACIO: CrearPacienteRequest = {
    nombre: '', apellido: '', correo: '', contrasena: '',
    tipoDocumento: 'CC', numeroDocumento: '', fechaNacimiento: '', telefono: '',
    autorizaTratamientoDatos: false,
    representanteNombre: '', representanteDocumento: '', representanteParentesco: '',
  };
  const [nuevoPac, setNuevoPac] = useState<CrearPacienteRequest>(NUEVO_PAC_VACIO);

  const sesionSeleccionada = sesiones.find(s => s.idSesion === idSesion);

  const cargar = async () => {
    try {
      const [citasRes, sesRes, pacRes] = await Promise.all([
        citaService.listarCitas(filtro === 'TODAS' ? undefined : filtro),
        sesionService.listar(),
        adminService.listarPacientes(),
      ]);
      setCitas(citasRes.data.data);
      setSesiones(sesRes.data.data.filter(s => s.activo));
      setPacientes(pacRes.data.data.filter(p => p.activo));
    } catch (err: any) {
      const backend = err?.response?.data?.message;
      const status = err?.response?.status;
      const msg = backend || `Error al cargar datos${status ? ` (HTTP ${status})` : ''}: ${err?.message || 'sin detalle'}`;
      setError(msg);
      toast.error(msg);
      console.error('[CitasTab] cargar() fallo:', err);
    }
  };

  useEffect(() => { cargar(); }, [filtro]);

  const resetNuevoPac = () => {
    setNuevoPac(NUEVO_PAC_VACIO);
    setShowNuevoPaciente(false);
  };

  const resetForm = () => {
    setIdSesion('');
    setIdEstudiante('');
    setIdEstudiante2('');
    setIdPaciente('');
    setObservaciones('');
    setShowForm(false);
    setEditandoId(null);
    resetNuevoPac();
    setError('');
  };

  const handleEditarCita = (c: CitaResponse) => {
    setIdSesion(c.idSesion);
    setIdEstudiante(c.idEstudiante);
    setIdEstudiante2(c.idEstudiante2 ?? '');
    setIdPaciente(c.idPaciente || '');
    setObservaciones(c.observaciones || '');
    setEditandoId(c.idCita);
    setShowForm(true);
    setError('');
  };

  const handleCrearPaciente = async () => {
    const { nombre, apellido, correo, contrasena, tipoDocumento, numeroDocumento, fechaNacimiento, telefono } = nuevoPac;
    if (!nombre || !apellido || !correo || !contrasena || !numeroDocumento || !fechaNacimiento || !telefono) {
      setError('Todos los campos del paciente son obligatorios');
      toast.error('Todos los campos del paciente son obligatorios');
      return;
    }
    const esMenor = tipoDocumento === 'TI' || tipoDocumento === 'RC';
    if (esMenor && (!nuevoPac.representanteNombre || !nuevoPac.representanteDocumento || !nuevoPac.representanteParentesco)) {
      setError('Paciente menor de edad: se requieren los datos del representante legal');
      toast.error('Paciente menor de edad: se requieren los datos del representante legal');
      return;
    }
    if (!nuevoPac.autorizaTratamientoDatos) {
      setError('El paciente (o su representante legal) debe autorizar el tratamiento de datos');
      toast.error('El paciente (o su representante legal) debe autorizar el tratamiento de datos');
      return;
    }
    setCreandoPaciente(true);
    try {
      const res = await adminService.crearPaciente(nuevoPac);
      const nuevo = res.data.data;
      // Recargar lista de pacientes y seleccionar el nuevo
      const pacRes = await adminService.listarPacientes();
      setPacientes(pacRes.data.data.filter(p => p.activo));
      setIdPaciente(nuevo.idPaciente);
      resetNuevoPac();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al crear paciente';
      setError(msg);
      toast.error(msg);
    } finally {
      setCreandoPaciente(false);
    }
  };

  const handleCrear = async () => {
    if (!idSesion || !idEstudiante) {
      setError('La sesión y el estudiante son obligatorios');
      toast.error('La sesión y el estudiante son obligatorios');
      return;
    }
    try {
      const data: CitaRequest = {
        idSesion: idSesion as number,
        idEstudiante: idEstudiante as number,
        idEstudiante2: idEstudiante2 ? (idEstudiante2 as number) : null,
        idPaciente: idPaciente ? (idPaciente as number) : undefined,
        observaciones: observaciones || undefined,
      };
      if (editandoId) {
        await citaService.editarCita(editandoId, data);
      } else {
        await citaService.crearBloque(data);
      }
      resetForm();
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al guardar cita';
      setError(msg);
      toast.error(msg);
    }
  };

  useSaveShortcut(handleCrear, showForm);

  // Cancelar con observaciones
  const [cancelandoId, setCancelandoId] = useState<number | null>(null);
  const [obsCancelar, setObsCancelar] = useState('');

  // Exportar citas
  const [formatoExport, setFormatoExport] = useState<'csv' | 'excel' | 'pdf'>('excel');
  const [exportando, setExportando] = useState(false);
  const [mostrarEnvioCorreo, setMostrarEnvioCorreo] = useState(false);
  const [correoExport, setCorreoExport] = useState('');
  const [enviandoCorreo, setEnviandoCorreo] = useState(false);

  // Eliminar todas las vencidas
  const [eliminandoVencidas, setEliminandoVencidas] = useState(false);

  const handleEliminarVencidas = async () => {
    if (!confirm('¿Eliminar permanentemente todas las citas vencidas? Esta acción no se puede deshacer. Las que ya tengan una historia clínica asociada se omiten.')) return;
    setEliminandoVencidas(true);
    setError('');
    setMensaje('');
    try {
      const res = await citaService.eliminarVencidas();
      setMensaje(res.data.message);
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al eliminar las citas vencidas';
      setError(msg);
      toast.error(msg);
    } finally {
      setEliminandoVencidas(false);
    }
  };

  const handleExportar = async () => {
    setExportando(true);
    try {
      const res = await citaService.exportarCitas(formatoExport, filtro);
      const extensiones = { csv: 'csv', excel: 'xlsx', pdf: 'pdf' };
      const mimes = {
        csv: 'text/csv;charset=utf-8;',
        excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        pdf: 'application/pdf',
      };
      const blob = new Blob([res.data as unknown as BlobPart], { type: mimes[formatoExport] });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `citas_neovision.${extensiones[formatoExport]}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al exportar';
      setError(msg);
      toast.error(msg);
    } finally {
      setExportando(false);
    }
  };

  const handleEnviarPorCorreo = async () => {
    if (!correoExport.trim()) { toast.error('Escribe el correo de destino'); return; }
    setEnviandoCorreo(true);
    try {
      const res = await citaService.enviarExportPorCorreo(formatoExport, correoExport.trim(), filtro);
      setMensaje(res.data.message);
      toast.success(res.data.message);
      setMostrarEnvioCorreo(false);
      setCorreoExport('');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al enviar el reporte por correo';
      setError(msg);
      toast.error(msg);
    } finally {
      setEnviandoCorreo(false);
    }
  };

  // Reprogramar
  const [reprogramandoId, setReprogramandoId] = useState<number | null>(null);
  const [bloquesReprogramar, setBloquesReprogramar] = useState<CitaResponse[]>([]);

  const handleCancelar = async (idCita: number) => {
    try {
      await citaService.cambiarEstado(idCita, 'CANCELADO', obsCancelar || undefined);
      setCancelandoId(null);
      setObsCancelar('');
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al cancelar';
      setError(msg);
      toast.error(msg);
    }
  };

  const handleIniciarReprogramar = async (idCita: number) => {
    setReprogramandoId(idCita);
    setCancelandoId(null);
    setError('');
    try {
      // Cargar bloques disponibles para seleccionar destino
      const res = await citaService.listarCitas('DISPONIBLE');
      setBloquesReprogramar(res.data.data);
    } catch { setError('Error al cargar bloques disponibles'); toast.error('Error al cargar bloques disponibles'); }
  };

  const handleReprogramar = async (idCitaNueva: number) => {
    if (!reprogramandoId) return;
    try {
      await citaService.reprogramarAdmin(reprogramandoId, idCitaNueva);
      setReprogramandoId(null);
      setBloquesReprogramar([]);
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al reprogramar';
      setError(msg);
      toast.error(msg);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
        <h3 className="text-lg font-semibold" style={{ color: '#14532D' }}>Gestion de Citas</h3>

        <div className="flex items-center gap-2 flex-wrap">
          {/* Export controls */}
          <div className="flex items-center gap-0 rounded-lg overflow-hidden border"
               style={{ borderColor: '#1F7A63' }}>
            <select
              value={formatoExport}
              onChange={e => setFormatoExport(e.target.value as 'csv' | 'excel' | 'pdf')}
              className="text-sm px-3 py-2 outline-none border-r"
              style={{
                borderColor: '#1F7A63',
                color: '#14532D',
                background: '#F0F7F5',
              }}>
              <option value="excel">Excel (.xlsx)</option>
              <option value="csv">CSV (.csv)</option>
              <option value="pdf">PDF</option>
            </select>
            <button
              type="button"
              onClick={handleExportar}
              disabled={exportando}
              className="text-sm font-medium px-4 py-2 transition disabled:opacity-60"
              style={{
                background: exportando ? '#166534' : '#1F7A63',
                color: '#FFFFFF',
              }}
              onMouseEnter={e => { if (!exportando) (e.currentTarget as HTMLButtonElement).style.background = '#166534'; }}
              onMouseLeave={e => { if (!exportando) (e.currentTarget as HTMLButtonElement).style.background = '#1F7A63'; }}>
              {exportando ? 'Exportando...' : '↓ Exportar'}
            </button>
          </div>

          {/* Enviar exportacion por correo */}
          <div className="relative">
            <button type="button" onClick={() => setMostrarEnvioCorreo(v => !v)}
              className="text-sm font-medium px-4 py-2 rounded-lg border transition"
              style={{ borderColor: '#1F7A63', color: '#14532D', background: mostrarEnvioCorreo ? '#DCFCE7' : '#F0F7F5' }}>
              ✉ Enviar por correo
            </button>
            {mostrarEnvioCorreo && (
              <div className="absolute right-0 z-20 mt-2 w-72 bg-white border border-slate-200 rounded-lg shadow-lg p-3 space-y-2">
                <label className="block text-xs font-medium text-slate-600">
                  Enviar reporte en formato <strong>{formatoExport.toUpperCase()}</strong> a:
                </label>
                <input type="email" value={correoExport} onChange={e => setCorreoExport(e.target.value)}
                  placeholder="correo@dominio.com"
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg outline-none focus:border-blue-400" />
                <div className="flex justify-end gap-2">
                  <button type="button" onClick={() => { setMostrarEnvioCorreo(false); setCorreoExport(''); }}
                    className="text-sm text-slate-500 hover:text-slate-700 px-2 py-1">Cancelar</button>
                  <button type="button" onClick={handleEnviarPorCorreo} disabled={enviandoCorreo}
                    className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-3 py-1.5 rounded-lg transition disabled:opacity-60">
                    {enviandoCorreo ? 'Enviando...' : 'Enviar'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Eliminar vencidas */}
          <button type="button" onClick={handleEliminarVencidas} disabled={eliminandoVencidas}
            className="bg-amber-100 hover:bg-amber-200 text-amber-800 text-sm font-medium px-4 py-2 rounded-lg transition disabled:opacity-60">
            {eliminandoVencidas ? 'Eliminando...' : 'Eliminar vencidas'}
          </button>

          {/* Create block */}
          <button type="button" onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
            className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg transition">
            {showForm ? 'Cancelar' : '+ Crear bloque'}
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}
          <button onClick={() => setError('')} className="ml-2 font-bold">&times;</button>
        </div>
      )}
      {mensaje && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {mensaje}
          <button onClick={() => setMensaje('')} className="ml-2 font-bold">&times;</button>
        </div>
      )}

      {/* Formulario crear bloque */}
      {showForm && (
        <div ref={formRef} className="bg-white border border-slate-200 rounded-lg p-5 mb-5 space-y-4">
          <h4 className="font-semibold text-slate-700">{editandoId ? 'Editar cita' : 'Crear bloque de cita'}</h4>

          {/* Sesion */}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1">Sesión *</label>
            <select value={idSesion} onChange={e => { setIdSesion(parseInt(e.target.value) || ''); setIdEstudiante(''); setIdEstudiante2(''); }}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
              <option value="">Seleccionar sesión...</option>
              {sesiones.map(s => (
                <option key={s.idSesion} value={s.idSesion}>
                  {s.fecha} | {s.horaInicio}-{s.horaFin} | {s.nombreEspacio ?? 'Sin espacio'}/{s.nombreSalon ?? 'Sin salon'} | Doc: {s.nombreDocente}
                </option>
              ))}
            </select>
          </div>

          {/* Estudiantes (de la sesion seleccionada): los pacientes se atienden normalmente en pareja */}
          <div className="grid md:grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1">Estudiante *</label>
              <select value={idEstudiante}
                onChange={e => { const v = parseInt(e.target.value) || ''; setIdEstudiante(v); if (v === idEstudiante2) setIdEstudiante2(''); }}
                disabled={!sesionSeleccionada}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400 disabled:bg-slate-100">
                <option value="">Seleccionar estudiante...</option>
                {sesionSeleccionada?.estudiantes.map(e => (
                  <option key={e.idEstudiante} value={e.idEstudiante}>{e.nombre}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1">
                Segundo estudiante <span className="text-slate-400 font-normal">(opcional)</span>
              </label>
              <select value={idEstudiante2} onChange={e => setIdEstudiante2(parseInt(e.target.value) || '')}
                disabled={!sesionSeleccionada}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400 disabled:bg-slate-100">
                <option value="">Sin segundo estudiante</option>
                {sesionSeleccionada?.estudiantes
                  .filter(e => e.idEstudiante !== idEstudiante)
                  .map(e => (
                    <option key={e.idEstudiante} value={e.idEstudiante}>{e.nombre}</option>
                  ))}
              </select>
            </div>
          </div>

          {/* Paciente (opcional) */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-sm font-medium text-slate-600">
                Paciente <span className="text-slate-400 font-normal">(opcional — dejar vacio para bloque disponible)</span>
              </label>
              {!showNuevoPaciente && (
                <button type="button" onClick={() => setShowNuevoPaciente(true)}
                  className="text-xs text-blue-600 hover:text-blue-700 font-medium">
                  + Nuevo paciente
                </button>
              )}
            </div>
            <select value={idPaciente} onChange={e => setIdPaciente(parseInt(e.target.value) || '')}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
              <option value="">Sin paciente (DISPONIBLE)</option>
              {pacientes.map(p => (
                <option key={p.idPaciente} value={p.idPaciente}>{p.nombre} {p.apellido} — {p.numeroDocumento}</option>
              ))}
            </select>

            {/* Mini-formulario nuevo paciente */}
            {showNuevoPaciente && (
              <div className="mt-3 bg-slate-50 border border-slate-200 rounded-lg p-4 space-y-3">
                <h5 className="text-sm font-semibold text-slate-700">Registrar nuevo paciente</h5>
                <div className="grid grid-cols-2 gap-3">
                  <input placeholder="Nombre *" value={nuevoPac.nombre}
                    onChange={e => setNuevoPac({ ...nuevoPac, nombre: e.target.value })}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                  <input placeholder="Apellido *" value={nuevoPac.apellido}
                    onChange={e => setNuevoPac({ ...nuevoPac, apellido: e.target.value })}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <input placeholder="Correo *" type="email" value={nuevoPac.correo}
                    onChange={e => setNuevoPac({ ...nuevoPac, correo: e.target.value })}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                  <input placeholder="Contraseña *" type="password" value={nuevoPac.contrasena}
                    onChange={e => setNuevoPac({ ...nuevoPac, contrasena: e.target.value })}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <select value={nuevoPac.tipoDocumento}
                    onChange={e => setNuevoPac({ ...nuevoPac, tipoDocumento: e.target.value })}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
                    {TIPOS_DOCUMENTO.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                  <input placeholder="Número documento *" value={nuevoPac.numeroDocumento}
                    onChange={e => setNuevoPac({ ...nuevoPac, numeroDocumento: e.target.value })}
                    className="col-span-2 px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Fecha nacimiento *</label>
                    <DateScroller value={nuevoPac.fechaNacimiento} onChange={fecha => setNuevoPac({ ...nuevoPac, fechaNacimiento: fecha })} maxDate={new Date().toISOString().split('T')[0]} />
                  </div>
                  <input placeholder="Teléfono *" value={nuevoPac.telefono}
                    onChange={e => setNuevoPac({ ...nuevoPac, telefono: e.target.value })}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400 mt-auto" />
                </div>
                {(nuevoPac.tipoDocumento === 'TI' || nuevoPac.tipoDocumento === 'RC') && (
                  <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-2">
                    <p className="text-xs text-amber-800">
                      Paciente menor de edad: la autorización de datos debe darla su representante legal.
                    </p>
                    <input placeholder="Nombre representante *" value={nuevoPac.representanteNombre}
                      onChange={e => setNuevoPac({ ...nuevoPac, representanteNombre: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                    <div className="grid grid-cols-2 gap-3">
                      <input placeholder="Documento representante *" value={nuevoPac.representanteDocumento}
                        onChange={e => setNuevoPac({ ...nuevoPac, representanteDocumento: e.target.value })}
                        className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                      <input placeholder="Parentesco *" value={nuevoPac.representanteParentesco}
                        onChange={e => setNuevoPac({ ...nuevoPac, representanteParentesco: e.target.value })}
                        className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
                    </div>
                  </div>
                )}
                <label className="flex items-start gap-2 text-xs text-slate-600">
                  <input type="checkbox" checked={!!nuevoPac.autorizaTratamientoDatos}
                    onChange={e => setNuevoPac({ ...nuevoPac, autorizaTratamientoDatos: e.target.checked })}
                    className="mt-0.5 rounded" />
                  <span>El paciente (o su representante legal) autoriza el tratamiento de sus datos (Ley 1581).</span>
                </label>
                <div className="flex items-center gap-3">
                  <button onClick={handleCrearPaciente} disabled={creandoPaciente}
                    className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-medium px-4 py-2 rounded-lg transition disabled:opacity-50">
                    {creandoPaciente ? 'Creando...' : 'Crear y seleccionar'}
                  </button>
                  <button onClick={resetNuevoPac} className="text-xs text-slate-500 hover:text-slate-700">Cancelar</button>
                </div>
              </div>
            )}
          </div>

          {/* Observaciones */}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1">Observaciones</label>
            <textarea value={observaciones} onChange={e => setObservaciones(e.target.value)} rows={2}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400 resize-none"
              placeholder="Notas opcionales..." />
          </div>

          <div className="flex items-center gap-3">
            <button onClick={handleCrear}
              className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-5 py-2 rounded-lg transition">
              {editandoId ? 'Guardar cambios' : idPaciente ? 'Crear y asignar paciente' : 'Crear bloque disponible'}
            </button>
            <button onClick={resetForm} className="text-sm text-slate-500 hover:text-slate-700">Cancelar</button>
          </div>
        </div>
      )}

      {/* Filtros */}
      <div className="bg-white border border-slate-200 rounded-lg p-4 mb-4 space-y-3">
        <div className="flex gap-2 flex-wrap">
          {ESTADOS.map(e => (
            <button key={e} onClick={() => setFiltro(e)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${
                filtro === e ? 'bg-bosque border-bosque text-white' : 'bg-white border-slate-200 text-slate-500 hover:border-slate-300'
              }`}>
              {e === 'TODAS' ? 'Todas' : e.charAt(0) + e.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        <div className="flex gap-3 flex-wrap">
          <select value={filtroEspacio} onChange={e => setFiltroEspacio(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
            <option value="">Todos los espacios</option>
            {[...new Set(citas.map(c => c.nombreEspacio ?? 'Sin espacio'))].sort().map(e => <option key={e} value={e}>{e}</option>)}
          </select>
          <select value={filtroDocente} onChange={e => setFiltroDocente(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
            <option value="">Todos los docentes</option>
            {[...new Set(citas.map(c => c.nombreDocente))].sort().map(d => <option key={d} value={d}>{d}</option>)}
          </select>
          <input type="text" value={busqueda} onChange={e => setBusqueda(e.target.value)}
            placeholder="Buscar paciente, estudiante, salon..."
            className="flex-1 min-w-[200px] px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
          <div className="flex items-center gap-1.5">
            <input type="date" value={fechaDesde} onChange={e => setFechaDesde(e.target.value)}
              max={fechaHasta || undefined}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
            <span className="text-xs text-slate-400">a</span>
            <input type="date" value={fechaHasta} onChange={e => setFechaHasta(e.target.value)}
              min={fechaDesde || undefined}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
          </div>
          {(filtroEspacio || filtroDocente || busqueda || filtro !== 'TODAS' || fechaDesde || fechaHasta) && (
            <button type="button" onClick={() => { setFiltroEspacio(''); setFiltroDocente(''); setBusqueda(''); setFiltro('TODAS'); setFechaDesde(''); setFechaHasta(''); }}
              className="text-xs text-slate-500 hover:text-slate-700 font-medium px-3 py-2">Limpiar filtros</button>
          )}
        </div>
      </div>

      {/* Lista de citas */}
      <div className="space-y-3">
        {(() => {
          const filtradas = citas.filter(c => {
            if (filtroEspacio && (c.nombreEspacio ?? 'Sin espacio') !== filtroEspacio) return false;
            if (filtroDocente && c.nombreDocente !== filtroDocente) return false;
            if (fechaDesde && c.fechaSesion < fechaDesde) return false;
            if (fechaHasta && c.fechaSesion > fechaHasta) return false;
            if (!busqueda) return true;
            const q = busqueda.toLowerCase();
            return (c.nombrePaciente || '').toLowerCase().includes(q)
              || nombresEstudiantes(c).toLowerCase().includes(q)
              || (c.nombreSalon ?? '').toLowerCase().includes(q)
              || c.horaInicio.includes(q);
          });
          return <>
          <div className="text-xs text-slate-400 mb-2">{filtradas.length} resultado{filtradas.length !== 1 ? 's' : ''}</div>
          {filtradas.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-lg p-8 text-center text-slate-400">
            No hay citas con los filtros seleccionados
          </div>
        ) : filtradas.map(c => (
          <div key={c.idCita} className="bg-white border border-slate-200 rounded-lg p-5">
            <div className="flex items-start justify-between">
              <div className="space-y-1">
                <div className="flex items-center gap-3">
                  <span className="font-semibold text-slate-800">
                    {c.nombrePaciente || 'Sin paciente'}
                  </span>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${estadoColor[c.estado] || ''}`}>
                    {c.estado}
                  </span>
                </div>
                <div className="text-sm text-slate-500">
                  {c.fechaSesion} | {c.horaInicio} - {c.horaFin} | {c.nombreEspacio ?? 'Sin espacio'} / {c.nombreSalon ?? 'Sin salon'}
                </div>
                <div className="text-sm text-slate-500">
                  Docente: {c.nombreDocente} | {c.nombreEstudiante2 ? 'Estudiantes' : 'Estudiante'}: {nombresEstudiantes(c)}
                </div>
                {c.observaciones && <div className="text-sm text-slate-400 italic">"{c.observaciones}"</div>}
                {c.fechaReserva && (
                  <div className="text-xs text-slate-400">Reservada: {c.fechaReserva.replace('T', ' ').substring(0, 16)}</div>
                )}
              </div>

              <div className="flex flex-col items-end gap-2">
                {reprogramandoId === c.idCita ? (
                  <div className="flex flex-col gap-2 items-end w-72">
                    <span className="text-xs font-medium text-slate-600">Mover a bloque disponible:</span>
                    {bloquesReprogramar.length === 0 ? (
                      <span className="text-xs text-slate-400">No hay bloques disponibles</span>
                    ) : (
                      <div className="max-h-40 overflow-y-auto w-full space-y-1">
                        {bloquesReprogramar.map(b => (
                          <button key={b.idCita} onClick={() => handleReprogramar(b.idCita)}
                            className="w-full text-left bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-300 rounded-lg px-3 py-2 text-xs transition">
                            <div className="font-medium text-slate-800">{b.fechaSesion} | {b.horaInicio} - {b.horaFin}</div>
                            <div className="text-slate-500">{b.nombreEspacio ?? 'Sin espacio'} / {b.nombreSalon ?? 'Sin salon'} | Est: {nombresEstudiantes(b)}</div>
                          </button>
                        ))}
                      </div>
                    )}
                    <button onClick={() => { setReprogramandoId(null); setBloquesReprogramar([]); }}
                      className="text-xs text-slate-500 hover:text-slate-700">Volver</button>
                  </div>
                ) : cancelandoId === c.idCita ? (
                  <div className="flex items-center gap-2">
                    <input placeholder="Motivo (opcional)" value={obsCancelar}
                      onChange={e => setObsCancelar(e.target.value)}
                      className="px-2 py-1.5 border border-slate-300 rounded-lg text-xs outline-none w-48" />
                    <button onClick={() => handleCancelar(c.idCita)}
                      className="bg-red-600 hover:bg-red-700 text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                      Confirmar
                    </button>
                    <button onClick={() => { setCancelandoId(null); setObsCancelar(''); }}
                      className="text-xs text-slate-500">X</button>
                  </div>
                ) : (c.estado === 'RESERVADO' || c.estado === 'DISPONIBLE') ? (
                  <div className="flex gap-2">
                    <button type="button" onClick={() => handleEditarCita(c)}
                      className="bg-bosque hover:bg-bosque-dark text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                      Editar
                    </button>
                    {c.estado === 'RESERVADO' && (
                      <>
                        <button
                          onClick={async () => {
                            try {
                              await citaService.cambiarEstado(c.idCita, 'COMPLETADO');
                              await cargar();
                            } catch (err: unknown) {
                              const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
                              const finalMsg = msg || 'Error al marcar como atendida';
                              setError(finalMsg);
                              toast.error(finalMsg);
                            }
                          }}
                          className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                          Atendida
                        </button>
                        <button onClick={() => handleIniciarReprogramar(c.idCita)}
                          className="bg-amber-500 hover:bg-amber-600 text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                          Reprogramar
                        </button>
                      </>
                    )}
                    <button onClick={() => { setCancelandoId(c.idCita); setReprogramandoId(null); }}
                      className="bg-red-600 hover:bg-red-700 text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                      Cancelar
                    </button>
                    <button onClick={async () => { if (confirm('¿Eliminar permanentemente esta cita? Esta acción no se puede deshacer.')) { try { await citaService.eliminarCita(c.idCita); await cargar(); } catch (err: any) { const msg = err.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); } } }}
                      className="bg-red-300 hover:bg-red-400 text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                      Eliminar
                    </button>
                  </div>
                ) : (
                  <button onClick={async () => { if (confirm('¿Eliminar permanentemente esta cita? Esta acción no se puede deshacer.')) { try { await citaService.eliminarCita(c.idCita); await cargar(); } catch (err: any) { const msg = err.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); } } }}
                    className="bg-red-300 hover:bg-red-400 text-white text-xs font-medium px-3 py-1.5 rounded-lg">
                    Eliminar
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
        </>;
        })()}
      </div>
    </div>
  );
}
