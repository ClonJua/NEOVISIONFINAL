import { useState, useEffect, useRef, useMemo } from 'react';
import { useSaveShortcut } from '../../hooks/useSaveShortcut';
import { useFocusShortcut } from '../../hooks/useFocusShortcut';
import { useRowNavigation } from '../../hooks/useRowNavigation';
import { adminService, type DocenteResponse, type CrearDocenteRequest } from '../../api/adminService';
import { docenteService } from '../../api/docenteService';
import { especialidadService, type EspecialidadResponse } from '../../api/especialidadService';
import { useToast } from '../../components/ui/Toast';
import CargaMasivaExcel from '../../components/CargaMasivaExcel';

export default function DocentesTab() {
  const toast = useToast();
  const [docentes, setDocentes] = useState<DocenteResponse[]>([]);
  const [especialidades, setEspecialidades] = useState<EspecialidadResponse[]>([]);
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
  const [form, setForm] = useState<CrearDocenteRequest>({
    nombre: '', apellido: '', correo: '', cedula: '', telefono: '', idsEspecialidades: [],
  });

  const resetForm = () => {
    setForm({ nombre: '', apellido: '', correo: '', cedula: '', telefono: '', idsEspecialidades: [] });
    setShowForm(false);
    setEditandoId(null);
  };

  const cargar = async () => {
    try {
      const [resD, resE] = await Promise.all([
        adminService.listarDocentes(),
        especialidadService.listarActivas(),
      ]);
      setDocentes(resD.data.data);
      setEspecialidades(resE.data.data);
    } catch {
      setError('Error al cargar docentes');
      toast.error('Error al cargar docentes');
    }
  };

  useEffect(() => { cargar(); }, []);

  const handleEditar = (d: DocenteResponse) => {
    setForm({
      nombre: d.nombre,
      apellido: d.apellido,
      correo: d.correo,
      cedula: d.cedula || '',
      telefono: d.telefono || '',
      idsEspecialidades: (d.especialidades || []).map(e => e.idEspecialidad),
    });
    setEditandoId(d.idDocente);
    setShowForm(true);
    setError('');
  };

  const toggleEspecialidad = (idEsp: number) => {
    const actuales = form.idsEspecialidades || [];
    const next = actuales.includes(idEsp)
      ? actuales.filter(x => x !== idEsp)
      : [...actuales, idEsp];
    setForm({ ...form, idsEspecialidades: next });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setSuccess(''); setLoading(true);
    try {
      if (editandoId) {
        await docenteService.editarDocenteAdmin(editandoId, {
          nombre: form.nombre,
          apellido: form.apellido,
          cedula: form.cedula || '',
          telefono: form.telefono || '',
          correo: form.correo,
          idsEspecialidades: form.idsEspecialidades || [],
        });
        setSuccess('Docente actualizado');
      } else {
        await adminService.crearDocente(form);
        setSuccess('Docente creado exitosamente');
      }
      resetForm();
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al guardar docente';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleDesactivar = async (id: number) => {
    try { await adminService.desactivarDocente(id); await cargar(); }
    catch { setError('Error al desactivar docente'); toast.error('Error al desactivar docente'); }
  };

  const searchRef = useRef<HTMLInputElement>(null);
  useFocusShortcut(searchRef);

  const filtrados = useMemo(() => docentes.filter(d => {
    if (filtroEstado === 'activo' && !d.activo) return false;
    if (filtroEstado === 'inactivo' && d.activo) return false;
    if (!busqueda) return true;
    const q = busqueda.toLowerCase();
    return `${d.nombre} ${d.apellido}`.toLowerCase().includes(q) || (d.cedula || '').includes(q) || d.correo.toLowerCase().includes(q);
  }), [docentes, filtroEstado, busqueda]);

  const { activeIndex, setActiveIndex } = useRowNavigation({
    items: filtrados, onSelect: handleEditar, enabled: !showForm, searchRef,
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-700">Docentes</h3>
        <button onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
          className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg transition">
          {showForm ? 'Cancelar' : '+ Agregar docente'}
        </button>
      </div>

      <CargaMasivaExcel
        entidad="docentes"
        onDescargarPlantilla={adminService.descargarPlantillaDocentes}
        onSubir={adminService.cargarDocentesExcel}
        onExito={cargar}
      />

      {error && <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">{error}<button onClick={() => setError('')} className="ml-2 font-bold">&times;</button></div>}
      {success && <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4 text-sm">{success}</div>}

      {showForm && (
        <form ref={formRef} onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-lg p-6 mb-6 space-y-4">
          <h4 className="font-semibold text-slate-700">{editandoId ? 'Editar docente' : 'Crear docente'}</h4>
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
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Correo institucional</label>
            <input type="email" value={form.correo} onChange={e => setForm({...form, correo: e.target.value})} required
              disabled={!!editandoId} placeholder="docente@unbosque.edu.co"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm disabled:bg-slate-100" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Cédula</label>
              <input type="text" value={form.cedula}
                onChange={e => { if (/^\d{0,15}$/.test(e.target.value)) setForm({...form, cedula: e.target.value}); }}
                placeholder="1234567890"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Teléfono</label>
              <input type="tel" value={form.telefono} onChange={e => setForm({...form, telefono: e.target.value})}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">Especialidades</label>
            <p className="text-xs text-slate-500 mb-2">
              <strong>Funcional</strong> se asigna automáticamente a todos los docentes (cita por defecto).
              Marca aquí las especialidades adicionales que el docente puede supervisar.
            </p>
            <div className="grid grid-cols-2 gap-2">
              {especialidades.map(e => {
                const checked = (form.idsEspecialidades || []).includes(e.idEspecialidad);
                return (
                  <label key={e.idEspecialidad}
                    className={`flex items-center gap-2 px-3 py-2 rounded-lg border cursor-pointer text-sm transition
                      ${checked ? 'bg-blue-50 border-blue-300 text-blue-800' : 'bg-white border-slate-200 hover:border-slate-300'}
                      ${e.esDefault ? 'opacity-70' : ''}`}>
                    <input type="checkbox"
                      checked={checked || e.esDefault}
                      disabled={e.esDefault}
                      onChange={() => toggleEspecialidad(e.idEspecialidad)} />
                    <span className="flex-1">
                      {e.nombre}
                      {e.esDefault && <span className="ml-1 text-[10px] text-slate-400">(por defecto)</span>}
                      {e.requiereSubtipo && <span className="ml-1 text-[10px] text-amber-600">(con subtipo)</span>}
                    </span>
                  </label>
                );
              })}
              {especialidades.length === 0 && (
                <p className="text-xs text-slate-400 col-span-2">No hay especialidades definidas. Crea el catálogo primero.</p>
              )}
            </div>
          </div>

          <div className="flex gap-3">
            <button type="submit" disabled={loading}
              className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-6 py-2 rounded-lg transition disabled:opacity-50">
              {loading ? 'Guardando...' : editandoId ? 'Guardar cambios' : 'Crear docente'}
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
        <input ref={searchRef} type="text" value={busqueda} onChange={e => setBusqueda(e.target.value)}
          placeholder="Buscar por nombre, correo o cédula... (/)"
          className="flex-1 min-w-[200px] px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
        {(filtroEstado || busqueda) && (
          <button type="button" onClick={() => { setFiltroEstado(''); setBusqueda(''); }}
            className="text-xs text-slate-500 hover:text-slate-700 font-medium px-3 py-2">Limpiar filtros</button>
        )}
      </div>

      <div className="bg-white border border-slate-200 rounded-lg overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Nombre</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Correo</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Cédula</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Especialidades</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Estado</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr><td colSpan={6} className="px-4 py-1 text-xs text-slate-400">{filtrados.length} resultado{filtrados.length !== 1 ? 's' : ''} · ↑↓ para moverse, Enter para editar</td></tr>
            {filtrados.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-8 text-slate-400">No se encontraron docentes</td></tr>
            ) : filtrados.map((d, i) => (
              <tr key={d.idDocente} onClick={() => setActiveIndex(i)}
                className={`border-b border-slate-100 cursor-pointer ${activeIndex === i ? 'bg-bosque/5 ring-1 ring-inset ring-bosque/30' : ''}`}>
                <td className="px-4 py-3">{d.nombre} {d.apellido}</td>
                <td className="px-4 py-3 text-slate-500">{d.correo}</td>
                <td className="px-4 py-3 text-slate-500">{d.cedula || '-'}</td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {(d.especialidades || []).map(e => (
                      <span key={e.idEspecialidad}
                        className={`text-[11px] px-2 py-0.5 rounded-full font-medium
                          ${e.esDefault ? 'bg-slate-100 text-slate-600' : 'bg-blue-50 text-blue-700'}`}>
                        {e.nombre}
                      </span>
                    ))}
                    {(d.especialidades || []).length === 0 && <span className="text-xs text-slate-400">—</span>}
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-1 rounded-full font-medium ${d.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {d.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button onClick={() => handleEditar(d)}
                      className="text-blue-600 hover:text-blue-700 text-xs font-medium">Editar</button>
                    {d.activo ? (
                      <button onClick={() => handleDesactivar(d.idDocente)}
                        className="text-red-600 hover:text-red-700 text-xs font-medium">Desactivar</button>
                    ) : (
                      <button onClick={async () => { await adminService.reactivarDocente(d.idDocente); await cargar(); }}
                        className="text-emerald-600 hover:text-emerald-700 text-xs font-medium">Reactivar</button>
                    )}
                    <button onClick={async () => { if (confirm('¿Eliminar permanentemente este docente? Esta acción no se puede deshacer.')) { try { await adminService.eliminarDocente(d.idDocente); await cargar(); } catch (err: any) { const msg = err.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); } } }}
                      className="text-red-400 hover:text-red-600 text-xs font-medium">Eliminar</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
