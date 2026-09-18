import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { useMsal } from '@azure/msal-react';
import { citaService, nombresEstudiantes, type CitaResponse } from '../../api/citaService';
import { pacienteService, type EditarPacienteRequest } from '../../api/pacienteService';
import type { PacienteResponse } from '../../api/adminService';
import { especialidadService, type EspecialidadResponse, SUBTIPOS_CONTROL } from '../../api/especialidadService';
import { SolicitudCopiaCard } from '../../components/historia/SolicitudCopiaCard';
import {
  Button, Card, StatCard, EstadoBadge, EmptyState, Modal, Textarea, Input,
  CitaSkeleton, useToast,
  CalendarIcon, ClockIcon, MapPinIcon, UserIcon, UsersIcon, LogOutIcon, XIcon,
  SearchIcon, PlusIcon, HistoryIcon, HomeIcon, SparklesIcon, CheckCircleIcon, InfoIcon, AyudaModal,
} from '../../components/ui';

type Tab = 'activas' | 'agendar' | 'historial';

// ---------------- Date helpers ----------------

function parseLocalDate(yyyy_mm_dd: string): Date {
  const [y, m, d] = yyyy_mm_dd.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function startOfDay(d: Date): Date {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

function diffDays(a: Date, b: Date): number {
  return Math.round((startOfDay(a).getTime() - startOfDay(b).getTime()) / 86400000);
}

const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'];
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

function formatFechaLarga(yyyy_mm_dd: string): string {
  const d = parseLocalDate(yyyy_mm_dd);
  return `${DIAS[d.getDay()]} ${d.getDate()} de ${MESES[d.getMonth()]}`;
}

function etiquetaRelativa(yyyy_mm_dd: string): string | null {
  const hoy = new Date();
  const d = parseLocalDate(yyyy_mm_dd);
  const delta = diffDays(d, hoy);
  if (delta === 0) return 'Hoy';
  if (delta === 1) return 'Mañana';
  if (delta === -1) return 'Ayer';
  if (delta > 1 && delta <= 7) return 'Esta semana';
  if (delta > 7 && delta <= 14) return 'Proxima semana';
  return null;
}

function agruparPorFecha(citas: CitaResponse[]): { fecha: string; items: CitaResponse[] }[] {
  const map = new Map<string, CitaResponse[]>();
  citas.forEach((c) => {
    const arr = map.get(c.fechaSesion) || [];
    arr.push(c);
    map.set(c.fechaSesion, arr);
  });
  return Array.from(map.entries())
    .map(([fecha, items]) => ({
      fecha,
      items: items.sort((a, b) => a.horaInicio.localeCompare(b.horaInicio)),
    }))
    .sort((a, b) => a.fecha.localeCompare(b.fecha));
}

// ---------------- Page ----------------

export default function DashboardPage() {
  const { correo, rol, logout } = useAuth();
  const { instance } = useMsal();
  const navigate = useNavigate();
  const toast = useToast();

  const [tab, setTab] = useState<Tab>('activas');
  const [misCitas, setMisCitas] = useState<CitaResponse[]>([]);
  const [disponibles, setDisponibles] = useState<CitaResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [cargandoCitas, setCargandoCitas] = useState(true);
  const [cargandoDisponibles, setCargandoDisponibles] = useState(false);

  // Cancelar modal
  const [citaACancelar, setCitaACancelar] = useState<CitaResponse | null>(null);
  const [obsCancelar, setObsCancelar] = useState('');

  // Reservar modal
  const [citaAReservar, setCitaAReservar] = useState<CitaResponse | null>(null);
  // Subtipo de control elegido al confirmar (solo si la especialidad lo requiere)
  const [subtipoControl, setSubtipoControl] = useState<string>('');

  // Catalogo de especialidades activas + filtro actual del paciente
  const [especialidades, setEspecialidades] = useState<EspecialidadResponse[]>([]);
  const [idEspecialidadFiltro, setIdEspecialidadFiltro] = useState<number | ''>('');

  // Busqueda en disponibles
  const [query, setQuery] = useState('');

  // Editar perfil (paciente)
  const [showEditPerfil, setShowEditPerfil] = useState(false);
  const [loadingPerfil, setLoadingPerfil] = useState(false);
  const [, setPerfil] = useState<PacienteResponse | null>(null);
  const [formPerfil, setFormPerfil] = useState<EditarPacienteRequest>({
    nombre: '',
    apellido: '',
    telefono: '',
  });

  const esPaciente = rol === 'ROLE_PACIENTE';

  const handleLogout = () => {
    logout(instance);
    navigate('/login');
  };

  const cargarMisCitas = async () => {
    setCargandoCitas(true);
    try {
      const res = await citaService.misCitas();
      setMisCitas(res.data.data);
    } catch {
      toast.error('Error al cargar citas', 'Intenta recargar la pagina.');
    } finally {
      setCargandoCitas(false);
    }
  };

  const cargarDisponibles = async (idEsp?: number | '') => {
    setCargandoDisponibles(true);
    try {
      const idEspecialidad = (idEsp === '' || idEsp === undefined) ? undefined : Number(idEsp);
      const res = await citaService.bloquesDisponibles(idEspecialidad);
      setDisponibles(res.data.data);
    } catch {
      toast.error('Error al cargar disponibilidad');
    } finally {
      setCargandoDisponibles(false);
    }
  };

  const cargarEspecialidades = async () => {
    try {
      const res = await especialidadService.listarActivas();
      setEspecialidades(res.data.data);
      // Default: seleccionar Funcional (esDefault=true) si existe
      const def = res.data.data.find(e => e.esDefault);
      if (def) setIdEspecialidadFiltro(def.idEspecialidad);
    } catch {
      // No bloquea el flujo: si falla, simplemente no hay filtro
    }
  };

  const cargarPerfil = async () => {
    try {
      const res = await pacienteService.miPerfil();
      setPerfil(res.data.data);
      setFormPerfil({
        nombre: res.data.data.nombre,
        apellido: res.data.data.apellido,
        telefono: res.data.data.telefono,
      });
    } catch {
      toast.error('Error al cargar perfil');
    }
  };

  useEffect(() => {
    if (esPaciente) {
      cargarMisCitas();
      cargarPerfil();
      cargarEspecialidades();
    }
  }, []);

  useEffect(() => {
    if (tab === 'agendar') cargarDisponibles(idEspecialidadFiltro);
  }, [tab, idEspecialidadFiltro]);

  const especialidadElegida = useMemo(
    () => especialidades.find(e => e.idEspecialidad === idEspecialidadFiltro) || null,
    [especialidades, idEspecialidadFiltro],
  );

  const confirmarReserva = async () => {
    if (!citaAReservar) return;
    // Si la especialidad requiere subtipo (CONTROL) y no se eligió, bloquear.
    if (especialidadElegida?.requiereSubtipo && !subtipoControl) {
      toast.error('Subtipo requerido', 'Selecciona el tipo de control.');
      return;
    }
    setLoading(true);
    try {
      await citaService.reservarBloque(citaAReservar.idCita, {
        idEspecialidad: especialidadElegida?.idEspecialidad,
        subtipoControl: especialidadElegida?.requiereSubtipo ? subtipoControl : undefined,
      });
      toast.success('Cita reservada', `${formatFechaLarga(citaAReservar.fechaSesion)} - ${citaAReservar.horaInicio}`);
      setCitaAReservar(null);
      setSubtipoControl('');
      await cargarMisCitas();
      setTab('activas');
    } catch (err: any) {
      toast.error('No se pudo reservar', err.response?.data?.message || 'Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const confirmarCancelacion = async () => {
    if (!citaACancelar) return;
    if (!obsCancelar.trim()) {
      toast.error('Motivo requerido', 'Debes indicar por que cancelas la cita.');
      return;
    }
    setLoading(true);
    try {
      await citaService.cancelarPaciente(citaACancelar.idCita, obsCancelar);
      toast.success('Cita cancelada', 'El bloque quedo disponible para otros pacientes.');
      setCitaACancelar(null);
      setObsCancelar('');
      await cargarMisCitas();
    } catch (err: any) {
      toast.error('No se pudo cancelar', err.response?.data?.message || 'Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const citasActivas = useMemo(() => misCitas.filter((c) => c.estado === 'RESERVADO'), [misCitas]);
  const citasHistorial = useMemo(
    () => misCitas.filter((c) => ['COMPLETADO', 'CANCELADO', 'VENCIDA'].includes(c.estado)),
    [misCitas],
  );

  const disponiblesFiltrados = useMemo(() => {
    if (!query.trim()) return disponibles;
    const q = query.toLowerCase();
    return disponibles.filter((c) =>
      [c.fechaSesion, c.nombreEspacio ?? '', c.nombreSalon ?? '', c.nombreDocente, nombresEstudiantes(c)]
        .join(' ')
        .toLowerCase()
        .includes(q),
    );
  }, [disponibles, query]);

  const proximaCita = useMemo(() => {
    const hoy = new Date();
    return citasActivas
      .slice()
      .sort((a, b) => {
        const da = parseLocalDate(a.fechaSesion).getTime() + timeToMinutes(a.horaInicio) * 60_000;
        const db = parseLocalDate(b.fechaSesion).getTime() + timeToMinutes(b.horaInicio) * 60_000;
        return da - db;
      })
      .find((c) => {
        const d = parseLocalDate(c.fechaSesion);
        return d.getTime() + timeToMinutes(c.horaFin) * 60_000 >= hoy.getTime();
      });
  }, [citasActivas]);

  // ---------------- Render NO paciente ----------------
  if (!esPaciente) {
    return (
      <div className="min-h-screen bg-slate-50">
        <TopBar correo={correo} rol={rol} onLogout={handleLogout} />
        <main className="max-w-7xl mx-auto px-4 py-12">
          <Card className="text-center py-16">
            <h2 className="text-2xl font-semibold text-slate-800">Bienvenido a NeoVision</h2>
            <p className="text-slate-500 mt-2">Has iniciado sesion como {rol?.replace('ROLE_', '')}.</p>
          </Card>
        </main>
      </div>
    );
  }

  // ---------------- Render PACIENTE ----------------

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
      <TopBar correo={correo} rol={rol} onLogout={handleLogout} onEditPerfil={() => setShowEditPerfil(!showEditPerfil)} />

      {/* Modal Editar Perfil */}
      {showEditPerfil && (
        <div className="bg-white border border-slate-200 rounded-lg p-5 mb-6 mx-4 mt-4 space-y-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-slate-800">Editar Perfil</h3>
            <button onClick={() => setShowEditPerfil(false)} className="text-slate-500 hover:text-slate-700 text-xl">&times;</button>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Nombre</label>
              <input type="text" value={formPerfil.nombre} onChange={e => setFormPerfil({...formPerfil, nombre: e.target.value})}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Apellido</label>
              <input type="text" value={formPerfil.apellido} onChange={e => setFormPerfil({...formPerfil, apellido: e.target.value})}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Telefono</label>
            <input type="text" value={formPerfil.telefono} onChange={e => setFormPerfil({...formPerfil, telefono: e.target.value})}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={async () => {
              setLoadingPerfil(true);
              try {
                await pacienteService.editarPerfil(formPerfil);
                toast.success('Perfil actualizado', 'Tus cambios han sido guardados.');
                setShowEditPerfil(false);
                await cargarPerfil();
              } catch (err: any) {
                toast.error('Error', err.response?.data?.message || 'Error al actualizar perfil');
              } finally {
                setLoadingPerfil(false);
              }
            }} disabled={loadingPerfil}
              className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg disabled:opacity-50">
              {loadingPerfil ? 'Guardando...' : 'Guardar cambios'}
            </button>
            <button onClick={() => setShowEditPerfil(false)} className="text-sm text-slate-500 hover:text-slate-700">Cancelar</button>
          </div>
        </div>
      )}

      <main className="max-w-6xl mx-auto px-4 py-8">
        {/* Hero / saludo */}
        <div className="flex items-start justify-between flex-wrap gap-4 mb-6">
          <div>
            <h1 className="text-3xl font-bold text-slate-800">Hola 👋</h1>
            <p className="text-slate-500 mt-1">Gestiona tus citas de una mejor manera. </p>
          </div>
          {tab === 'activas' && (
            <Button
              size="lg"
              leftIcon={<PlusIcon size={18} />}
              onClick={() => setTab('agendar')}
            >
              Agendar nueva cita
            </Button>
          )}
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <StatCard
            label="Citas activas"
            value={citasActivas.length}
            icon={<CalendarIcon size={22} />}
            tone="blue"
          />
          <StatCard
            label="Historial"
            value={citasHistorial.length}
            icon={<HistoryIcon size={22} />}
            tone="slate"
          />
          <StatCard
            label="Proxima cita"
            value={proximaCita ? (etiquetaRelativa(proximaCita.fechaSesion) || formatFechaCorta(proximaCita.fechaSesion)) : '—'}
            icon={<SparklesIcon size={22} />}
            tone="indigo"
          />
        </div>

        {/* Tabs */}
        <div className="flex gap-1 p-1 bg-slate-100 rounded-xl mb-6 w-fit">
          <TabButton active={tab === 'activas'} onClick={() => setTab('activas')} icon={<HomeIcon size={15} />}>
            Activas {citasActivas.length > 0 && <Count>{citasActivas.length}</Count>}
          </TabButton>
          <TabButton active={tab === 'agendar'} onClick={() => setTab('agendar')} icon={<PlusIcon size={15} />}>
            Agendar
          </TabButton>
          <TabButton active={tab === 'historial'} onClick={() => setTab('historial')} icon={<HistoryIcon size={15} />}>
            Historial {citasHistorial.length > 0 && <Count>{citasHistorial.length}</Count>}
          </TabButton>
        </div>

        {/* ACTIVAS */}
        {tab === 'activas' && (
          <Section>
            {cargandoCitas ? (
              <div className="space-y-3"><CitaSkeleton /><CitaSkeleton /></div>
            ) : citasActivas.length === 0 ? (
              <EmptyState
                icon={<CalendarIcon size={24} />}
                title="No tienes citas activas"
                description="Cuando reserves un horario, aparecera aqui."
                action={
                  <Button leftIcon={<PlusIcon size={16} />} onClick={() => setTab('agendar')}>
                    Agendar mi primera cita
                  </Button>
                }
              />
            ) : (
              <GrupoCitas
                grupos={agruparPorFecha(citasActivas)}
                renderAccion={(c) => (
                  <Button variant="outline" size="sm" onClick={() => setCitaACancelar(c)}>
                    Cancelar
                  </Button>
                )}
              />
            )}
          </Section>
        )}

        {/* AGENDAR */}
        {tab === 'agendar' && (
          <Section>
            <div className="mb-4 grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Tipo de cita (especialidad)</label>
                <select
                  value={idEspecialidadFiltro}
                  onChange={(e) => {
                    const v = e.target.value === '' ? '' : Number(e.target.value);
                    setIdEspecialidadFiltro(v as number | '');
                  }}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
                  {especialidades.map(e => (
                    <option key={e.idEspecialidad} value={e.idEspecialidad}>
                      {e.nombre}{e.esDefault ? ' (por defecto)' : ''}
                    </option>
                  ))}
                </select>
                {especialidadElegida?.observacionRecordatorio && (
                  <p className="text-xs text-amber-700 mt-1">
                    📋 {especialidadElegida.observacionRecordatorio}
                  </p>
                )}
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Buscar</label>
                <Input
                  placeholder="Fecha, espacio, salon, docente..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  leftIcon={<SearchIcon size={16} />}
                />
              </div>
            </div>
            {cargandoDisponibles ? (
              <div className="space-y-3"><CitaSkeleton /><CitaSkeleton /><CitaSkeleton /></div>
            ) : disponiblesFiltrados.length === 0 ? (
              <EmptyState
                icon={<SearchIcon size={24} />}
                title={query ? 'Sin resultados' : 'No hay horarios disponibles'}
                description={query ? 'Intenta con otra palabra clave.' : 'Por ahora no hay bloques libres. Vuelve a intentar mas tarde.'}
              />
            ) : (
              <GrupoCitas
                grupos={agruparPorFecha(disponiblesFiltrados)}
                renderAccion={(c) => (
                  <Button size="sm" leftIcon={<CheckCircleIcon size={14} />} onClick={() => setCitaAReservar(c)}>
                    Reservar
                  </Button>
                )}
              />
            )}
          </Section>
        )}

        {/* HISTORIAL */}
        {tab === 'historial' && (
          <Section>
            {cargandoCitas ? (
              <div className="space-y-3"><CitaSkeleton /><CitaSkeleton /></div>
            ) : citasHistorial.length === 0 ? (
              <EmptyState
                icon={<HistoryIcon size={24} />}
                title="Sin historial"
                description="Las citas completadas, canceladas o vencidas apareceran aqui."
              />
            ) : (
              <GrupoCitas
                grupos={agruparPorFecha(citasHistorial).reverse()}
                mostrarEstado
              />
            )}
          </Section>
        )}

        <div className="mt-8">
          <SolicitudCopiaCard />
        </div>
      </main>

      {/* Modal confirmar reserva */}
      <Modal
        open={!!citaAReservar}
        onClose={() => { setCitaAReservar(null); setSubtipoControl(''); }}
        title="Confirmar reserva"
        description="Revisa los detalles antes de confirmar tu cita."
        footer={
          <>
            <Button variant="ghost" onClick={() => { setCitaAReservar(null); setSubtipoControl(''); }}>Cancelar</Button>
            <Button loading={loading} leftIcon={<CheckCircleIcon size={15} />} onClick={confirmarReserva}>
              Confirmar reserva
            </Button>
          </>
        }
      >
        {citaAReservar && (
          <div className="space-y-3">
            <DetallesCita cita={citaAReservar} />
            {especialidadElegida && (
              <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3">
                <div className="text-xs font-semibold text-blue-700 uppercase tracking-wide">Tipo de cita</div>
                <div className="text-sm font-medium text-blue-900">{especialidadElegida.nombre}</div>
                {especialidadElegida.observacionRecordatorio && (
                  <p className="text-xs text-amber-800 mt-1">
                    📋 {especialidadElegida.observacionRecordatorio}
                  </p>
                )}
              </div>
            )}
            {especialidadElegida?.requiereSubtipo && (
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">¿Para qué es el control?</label>
                <select
                  value={subtipoControl}
                  onChange={(e) => setSubtipoControl(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-blue-400">
                  <option value="">Selecciona...</option>
                  {SUBTIPOS_CONTROL.map(s => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <p className="text-xs text-slate-500 mt-1">Esta información se incluirá en el recordatorio por correo.</p>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Modal cancelar */}
      <Modal
        open={!!citaACancelar}
        onClose={() => { setCitaACancelar(null); setObsCancelar(''); }}
        title="Cancelar cita"
        description="Esta accion liberara el bloque para otros pacientes."
        footer={
          <>
            <Button variant="ghost" onClick={() => { setCitaACancelar(null); setObsCancelar(''); }}>
              Volver
            </Button>
            <Button variant="danger" loading={loading} leftIcon={<XIcon size={15} />} onClick={confirmarCancelacion}>
              Si, cancelar
            </Button>
          </>
        }
      >
        {citaACancelar && (
          <div className="space-y-4">
            <DetallesCita cita={citaACancelar} />
            <Textarea
              label="Motivo de la cancelacion"
              placeholder="Ej: tengo un imprevisto laboral"
              value={obsCancelar}
              onChange={(e) => setObsCancelar(e.target.value)}
              rows={3}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}

// ---------------- Subcomponents ----------------

function TopBar({ correo, rol, onLogout, onEditPerfil }: { correo: string | null; rol: string | null; onLogout: () => void; onEditPerfil?: () => void }) {
  const initials = (correo || '?').slice(0, 2).toUpperCase();
  const esPaciente = rol === 'ROLE_PACIENTE';
  const [ayudaOpen, setAyudaOpen] = useState(false);
  return (
    <nav className="bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-bosque to-bosqueOrange flex items-center justify-center text-white">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          </div>
          <div>
            <div className="text-base font-bold text-slate-800 leading-none">NeoVision</div>
            <div className="text-[10px] text-slate-400 uppercase tracking-wider">Clinica Optometria</div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="hidden sm:flex items-center gap-2 px-2.5 py-1.5 bg-slate-50 rounded-lg">
            <div className="w-7 h-7 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-xs font-semibold">
              {initials}
            </div>
            <div className="text-xs">
              <div className="text-slate-800 font-medium leading-tight">{correo}</div>
              <div className="text-slate-400 leading-tight">{rol?.replace('ROLE_', '')}</div>
            </div>
          </div>
          {esPaciente && onEditPerfil && (
            <button onClick={onEditPerfil} className="text-sm text-blue-600 hover:text-blue-700 font-medium">
              Editar perfil
            </button>
          )}
          {esPaciente && (
            <button onClick={() => setAyudaOpen(true)} aria-label="Ayuda"
              className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg">
              <InfoIcon size={18} />
            </button>
          )}
          <button
            onClick={onLogout}
            className="text-slate-500 hover:text-red-600 hover:bg-red-50 p-2 rounded-lg transition"
            aria-label="Cerrar sesion"
            title="Cerrar sesion"
          >
            <LogOutIcon size={18} />
          </button>
          {esPaciente && <AyudaModal open={ayudaOpen} onClose={() => setAyudaOpen(false)} rol="paciente" />}
        </div>
      </div>
    </nav>
  );
}

function TabButton({
  active, onClick, icon, children,
}: { active: boolean; onClick: () => void; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`
        inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition
        ${active ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'}
      `}
    >
      {icon} {children}
    </button>
  );
}

function Count({ children }: { children: React.ReactNode }) {
  return (
    <span className="ml-1 inline-flex items-center justify-center min-w-[18px] h-[18px] text-[10px] font-bold rounded-full bg-blue-100 text-blue-700 px-1">
      {children}
    </span>
  );
}

function Section({ children }: { children: React.ReactNode }) {
  return <div className="animate-[fadeIn_200ms_ease-out]">{children}</div>;
}

function GrupoCitas({
  grupos,
  renderAccion,
  mostrarEstado,
}: {
  grupos: { fecha: string; items: CitaResponse[] }[];
  renderAccion?: (c: CitaResponse) => React.ReactNode;
  mostrarEstado?: boolean;
}) {
  return (
    <div className="space-y-6">
      {grupos.map(({ fecha, items }) => {
        const rel = etiquetaRelativa(fecha);
        return (
          <div key={fecha}>
            <div className="flex items-baseline gap-3 mb-2 px-1">
              <h3 className="text-sm font-semibold text-slate-800 capitalize">{formatFechaLarga(fecha)}</h3>
              {rel && (
                <span className="text-[10px] font-bold uppercase tracking-wider text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full">
                  {rel}
                </span>
              )}
            </div>
            <div className="space-y-3">
              {items.map((c) => (
                <CitaCard key={c.idCita} cita={c} accion={renderAccion?.(c)} mostrarEstado={mostrarEstado} />
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function CitaCard({
  cita, accion, mostrarEstado,
}: { cita: CitaResponse; accion?: React.ReactNode; mostrarEstado?: boolean }) {
  return (
    <Card hover padding="md" className="flex items-start gap-4">
      <div className="flex flex-col items-center justify-center bg-bosque/10 text-bosque-dark rounded-lg px-3 py-2 min-w-[68px]">
        <div className="text-[10px] font-semibold uppercase">{cita.horaInicio}</div>
        <div className="text-[10px] text-slate-400 leading-none">a</div>
        <div className="text-[10px] font-semibold uppercase">{cita.horaFin}</div>
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          {mostrarEstado && <EstadoBadge estado={cita.estado} />}
        </div>
        <div className="mt-1 grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1 text-sm">
          <Info icon={<MapPinIcon size={14} />}>{cita.nombreEspacio ?? 'Sin espacio'} · {cita.nombreSalon ?? 'Sin salon'}</Info>
          <Info icon={<UserIcon size={14} />}>Dr(a). {cita.nombreDocente}</Info>
          <Info icon={<UsersIcon size={14} />}>{nombresEstudiantes(cita)}</Info>
          {cita.fechaReserva && (
            <Info icon={<ClockIcon size={14} />} muted>
              Reservada {cita.fechaReserva.replace('T', ' ').substring(0, 16)}
            </Info>
          )}
        </div>
        {cita.observaciones && (
          <p className="mt-2 text-xs text-slate-500 italic bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100">
            "{cita.observaciones}"
          </p>
        )}
      </div>
      {accion && <div className="shrink-0">{accion}</div>}
    </Card>
  );
}

function Info({ icon, children, muted = false }: { icon: React.ReactNode; children: React.ReactNode; muted?: boolean }) {
  return (
    <div className={`flex items-center gap-1.5 ${muted ? 'text-slate-400' : 'text-slate-600'}`}>
      <span className="text-slate-400">{icon}</span>
      <span className="truncate">{children}</span>
    </div>
  );
}

function DetallesCita({ cita }: { cita: CitaResponse }) {
  return (
    <div className="bg-slate-50 rounded-xl p-4 space-y-2.5 text-sm">
      <Row icon={<CalendarIcon size={15} />} label="Fecha" value={formatFechaLarga(cita.fechaSesion)} />
      <Row icon={<ClockIcon size={15} />} label="Horario" value={`${cita.horaInicio} - ${cita.horaFin}`} />
      <Row icon={<MapPinIcon size={15} />} label="Lugar" value={`${cita.nombreEspacio ?? 'Sin espacio'} · ${cita.nombreSalon ?? 'Sin salon'}`} />
      <Row icon={<UserIcon size={15} />} label="Docente" value={`Dr(a). ${cita.nombreDocente}`} />
      <Row icon={<UsersIcon size={15} />} label={cita.nombreEstudiante2 ? 'Estudiantes' : 'Estudiante'} value={nombresEstudiantes(cita)} />
    </div>
  );
}

function Row({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3">
      <div className="text-slate-400">{icon}</div>
      <div className="flex-1 flex items-center justify-between gap-3">
        <span className="text-xs uppercase tracking-wide text-slate-500 font-medium">{label}</span>
        <span className="text-sm text-slate-800 font-medium text-right">{value}</span>
      </div>
    </div>
  );
}

// ---------------- helpers ----------------

function timeToMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
}

function formatFechaCorta(yyyy_mm_dd: string): string {
  const d = parseLocalDate(yyyy_mm_dd);
  return `${d.getDate()} ${MESES[d.getMonth()].substring(0, 3)}`;
}
