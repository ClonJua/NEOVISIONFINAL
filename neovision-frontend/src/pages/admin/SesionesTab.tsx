import { useState, useEffect, useRef } from 'react';
import { useSaveShortcut } from '../../hooks/useSaveShortcut';
import { sesionService, type SesionResponse, type SesionRequest } from '../../api/sesionService';
import { espacioService, type EspacioResponse } from '../../api/espacioService';
import { adminService, type DocenteResponse, type EstudianteResponse } from '../../api/adminService';
import DateScroller from '../../components/DateScroller';
import { useToast } from '../../components/ui/Toast';
import { HelpTip } from '../../components/ui/HelpTip';

const C = {
  primary:   '#1F7A63',
  dark:      '#14532D',
  bg:        '#F0F7F5',
  bgLight:   '#F7FBF9',
  border:    '#A7D7C5',
  chip:      '#DCFCE7',
  chipText:  '#166534',
};

const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes'];

function getMonday(d: Date): Date {
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  return new Date(d.getFullYear(), d.getMonth(), diff);
}

function formatDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

function addMonths(d: Date, n: number): Date {
  const r = new Date(d);
  r.setMonth(r.getMonth() + n);
  return r;
}

const MAX_FECHA_SESION = formatDate(addMonths(new Date(), 6));

export default function SesionesTab() {
  const toast = useToast();
  const [sesiones, setSesiones] = useState<SesionResponse[]>([]);
  const [espacios, setEspacios] = useState<EspacioResponse[]>([]);
  const [docentes, setDocentes] = useState<DocenteResponse[]>([]);
  const [estudiantes, setEstudiantes] = useState<EstudianteResponse[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const [filtroEspacio, setFiltroEspacio] = useState('');
  const [filtroDocente, setFiltroDocente] = useState('');
  const [filtroEstudiante, setFiltroEstudiante] = useState('');

  // Buscador + filtro por semestre para elegir estudiantes al crear una sesion:
  // con 70 estudiantes, marcarlos uno a uno en una nube de chips es inviable.
  const [buscarSel, setBuscarSel] = useState('');
  const [semestreSel, setSemestreSel] = useState('');
  const [buscarSelRec, setBuscarSelRec] = useState('');
  const [semestreSelRec, setSemestreSelRec] = useState('');

  const [lunes, setLunes] = useState(() => getMonday(new Date()));
  // Vista "lista": todas las sesiones activas en una sola tabla, sin importar
  // la semana — para encontrar una sesion perdida sin tener que navegar
  // semana por semana (la grilla semanal solo muestra 5 dias a la vez).
  const [vista, setVista] = useState<'semana' | 'lista'>('semana');
  const [ordenDesc, setOrdenDesc] = useState(false);

  const [showForm, setShowForm] = useState(false);
  const [showFormRecurrente, setShowFormRecurrente] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);
  const formRecurrenteRef = useRef<HTMLFormElement>(null);
  useSaveShortcut(() => formRef.current?.requestSubmit(), showForm);
  useSaveShortcut(() => formRecurrenteRef.current?.requestSubmit(), showFormRecurrente);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  useEffect(() => {
    if (editandoId != null) formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [editandoId]);
  const [form, setForm] = useState<SesionRequest>({
    idSalon: null, idDocente: 0, fecha: '', cuposTotales: 1, idsEstudiantes: [],
  });
  const [formRecurrente, setFormRecurrente] = useState({
    diaSemana: 'MONDAY' as const,
    fechaInicio: '',
    fechaFin: '',
    horaInicio: '08:00',
    horaFin: '12:00',
    cuposTotales: 3,
    idDocente: 0,
    idSalon: null as number | null,
    idsEstudiantes: [] as number[],
  });
  const [selectedEspacio, setSelectedEspacio] = useState<number | null>(null);

  const semanaLabel = `${formatDate(lunes)} al ${formatDate(addDays(lunes, 4))}`;
  const diasSemana = Array.from({ length: 5 }, (_, i) => addDays(lunes, i));

  const cargar = async () => {
    try {
      const [sesRes, espRes, docRes, estRes] = await Promise.all([
        sesionService.listar(),
        espacioService.listar(),
        adminService.listarDocentes(),
        adminService.listarEstudiantes(),
      ]);
      setSesiones(sesRes.data.data);
      setEspacios(espRes.data.data.filter(e => e.activo));
      setDocentes(docRes.data.data.filter(d => d.activo));
      setEstudiantes(estRes.data.data.filter(e => e.activo));
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string }; status?: number }; message?: string };
      const msg = e?.response?.data?.message || `Error al cargar datos${e?.response?.status ? ` (HTTP ${e.response.status})` : ''}: ${e?.message || 'sin detalle'}`;
      setError(msg);
      toast.error(msg);
    }
  };

  useEffect(() => { cargar(); }, []);

  // Compartido entre la grilla semanal y la vista de lista completa. El texto
  // de busqueda revisa salon, hora, docente, fecha y estudiantes, para poder
  // encontrar una sesion sin saber de antemano en cual de los 3 selects filtrarla.
  const coincideFiltros = (s: SesionResponse) => {
    if (filtroEspacio && (s.nombreEspacio ?? 'Sin salon') !== filtroEspacio) return false;
    if (filtroDocente && s.nombreDocente !== filtroDocente) return false;
    if (filtroEstudiante && !s.estudiantes.some(e => e.nombre === filtroEstudiante)) return false;
    if (!busqueda) return true;
    const q = busqueda.toLowerCase();
    return (s.nombreSalon ?? '').toLowerCase().includes(q)
      || (s.horaInicio ?? '').includes(q)
      || s.nombreDocente.toLowerCase().includes(q)
      || s.fecha.includes(q)
      || s.estudiantes.some(e => e.nombre.toLowerCase().includes(q));
  };

  const sesionesPorFecha = (fecha: string) =>
    sesiones.filter(s => s.fecha === fecha && s.activo).filter(coincideFiltros);

  const todasLasSesiones = sesiones.filter(s => s.activo).filter(coincideFiltros).sort((a, b) => {
    const cmp = a.fecha === b.fecha
      ? (a.horaInicio ?? '').localeCompare(b.horaInicio ?? '')
      : a.fecha.localeCompare(b.fecha);
    return ordenDesc ? -cmp : cmp;
  });

  const espaciosUnicos = [...new Set(sesiones.filter(s => s.activo).map(s => s.nombreEspacio ?? 'Sin salon'))].sort();
  const docentesUnicos = [...new Set(sesiones.filter(s => s.activo).map(s => s.nombreDocente))].sort();
  const estudiantesUnicos = [...new Set(sesiones.filter(s => s.activo).flatMap(s => s.estudiantes.map(e => e.nombre)))].sort();

  const sesionesPorEspacio = (fecha: string) => {
    const del_dia = sesionesPorFecha(fecha);
    const grupos: Record<string, SesionResponse[]> = {};
    del_dia.forEach(s => {
      const key = s.nombreEspacio ?? 'Sin salon';
      if (!grupos[key]) grupos[key] = [];
      grupos[key].push(s);
    });
    return grupos;
  };

  const salonesDelEspacio = selectedEspacio
    ? espacios.find(e => e.idEspacio === selectedEspacio)?.salones.filter(s => s.activo) || []
    : [];

  const toggleEstudiante = (id: number) => {
    setForm(prev => ({
      ...prev,
      idsEstudiantes: prev.idsEstudiantes.includes(id)
        ? prev.idsEstudiantes.filter(x => x !== id)
        : [...prev.idsEstudiantes, id],
    }));
  };

  const toggleEstudianteRecurrente = (id: number) => {
    setFormRecurrente(prev => ({
      ...prev,
      idsEstudiantes: prev.idsEstudiantes.includes(id)
        ? prev.idsEstudiantes.filter(x => x !== id)
        : [...prev.idsEstudiantes, id],
    }));
  };

  const semestresDisponibles = [...new Set(estudiantes.map(e => e.semestre))].sort((a, b) => a - b);

  const filtrarEstudiantes = (busqueda: string, semestre: string) => estudiantes.filter(e =>
    (semestre === '' || e.semestre === Number(semestre)) &&
    (busqueda.trim() === '' || `${e.nombre} ${e.apellido}`.toLowerCase().includes(busqueda.trim().toLowerCase()))
  );

  const resetForm = () => {
    setForm({ idSalon: null, idDocente: 0, fecha: '', horaInicio: '', horaFin: '', cuposTotales: 1, idsEstudiantes: [] });
    setSelectedEspacio(null);
    setShowForm(false);
    setEditandoId(null);
  };

  const handleEditarSesion = (s: SesionResponse) => {
    const esp = s.idSalon != null ? espacios.find(e => e.salones.some(sl => sl.idSalon === s.idSalon)) : null;
    setSelectedEspacio(esp?.idEspacio ?? null);
    setForm({ idSalon: s.idSalon, idDocente: s.idDocente, fecha: s.fecha, horaInicio: s.horaInicio || '', horaFin: s.horaFin || '', cuposTotales: s.cuposTotales, idsEstudiantes: s.estudiantes.map(e => e.idEstudiante) });
    setEditandoId(s.idSesion);
    setShowForm(true);
    setError('');
  };

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.idDocente || !form.fecha) { setError('Completa docente y fecha'); toast.error('Completa docente y fecha'); return; }
    setError(''); setLoading(true);
    try {
      if (editandoId) {
        await sesionService.editar(editandoId, form);
        setSuccess('Sesion actualizada');
      } else {
        const res = await sesionService.crear(form);
        const s = res.data.data;
        const bloquesCreados = s.cuposTotales - s.cuposDisponibles;
        setSuccess(bloquesCreados > 0
          ? `Sesion creada con ${bloquesCreados} de ${s.cuposTotales} bloques generados automaticamente${bloquesCreados < s.cuposTotales ? ' (agrega mas estudiantes o crea el resto en Citas)' : ''}`
          : 'Sesion creada sin bloques: asigna estudiantes para generarlos automaticamente, o crealos en Citas');
      }
      resetForm();
      await cargar();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Error al guardar';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCrearRecurrente = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formRecurrente.idDocente || !formRecurrente.fechaInicio || !formRecurrente.fechaFin) {
      setError('Completa todos los campos requeridos');
      toast.error('Completa todos los campos requeridos');
      return;
    }
    setError(''); setLoading(true);
    try {
      const response = await sesionService.crearRecurrente(formRecurrente);
      setSuccess(response.data.data.mensaje);
      setFormRecurrente({ diaSemana: 'MONDAY', fechaInicio: '', fechaFin: '', horaInicio: '08:00', horaFin: '12:00', cuposTotales: 3, idDocente: 0, idSalon: null, idsEstudiantes: [] });
      setShowFormRecurrente(false);
      await cargar();
      setTimeout(() => setSuccess(''), 5000);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Error al crear sesiones recurrentes';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const crearEnDia = (fecha: string) => {
    setForm({ ...form, fecha });
    setShowForm(true);
  };

  const inputCls = 'w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-[#1F7A63]';

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold" style={{ color: C.dark }}>Sesiones de Práctica</h3>
        <div className="flex gap-2">
          <div className="flex rounded-lg overflow-hidden border" style={{ borderColor: C.border }}>
            <button type="button" onClick={() => setVista('semana')}
              className="text-sm font-medium px-3 py-2 transition"
              style={vista === 'semana' ? { background: C.primary, color: '#fff' } : { background: '#fff', color: C.dark }}>
              Semana
            </button>
            <button type="button" onClick={() => setVista('lista')}
              className="text-sm font-medium px-3 py-2 transition"
              style={vista === 'lista' ? { background: C.primary, color: '#fff' } : { background: '#fff', color: C.dark }}>
              Todas ({sesiones.filter(s => s.activo).length})
            </button>
          </div>
          <button type="button" onClick={() => setShowFormRecurrente(!showFormRecurrente)}
            className="text-white text-sm font-medium px-4 py-2 rounded-lg transition"
            style={{ background: showFormRecurrente ? C.dark : C.primary }}
            onMouseEnter={e => (e.currentTarget.style.background = C.dark)}
            onMouseLeave={e => (e.currentTarget.style.background = showFormRecurrente ? C.dark : C.primary)}>
            {showFormRecurrente ? 'Cancelar' : '📅 Recurrente'}
          </button>
          <button type="button" onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
            className="text-white text-sm font-medium px-4 py-2 rounded-lg transition"
            style={{ background: showForm ? C.dark : C.primary }}
            onMouseEnter={e => (e.currentTarget.style.background = C.dark)}
            onMouseLeave={e => (e.currentTarget.style.background = showForm ? C.dark : C.primary)}>
            {showForm ? 'Cancelar' : '+ Nueva sesion'}
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
          {error}<button onClick={() => setError('')} className="ml-2 font-bold">&times;</button>
        </div>
      )}
      {success && (
        <div className="px-4 py-3 rounded-lg mb-4 text-sm" style={{ background: C.chip, color: C.chipText, border: `1px solid ${C.border}` }}>
          {success}
        </div>
      )}

      {/* Formulario crear / editar */}
      {showForm && (
        <form ref={formRef} onSubmit={handleCrear} className="bg-white rounded-xl p-5 mb-5 space-y-3"
          style={{ border: `1px solid ${C.border}` }}>
          <h4 className="font-semibold" style={{ color: C.dark }}>
            {editandoId ? 'Editar sesion' : 'Nueva sesion'}
          </h4>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Espacio <span className="text-slate-400">(opcional)</span></label>
              <select value={selectedEspacio ?? ''} onChange={e => { setSelectedEspacio(parseInt(e.target.value) || null); setForm({...form, idSalon: null}); }}
                className={inputCls}>
                <option value="">Sin espacio</option>
                {espacios.map(e => <option key={e.idEspacio} value={e.idEspacio}>{e.nombre}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Salon <span className="text-slate-400">(opcional)</span></label>
              <select value={form.idSalon ?? ''} onChange={e => setForm({...form, idSalon: e.target.value ? parseInt(e.target.value) : null})}
                disabled={!selectedEspacio} className={inputCls + ' disabled:bg-slate-100'}>
                <option value="">Sin salon</option>
                {salonesDelEspacio.map(s => <option key={s.idSalon} value={s.idSalon}>{s.nombre}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Docente *</label>
              <select value={form.idDocente || ''} onChange={e => setForm({...form, idDocente: parseInt(e.target.value)})}
                className={inputCls}>
                <option value="">Seleccionar...</option>
                {docentes.map(d => <option key={d.idDocente} value={d.idDocente}>{d.nombre} {d.apellido}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Fecha *</label>
              <DateScroller value={form.fecha} onChange={fecha => setForm({...form, fecha})} minDate={formatDate(new Date())} maxDate={MAX_FECHA_SESION} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                Cupos
                <HelpTip text="Pacientes que admite la jornada, no estudiantes. Cada cupo es un bloque: un paciente atendido por una pareja. Con N estudiantes se generan N/2 bloques como máximo; sube los cupos solo si habrá varias rondas." />
              </label>
              <input type="number" min={1} value={form.cuposTotales} onChange={e => setForm({...form, cuposTotales: parseInt(e.target.value)})}
                className={inputCls} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Hora inicio <span className="text-slate-400">(opcional)</span></label>
              <input type="time" value={form.horaInicio || ''} onChange={e => setForm({...form, horaInicio: e.target.value})}
                className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Hora fin <span className="text-slate-400">(opcional)</span></label>
              <input type="time" value={form.horaFin || ''} onChange={e => setForm({...form, horaFin: e.target.value})}
                className={inputCls} />
            </div>
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-medium text-slate-600">Estudiantes</label>
              <span className="text-[11px] text-slate-500">{form.idsEstudiantes.length} de {estudiantes.length} seleccionados</span>
            </div>
            <div className="flex flex-wrap gap-2 mb-2">
              <input type="text" placeholder="Buscar por nombre..." value={buscarSel}
                onChange={e => setBuscarSel(e.target.value)}
                className="flex-1 min-w-[140px] px-2.5 py-1 text-xs border border-slate-300 rounded-md focus:ring-2 focus:ring-bosque outline-none" />
              <select value={semestreSel} onChange={e => setSemestreSel(e.target.value)}
                className="px-2 py-1 text-xs border border-slate-300 rounded-md">
                <option value="">Todos los semestres</option>
                {semestresDisponibles.map(s => <option key={s} value={s}>{s}° semestre</option>)}
              </select>
              <button type="button"
                onClick={() => setForm(prev => ({ ...prev, idsEstudiantes: [...new Set([...prev.idsEstudiantes, ...filtrarEstudiantes(buscarSel, semestreSel).map(e => e.idEstudiante)])] }))}
                className="px-2.5 py-1 text-xs font-medium rounded-md border border-slate-300 text-slate-600 hover:bg-slate-50">
                Seleccionar visibles ({filtrarEstudiantes(buscarSel, semestreSel).length})
              </button>
              <button type="button" onClick={() => setForm(prev => ({ ...prev, idsEstudiantes: [] }))}
                className="px-2.5 py-1 text-xs font-medium rounded-md border border-slate-300 text-slate-600 hover:bg-slate-50">
                Quitar todos
              </button>
            </div>
            <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto">
              {filtrarEstudiantes(buscarSel, semestreSel).map(e => (
                <button type="button" key={e.idEstudiante} onClick={() => toggleEstudiante(e.idEstudiante)}
                  className="px-2.5 py-1 rounded-full text-xs font-medium border transition"
                  style={form.idsEstudiantes.includes(e.idEstudiante)
                    ? { background: C.chip, borderColor: C.border, color: C.chipText }
                    : { background: '#fff', borderColor: '#E2E8F0', color: '#64748B' }}>
                  {e.nombre} {e.apellido}
                </button>
              ))}
              {filtrarEstudiantes(buscarSel, semestreSel).length === 0 && (
                <span className="text-xs text-slate-400 py-1">Sin resultados para ese filtro.</span>
              )}
            </div>
          </div>
          <div className="flex gap-3 pt-1">
            <button type="submit" disabled={loading}
              className="text-white text-sm font-medium px-5 py-2 rounded-lg disabled:opacity-50 transition"
              style={{ background: C.primary }}
              onMouseEnter={e => (e.currentTarget.style.background = C.dark)}
              onMouseLeave={e => (e.currentTarget.style.background = C.primary)}>
              {loading ? 'Guardando...' : editandoId ? 'Guardar cambios' : 'Enviar'}
            </button>
            {editandoId && (
              <button type="button" onClick={resetForm} className="text-sm text-slate-500 hover:text-slate-700">Cancelar</button>
            )}
          </div>
        </form>
      )}

      {/* Formulario recurrente */}
      {showFormRecurrente && (
        <form ref={formRecurrenteRef} onSubmit={handleCrearRecurrente} className="rounded-xl p-5 mb-5 space-y-3"
          style={{ background: C.bg, border: `1px solid ${C.border}` }}>
          <h4 className="font-semibold" style={{ color: C.dark }}>Crear Sesiones Recurrentes</h4>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Día de la Semana</label>
              <select value={formRecurrente.diaSemana} onChange={e => setFormRecurrente({...formRecurrente, diaSemana: e.target.value as 'MONDAY'})}
                className={inputCls}>
                <option value="MONDAY">Lunes</option>
                <option value="TUESDAY">Martes</option>
                <option value="WEDNESDAY">Miercoles</option>
                <option value="THURSDAY">Jueves</option>
                <option value="FRIDAY">Viernes</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Docente *</label>
              <select value={formRecurrente.idDocente} onChange={e => setFormRecurrente({...formRecurrente, idDocente: parseInt(e.target.value)})}
                className={inputCls}>
                <option value={0}>Seleccionar...</option>
                {docentes.map(d => <option key={d.idDocente} value={d.idDocente}>{d.nombre} {d.apellido}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Fecha Inicio *</label>
              <input type="date" value={formRecurrente.fechaInicio} onChange={e => setFormRecurrente({...formRecurrente, fechaInicio: e.target.value})}
                min={formatDate(new Date())} max={MAX_FECHA_SESION} className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Fecha Fin *</label>
              <input type="date" value={formRecurrente.fechaFin} onChange={e => setFormRecurrente({...formRecurrente, fechaFin: e.target.value})}
                min={formatDate(new Date())} max={MAX_FECHA_SESION} className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Hora Inicio</label>
              <input type="time" value={formRecurrente.horaInicio} onChange={e => setFormRecurrente({...formRecurrente, horaInicio: e.target.value})}
                className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Hora Fin</label>
              <input type="time" value={formRecurrente.horaFin} onChange={e => setFormRecurrente({...formRecurrente, horaFin: e.target.value})}
                className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                Cupos por Sesion
                <HelpTip text="Pacientes que admite cada jornada, no estudiantes. Cada cupo es un bloque: un paciente atendido por una pareja." />
              </label>
              <input type="number" min={1} value={formRecurrente.cuposTotales} onChange={e => setFormRecurrente({...formRecurrente, cuposTotales: parseInt(e.target.value)})}
                className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Espacio (opcional)</label>
              <select value={selectedEspacio ?? ''} onChange={e => { setSelectedEspacio(parseInt(e.target.value) || null); setFormRecurrente({...formRecurrente, idSalon: null}); }}
                className={inputCls}>
                <option value="">Sin espacio</option>
                {espacios.map(e => <option key={e.idEspacio} value={e.idEspacio}>{e.nombre}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Salon <span className="text-slate-400">(opcional)</span></label>
              <select value={formRecurrente.idSalon ?? ''} onChange={e => setFormRecurrente({...formRecurrente, idSalon: e.target.value ? parseInt(e.target.value) : null})}
                disabled={!selectedEspacio} className={inputCls + ' disabled:bg-slate-100'}>
                <option value="">Sin salon</option>
                {salonesDelEspacio.map(s => <option key={s.idSalon} value={s.idSalon}>{s.nombre}</option>)}
              </select>
            </div>
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-medium text-slate-600">Estudiantes <span className="text-slate-400">(opcional)</span></label>
              <span className="text-[11px] text-slate-500">{formRecurrente.idsEstudiantes.length} de {estudiantes.length} seleccionados</span>
            </div>
            <div className="flex flex-wrap gap-2 mb-2">
              <input type="text" placeholder="Buscar por nombre..." value={buscarSelRec}
                onChange={e => setBuscarSelRec(e.target.value)}
                className="flex-1 min-w-[140px] px-2.5 py-1 text-xs border border-slate-300 rounded-md focus:ring-2 focus:ring-bosque outline-none" />
              <select value={semestreSelRec} onChange={e => setSemestreSelRec(e.target.value)}
                className="px-2 py-1 text-xs border border-slate-300 rounded-md">
                <option value="">Todos los semestres</option>
                {semestresDisponibles.map(s => <option key={s} value={s}>{s}° semestre</option>)}
              </select>
              <button type="button"
                onClick={() => setFormRecurrente(prev => ({ ...prev, idsEstudiantes: [...new Set([...prev.idsEstudiantes, ...filtrarEstudiantes(buscarSelRec, semestreSelRec).map(e => e.idEstudiante)])] }))}
                className="px-2.5 py-1 text-xs font-medium rounded-md border border-slate-300 text-slate-600 hover:bg-slate-50">
                Seleccionar visibles ({filtrarEstudiantes(buscarSelRec, semestreSelRec).length})
              </button>
              <button type="button" onClick={() => setFormRecurrente(prev => ({ ...prev, idsEstudiantes: [] }))}
                className="px-2.5 py-1 text-xs font-medium rounded-md border border-slate-300 text-slate-600 hover:bg-slate-50">
                Quitar todos
              </button>
            </div>
            <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto">
              {filtrarEstudiantes(buscarSelRec, semestreSelRec).map(e => (
                <button type="button" key={e.idEstudiante} onClick={() => toggleEstudianteRecurrente(e.idEstudiante)}
                  className="px-2.5 py-1 rounded-full text-xs font-medium border transition"
                  style={formRecurrente.idsEstudiantes.includes(e.idEstudiante)
                    ? { background: C.chip, borderColor: C.border, color: C.chipText }
                    : { background: '#fff', borderColor: '#E2E8F0', color: '#64748B' }}>
                  {e.nombre} {e.apellido}
                </button>
              ))}
              {filtrarEstudiantes(buscarSelRec, semestreSelRec).length === 0 && (
                <span className="text-xs text-slate-400 py-1">Sin resultados para ese filtro.</span>
              )}
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={loading}
              className="text-white text-sm font-medium px-5 py-2 rounded-lg disabled:opacity-50 transition"
              style={{ background: C.primary }}
              onMouseEnter={e => (e.currentTarget.style.background = C.dark)}
              onMouseLeave={e => (e.currentTarget.style.background = C.primary)}>
              {loading ? 'Creando...' : 'Enviar'}
            </button>
          </div>
        </form>
      )}

      {/* Navegacion semanal */}
      {vista === 'semana' && (
      <div className="flex items-center justify-between bg-white rounded-lg px-4 py-3 mb-4"
        style={{ border: `1px solid ${C.border}` }}>
        <button type="button" onClick={() => setLunes(addDays(lunes, -7))}
          className="text-sm font-medium px-3 py-1 rounded transition"
          style={{ color: C.primary }}
          onMouseEnter={e => (e.currentTarget.style.background = C.bg)}
          onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
          &larr; Anterior
        </button>
        <div className="text-center">
          <span className="font-semibold text-slate-700">Semana: {semanaLabel}</span>
          <button type="button" onClick={() => setLunes(getMonday(new Date()))}
            className="ml-3 text-xs font-medium" style={{ color: C.primary }}>
            Hoy
          </button>
        </div>
        <button type="button" onClick={() => setLunes(addDays(lunes, 7))}
          className="text-sm font-medium px-3 py-1 rounded transition"
          style={{ color: C.primary }}
          onMouseEnter={e => (e.currentTarget.style.background = C.bg)}
          onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
          Siguiente &rarr;
        </button>
      </div>
      )}

      {/* Filtros */}
      <div className="flex gap-3 mb-4 flex-wrap">
        <select value={filtroEspacio} onChange={e => setFiltroEspacio(e.target.value)} className={inputCls.replace('w-full', '')}>
          <option value="">Todos los espacios</option>
          {espaciosUnicos.map(e => <option key={e} value={e}>{e}</option>)}
        </select>
        <select value={filtroDocente} onChange={e => setFiltroDocente(e.target.value)} className={inputCls.replace('w-full', '')}>
          <option value="">Todos los docentes</option>
          {docentesUnicos.map(d => <option key={d} value={d}>{d}</option>)}
        </select>
        <select value={filtroEstudiante} onChange={e => setFiltroEstudiante(e.target.value)} className={inputCls.replace('w-full', '')}>
          <option value="">Todos los estudiantes</option>
          {estudiantesUnicos.map(e => <option key={e} value={e}>{e}</option>)}
        </select>
        <input type="text" value={busqueda} onChange={e => setBusqueda(e.target.value)}
          placeholder="Buscar por salon, hora, docente, fecha o estudiante..."
          className="flex-1 min-w-[200px] px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-[#1F7A63]" />
        {(filtroEspacio || filtroDocente || filtroEstudiante || busqueda) && (
          <button type="button" onClick={() => { setFiltroEspacio(''); setFiltroDocente(''); setFiltroEstudiante(''); setBusqueda(''); }}
            className="text-xs font-medium px-3 py-2" style={{ color: C.primary }}>
            Limpiar filtros
          </button>
        )}
      </div>

      {/* Grilla semanal */}
      {vista === 'semana' && (
      <div className="space-y-3">
        {diasSemana.map(dia => {
          const fechaStr = formatDate(dia);
          const porEspacio = sesionesPorEspacio(fechaStr);
          const espacioKeys = Object.keys(porEspacio);
          const esHoy = fechaStr === formatDate(new Date());

          return (
            <div key={fechaStr} className="bg-white rounded-lg overflow-hidden"
              style={{ border: `1px solid ${esHoy ? C.primary : '#E2E8F0'}`, boxShadow: esHoy ? `0 0 0 1px ${C.border}` : undefined }}>
              {/* Dia header */}
              <div className="flex items-center justify-between px-4 py-2.5 border-b border-slate-100"
                style={{ background: esHoy ? C.bg : '#F8FAFC' }}>
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-sm" style={{ color: esHoy ? C.dark : '#475569' }}>
                    {DIAS[dia.getDay()]} {dia.getDate()}/{dia.getMonth() + 1}
                  </span>
                  {esHoy && (
                    <span className="text-xs text-white px-2 py-0.5 rounded-full"
                      style={{ background: C.primary }}>Hoy</span>
                  )}
                  {espacioKeys.length === 0 && (
                    <span className="text-xs text-slate-400 ml-2">Sin sesiones</span>
                  )}
                </div>
                <button onClick={() => crearEnDia(fechaStr)}
                  className="text-xs font-medium transition" style={{ color: C.primary }}
                  onMouseEnter={e => (e.currentTarget.style.color = C.dark)}
                  onMouseLeave={e => (e.currentTarget.style.color = C.primary)}>
                  + Agregar
                </button>
              </div>

              {/* Sesiones del dia agrupadas por espacio */}
              {espacioKeys.length > 0 && (
                <div className="divide-y divide-slate-100">
                  {espacioKeys.map(espacio => (
                    <div key={espacio} className="px-4 py-3">
                      <div className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: C.primary }}>
                        {espacio}
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
                        {porEspacio[espacio]
                          .sort((a, b) => (a.horaInicio ?? '').localeCompare(b.horaInicio ?? ''))
                          .map(s => (
                            <div key={s.idSesion}
                              className="rounded-lg p-3 transition group"
                              style={{ border: `1px solid ${C.border}`, background: C.bgLight }}>
                              <div className="flex items-center justify-between mb-1.5">
                                <span className="text-sm font-semibold" style={{ color: C.dark }}>
                                  {s.nombreSalon ?? 'Sin salon'}
                                </span>
                                <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition">
                                  <button type="button" onClick={() => handleEditarSesion(s)}
                                    className="text-xs font-medium" style={{ color: C.primary }}>Editar</button>
                                  <button type="button"
                                    onClick={async () => {
                                      if (confirm('¿Eliminar permanentemente esta sesión? Esta acción no se puede deshacer.')) {
                                        try { await sesionService.eliminar(s.idSesion); await cargar(); }
                                        catch (err: unknown) { const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); }
                                      }
                                    }}
                                    className="text-xs text-red-400 hover:text-red-600">Eliminar</button>
                                </div>
                              </div>

                              {/* Horario */}
                              {(s.horaInicio || s.horaFin) && (
                                <div className="text-xs font-medium mb-1" style={{ color: C.primary }}>
                                  🕐 {s.horaInicio ?? '?'} – {s.horaFin ?? '?'}
                                </div>
                              )}

                              <div className="text-xs text-slate-500 mb-1.5">
                                Doc: {s.nombreDocente} · Cupos: {s.cuposDisponibles}/{s.cuposTotales}
                              </div>
                              <div className="flex flex-wrap gap-1">
                                {s.estudiantes.map(e => (
                                  <span key={e.idEstudiante}
                                    className="text-[10px] px-2 py-0.5 rounded-full border"
                                    style={{ background: C.chip, color: C.chipText, borderColor: C.border }}>
                                    {e.nombre}
                                  </span>
                                ))}
                                {s.estudiantes.length === 0 && (
                                  <span className="text-[10px] text-slate-400">Sin estudiantes</span>
                                )}
                              </div>
                            </div>
                          ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
      )}

      {/* Vista de lista: todas las sesiones activas en una sola tabla, para
          encontrar una sesion perdida sin navegar semana por semana. */}
      {vista === 'lista' && (
        <div className="bg-white rounded-lg overflow-x-auto" style={{ border: `1px solid ${C.border}` }}>
          <table className="w-full text-sm">
            <thead className="border-b" style={{ background: C.bg, borderColor: C.border }}>
              <tr>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>
                  <button type="button" onClick={() => setOrdenDesc(!ordenDesc)} className="flex items-center gap-1 hover:underline">
                    Fecha {ordenDesc ? '↓' : '↑'}
                  </button>
                </th>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>Horario</th>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>Docente</th>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>Espacio / Salon</th>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>Cupos</th>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>Estudiantes</th>
                <th className="text-left px-4 py-3 font-medium" style={{ color: C.dark }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td colSpan={7} className="px-4 py-1 text-xs text-slate-400">
                  {todasLasSesiones.length} de {sesiones.filter(s => s.activo).length} sesiones activas
                </td>
              </tr>
              {todasLasSesiones.length === 0 ? (
                <tr><td colSpan={7} className="text-center py-8 text-slate-400">No se encontraron sesiones con esos filtros</td></tr>
              ) : todasLasSesiones.map(s => {
                const hoy = formatDate(new Date());
                const esHoy = s.fecha === hoy;
                const esPasada = s.fecha < hoy;
                return (
                  <tr key={s.idSesion} className="border-b border-slate-100">
                    <td className="px-4 py-3">
                      <span className={esPasada ? 'text-slate-400' : 'font-medium'} style={!esPasada ? { color: C.dark } : undefined}>
                        {s.fecha}
                      </span>
                      {esHoy && (
                        <span className="ml-2 text-[10px] text-white px-1.5 py-0.5 rounded-full" style={{ background: C.primary }}>Hoy</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {s.horaInicio || s.horaFin ? `${s.horaInicio ?? '?'} – ${s.horaFin ?? '?'}` : '-'}
                    </td>
                    <td className="px-4 py-3">{s.nombreDocente}</td>
                    <td className="px-4 py-3 text-slate-500">
                      {s.nombreEspacio ?? 'Sin espacio'}{s.nombreSalon ? ` · ${s.nombreSalon}` : ''}
                    </td>
                    <td className="px-4 py-3 text-slate-500">{s.cuposDisponibles}/{s.cuposTotales}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1 max-w-xs">
                        {s.estudiantes.map(e => (
                          <span key={e.idEstudiante} className="text-[10px] px-2 py-0.5 rounded-full border"
                            style={{ background: C.chip, color: C.chipText, borderColor: C.border }}>
                            {e.nombre}
                          </span>
                        ))}
                        {s.estudiantes.length === 0 && <span className="text-xs text-slate-400">Sin estudiantes</span>}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <button type="button" onClick={() => { setVista('semana'); handleEditarSesion(s); }}
                          className="text-xs font-medium" style={{ color: C.primary }}>Editar</button>
                        <button type="button"
                          onClick={async () => {
                            if (confirm('¿Eliminar permanentemente esta sesión? Esta acción no se puede deshacer.')) {
                              try { await sesionService.eliminar(s.idSesion); await cargar(); }
                              catch (err: unknown) { const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Error al eliminar'; setError(msg); toast.error(msg); }
                            }
                          }}
                          className="text-xs text-red-400 hover:text-red-600">Eliminar</button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
