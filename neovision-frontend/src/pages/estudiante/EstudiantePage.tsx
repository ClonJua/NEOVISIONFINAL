import { useEffect, useState, type ReactNode } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { useMsal } from '@azure/msal-react';
import { citaService, type CitaResponse } from '../../api/citaService';
import { estudianteService } from '../../api/estudianteService';
import type { PacienteResponse, EstudianteResponse } from '../../api/adminService';
import { historiaClinicaService, type AtencionResumenResponse } from '../../api/historiaClinicaService';
import { HistoriaClinicaLauncher } from '../../components/historia/HistoriaClinicaLauncher';
import { AtencionDetailModal } from '../../components/historia/AtencionDetailModal';
import ResumenTab from './ResumenTab';
import DisponiblesTab from './DisponiblesTab';
import AgendadasTab from './AgendadasTab';
import HistorialTab from './HistorialTab';
import AtencionesTab from './AtencionesTab';
import PerfilTab from './PerfilTab';
import { GuiaPestanas } from './GuiaPestanas';
import {
  Badge, LogOutIcon, HomeIcon, CalendarIcon, ClipboardListIcon, HistoryIcon, ClockIcon, UserIcon, InfoIcon, AyudaModal,
  useToast,
} from '../../components/ui';

type Tab = 'resumen' | 'disponibles' | 'agendadas' | 'historial' | 'atenciones' | 'perfil';

interface TabDef {
  key: Tab;
  label: string;
  icon: ReactNode;
  /** Una línea que dice qué contiene la pestaña, bajo el título. */
  descripcion: string;
}

export default function EstudiantePage() {
  const toast = useToast();
  const { correo, logout } = useAuth();
  const { instance } = useMsal();
  const navigate = useNavigate();

  const [tab, setTab] = useState<Tab>('resumen');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [ayudaOpen, setAyudaOpen] = useState(false);

  const [bloques, setBloques] = useState<CitaResponse[]>([]);
  const [pacientes, setPacientes] = useState<PacienteResponse[]>([]);
  const [perfil, setPerfil] = useState<EstudianteResponse | null>(null);
  const [progreso, setProgreso] = useState<{ semestre: number; atendidos: number; meta: number } | null>(null);
  const [atenciones, setAtenciones] = useState<AtencionResumenResponse[]>([]);
  const [atencionAbierta, setAtencionAbierta] = useState<number | null>(null);
  const [historiaCita, setHistoriaCita] = useState<CitaResponse | null>(null);
  const [error, setError] = useState('');

  const handleLogout = () => { logout(instance); navigate('/login'); };

  const cargar = async () => {
    try {
      const [bloquesRes, pacRes, perfRes, progresoRes, atencionesRes] = await Promise.all([
        citaService.misBloques(),
        estudianteService.listarPacientes(),
        estudianteService.miPerfil(),
        citaService.progresoEstudiante(),
        historiaClinicaService.misAtenciones(),
      ]);
      setBloques(bloquesRes.data.data);
      setPacientes(pacRes.data.data.filter((p) => p.activo));
      setPerfil(perfRes.data.data);
      setProgreso(progresoRes.data.data);
      setAtenciones(atencionesRes.data.data);
    } catch (err: any) {
      const backend = err?.response?.data?.message;
      const status = err?.response?.status;
      const msg = backend || `Error al cargar datos${status ? ` (HTTP ${status})` : ''}: ${err?.message || 'sin detalle'}`;
      setError(msg);
      toast.error(msg);
    }
  };

  useEffect(() => { cargar(); /* eslint-disable-next-line */ }, []);

  const bloquesDisponibles = bloques.filter((b) => b.estado === 'DISPONIBLE');
  const bloquesAgendados = bloques.filter((b) => b.estado === 'RESERVADO');
  const bloquesHistorial = bloques.filter((b) => ['COMPLETADO', 'CANCELADO', 'VENCIDA'].includes(b.estado));
  const atencionesDevueltas = atenciones.filter((a) => a.estado === 'DEVUELTA').length;

  const TABS: TabDef[] = [
    { key: 'resumen', label: 'Resumen', icon: <HomeIcon size={18} />, descripcion: 'Tu progreso del semestre y lo que tienes pendiente' },
    { key: 'disponibles', label: 'Disponibles', icon: <CalendarIcon size={18} />, descripcion: 'Tus bloques sin paciente asignado — asígnales uno' },
    { key: 'agendadas', label: 'Agendadas', icon: <ClipboardListIcon size={18} />, descripcion: 'Bloques con paciente que aún no atiendes — aquí abres la historia clínica' },
    { key: 'historial', label: 'Historial', icon: <HistoryIcon size={18} />, descripcion: 'Citas que ya terminaron: completadas, canceladas o vencidas' },
    {
      key: 'atenciones',
      label: 'Mis atenciones',
      descripcion: 'Las historias clínicas que diligenciaste y en qué punto de revisión va cada una',
      icon: (
        <span className="relative inline-flex">
          <ClockIcon size={18} />
          {atencionesDevueltas > 0 && (
            <span className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
              {atencionesDevueltas > 9 ? '9+' : atencionesDevueltas}
            </span>
          )}
        </span>
      ),
    },
    { key: 'perfil', label: 'Mi perfil', icon: <UserIcon size={18} />, descripcion: 'Tus datos y tu firma para las historias clínicas' },
  ];

  const activeTabDef = TABS.find((t) => t.key === tab)!;
  const initials = (correo || '?').slice(0, 2).toUpperCase();

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Sidebar */}
      <aside
        className={`
          fixed lg:sticky top-0 left-0 z-40 h-screen w-64 bg-white border-r border-slate-200
          transform transition-transform duration-200 ease-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
          flex flex-col
        `}
      >
        <div className="h-16 px-5 flex items-center gap-2 border-b border-slate-200">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-bosque to-bosqueOrange flex items-center justify-center text-white">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          </div>
          <div>
            <div className="text-base font-bold text-slate-800 leading-none">NeoVision</div>
            <div className="text-[10px] text-slate-400 uppercase tracking-wider">Panel Estudiante</div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 overflow-y-auto space-y-0.5">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => { setTab(t.key); setSidebarOpen(false); }}
              className={`
                w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition
                ${tab === t.key ? 'bg-bosque/10 text-bosque-dark' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'}
              `}
            >
              <span className={tab === t.key ? 'text-bosque' : 'text-slate-400'}>{t.icon}</span>
              {t.label}
              {tab === t.key && <span className="ml-auto w-1.5 h-1.5 rounded-full bg-bosque" />}
            </button>
          ))}
        </nav>

        <div className="p-3 border-t border-slate-200">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-slate-50">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-bosque to-bosque-dark text-white flex items-center justify-center text-xs font-bold shrink-0">
              {initials}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-xs font-semibold text-slate-800 truncate">{correo}</div>
              <div className="text-[10px] text-slate-400 uppercase tracking-wide">Estudiante</div>
            </div>
            <button
              onClick={handleLogout}
              className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-md transition"
              title="Cerrar sesión"
              aria-label="Cerrar sesión"
            >
              <LogOutIcon size={16} />
            </button>
          </div>
        </div>
      </aside>

      {/* Overlay movil */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Content */}
      <div className="flex-1 min-w-0">
        <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-20 flex items-center px-4 lg:px-8 gap-3">
          <button
            className="lg:hidden p-2 text-slate-500 hover:bg-slate-100 rounded-lg"
            onClick={() => setSidebarOpen(true)}
            aria-label="Abrir menu"
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <HomeIcon size={14} />
            <span>Estudiante</span>
            <span className="text-slate-300">/</span>
            <span className="text-slate-800 font-medium">{activeTabDef.label}</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <button onClick={() => setAyudaOpen(true)} aria-label="Ayuda"
              className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg">
              <InfoIcon size={18} />
            </button>
            <Badge tone="blue">Estudiante</Badge>
          </div>
        </header>

        <AyudaModal open={ayudaOpen} onClose={() => setAyudaOpen(false)} rol="estudiante" />

        <main className="p-4 lg:p-8 max-w-7xl">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6 text-sm">
              {error}<button onClick={() => setError('')} className="ml-2 font-bold">&times;</button>
            </div>
          )}

          <div className="mb-6 flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-bosque/10 text-bosque flex items-center justify-center">
              {activeTabDef.icon}
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-800">{activeTabDef.label}</h1>
              <p className="text-sm text-slate-500">{activeTabDef.descripcion}</p>
            </div>
          </div>

          <div className="animate-[fadeIn_200ms_ease-out]">
            {(tab === 'disponibles' || tab === 'agendadas' || tab === 'historial' || tab === 'atenciones') && (
              <GuiaPestanas actual={tab} />
            )}
            {tab === 'resumen' && (
              <ResumenTab
                bloques={bloques}
                progreso={progreso}
                atencionesDevueltas={atencionesDevueltas}
                onIrAAtenciones={() => setTab('atenciones')}
              />
            )}
            {tab === 'disponibles' && (
              <DisponiblesTab bloques={bloquesDisponibles} pacientes={pacientes} onRefrescar={cargar} />
            )}
            {tab === 'agendadas' && (
              <AgendadasTab bloques={bloquesAgendados} disponibles={bloquesDisponibles} onRefrescar={cargar} onVerHistoria={setHistoriaCita} />
            )}
            {tab === 'historial' && (
              <HistorialTab bloques={bloquesHistorial} onVerHistoria={setHistoriaCita} />
            )}
            {tab === 'atenciones' && (
              <AtencionesTab atenciones={atenciones} atencionesDevueltas={atencionesDevueltas} onOpen={setAtencionAbierta} />
            )}
            {tab === 'perfil' && <PerfilTab perfil={perfil} onActualizado={cargar} />}
          </div>
        </main>
      </div>

      {historiaCita && (
        <HistoriaClinicaLauncher
          idCita={historiaCita.idCita}
          idPaciente={historiaCita.idPaciente!}
          nombrePaciente={historiaCita.nombrePaciente ?? 'Paciente'}
          onClose={() => { setHistoriaCita(null); cargar(); }}
        />
      )}

      {atencionAbierta != null && (
        <AtencionDetailModal idAtencion={atencionAbierta} open onClose={() => setAtencionAbierta(null)} onChanged={cargar} />
      )}
    </div>
  );
}
