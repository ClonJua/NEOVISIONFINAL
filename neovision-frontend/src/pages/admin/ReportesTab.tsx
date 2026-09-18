import { useState, useEffect, useMemo } from 'react';
import {
  adminService,
  type CitaResponse,
  type DistribucionEstudianteResponse,
  type IndicadoresResponse,
  type MetaSemestreResponse,
} from '../../api/adminService';
import { useToast } from '../../components/ui';

type Reporte = 'indicadores' | 'distribucion' | 'ocupacion' | 'inasistencias' | 'estados';

const REPORTES: { key: Reporte; label: string }[] = [
  { key: 'indicadores',   label: 'Indicadores' },
  { key: 'distribucion',  label: 'Distribución por estudiante' },
  { key: 'ocupacion',     label: 'Ocupación por periodo' },
  { key: 'inasistencias', label: 'Inasistencias' },
  { key: 'estados',       label: 'Estados de citas' },
];

const C = {
  primary:   '#1F7A63',
  secondary: '#DCFCE7',
  bg:        '#F7FBF9',
  text:      '#1C1C1E',
  success:   '#22C55E',
  error:     '#DC2626',
  action:    '#2D6CDF',
};

const ESTADO_META: Record<string, { bg: string; text: string; label: string }> = {
  DISPONIBLE: { bg: '#D1FAE5', text: '#065F46', label: 'Disponible' },
  RESERVADO:  { bg: '#DBEAFE', text: '#1E40AF', label: 'Reservado' },
  COMPLETADO: { bg: '#DCFCE7', text: '#166534', label: 'Completado' },
  CANCELADO:  { bg: '#FEE2E2', text: '#991B1B', label: 'Cancelado' },
  VENCIDA:    { bg: '#FEF3C7', text: '#92400E', label: 'Vencida' },
};

// Metas configurables por el admin (ver MetasSemestreEditor); se cargan desde
// CitaServiceImpl.listarMetasSemestre() para que el % mostrado aquí coincida
// con el ProgresoWidget que ve el propio estudiante (progresoEstudiante() lee
// la misma tabla). 35 es el respaldo si un semestre no tiene meta configurada.
function getMeta(semestre: number, metas: MetaSemestreResponse[]): { target: number; label: string } {
  const target = metas.find(m => m.semestre === semestre)?.minimoPacientes ?? 35;
  return { target, label: String(target) };
}

function fmtDate(s: string): string {
  if (!s) return '—';
  const parts = s.split('-');
  if (parts.length !== 3) return s;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

// ─── Shared primitives ────────────────────────────────────────────────────────

function KpiCard({ label, value, sub, color, accent }: {
  label: string; value: string | number; sub?: string; color?: string; accent?: boolean;
}) {
  return (
    <div
      className="rounded-xl p-5 border shadow-sm"
      style={{ background: accent ? C.secondary : '#fff', borderColor: accent ? C.primary + '40' : '#E2E8F0' }}
    >
      <div className="text-3xl font-bold mb-1" style={{ color: color ?? C.primary }}>{value}</div>
      <div className="text-sm font-semibold text-slate-700">{label}</div>
      {sub && <div className="text-xs text-slate-400 mt-0.5">{sub}</div>}
    </div>
  );
}

function Bar({ value, max, color }: { value: number; max: number; color: string }) {
  const pct = max > 0 ? Math.min((value / max) * 100, 100) : 0;
  return (
    <div className="h-2 rounded-full overflow-hidden" style={{ background: C.secondary }}>
      <div className="h-full rounded-full transition-all duration-500" style={{ width: `${pct}%`, background: color }} />
    </div>
  );
}

function THead({ cols }: { cols: string[] }) {
  return (
    <thead>
      <tr style={{ background: C.bg }}>
        {cols.map(c => (
          <th key={c} className="px-4 py-3 text-left font-semibold text-xs uppercase tracking-wide" style={{ color: C.primary }}>
            {c}
          </th>
        ))}
      </tr>
    </thead>
  );
}

function Empty({ msg }: { msg: string }) {
  return (
    <div className="py-14 text-center rounded-xl border border-dashed" style={{ borderColor: C.primary + '40', background: C.secondary + '60' }}>
      <p className="text-sm" style={{ color: C.primary }}>
        {msg}
      </p>
    </div>
  );
}

function SectionTitle({ title, sub }: { title: string; sub: string }) {
  return (
    <div className="mb-5">
      <h3 className="text-base font-bold" style={{ color: C.text }}>{title}</h3>
      <p className="text-xs text-slate-500 mt-0.5">{sub}</p>
    </div>
  );
}

// ─── Indicadores ─────────────────────────────────────────────────────────────

type Indicadores = {
  total: number; completadas: number; canceladas: number; reservadas: number;
  vencidas: number; disponibles: number; tasaCancelacion: string;
  consultorios: number; estudiantesActivos: number;
};

function IndicadoresView({ d }: { d: Indicadores }) {
  const tasaNum = parseFloat(d.tasaCancelacion);
  return (
    <div>
      <SectionTitle
        title="Indicadores del periodo académico"
        sub="Resumen ejecutivo con métricas clave del sistema"
      />
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
        <KpiCard label="Total de citas" value={d.total} sub="Registradas en el sistema" />
        <KpiCard
          label="Citas completadas"
          value={d.completadas}
          sub={`${d.total > 0 ? ((d.completadas / d.total) * 100).toFixed(1) : 0}% del total`}
          color={C.success}
        />
        <KpiCard
          label="Citas canceladas"
          value={d.canceladas}
          sub="Canceladas por paciente o administración"
          color={C.error}
        />
        <KpiCard
          label="Tasa de cancelación"
          value={`${d.tasaCancelacion}%`}
          sub="Del total de citas registradas"
          color={tasaNum > 20 ? C.error : C.primary}
        />
        <KpiCard label="Citas reservadas" value={d.reservadas} sub="Pendientes de atención" color={C.action} />
        <KpiCard label="Citas vencidas" value={d.vencidas} sub="Sin atención ni cancelación" color="#D97706" />
        <KpiCard label="Estudiantes activos" value={d.estudiantesActivos} sub="Con al menos 1 cita asignada" accent />
        <KpiCard label="Consultorios usados" value={d.consultorios} sub="Salones distintos utilizados" accent />
      </div>
    </div>
  );
}

// ─── Distribución ─────────────────────────────────────────────────────────────

type DistItem = { id: number; nombre: string; semestre: number; count: number };

function DistribucionView({ datos, metas, onMetasActualizadas }: {
  datos: DistItem[]; metas: MetaSemestreResponse[]; onMetasActualizadas: () => void;
}) {
  const toast = useToast();
  const [avanzando, setAvanzando] = useState(false);
  const metasLabel = metas
    .slice().sort((a, b) => a.semestre - b.semestre)
    .map(m => `${m.semestre}°: ${m.minimoPacientes}`).join(' · ');

  // 9° es terminal (no existe semestre 10): solo 7° y 8° pueden avanzar.
  const elegibles = datos.filter(d => {
    const { target } = getMeta(d.semestre, metas);
    return d.semestre <= 8 && target > 0 && d.count >= target;
  });

  const handleAvanzar = async (ids: number[]) => {
    setAvanzando(true);
    try {
      const res = await adminService.avanzarSemestre(ids);
      toast.success(`${res.data.data} estudiante(s) avanzados de semestre`);
      onMetasActualizadas();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Error al avanzar de semestre');
    } finally {
      setAvanzando(false);
    }
  };

  return (
    <div>
      <SectionTitle
        title="Distribución de pacientes por estudiante"
        sub={`Pacientes atendidos (completados) vs. meta semestral${metasLabel ? ` — ${metasLabel}` : ''}`}
      />
      <MetasSemestreEditor metas={metas} onActualizado={onMetasActualizadas} />

      {elegibles.length > 0 && (
        <div className="flex items-center justify-between bg-white border border-slate-100 shadow-sm rounded-xl px-4 py-3 mb-4">
          <p className="text-sm text-slate-600">
            <strong>{elegibles.length}</strong> estudiante{elegibles.length !== 1 ? 's' : ''} ya cumplió su meta y puede avanzar de semestre.
          </p>
          <button onClick={() => handleAvanzar(elegibles.map(e => e.id))} disabled={avanzando}
            className="text-xs font-medium px-3 py-1.5 rounded-lg text-white disabled:opacity-50"
            style={{ background: C.primary }}>
            {avanzando ? 'Avanzando...' : `Avanzar a los ${elegibles.length} que cumplieron`}
          </button>
        </div>
      )}

      {!datos.length ? <Empty msg="No hay datos de distribución disponibles" /> : (
      <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <THead cols={['Estudiante', 'Semestre', 'Atendidos', 'Meta', 'Progreso', 'Estado', '']} />
          <tbody>
            {datos.map((d, i) => {
              const { target, label } = getMeta(d.semestre, metas);
              const pct = target > 0 ? (d.count / target) * 100 : 0;
              const barColor = pct >= 100 ? C.success : pct >= 60 ? C.action : C.error;
              const badge =
                pct >= 100 ? { bg: '#DCFCE7', text: '#166534', label: 'Meta cumplida' }
                : pct >= 60 ? { bg: '#DBEAFE', text: '#1E40AF', label: 'En progreso' }
                : { bg: '#FEE2E2', text: '#991B1B', label: 'Por debajo' };
              const puedeAvanzar = d.semestre <= 8 && target > 0 && d.count >= target;
              return (
                <tr key={d.id} style={i % 2 !== 0 ? { background: C.bg } : {}}>
                  <td className="px-4 py-3 font-medium" style={{ color: C.text }}>{d.nombre}</td>
                  <td className="px-4 py-3 text-center text-slate-500">{d.semestre || '—'}</td>
                  <td className="px-4 py-3 text-center font-bold text-lg" style={{ color: C.primary }}>{d.count}</td>
                  <td className="px-4 py-3 text-center text-slate-500">{label}</td>
                  <td className="px-4 py-3 w-40">
                    <div className="flex items-center gap-2">
                      <div className="flex-1"><Bar value={d.count} max={target} color={barColor} /></div>
                      <span className="text-xs text-slate-400 shrink-0 w-8 text-right">{Math.min(pct, 100).toFixed(0)}%</span>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap"
                      style={{ background: badge.bg, color: badge.text }}>
                      {badge.label}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    {puedeAvanzar && (
                      <button onClick={() => handleAvanzar([d.id])} disabled={avanzando}
                        className="text-xs font-medium hover:underline disabled:opacity-50" style={{ color: C.primary }}>
                        Avanzar a {d.semestre + 1}°
                      </button>
                    )}
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

function MetasSemestreEditor({ metas, onActualizado }: { metas: MetaSemestreResponse[]; onActualizado: () => void }) {
  const toast = useToast();
  const [valores, setValores] = useState<Record<number, string>>({});
  const [guardando, setGuardando] = useState<number | null>(null);
  const SEMESTRES = [7, 8, 9];

  const valorDe = (semestre: number) =>
    valores[semestre] ?? String(metas.find(m => m.semestre === semestre)?.minimoPacientes ?? '');

  const handleGuardar = async (semestre: number) => {
    const minimo = parseInt(valorDe(semestre), 10);
    if (!minimo || minimo < 1) { toast.error('El mínimo debe ser mayor a 0'); return; }
    setGuardando(semestre);
    try {
      await adminService.actualizarMetaSemestre(semestre, minimo);
      toast.success(`Meta de ${semestre}° semestre actualizada`);
      onActualizado();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Error al actualizar la meta');
    } finally {
      setGuardando(null);
    }
  };

  return (
    <div className="bg-white rounded-xl border border-slate-100 shadow-sm p-4 mb-4">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
        Mínimo de pacientes para aprobar, por semestre
      </p>
      <div className="flex flex-wrap gap-4">
        {SEMESTRES.map(s => (
          <div key={s} className="flex items-center gap-2">
            <span className="text-sm text-slate-600 font-medium">{s}° semestre</span>
            <input type="number" min={1} value={valorDe(s)}
              onChange={e => setValores(v => ({ ...v, [s]: e.target.value }))}
              className="w-20 px-2 py-1.5 border border-slate-300 rounded-lg text-sm outline-none focus:border-[#1F7A63]" />
            <button onClick={() => handleGuardar(s)} disabled={guardando === s}
              className="text-xs font-medium px-3 py-1.5 rounded-lg text-white disabled:opacity-50"
              style={{ background: C.primary }}>
              {guardando === s ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Ocupación ────────────────────────────────────────────────────────────────

type OcupItem = { week: string; agendadas: number; canceladas: number; completadas: number; total: number };

function OcupacionView({ datos }: { datos: OcupItem[] }) {
  if (!datos.length) return <Empty msg="No hay datos de ocupación disponibles" />;
  const max = Math.max(...datos.map(d => d.total), 1);

  return (
    <div>
      <SectionTitle
        title="Ocupación por periodo"
        sub="Citas por semana (últimas 10 semanas con actividad registrada)"
      />
      <div className="bg-white rounded-xl border border-slate-100 shadow-sm p-6">
        <div className="flex flex-wrap gap-5 mb-6 text-xs text-slate-500">
          {[
            { color: C.primary, label: 'Completadas' },
            { color: C.action,  label: 'Reservadas' },
            { color: C.error,   label: 'Canceladas/Vencidas' },
          ].map(l => (
            <span key={l.label} className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded-sm inline-block" style={{ background: l.color }} />
              {l.label}
            </span>
          ))}
        </div>
        <div className="space-y-4">
          {datos.map(d => (
            <div key={d.week} className="flex items-center gap-3">
              <span className="text-xs text-slate-500 shrink-0 w-24">{fmtDate(d.week)}</span>
              <div className="flex-1 flex gap-1 h-7">
                {d.completadas > 0 && (
                  <div className="h-full rounded-sm flex items-center justify-center text-white text-xs font-medium"
                    style={{ width: `${(d.completadas / max) * 100}%`, background: C.primary, minWidth: '2rem' }}
                    title={`Completadas: ${d.completadas}`}>
                    {d.completadas}
                  </div>
                )}
                {d.agendadas > 0 && (
                  <div className="h-full rounded-sm flex items-center justify-center text-white text-xs font-medium"
                    style={{ width: `${(d.agendadas / max) * 100}%`, background: C.action, minWidth: '2rem' }}
                    title={`Reservadas: ${d.agendadas}`}>
                    {d.agendadas}
                  </div>
                )}
                {d.canceladas > 0 && (
                  <div className="h-full rounded-sm flex items-center justify-center text-white text-xs font-medium"
                    style={{ width: `${(d.canceladas / max) * 100}%`, background: C.error, minWidth: '2rem' }}
                    title={`Canceladas/Vencidas: ${d.canceladas}`}>
                    {d.canceladas}
                  </div>
                )}
              </div>
              <span className="text-xs text-slate-400 w-8 text-right">{d.total}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Inasistencias ────────────────────────────────────────────────────────────

type InasItem = { idCita: number; fecha: string; estudiante: string; estado: string; paciente: string };

function InasistenciasView({ datos }: { datos: InasItem[] }) {
  if (!datos.length) return <Empty msg="No hay citas canceladas ni vencidas registradas" />;

  return (
    <div>
      <SectionTitle
        title="Inasistencias y cancelaciones"
        sub="Cupos cancelados o vencidos — las cancelaciones iniciadas por el paciente no retienen datos del paciente"
      />
      <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <THead cols={['Fecha', 'Estudiante', 'Paciente', 'Estado']} />
          <tbody>
            {datos.map((d, i) => {
              const m = ESTADO_META[d.estado] ?? { bg: '#F1F5F9', text: '#64748B', label: d.estado };
              return (
                <tr key={d.idCita} style={i % 2 !== 0 ? { background: C.bg } : {}}>
                  <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{fmtDate(d.fecha)}</td>
                  <td className="px-4 py-3 font-medium" style={{ color: C.text }}>{d.estudiante}</td>
                  <td className="px-4 py-3 text-slate-500 italic">{d.paciente}</td>
                  <td className="px-4 py-3">
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap"
                      style={{ background: m.bg, color: m.text }}>
                      {m.label}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Estados ──────────────────────────────────────────────────────────────────

function EstadosView({ datos, total }: { datos: [string, number][]; total: number }) {
  if (!datos.length) return <Empty msg="No hay datos de estados disponibles" />;

  return (
    <div>
      <SectionTitle
        title="Estados de citas"
        sub="Vista del ciclo completo: agendada → atendida → cancelada → reprogramada"
      />
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Barras */}
        <div className="bg-white rounded-xl border border-slate-100 shadow-sm p-6">
          <p className="text-sm font-semibold text-slate-700 mb-5">Distribución por estado</p>
          <div className="space-y-5">
            {datos.map(([estado, count]) => {
              const m = ESTADO_META[estado] ?? { bg: '#F1F5F9', text: '#64748B', label: estado };
              const pct = total > 0 ? (count / total) * 100 : 0;
              return (
                <div key={estado}>
                  <div className="flex justify-between items-center mb-1.5">
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium"
                      style={{ background: m.bg, color: m.text }}>
                      {m.label}
                    </span>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm" style={{ color: C.text }}>{count}</span>
                      <span className="text-xs text-slate-400">{pct.toFixed(1)}%</span>
                    </div>
                  </div>
                  <div className="h-3 rounded-full overflow-hidden" style={{ background: '#F1F5F9' }}>
                    <div className="h-full rounded-full transition-all duration-700"
                      style={{ width: `${pct}%`, background: m.text }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Cards */}
        <div className="space-y-3">
          {datos.map(([estado, count]) => {
            const m = ESTADO_META[estado] ?? { bg: '#F1F5F9', text: '#64748B', label: estado };
            const pct = total > 0 ? (count / total) * 100 : 0;
            return (
              <div key={estado}
                className="flex items-center gap-4 p-4 rounded-xl border border-slate-100 bg-white shadow-sm">
                <div className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0 text-xl font-bold"
                  style={{ background: m.bg, color: m.text }}>
                  {count}
                </div>
                <div>
                  <div className="text-sm font-semibold" style={{ color: C.text }}>{m.label}</div>
                  <div className="text-xs text-slate-400">{pct.toFixed(1)}% del total</div>
                </div>
              </div>
            );
          })}
          <div className="flex items-center gap-4 p-4 rounded-xl border-2 bg-white"
            style={{ borderColor: C.primary }}>
            <div className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0 text-xl font-bold"
              style={{ background: C.secondary, color: C.primary }}>
              {total}
            </div>
            <div>
              <div className="text-sm font-bold" style={{ color: C.primary }}>Total registros</div>
              <div className="text-xs text-slate-400">Todos los estados incluidos</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Main ────────────────────────────────────────────────────────────────────

export default function ReportesTab() {
  const toast = useToast();
  // Cada reporte trae su propio dato ya agregado o acotado por el backend —
  // ya no se carga el historico completo de citas al navegador para calcular
  // todo en el cliente (ver CitaServiceImpl: indicadores/distribucionPorEstudiante/
  // ocupacionReciente/inasistencias).
  const [indicadoresData, setIndicadoresData]     = useState<IndicadoresResponse | null>(null);
  const [distribucionData, setDistribucionData]   = useState<DistribucionEstudianteResponse[]>([]);
  const [ocupacionCitas, setOcupacionCitas]       = useState<CitaResponse[]>([]);
  const [inasistenciasCitas, setInasistenciasCitas] = useState<CitaResponse[]>([]);
  const [metas, setMetas]                         = useState<MetaSemestreResponse[]>([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [reporte, setReporte]       = useState<Reporte>('indicadores');

  useEffect(() => { cargar(); }, []);

  const cargar = async () => {
    setLoading(true);
    setError('');
    try {
      const [ind, dist, ocup, inas, met] = await Promise.all([
        adminService.reporteIndicadores(),
        adminService.reporteDistribucion(),
        adminService.reporteOcupacion(),
        adminService.reporteInasistencias(),
        adminService.listarMetasSemestre(),
      ]);
      setIndicadoresData(ind.data.data);
      setDistribucionData(dist.data.data ?? []);
      setOcupacionCitas(ocup.data.data ?? []);
      setInasistenciasCitas(inas.data.data ?? []);
      setMetas(met.data.data ?? []);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Error al cargar los datos de reportes';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const indicadores = useMemo<Indicadores>(() => {
    const porEstado = indicadoresData?.porEstado ?? {};
    const completadas = porEstado.COMPLETADO ?? 0;
    const canceladas  = porEstado.CANCELADO ?? 0;
    const reservadas  = porEstado.RESERVADO ?? 0;
    const vencidas    = porEstado.VENCIDA ?? 0;
    const disponibles = porEstado.DISPONIBLE ?? 0;
    const total = indicadoresData?.total ?? 0;
    const tasaCancelacion = total > 0 ? ((canceladas / total) * 100).toFixed(1) : '0.0';
    return {
      total, completadas, canceladas, reservadas, vencidas, disponibles, tasaCancelacion,
      consultorios: indicadoresData?.consultoriosUsados ?? 0,
      estudiantesActivos: indicadoresData?.estudiantesActivos ?? 0,
    };
  }, [indicadoresData]);

  const distribucion = useMemo<DistItem[]>(() => {
    return distribucionData
      .map(d => ({ id: d.idEstudiante, nombre: d.nombre, semestre: d.semestre, count: d.completadas }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
  }, [distribucionData]);

  const inasistencias = useMemo<InasItem[]>(() => {
    return inasistenciasCitas
      .map(c => ({
        idCita: c.idCita,
        fecha: c.fechaSesion || '',
        estudiante: (c.nombreEstudiante2 ? `${c.nombreEstudiante} y ${c.nombreEstudiante2}` : c.nombreEstudiante) || '—',
        estado: c.estado,
        paciente: c.nombrePaciente || 'Sin datos de paciente',
      }))
      .sort((a, b) => b.fecha.localeCompare(a.fecha));
  }, [inasistenciasCitas]);

  const ocupacion = useMemo<OcupItem[]>(() => {
    const weeks = new Map<string, OcupItem>();
    for (const c of ocupacionCitas) {
      if (!c.fechaSesion) continue;
      const d   = new Date(c.fechaSesion + 'T00:00:00');
      const day = d.getDay();
      const mon = new Date(d);
      mon.setDate(d.getDate() - (day === 0 ? 6 : day - 1));
      const key = mon.toISOString().split('T')[0];
      if (!weeks.has(key)) weeks.set(key, { week: key, agendadas: 0, canceladas: 0, completadas: 0, total: 0 });
      const w = weeks.get(key)!;
      w.total++;
      if (c.estado === 'RESERVADO')                            w.agendadas++;
      else if (c.estado === 'COMPLETADO')                      w.completadas++;
      else if (c.estado === 'CANCELADO' || c.estado === 'VENCIDA') w.canceladas++;
    }
    return Array.from(weeks.values()).sort((a, b) => a.week.localeCompare(b.week)).slice(-10);
  }, [ocupacionCitas]);

  const estadosCounts = useMemo<[string, number][]>(() => {
    const porEstado = indicadoresData?.porEstado ?? {};
    return Object.entries(porEstado).sort(([, a], [, b]) => b - a);
  }, [indicadoresData]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-center">
          <div className="w-10 h-10 border-4 rounded-full mx-auto mb-3 animate-spin"
            style={{ borderColor: C.secondary, borderTopColor: C.primary }} />
          <p className="text-sm text-slate-500">Cargando reportes...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl p-8 text-center border"
        style={{ background: '#FEF2F2', borderColor: '#FECACA' }}>
        <p className="font-medium mb-3" style={{ color: C.error }}>{error}</p>
        <button onClick={cargar}
          className="px-4 py-2 rounded-lg text-sm text-white transition hover:opacity-90"
          style={{ background: C.primary }}>
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div style={{ background: C.bg }} className="rounded-xl">
      {/* Header */}
      <div className="flex items-center justify-between mb-5">
        <p className="text-xs text-slate-500">
          {indicadores.total} citas · {distribucion.length} estudiantes activos
        </p>
        <button onClick={cargar}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium text-white transition hover:opacity-90"
          style={{ background: C.primary }}>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
          </svg>
          Actualizar
        </button>
      </div>

      {/* Sub-nav */}
      <div className="flex gap-1 flex-wrap mb-6 bg-white rounded-xl p-1.5 border border-slate-100 shadow-sm">
        {REPORTES.map(r => (
          <button key={r.key} onClick={() => setReporte(r.key)}
            className="px-3 py-2 rounded-lg text-sm font-medium transition whitespace-nowrap"
            style={reporte === r.key
              ? { background: C.secondary, color: C.primary }
              : { color: '#64748b' }}>
            {r.label}
          </button>
        ))}
      </div>

      {/* Views */}
      {reporte === 'indicadores'   && <IndicadoresView   d={indicadores} />}
      {reporte === 'distribucion'  && <DistribucionView  datos={distribucion} metas={metas} onMetasActualizadas={cargar} />}
      {reporte === 'ocupacion'     && <OcupacionView     datos={ocupacion} />}
      {reporte === 'inasistencias' && <InasistenciasView datos={inasistencias} />}
      {reporte === 'estados'       && <EstadosView       datos={estadosCounts} total={indicadores.total} />}
    </div>
  );
}