import { useState, useEffect, useRef, useMemo } from 'react';
import { useSaveShortcut } from '../../hooks/useSaveShortcut';
import { useFocusShortcut } from '../../hooks/useFocusShortcut';
import { useRowNavigation } from '../../hooks/useRowNavigation';
import { adminService, type EstudianteResponse, type CrearEstudianteRequest } from '../../api/adminService';
import { estudianteService } from '../../api/estudianteService';
import { useToast } from '../../components/ui/Toast';
import CargaMasivaExcel from '../../components/CargaMasivaExcel';

export default function EstudiantesTab() {
  const toast = useToast();
  const [estudiantes, setEstudiantes] = useState<EstudianteResponse[]>([]);
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
  const [filtroSemestre, setFiltroSemestre] = useState('');
  const [form, setForm] = useState<CrearEstudianteRequest>({
    nombre: '', apellido: '', correo: '', cedula: '', semestre: 7, telefono: '',
  });

  const resetForm = () => {
    setForm({ nombre: '', apellido: '', correo: '', cedula: '', semestre: 7, telefono: '' });
    setShowForm(false);
    setEditandoId(null);
  };

  const cargar = async () => {
    try {
      const res = await adminService.listarEstudiantes();
      setEstudiantes(res.data.data);
    } catch (err: any) {
      const backend = err?.response?.data?.message;
      const status = err?.response?.status;
      const msg = backend || `Error al cargar estudiantes${status ? ` (HTTP ${status})` : ''}`;
      setError(msg);
      toast.error(msg);
    }
  };

  useEffect(() => { cargar(); }, []);

  const handleEditar = (e: EstudianteResponse) => {
    setForm({ nombre: e.nombre, apellido: e.apellido, correo: e.correo, cedula: e.cedula, semestre: e.semestre, telefono: e.telefono || '' });
    setEditandoId(e.idEstudiante);
    setShowForm(true);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setSuccess(''); setLoading(true);
    try {
      if (editandoId) {
        await estudianteService.editarEstudianteAdmin(editandoId, {
          nombre: form.nombre,
          apellido: form.apellido,
          cedula: form.cedula,
          semestre: form.semestre,
          telefono: form.telefono,
          correo: form.correo,
        });
        setSuccess('Estudiante actualizado');
      } else {
        await adminService.crearEstudiante(form);
        setSuccess('Estudiante creado exitosamente');
      }
      resetForm();
      await cargar();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al guardar estudiante';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleDesactivar = async (id: number) => {
    try { await adminService.desactivarEstudiante(id); await cargar(); }
    catch { setError('Error al desactivar estudiante'); toast.error('Error al desactivar estudiante'); }
  };

  const searchRef = useRef<HTMLInputElement>(null);
  useFocusShortcut(searchRef);

  const filtrados = useMemo(() => estudiantes.filter(e => {
    if (filtroEstado === 'activo' && !e.activo) return false;
    if (filtroEstado === 'inactivo' && e.activo) return false;
    if (filtroSemestre && e.semestre !== parseInt(filtroSemestre)) return false;
    if (!busqueda) return true;
    const q = busqueda.toLowerCase();
    return `${e.nombre} ${e.apellido}`.toLowerCase().includes(q) || e.cedula.toLowerCase().includes(q) || e.correo.toLowerCase().includes(q);
  }), [estudiantes, filtroEstado, filtroSemestre, busqueda]);

  const { activeIndex, setActiveIndex } = useRowNavigation({
    items: filtrados, onSelect: handleEditar, enabled: !showForm, searchRef,
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-slate-700">Estudiantes</h3>
        <button onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
          className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg transition">
          {showForm ? 'Cancelar' : '+ Agregar estudiante'}
        </button>
      </div>

      <CargaMasivaExcel
        entidad="estudiantes"
        onDescargarPlantilla={adminService.descargarPlantillaEstudiantes}
        onSubir={adminService.cargarEstudiantesExcel}
        onExito={cargar}
      />

      {error && <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">{error}<button onClick={() => setError('')} className="ml-2 font-bold">&times;</button></div>}
      {success && <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4 text-sm">{success}</div>}

      {showForm && (
        <form ref={formRef} onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-lg p-6 mb-6 space-y-4">
          <h4 className="font-semibold text-slate-700">{editandoId ? 'Editar estudiante' : 'Crear estudiante'}</h4>
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
              disabled={!!editandoId} placeholder="estudiante@unbosque.edu.co"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm disabled:bg-slate-100" />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Cédula</label>
              <input type="text" value={form.cedula}
                onChange={e => { if (/^\d{0,15}$/.test(e.target.value)) setForm({...form, cedula: e.target.value}); }}
                required placeholder="1234567890"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Semestre</label>
              <select value={form.semestre} onChange={e => setForm({...form, semestre: parseInt(e.target.value)})} required
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm bg-white">
                <option value={7}>7</option>
                <option value={8}>8</option>
                <option value={9}>9</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Teléfono</label>
              <input type="tel" value={form.telefono} onChange={e => setForm({...form, telefono: e.target.value})}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm" />
            </div>
          </div>
          <div className="flex gap-3">
            <button type="submit" disabled={loading}
              className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-6 py-2 rounded-lg transition disabled:opacity-50">
              {loading ? 'Guardando...' : editandoId ? 'Guardar cambios' : 'Crear estudiante'}
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
        <select value={filtroSemestre} onChange={e => setFiltroSemestre(e.target.value)}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
          <option value="">Todos los semestres</option>
          {[...new Set(estudiantes.map(e => e.semestre))].sort((a, b) => a - b).map(s => <option key={s} value={s}>Semestre {s}</option>)}
        </select>
        <input ref={searchRef} type="text" value={busqueda} onChange={e => setBusqueda(e.target.value)}
          placeholder="Buscar por nombre, correo o cédula... (/)"
          className="flex-1 min-w-[200px] px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400" />
        {(filtroEstado || filtroSemestre || busqueda) && (
          <button type="button" onClick={() => { setFiltroEstado(''); setFiltroSemestre(''); setBusqueda(''); }}
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
              <th className="text-left px-4 py-3 font-medium text-slate-600">Semestre</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Estado</th>
              <th className="text-left px-4 py-3 font-medium text-slate-600">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr><td colSpan={6} className="px-4 py-1 text-xs text-slate-400">{filtrados.length} resultado{filtrados.length !== 1 ? 's' : ''} · ↑↓ para moverse, Enter para editar</td></tr>
            {filtrados.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-8 text-slate-400">No se encontraron estudiantes</td></tr>
            ) : filtrados.map((e, i) => (
              <tr key={e.idEstudiante} onClick={() => setActiveIndex(i)}
                className={`border-b border-slate-100 cursor-pointer ${activeIndex === i ? 'bg-bosque/5 ring-1 ring-inset ring-bosque/30' : ''}`}>
                <td className="px-4 py-3">{e.nombre} {e.apellido}</td>
                <td className="px-4 py-3 text-slate-500">{e.correo}</td>
                <td className="px-4 py-3 text-slate-500">{e.cedula}</td>
                <td className="px-4 py-3 text-slate-500">{e.semestre}</td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-1 rounded-full font-medium ${e.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {e.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button onClick={() => handleEditar(e)}
                      className="text-blue-600 hover:text-blue-700 text-xs font-medium">Editar</button>
                    {e.activo ? (
                      <button onClick={() => handleDesactivar(e.idEstudiante)}
                        className="text-red-600 hover:text-red-700 text-xs font-medium">Desactivar</button>
                    ) : (
                      <button onClick={async () => { await adminService.reactivarEstudiante(e.idEstudiante); await cargar(); }}
                        className="text-emerald-600 hover:text-emerald-700 text-xs font-medium">Reactivar</button>
                    )}
                    <button onClick={async () => { if (confirm('¿Eliminar permanentemente este estudiante? Esta acción no se puede deshacer.')) { try { await adminService.eliminarEstudiante(e.idEstudiante); await cargar(); } catch (err: any) { const msg = err.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); } } }}
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
