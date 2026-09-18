import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useSaveShortcut } from '../../hooks/useSaveShortcut';
import { useFocusShortcut } from '../../hooks/useFocusShortcut';
import { useRowNavigation } from '../../hooks/useRowNavigation';
import { adminService, type PacienteResponse, type CrearPacienteRequest, type CitaResponse } from '../../api/adminService';
import { pacienteService, type EditarPacienteAdminRequest } from '../../api/pacienteService';
import DateScroller from '../../components/DateScroller';
import { Modal } from '../../components/ui/Modal';
import { useToast } from '../../components/ui';

const ESTADO_COLOR: Record<string, string> = {
  DISPONIBLE: 'bg-emerald-100 text-emerald-700',
  RESERVADO:  'bg-blue-100 text-blue-700',
  COMPLETADO: 'bg-slate-100 text-slate-600',
  CANCELADO:  'bg-red-100 text-red-700',
  VENCIDA:    'bg-amber-100 text-amber-700',
};

function HistorialPanel({ paciente, onClose }: { paciente: PacienteResponse; onClose: () => void }) {
  const toast = useToast();
  const [citas, setCitas] = useState<CitaResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    adminService.historialPaciente(paciente.idPaciente)
      .then(res => setCitas(res.data.data ?? []))
      .catch(() => { setError('Error al cargar historial'); toast.error('Error al cargar historial'); })
      .finally(() => setLoading(false));
  }, [paciente.idPaciente]);

  const completadas = citas.filter(c => c.estado === 'COMPLETADO').length;
  const reservadas  = citas.filter(c => c.estado === 'RESERVADO').length;

  return (
    <div className="mt-2 mb-4 bg-slate-50 border border-slate-200 rounded-xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <span className="font-semibold text-slate-800">
            Historial — {paciente.nombre} {paciente.apellido}
          </span>
          {!loading && !error && (
            <span className="ml-3 text-xs text-slate-400">
              {citas.length} cita{citas.length !== 1 ? 's' : ''} · {completadas} completada{completadas !== 1 ? 's' : ''} · {reservadas} reservada{reservadas !== 1 ? 's' : ''}
            </span>
          )}
        </div>
        <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-lg leading-none">&times;</button>
      </div>

      {loading && <p className="text-sm text-slate-400 py-4 text-center">Cargando...</p>}
      {error   && <p className="text-sm text-red-600 py-2">{error}</p>}
      {!loading && !error && citas.length === 0 && (
        <p className="text-sm text-slate-400 py-4 text-center">Este paciente no tiene citas registradas</p>
      )}
      {!loading && !error && citas.length > 0 && (
        <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
          {citas.map(c => (
            <div key={c.idCita} className="bg-white border border-slate-100 rounded-lg px-4 py-3 flex items-start justify-between gap-3">
              <div className="space-y-0.5">
                <div className="text-sm font-medium text-slate-800">
                  {c.fechaSesion} · {c.horaInicio} – {c.horaFin}
                </div>
                <div className="text-xs text-slate-500">
                  {c.nombreEspacio ?? 'Sin espacio'} / {c.nombreSalon ?? 'Sin salon'} · Est: {c.nombreEstudiante2 ? `${c.nombreEstudiante} y ${c.nombreEstudiante2}` : c.nombreEstudiante}
                </div>
                {c.observaciones && (
                  <div className="text-xs text-slate-400 italic">"{c.observaciones}"</div>
                )}
              </div>
              <span className={`shrink-0 text-xs px-2 py-0.5 rounded-full font-medium ${ESTADO_COLOR[c.estado] ?? 'bg-slate-100 text-slate-500'}`}>
                {c.estado}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function PacientesTab() {
  const toast = useToast();
  const [pacientes, setPacientes] = useState<PacienteResponse[]>([]);
  const [showForm, setShowForm] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);
  useSaveShortcut(() => formRef.current?.requestSubmit(), showForm);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  useEffect(() => {
    if (editandoId != null) formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [editandoId]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState<'' | 'activo' | 'inactivo'>('');
  const [filtroTipoDoc, setFiltroTipoDoc] = useState('');
  const [historialPacienteId, setHistorialPacienteId] = useState<number | null>(null);
  const [resetPassPaciente, setResetPassPaciente] = useState<PacienteResponse | null>(null);
  const [nuevaContrasena, setNuevaContrasena] = useState('');
  const [reseteando, setReseteando] = useState(false);
  const FORM_VACIO: CrearPacienteRequest = {
    nombre: '', apellido: '', correo: '', contrasena: '',
    tipoDocumento: 'CC', numeroDocumento: '', fechaNacimiento: '', telefono: '',
    autorizaTratamientoDatos: false,
    representanteNombre: '', representanteDocumento: '', representanteParentesco: '',
  };
  const [form, setForm] = useState<CrearPacienteRequest>(FORM_VACIO);
  const esMenor = form.tipoDocumento === 'TI' || form.tipoDocumento === 'RC';
  // Sin correo no se crea cuenta de acceso: el paciente existe solo como sujeto
  // clinico (tipico en adultos mayores que no tienen correo).
  const quiereCuenta = form.correo.trim() !== '';
  // Al editar: si el paciente ya tenia correo, es su usuario de ingreso.
  const tieneCuenta = !!editandoId && quiereCuenta;

  const resetForm = () => {
    setForm(FORM_VACIO);
    setShowForm(false);
    setEditandoId(null);
  };

  const cargar = async () => {
    try {
      const res = await adminService.listarPacientes();
      setPacientes(res.data.data);
    } catch {
      setError('Error al cargar pacientes');
      toast.error('Error al cargar pacientes');
    }
  };

  useEffect(() => { cargar(); }, []);

  const handleEditar = (p: PacienteResponse) => {
    setForm({
      nombre: p.nombre, apellido: p.apellido, correo: p.correo ?? '', contrasena: '',
      tipoDocumento: p.tipoDocumento, numeroDocumento: p.numeroDocumento, fechaNacimiento: p.fechaNacimiento, telefono: p.telefono,
      representanteNombre: p.representanteNombre ?? '', representanteDocumento: p.representanteDocumento ?? '', representanteParentesco: p.representanteParentesco ?? '',
    });
    setEditandoId(p.idPaciente);
    setShowForm(true);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setSuccess(''); setLoading(true);
    try {
      if (editandoId) {
        const editData: EditarPacienteAdminRequest = {
          nombre: form.nombre,
          apellido: form.apellido,
          tipoDocumento: form.tipoDocumento,
          numeroDocumento: form.numeroDocumento,
          fechaNacimiento: form.fechaNacimiento,
          telefono: form.telefono,
          correo: form.correo,
          representanteNombre: form.representanteNombre,
          representanteDocumento: form.representanteDocumento,
          representanteParentesco: form.representanteParentesco,
        };
        await pacienteService.editarPacienteAdmin(editandoId, editData);
        setSuccess('Paciente actualizado');
      } else {
        await adminService.crearPaciente(form);
        setSuccess('Paciente creado exitosamente');
      }
      resetForm();
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al guardar paciente';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleDesactivar = async (id: number) => {
    try { await adminService.desactivarPaciente(id); await cargar(); }
    catch { setError('Error al desactivar paciente'); toast.error('Error al desactivar paciente'); }
  };

  const handleResetearContrasena = async () => {
    if (!resetPassPaciente) return;
    if (nuevaContrasena.length < 12) { toast.error('Mínimo 12 caracteres'); return; }
    setReseteando(true);
    try {
      await adminService.resetearContrasenaPaciente(resetPassPaciente.idPaciente, nuevaContrasena);
      toast.success('Contraseña restablecida');
      setResetPassPaciente(null);
      setNuevaContrasena('');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Error al restablecer la contraseña');
    } finally {
      setReseteando(false);
    }
  };

  const searchRef = useRef<HTMLInputElement>(null);
  useFocusShortcut(searchRef);

  const filtrados = useMemo(() => pacientes.filter(p => {
    if (filtroEstado === 'activo' && !p.activo) return false;
    if (filtroEstado === 'inactivo' && p.activo) return false;
    if (filtroTipoDoc && p.tipoDocumento !== filtroTipoDoc) return false;
    if (!busqueda) return true;
    const q = busqueda.toLowerCase();
    return `${p.nombre} ${p.apellido}`.toLowerCase().includes(q) || p.numeroDocumento.includes(q) || (p.correo ?? '').toLowerCase().includes(q);
  }), [pacientes, filtroEstado, filtroTipoDoc, busqueda]);

  const { activeIndex, setActiveIndex } = useRowNavigation({
    items: filtrados, onSelect: handleEditar, enabled: !showForm && !resetPassPaciente, searchRef,
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-700">Pacientes</h3>
        <button onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
          className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg transition">
          {showForm ? 'Cancelar' : '+ Agregar paciente'}
        </button>
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">{error}<button onClick={() => setError('')} className="ml-2 font-bold">&times;</button></div>}
      {success && <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4 text-sm">{success}</div>}

      {showForm && (
        <form ref={formRef} onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-lg p-6 mb-6 space-y-4">
          <h4 className="font-semibold text-slate-700">{editandoId ? 'Editar paciente' : 'Crear paciente'}</h4>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Nombre</label>
              <input type="text" value={form.nombre} onChange={e => setForm({...form, nombre: e.target.value})} required
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Apellido</label>
              <input type="text" value={form.apellido} onChange={e => setForm({...form, apellido: e.target.value})} required
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
          </div>
          <div className={editandoId || !quiereCuenta ? '' : 'grid grid-cols-2 gap-4'}>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Correo <span className="font-normal text-slate-400">(opcional)</span>
              </label>
              <input type="email" value={form.correo} onChange={e => setForm({...form, correo: e.target.value})}
                placeholder="Dejalo vacio si el paciente no tiene correo"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
              <p className="text-xs text-slate-500 mt-1">
                {tieneCuenta
                  ? 'Este paciente ingresa al sistema con este correo: si lo cambias, debera volver a iniciar sesion.'
                  : 'Sin correo no se crea cuenta de acceso: el paciente no ingresa al sistema y se le avisa por telefono.'}
              </p>
            </div>
            {!editandoId && quiereCuenta && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Contraseña</label>
                <input type="password" value={form.contrasena} onChange={e => setForm({...form, contrasena: e.target.value})}
                  required minLength={12} placeholder="Mínimo 12 caracteres"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
              </div>
            )}
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Tipo documento</label>
              <select value={form.tipoDocumento} onChange={e => setForm({...form, tipoDocumento: e.target.value})}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm bg-white">
                <option value="CC">CC</option>
                <option value="TI">TI</option>
                <option value="RC">RC</option>
                <option value="CE">CE</option>
                <option value="PA">PA</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Número documento</label>
              <input type="text" value={form.numeroDocumento}
                onChange={e => { if (/^\d{0,15}$/.test(e.target.value)) setForm({...form, numeroDocumento: e.target.value}); }}
                required placeholder="1234567890"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Celular</label>
              <input type="tel" value={form.telefono}
                onChange={e => { if (/^\d{0,10}$/.test(e.target.value)) setForm({...form, telefono: e.target.value}); }}
                required pattern="3\d{9}" maxLength={10} placeholder="3001234567"
                title="Celular colombiano: 10 dígitos, inicia con 3"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Fecha de nacimiento</label>
            <DateScroller value={form.fechaNacimiento} onChange={fecha => setForm({...form, fechaNacimiento: fecha})} maxDate={new Date().toISOString().split('T')[0]} />
          </div>
          {esMenor && (
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-3">
              <p className="text-xs text-amber-800">
                Paciente menor de edad: por la Ley 1581 de 2012, la autorización de tratamiento de datos debe
                darla su representante legal, no el menor.
              </p>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Nombre del representante</label>
                  <input type="text" value={form.representanteNombre} onChange={e => setForm({...form, representanteNombre: e.target.value})} required
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Documento del representante</label>
                  <input type="text" value={form.representanteDocumento}
                    onChange={e => { if (/^\d{0,15}$/.test(e.target.value)) setForm({...form, representanteDocumento: e.target.value}); }}
                    required placeholder="1234567890"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Parentesco</label>
                  <input type="text" value={form.representanteParentesco} onChange={e => setForm({...form, representanteParentesco: e.target.value})} required
                    placeholder="Madre, padre, acudiente..."
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
                </div>
              </div>
            </div>
          )}
          {!editandoId && (
            <label className="flex items-start gap-2 text-sm text-slate-600 bg-slate-50 border border-slate-200 rounded-lg p-3">
              <input type="checkbox" checked={!!form.autorizaTratamientoDatos}
                onChange={e => setForm({ ...form, autorizaTratamientoDatos: e.target.checked })}
                className="mt-0.5 rounded" />
              <span>El paciente autoriza el tratamiento de sus datos personales y clínicos (Ley 1581 de 2012).</span>
            </label>
          )}
          <div className="flex gap-3">
            <button type="submit" disabled={loading}
              className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-6 py-2 rounded-lg transition disabled:opacity-50">
              {loading ? 'Guardando...' : editandoId ? 'Guardar cambios' : 'Crear paciente'}
            </button>
            {editandoId && <button type="button" onClick={resetForm} className="text-sm text-slate-500 hover:text-slate-700">Cancelar</button>}
          </div>
        </form>
      )}

      {/* Filtros */}
      <div className="flex gap-3 mb-4 flex-wrap">
        <select value={filtroEstado} onChange={e => setFiltroEstado(e.target.value as any)}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
          <option value="">Todos los estados</option>
          <option value="activo">Activos</option>
          <option value="inactivo">Inactivos</option>
        </select>
        <select value={filtroTipoDoc} onChange={e => setFiltroTipoDoc(e.target.value)}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
          <option value="">Todos los documentos</option>
          <option value="CC">CC - Cédula de Ciudadanía</option>
          <option value="TI">TI - Tarjeta de Identidad</option>
          <option value="RC">RC - Registro Civil</option>
          <option value="CE">CE - Cédula de Extranjería</option>
          <option value="PA">PA - Pasaporte</option>
        </select>
        <input ref={searchRef} type="text" value={busqueda} onChange={e => setBusqueda(e.target.value)}
          placeholder="Buscar por nombre, correo o documento... (/)"
          className="flex-1 min-w-[200px] px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
        {(filtroEstado || filtroTipoDoc || busqueda) && (
          <button type="button" onClick={() => { setFiltroEstado(''); setFiltroTipoDoc(''); setBusqueda(''); }}
            className="text-xs text-slate-500 hover:text-slate-700 font-medium px-3 py-2">Limpiar filtros</button>
        )}
      </div>

      <div className="bg-white border border-slate-200 rounded-lg overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Nombre</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Correo</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Documento</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Teléfono</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Estado</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr><td colSpan={6} className="px-4 py-1 text-xs text-slate-400">{filtrados.length} resultado{filtrados.length !== 1 ? 's' : ''} · ↑↓ para moverse, Enter para editar</td></tr>
            {filtrados.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-8 text-slate-400">No se encontraron pacientes</td></tr>
            ) : filtrados.map((p, i) => (
              <React.Fragment key={p.idPaciente}>
                <tr onClick={() => setActiveIndex(i)}
                  className={`border-b border-slate-100 cursor-pointer ${activeIndex === i ? 'bg-bosque/5 ring-1 ring-inset ring-bosque/30' : ''}`}>
                  <td className="px-4 py-3">{p.nombre} {p.apellido}</td>
                  <td className="px-4 py-3 text-slate-500">
                    {p.correo ?? <span className="italic text-slate-400">Sin correo · avisar por teléfono</span>}
                  </td>
                  <td className="px-4 py-3 text-slate-500">{p.tipoDocumento} {p.numeroDocumento}</td>
                  <td className="px-4 py-3 text-slate-500">{p.telefono}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-1 rounded-full font-medium ${p.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                      {p.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button onClick={() => setHistorialPacienteId(historialPacienteId === p.idPaciente ? null : p.idPaciente)}
                        className="text-emerald-700 hover:text-emerald-800 text-xs font-medium">Historial</button>
                      <button onClick={() => handleEditar(p)}
                        className="text-blue-600 hover:text-blue-700 text-xs font-medium">Editar</button>
                      {/* Sin correo no hay cuenta: restablecer contrasena o activar/desactivar
                          el acceso no significa nada para estos pacientes. */}
                      {p.correo && (
                        <>
                          <button onClick={() => { setResetPassPaciente(p); setNuevaContrasena(''); }}
                            className="text-slate-500 hover:text-slate-700 text-xs font-medium">Restablecer contraseña</button>
                          {p.activo ? (
                            <button onClick={() => handleDesactivar(p.idPaciente)}
                              className="text-red-600 hover:text-red-700 text-xs font-medium">Desactivar</button>
                          ) : (
                            <button onClick={async () => { await adminService.reactivarPaciente(p.idPaciente); await cargar(); }}
                              className="text-emerald-600 hover:text-emerald-700 text-xs font-medium">Reactivar</button>
                          )}
                        </>
                      )}
                      <button onClick={async () => { if (confirm('¿Eliminar permanentemente este paciente? Esta acción no se puede deshacer.')) { try { await adminService.eliminarPaciente(p.idPaciente); await cargar(); } catch (err: unknown) { const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); } } }}
                        className="text-red-400 hover:text-red-600 text-xs font-medium">Eliminar</button>
                    </div>
                  </td>
                </tr>
                {historialPacienteId === p.idPaciente && (
                  <tr>
                    <td colSpan={6} className="px-4 pb-2">
                      <HistorialPanel paciente={p} onClose={() => setHistorialPacienteId(null)} />
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>

      <Modal open={!!resetPassPaciente} onClose={() => setResetPassPaciente(null)}
        title="Restablecer contraseña" description={resetPassPaciente ? `${resetPassPaciente.nombre} ${resetPassPaciente.apellido}` : ''}>
        <p className="text-xs text-slate-500 mb-3">
          No hay flujo de recuperación por correo: fija aquí una nueva contraseña para el paciente.
        </p>
        <input type="password" value={nuevaContrasena} onChange={e => setNuevaContrasena(e.target.value)}
          placeholder="Mínimo 12 caracteres" minLength={12} autoFocus
          className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm mb-4" />
        <button onClick={handleResetearContrasena} disabled={reseteando}
          className="w-full bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg transition disabled:opacity-50">
          {reseteando ? 'Guardando...' : 'Restablecer'}
        </button>
      </Modal>
    </div>
  );
}
