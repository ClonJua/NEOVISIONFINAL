import { useState } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import { useMsal } from '@azure/msal-react';
import { useNavigate } from 'react-router-dom';
import DocentesTab from './DocentesTab';
import EstudiantesTab from './EstudiantesTab';
import PacientesTab from './PacientesTab';
import EspaciosTab from './EspaciosTab';
import SesionesTab from './SesionesTab';
import CalendarTab from './CalendarTab';
import CitasTab from './CitasTab';
import ReportesTab from './ReportesTab';
import HistoriasClinicasTab from './HistoriasClinicasTab';
import SolicitudesTab from './SolicitudesTab';
import { GuiaPestanas } from './GuiaPestanas';
import {
  Badge, LogOutIcon, HomeIcon, CalendarIcon, UsersIcon, UserIcon, MapPinIcon, ClockIcon, ClipboardListIcon, BarChartIcon, HistoryIcon, InfoIcon, AyudaModal,
} from '../../components/ui';
import type { ReactNode } from 'react';

type Tab = 'docentes' | 'estudiantes' | 'pacientes' | 'espacios' | 'sesiones' | 'calendario' | 'citas' | 'reportes' | 'historias' | 'solicitudes';
type Group = 'agenda' | 'historias' | 'personas' | 'reportes';

interface TabDef {
  key: Tab;
  label: string;
  icon: ReactNode;
  group: Group;
  /** Una línea que dice qué contiene la pestaña, bajo el título. */
  descripcion: string;
}

/** Grupos del menú, en orden de uso (lo más frecuente arriba). */
const GROUPS: { key: Group; label: string }[] = [
  { key: 'agenda',    label: 'Agendamiento' },
  { key: 'historias', label: 'Historias clínicas' },
  { key: 'personas',  label: 'Personas' },
  { key: 'reportes',  label: 'Reportes' },
];

const TABS: TabDef[] = [
  // Agendamiento — lo que más se usa: crear sesiones y citas, primero.
  { key: 'citas',       label: 'Citas',       icon: <ClipboardListIcon size={18} />, group: 'agenda',
    descripcion: 'Los bloques que viven dentro de tus sesiones — aquí asignas y reprogramas pacientes' },
  { key: 'sesiones',    label: 'Sesiones',    icon: <ClockIcon size={18} />, group: 'agenda',
    descripcion: 'Las jornadas de práctica; al crearlas se generan solas sus citas' },
  { key: 'calendario',  label: 'Calendario',  icon: <CalendarIcon size={18} />, group: 'agenda',
    descripcion: 'Las mismas citas vistas sobre el mes, para revisar la ocupación' },
  { key: 'espacios',    label: 'Espacios',    icon: <MapPinIcon size={18} />, group: 'agenda',
    descripcion: 'Los lugares físicos y los salones donde ocurre la práctica' },
  // Historias clínicas
  { key: 'historias',   label: 'Historias Clínicas', icon: <HistoryIcon size={18} />, group: 'historias',
    descripcion: 'Archivo clínico de todos los pacientes, con su auditoría de accesos (solo consulta)' },
  { key: 'solicitudes', label: 'Solicitudes HC',     icon: <InfoIcon size={18} />, group: 'historias',
    descripcion: 'Pacientes que pidieron copia de su historia clínica' },
  // Personas — manejo de docentes, estudiantes y pacientes.
  { key: 'docentes',    label: 'Docentes',    icon: <UserIcon size={18} />, group: 'personas',
    descripcion: 'Quienes supervisan las sesiones y firman las historias clínicas' },
  { key: 'estudiantes', label: 'Estudiantes', icon: <UsersIcon size={18} />, group: 'personas',
    descripcion: 'Quienes atienden a los pacientes, organizados por semestre' },
  { key: 'pacientes',   label: 'Pacientes',   icon: <UsersIcon size={18} />, group: 'personas',
    descripcion: 'Las personas que se atienden en la clínica' },
  // Reportes
  { key: 'reportes',    label: 'Reportes',    icon: <BarChartIcon size={18} />, group: 'reportes',
    descripcion: 'Indicadores de ocupación y avance de los estudiantes frente a su meta' },
];

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>('citas');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [ayudaOpen, setAyudaOpen] = useState(false);
  const { correo, logout } = useAuth();
  const { instance } = useMsal();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout(instance);
    navigate('/login');
  };

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
            <div className="text-[10px] text-slate-400 uppercase tracking-wider">Admin Panel</div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 overflow-y-auto">
          {GROUPS.map((g) => (
            <SidebarGroup key={g.key} label={g.label}>
              {TABS.filter((t) => t.group === g.key).map((t) => (
                <SidebarItem
                  key={t.key}
                  active={tab === t.key}
                  icon={t.icon}
                  onClick={() => { setTab(t.key); setSidebarOpen(false); }}
                >
                  {t.label}
                </SidebarItem>
              ))}
            </SidebarGroup>
          ))}
        </nav>

        <div className="p-3 border-t border-slate-200">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-slate-50">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 text-white flex items-center justify-center text-xs font-bold shrink-0">
              {initials}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-xs font-semibold text-slate-800 truncate">{correo}</div>
              <div className="text-[10px] text-slate-400 uppercase tracking-wide">Administrador</div>
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
        <div
          className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
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
            <span>Admin</span>
            <span className="text-slate-300">/</span>
            <span className="text-slate-800 font-medium">{activeTabDef.label}</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <button onClick={() => setAyudaOpen(true)} aria-label="Ayuda"
              className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg">
              <InfoIcon size={18} />
            </button>
            <Badge tone="amber">Administrador</Badge>
          </div>
        </header>

        <AyudaModal open={ayudaOpen} onClose={() => setAyudaOpen(false)} rol="admin" />

        <main className="p-4 lg:p-8 max-w-7xl">
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
            <GuiaPestanas actual={tab} />
            {tab === 'calendario' && <CalendarTab />}
            {tab === 'citas' && <CitasTab />}
            {tab === 'espacios' && <EspaciosTab />}
            {tab === 'sesiones' && <SesionesTab />}
            {tab === 'docentes' && <DocentesTab />}
            {tab === 'estudiantes' && <EstudiantesTab />}
            {tab === 'pacientes' && <PacientesTab />}
            {tab === 'reportes' && <ReportesTab />}
            {tab === 'historias' && <HistoriasClinicasTab />}
            {tab === 'solicitudes' && <SolicitudesTab />}
          </div>
        </main>
      </div>
    </div>
  );
}

function SidebarGroup({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="mb-5">
      <div className="px-3 mb-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">{label}</div>
      <div className="space-y-0.5">{children}</div>
    </div>
  );
}

function SidebarItem({
  active, icon, onClick, children,
}: { active: boolean; icon: ReactNode; onClick: () => void; children: ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`
        w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition
        ${active
          ? 'bg-bosque/10 text-bosque-dark'
          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'}
      `}
    >
      <span className={active ? 'text-bosque' : 'text-slate-400'}>{icon}</span>
      {children}
      {active && <span className="ml-auto w-1.5 h-1.5 rounded-full bg-bosque" />}
    </button>
  );
}
