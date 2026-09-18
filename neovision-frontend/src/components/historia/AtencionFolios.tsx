import { useEffect, useRef, useState, type ReactNode } from 'react';
import type { DiagnosticoDto, ProcedimientoDto } from '../../api/historiaClinicaService';
import { cie10Service, type Cie10Item } from '../../api/cie10Service';
import { cupsService, type CupsItem } from '../../api/cupsService';
import { DiagramaCanvas } from './DiagramaCanvas';
import DateScroller from '../DateScroller';
import { HelpTip } from '../ui/HelpTip';

// Emojis no aportan a un registro clínico y ensucian el texto impreso/exportado;
// se descartan en el momento de escribir, no se bloquea el resto del campo.
const EMOJI_RE = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/gu;
const sinEmoji = (v: string) => v.replace(EMOJI_RE, '');

function formatDateEs(iso: string): string {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  if (!y || !m || !d) return iso;
  return `${d}/${m}/${y}`;
}

export type Mode = 'edit' | 'view';

// Estructura clínica completa (todas las secciones). Se mantiene laxa (any) para
// el acceso genérico por sección/campo; el tipado fuerte vive en el service.
export type ClinicalData = Record<string, any>;

interface Props {
  data: ClinicalData;
  mode: Mode;
  onField?: (section: string, field: string, value: any) => void;
  onMotivo?: (value: string) => void;
  diagnosticos: DiagnosticoDto[];
  onDiagnosticos?: (list: DiagnosticoDto[]) => void;
  procedimientos: ProcedimientoDto[];
  onProcedimientos?: (list: ProcedimientoDto[]) => void;
}

const SINTOMAS: [string, string][] = [
  ['ardor', 'Ardor'], ['cansancio', 'Cansancio'], ['cefalea', 'Cefalea'],
  ['dolorOcular', 'Dolor ocular'], ['fotofobia', 'Fotofobia'], ['lagrimeo', 'Lagrimeo'],
  ['visionDoble', 'Visión doble'], ['hiperemia', 'Hiperemia'], ['secreciones', 'Secreciones'],
  ['prurito', 'Prurito'], ['miodesopsias', 'Miodesopsias'], ['confusionAlLeer', 'Confusión al leer'],
  ['mareo', 'Mareo'], ['otros', 'Otros'],
];

const MEDIDA_COLS = ['Esfera', 'Cilindro', 'Eje', 'Prisma', 'Base'];

const EXAMEN_EXTERNO_ESTRUCTURAS: [string, string][] = [
  ['orbita', 'Órbita'], ['cejas', 'Cejas'], ['pestanas', 'Pestañas'], ['parpados', 'Párpados'],
  ['conjuntiva', 'Conjuntiva'], ['esclera', 'Esclera'], ['cornea', 'Córnea'], ['iris', 'Iris'],
  ['pupila', 'Pupila'], ['camaraAnterior', 'Cámara ant./Van Herick'], ['cristalino', 'Cristalino'],
];
const FONDO_OJO_HALLAZGOS: [string, string][] = [
  ['mediosRefringentes', 'Medios refringentes'], ['coloracionFondo', 'Coloración del fondo'],
  ['papilaExcavacion', 'Papila / Excavación'], ['relacionArteriaVena', 'Relación arteria/vena'],
  ['maculaFijacion', 'Mácula / Fijación'],
];
const REFLEJOS_PUPILARES = ['fotomotorDirectoOd', 'consensualOd', 'acomodativoOd', 'fotomotorDirectoOi', 'consensualOi', 'acomodativoOi'];

// ---- Sugerencias para digitar rápido (listas desplegables con texto libre) ----
// Son solo ayudas de entrada: el dato guardado sigue siendo el mismo texto de siempre.
const rango = (min: number, max: number, paso: number, decimales: number, conSigno = false): string[] => {
  const out: string[] = [];
  for (let v = min; v <= max + 1e-9; v += paso) {
    const s = v.toFixed(decimales);
    out.push(conSigno && v > 0 ? `+${s}` : s);
  }
  return out;
};
const DL: Record<string, string[]> = {
  esfera:   rango(-20, 20, 0.25, 2, true),
  cilindro: rango(-10, 0, 0.25, 2),
  eje:      rango(0, 180, 5, 0),
  prisma:   rango(0.5, 10, 0.5, 2),
  base:     ['Nasal', 'Temporal', 'Superior', 'Inferior'],
  adicion:  rango(0.75, 3.5, 0.25, 2, true),
  av:       ['20/10', '20/15', '20/20', '20/25', '20/30', '20/40', '20/50', '20/60', '20/70', '20/80',
             '20/100', '20/200', '20/400', 'CD', 'MM', 'PL', 'NPL'],
  normal:   ['Normal', 'Sin alteraciones'],
  // Fondo de ojo (por hallazgo)
  mediosRefringentes:  ['Transparentes', 'Opacidad leve', 'Opacidad densa'],
  coloracionFondo:     ['Normal', 'Rojo naranja', 'Pálido', 'Atigrado'],
  papilaExcavacion:    ['Normal', '0.2', '0.3', '0.4', '0.5', '0.6', '0.7', 'Bordes definidos'],
  relacionArteriaVena: ['2/3', '1/2', '3/4', 'Normal'],
  maculaFijacion:      ['Central', 'Brillo foveal presente', 'Normal', 'Excéntrica'],
  // Motilidad
  reflejo:     ['Normal', 'Presente', 'Lento', 'Ausente'],
  hirschberg:  ['Centrado', 'Ortoforia', 'Endotropia', 'Exotropia', 'Hipertropia', 'Hipotropia'],
  estadoMotor: ['Ortoforia', 'Endoforia', 'Exoforia', 'Hiperforia', 'Hipoforia', 'Endotropia', 'Exotropia'],
  ducciones:   ['Completas', 'Limitadas'],
  ppc:         ['HLN (hasta la nariz)'],
  kappa:       ['0', '+0.5', '+1', '-0.5'],
  // Examen externo
  tonometro:   ['Aire (no contacto)', 'Goldmann', 'Perkins', 'iCare', 'Tono-Pen', 'Schiötz'],
  // Queratometría
  miras:       ['Claras', 'Regulares', 'Distorsionadas', 'Irregulares'],
  // Pruebas complementarias
  vcTest:      ['Ishihara', 'Farnsworth D-15', 'HRR', 'Otro'],
  normalAlt:   ['Normal', 'Alterada'],
  normalAltM:  ['Normal', 'Alterado'],
  estereoTest: ['Titmus', 'Randot', 'TNO', 'Lang', 'Frisby'],
  cvTest:      ['Confrontación', 'Campímetro', 'Pantalla tangente', 'Rejilla de Amsler'],
  scTest:      ['Pelli-Robson', 'Vistech', 'CSV-1000'],
  // Anamnesis
  comoEmpezo:  ['Súbito', 'Progresivo', 'Intermitente'],
  asociado:    ['Sí, visión cercana', 'Sí, visión lejana', 'Sí, uso de pantallas', 'No'],
  antiguedad:  ['Días', 'Semanas', 'Meses', 'Años'],
  // Datos generales
  parentesco:  ['Madre', 'Padre', 'Hijo(a)', 'Cónyuge', 'Hermano(a)', 'Abuelo(a)', 'Tío(a)', 'Otro'],
  eps:         ['Nueva EPS', 'Sanitas', 'Sura', 'Compensar', 'Famisanar', 'Salud Total', 'Coosalud',
                'Mutual Ser', 'Aliansalud', 'Savia Salud', 'Capital Salud', 'Asmet Salud', 'Emssanar', 'Particular'],
  localidad:   ['Usaquén', 'Chapinero', 'Santa Fe', 'San Cristóbal', 'Usme', 'Tunjuelito', 'Bosa', 'Kennedy',
                'Fontibón', 'Engativá', 'Suba', 'Barrios Unidos', 'Teusaquillo', 'Los Mártires', 'Antonio Nariño',
                'Puente Aranda', 'La Candelaria', 'Rafael Uribe Uribe', 'Ciudad Bolívar', 'Sumapaz'],
  // Lentes
  disenoLente: ['Monofocal', 'Bifocal', 'Progresivo', 'Ocupacional'],
  material:    ['CR-39', 'Policarbonato', 'Trivex', 'Alto índice', 'Mineral (vidrio)'],
  usoLente:    ['Permanente', 'Lejos', 'Cerca', 'Lectura', 'Ocasional'],
  filtros:     ['Antirreflejo', 'Filtro luz azul', 'Fotocromático', 'UV', 'Ninguno'],
  // Agudeza visual
  notacion:    ['Snellen 20/', 'Snellen 6/', 'Decimal', 'LogMAR'],
  optotipo:    ['Snellen', 'E de Snellen', 'LEA', 'HOTV', 'Anillos de Landolt', 'Cartilla de cerca'],
};
const dlId = (k: string) => `hc-dl-${k}`;

/** Edad en años cumplidos a la fecha, a partir de "YYYY-MM-DD". */
function calcularEdad(fechaNacimiento: string): number | null {
  const [y, m, d] = fechaNacimiento.split('-').map(Number);
  if (!y || !m || !d) return null;
  const hoy = new Date();
  let edad = hoy.getFullYear() - y;
  if (hoy.getMonth() + 1 < m || (hoy.getMonth() + 1 === m && hoy.getDate() < d)) edad--;
  return edad >= 0 && edad <= 120 ? edad : null;
}

const aNumero = (v: unknown): number => (v === '' || v == null ? NaN : Number(String(v).replace(',', '.')));

export function AtencionFolios({ data, mode, onField, onMotivo, diagnosticos, onDiagnosticos, procedimientos, onProcedimientos }: Props) {
  const view = mode === 'view';
  const get = (s: string, f: string) => data[s]?.[f] ?? '';
  const hoy = new Date().toISOString().split('T')[0];

  // Valores derivados: la edad sale de la fecha de nacimiento y el IMC de
  // peso/estatura. Se calculan solos y se guardan en el mismo campo de siempre;
  // solo se escribe cuando el valor cambia, para no entrar en bucle.
  const fechaNac = data.datosGenerales?.fechaNacimiento ?? '';
  const edadGuardada = data.datosGenerales?.edad ?? '';
  const peso = data.signosVitales?.peso ?? '';
  const estatura = data.signosVitales?.estatura ?? '';
  const imcGuardado = data.signosVitales?.imc ?? '';
  const edadCalculada = fechaNac ? calcularEdad(fechaNac) : null;
  const imcCalculado = (() => {
    const p = aNumero(peso), e = aNumero(estatura);
    return p > 0 && e > 0 ? (p / Math.pow(e / 100, 2)).toFixed(1) : null;
  })();
  useEffect(() => {
    if (view) return;
    if (edadCalculada != null && String(edadCalculada) !== String(edadGuardada)) {
      onField?.('datosGenerales', 'edad', String(edadCalculada));
    }
    if (imcCalculado != null && imcCalculado !== String(imcGuardado)) {
      onField?.('signosVitales', 'imc', imcCalculado);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, edadCalculada, edadGuardada, imcCalculado, imcGuardado]);

  /** Campo de texto etiquetado (bloque). Con `list` ofrece sugerencias sin impedir texto libre. */
  const T = (s: string, f: string, label: string,
             opts?: { area?: boolean; col?: string; help?: string; list?: string; placeholder?: string;
                      inputMode?: 'text' | 'numeric' | 'decimal' | 'tel' }) => (
    <label className={`block ${opts?.col ?? ''}`}>
      <span className="block text-[11px] font-medium text-slate-500 mb-0.5">
        {label}{opts?.help && <HelpTip text={opts.help} />}
      </span>
      {view ? (
        <span className="block text-sm text-slate-800 min-h-[20px] whitespace-pre-wrap">{get(s, f) || '—'}</span>
      ) : opts?.area ? (
        <textarea rows={2} value={get(s, f)} onChange={(e) => onField?.(s, f, sinEmoji(e.target.value))}
          placeholder={opts?.placeholder}
          className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
      ) : (
        <input value={get(s, f)} onChange={(e) => onField?.(s, f, sinEmoji(e.target.value))}
          list={opts?.list ? dlId(opts.list) : undefined} placeholder={opts?.placeholder} inputMode={opts?.inputMode}
          className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
      )}
    </label>
  );

  /** Campo de lista desplegable etiquetado (evita texto libre en campos de opciones cerradas). */
  const S = (s: string, f: string, label: string, options: string[]) => (
    <label className="block">
      <span className="block text-[11px] font-medium text-slate-500 mb-0.5">{label}</span>
      {view ? (
        <span className="block text-sm text-slate-800 min-h-[20px]">{get(s, f) || '—'}</span>
      ) : (
        <select value={get(s, f)} onChange={(e) => onField?.(s, f, e.target.value)}
          className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none bg-white">
          <option value="">Seleccionar...</option>
          {options.map(o => <option key={o} value={o}>{o}</option>)}
        </select>
      )}
    </label>
  );

  /** Celda compacta para tablas (sin etiqueta). Con `list` sugiere valores típicos (Snellen, dioptrías, "Normal"…). */
  const Cell = (s: string, f: string, list?: string) =>
    view ? (
      <span className="block text-sm text-slate-800 px-1">{get(s, f) || '—'}</span>
    ) : (
      <input value={get(s, f)} onChange={(e) => onField?.(s, f, sinEmoji(e.target.value))}
        list={list ? dlId(list) : undefined}
        inputMode={list && ['esfera', 'cilindro', 'eje', 'prisma', 'adicion'].includes(list) ? 'decimal' : undefined}
        className="w-full px-1.5 py-1 text-sm border border-slate-300 rounded focus:ring-1 focus:ring-blue-500 outline-none" />
    );

  /** Rellena con "Normal" las celdas vacías de una lista de campos (un clic en vez de 22). */
  const marcarNormales = (s: string, campos: string[]) => {
    campos.forEach((f) => { if (!get(s, f)) onField?.(s, f, 'Normal'); });
  };
  const BotonTodoNormal = ({ s, campos }: { s: string; campos: string[] }) => view ? null : (
    <button type="button" onClick={() => marcarNormales(s, campos)}
      className="text-[11px] text-blue-600 hover:underline">
      Marcar vacíos como "Normal"
    </button>
  );

  /** Campo de fecha etiquetado: selector desplegable (día/mes/año) en vez de texto libre. */
  const Fecha = (s: string, f: string, label: string, opts?: { maxDate?: string; minDate?: string; col?: string }) => (
    <label className={`block ${opts?.col ?? ''}`}>
      <span className="block text-[11px] font-medium text-slate-500 mb-0.5">{label}</span>
      {view ? (
        <span className="block text-sm text-slate-800 min-h-[20px]">{formatDateEs(get(s, f))}</span>
      ) : (
        <DateScroller value={get(s, f)} onChange={(v) => onField?.(s, f, v)}
          minDate={opts?.minDate} maxDate={opts?.maxDate} />
      )}
    </label>
  );

  /** Campo numérico con rango fisiológicamente plausible (evita valores absurdos como un peso de 1000 kg). */
  const N = (s: string, f: string, label: string,
             opts: { min: number; max: number; unidad?: string; readOnly?: boolean; hint?: string; help?: string }) => {
    const raw = get(s, f);
    const num = raw === '' ? NaN : Number(String(raw).replace(',', '.'));
    const fueraDeRango = raw !== '' && !Number.isNaN(num) && (num < opts.min || num > opts.max);
    return (
      <label className="block">
        <span className="block text-[11px] font-medium text-slate-500 mb-0.5">
          {label}{opts.unidad ? ` (${opts.unidad})` : ''}{opts.help && <HelpTip text={opts.help} />}
        </span>
        {view ? (
          <span className="block text-sm text-slate-800 min-h-[20px]">{raw || '—'}</span>
        ) : (
          <>
            <input type="number" step="any" min={opts.min} max={opts.max} value={raw}
              readOnly={opts.readOnly} inputMode="decimal"
              onChange={(e) => onField?.(s, f, e.target.value)}
              className={`w-full px-2.5 py-1.5 text-sm border rounded-md focus:ring-2 outline-none ${
                opts.readOnly ? 'bg-slate-50 text-slate-600' : ''} ${
                fueraDeRango ? 'border-red-400 focus:ring-red-400' : 'border-slate-300 focus:ring-blue-500'}`} />
            {opts.hint && !fueraDeRango && (
              <span className="block text-[11px] text-slate-400 mt-0.5">{opts.hint}</span>
            )}
            {fueraDeRango && (
              <span className="block text-[11px] text-red-600 mt-0.5">
                Fuera de rango plausible ({opts.min}–{opts.max}{opts.unidad ? ` ${opts.unidad}` : ''})
              </span>
            )}
          </>
        )}
      </label>
    );
  };

  const Chk = (s: string, f: string, label: string) => (
    <label className="flex items-center gap-1.5 text-sm text-slate-700">
      <input type="checkbox" disabled={view} checked={!!data[s]?.[f]}
        onChange={(e) => onField?.(s, f, e.target.checked)} className="rounded" />
      {label}
    </label>
  );

  return (
    <div className="space-y-6">
      {/* Listas de sugerencias compartidas por los campos con `list` (solo en edición). */}
      {!view && Object.entries(DL).map(([k, opciones]) => (
        <datalist key={k} id={dlId(k)}>
          {opciones.map((o) => <option key={o} value={o} />)}
        </datalist>
      ))}

      {/* ===================== FOLIO 1 ===================== */}
      <Folio n={1} title="Datos, anamnesis y agudeza visual">
        <Sub title="Datos generales">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
            {S('datosGenerales', 'tipoDocumento', 'Tipo documento', ['CC', 'TI', 'RC', 'CE', 'PA'])}
            {T('datosGenerales', 'numeroDocumento', 'N° documento', { inputMode: 'numeric' })}
            {T('datosGenerales', 'apellidos', 'Apellidos')}
            {T('datosGenerales', 'nombres', 'Nombres')}
            {T('datosGenerales', 'numeroConsultorio', 'N° consultorio', { inputMode: 'numeric' })}
            {Fecha('datosGenerales', 'fechaNacimiento', 'Fecha nacimiento', { maxDate: hoy })}
            {N('datosGenerales', 'edad', 'Edad', { min: 0, max: 120, unidad: 'años',
              readOnly: edadCalculada != null,
              hint: edadCalculada != null ? 'Calculada desde la fecha de nacimiento' : undefined })}
            {S('datosGenerales', 'estadoCivil', 'Estado civil', ['Soltero(a)', 'Casado(a)', 'Unión libre', 'Divorciado(a)', 'Viudo(a)'])}
            {S('datosGenerales', 'sexo', 'Sexo', ['Femenino', 'Masculino', 'Otro'])}
            {T('datosGenerales', 'direccion', 'Dirección')}
            {T('datosGenerales', 'localidad', 'Localidad', { list: 'localidad' })}
            {T('datosGenerales', 'ciudad', 'Ciudad', { placeholder: 'Bogotá D.C.' })}
            {T('datosGenerales', 'celular', 'Celular', { inputMode: 'tel' })}
            {T('datosGenerales', 'telefono', 'Teléfono', { inputMode: 'tel' })}
            {T('datosGenerales', 'ocupacion', 'Ocupación')}
            {T('datosGenerales', 'acompanante', 'Nombre del acompañante')}
            {T('datosGenerales', 'acompananteTelefono', 'Tel. acompañante', { inputMode: 'tel' })}
            {T('datosGenerales', 'responsable', 'Nombre del responsable')}
            {T('datosGenerales', 'responsableTelefono', 'Tel. responsable', { inputMode: 'tel' })}
            {T('datosGenerales', 'responsableParentesco', 'Parentesco', { list: 'parentesco' })}
            {T('datosGenerales', 'eps', 'Entidad promotora de salud (EPS)', { list: 'eps' })}
            {S('datosGenerales', 'tipoVinculacion', 'Tipo de vinculación', ['Contributivo', 'Subsidiado', 'Régimen especial', 'Particular', 'Vinculado'])}
            {T('datosGenerales', 'remitidoPor', 'Remitido por')}
            {Fecha('datosGenerales', 'ultimoControlVisual', 'Último control ocular/visual', { maxDate: hoy })}
          </div>
        </Sub>

        <Sub title="Motivo de consulta">
          {view ? (
            <p className="text-sm text-slate-800 whitespace-pre-wrap">{data.motivoConsulta || '—'}</p>
          ) : (
            <textarea rows={2} value={data.motivoConsulta ?? ''} onChange={(e) => onMotivo?.(e.target.value)}
              className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
          )}
        </Sub>

        <Sub title="Anamnesis">
          <div className="grid md:grid-cols-3 gap-3 mb-3">
            {T('anamnesis', 'comoEmpezo', 'Cómo empezó a presentarse', { list: 'comoEmpezo' })}
            {T('anamnesis', 'asociadoActividades', 'Asociado o no con actividades oculares', { list: 'asociado' })}
            {T('anamnesis', 'antiguedadMolestia', 'Antigüedad de la molestia', { list: 'antiguedad', placeholder: 'Ej. 3 meses' })}
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2 mb-3">
            {SINTOMAS.map(([f, l]) => <div key={f}>{Chk('anamnesis', f, l)}</div>)}
          </div>
          {T('anamnesis', 'otrosDetalle', 'Otros (detalle)')}
          <div className="grid md:grid-cols-2 gap-3 mt-3">
            {T('anamnesis', 'antecedentesPersonales', 'Antecedentes personales (estado de salud)', { area: true })}
            {T('anamnesis', 'antecedentesOculares', 'Antecedentes oculares (correcciones, cirugías, traumas)', { area: true })}
            {T('anamnesis', 'medicamentos', 'Medicamentos (nombre, dosis, frecuencia, tiempo)', { area: true })}
            {T('anamnesis', 'antecedentesFamiliares', 'Antecedentes familiares', { area: true })}
          </div>
        </Sub>

        <Sub title="Signos vitales">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {N('signosVitales', 'peso', 'Peso', { min: 0.5, max: 300, unidad: 'kg' })}
            {N('signosVitales', 'estatura', 'Estatura', { min: 30, max: 250, unidad: 'cm' })}
            {N('signosVitales', 'imc', 'IMC', { min: 10, max: 80,
              readOnly: imcCalculado != null,
              hint: imcCalculado != null ? 'Calculado desde peso y estatura' : 'Se calcula al ingresar peso y estatura' })}
            {T('signosVitales', 'tensionArterial', 'Tensión arterial (ej. 120/80)', { placeholder: '120/80', inputMode: 'numeric' })}
          </div>
        </Sub>

        <Sub title="Lensometría">
          <MedidaTabla rows={[['Derecho', 'od'], ['Izquierdo', 'oi']]} prefix="" section="lensometria"
            cols={[...MEDIDA_COLS, 'Adición', 'Dist. mec.']} render={Cell}
            listIds={['esfera', 'cilindro', 'eje', 'prisma', 'base', 'adicion', undefined]}
            fields={(eye) => [
              `${eye}Esfera`, `${eye}Cilindro`, `${eye}Eje`, `${eye}Prisma`, `${eye}Base`, `${eye}Adicion`, `${eye}DistMecanica`]} />
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mt-3">
            {T('lensometria', 'diseno', 'Diseño', { list: 'disenoLente' })}
            {T('lensometria', 'material', 'Material', { list: 'material' })}
            {T('lensometria', 'uso', 'Uso', { list: 'usoLente' })}
            {T('lensometria', 'otros', 'Otros (color, tratamiento, etc.)')}
            {T('lensometria', 'distanciaNasoPupilar', 'Dist. naso pupilar', { inputMode: 'decimal', placeholder: 'mm' })}
          </div>
        </Sub>

        <Sub title="Agudeza visual">
          <AvTabla title="Sin corrección" p="sc" Cell={Cell} />
          <div className="mt-3"><AvTabla title="Con corrección" p="cc" Cell={Cell} /></div>
          <div className="grid grid-cols-2 gap-3 mt-3">
            {T('agudezaVisual', 'sistemaNotacion', 'Sistema de notación', { list: 'notacion' })}
            {T('agudezaVisual', 'optotipo', 'Optotipo', { list: 'optotipo' })}
          </div>
        </Sub>
      </Folio>

      {/* ===================== FOLIO 2 ===================== */}
      {/* El papel fisico trae esta hoja (examen externo/motilidad/fondo de ojo)
          justo despues de la hoja 1 — antes estaba en la posicion 3. */}
      <Folio n={2} title="Examen externo, motilidad y fondo de ojo">
        <Sub title="Examen externo / Biomicroscopía">
          <div className="mb-3">{T('examenExterno', 'general', 'General', { area: true })}</div>
          <div className="flex justify-end mb-1">
            <BotonTodoNormal s="examenExterno" campos={EXAMEN_EXTERNO_ESTRUCTURAS.flatMap(([k]) => [`${k}Od`, `${k}Oi`])} />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead><tr className="text-[11px] text-slate-500"><th className="text-left py-1 pr-2">Estructura</th><th className="px-1 text-left">Ojo derecho</th><th className="px-1 text-left">Ojo izquierdo</th></tr></thead>
              <tbody>
                {EXAMEN_EXTERNO_ESTRUCTURAS.map(([k, l]) => (
                  <tr key={k} className="border-t border-slate-100">
                    <td className="py-1 pr-2 text-slate-600 whitespace-nowrap">
                      {l}{k === 'camaraAnterior' && <HelpTip text="Clasificación de Van Herick: profundidad de la cámara anterior comparada con el grosor de la córnea." />}
                    </td>
                    <td className="px-1">{Cell('examenExterno', `${k}Od`, 'normal')}</td>
                    <td className="px-1">{Cell('examenExterno', `${k}Oi`, 'normal')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="grid grid-cols-2 gap-4 mt-4">
            <DiagramaCanvas label="Segmento anterior OD" value={get('examenExterno', 'diagramaOd')} readOnly={view} template="ojo"
              onChange={(d) => onField?.('examenExterno', 'diagramaOd', d)} />
            <DiagramaCanvas label="Segmento anterior OI" value={get('examenExterno', 'diagramaOi')} readOnly={view} template="ojo"
              onChange={(d) => onField?.('examenExterno', 'diagramaOi', d)} />
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-4">
            {N('examenExterno', 'schirmerOd', 'Schirmer OD', { min: 0, max: 35, unidad: 'mm' })}
            {N('examenExterno', 'schirmerOi', 'Schirmer OI', { min: 0, max: 35, unidad: 'mm' })}
            {N('examenExterno', 'rupturaLagrimalOd', 'Tiempo de rompimiento lagrimal OD', { min: 0, max: 60, unidad: 's', help: 'Tiempo de rompimiento (ruptura) de la película lagrimal (BUT/BRUT), en segundos.' })}
            {N('examenExterno', 'rupturaLagrimalOi', 'Tiempo de rompimiento lagrimal OI', { min: 0, max: 60, unidad: 's', help: 'Tiempo de rompimiento (ruptura) de la película lagrimal (BUT/BRUT), en segundos.' })}
            {T('examenExterno', 'tonometriaInstrumento', 'Tonómetro', { list: 'tonometro' })}
            <label className="block">
              <span className="block text-[11px] font-medium text-slate-500 mb-0.5">Hora</span>
              {view ? (
                <span className="block text-sm text-slate-800 min-h-[20px]">{get('examenExterno', 'tonometriaHora') || '—'}</span>
              ) : (
                <input type="time" value={get('examenExterno', 'tonometriaHora')}
                  onChange={(e) => onField?.('examenExterno', 'tonometriaHora', e.target.value)}
                  className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none" />
              )}
            </label>
            {N('examenExterno', 'tonometriaOd', 'Tonometría OD', { min: 0, max: 80, unidad: 'mmHg' })}
            {N('examenExterno', 'tonometriaOi', 'Tonometría OI', { min: 0, max: 80, unidad: 'mmHg' })}
          </div>
        </Sub>

        <Sub title="Motilidad">
          <div className="flex items-center justify-between mb-1.5">
            <span className="block text-[11px] font-semibold text-slate-600">Reflejos pupilares</span>
            <BotonTodoNormal s="motilidad" campos={REFLEJOS_PUPILARES} />
          </div>
          <div className="grid md:grid-cols-3 gap-3">
            {T('motilidad', 'fotomotorDirectoOd', 'Fotomotor directo OD', { list: 'reflejo' })}
            {T('motilidad', 'consensualOd', 'Consensual OD', { list: 'reflejo' })}
            {T('motilidad', 'acomodativoOd', 'Acomodativo OD', { list: 'reflejo' })}
            {T('motilidad', 'fotomotorDirectoOi', 'Fotomotor directo OI', { list: 'reflejo' })}
            {T('motilidad', 'consensualOi', 'Consensual OI', { list: 'reflejo' })}
            {T('motilidad', 'acomodativoOi', 'Acomodativo OI', { list: 'reflejo' })}
          </div>
          <div className="mt-3">{T('motilidad', 'defectoPupilar', 'Presencia de defecto pupilar', { area: true })}</div>
          <div className="grid md:grid-cols-3 gap-3 mt-3">
            {S('motilidad', 'ojoDominante', 'Ojo dominante', ['OD', 'OI'])}
            {T('motilidad', 'kappaOd', 'Kappa OD', { list: 'kappa', help: 'Ángulo entre el eje visual y el eje pupilar, estimado con el reflejo corneal.' })}
            {T('motilidad', 'kappaOi', 'Kappa OI', { list: 'kappa', help: 'Ángulo entre el eje visual y el eje pupilar, estimado con el reflejo corneal.' })}
            {T('motilidad', 'hirschberg', 'Hirschberg', { list: 'hirschberg', help: 'Prueba del reflejo corneal a la luz para estimar el ángulo de desviación ocular.' })}
            {S('motilidad', 'estadoMotorTest', 'Estado motor - test', ['Cover test', 'Varilla de Maddox', 'Risley', 'Krimsky'])}
            {S('motilidad', 'estadoMotorCorreccion', 'Se hizo', ['Sin corrección', 'Con corrección'])}
            {T('motilidad', 'estadoMotor6m', 'Estado motor 6 m', { list: 'estadoMotor', placeholder: 'Ej. Exoforia 2Δ' })}
            {T('motilidad', 'estadoMotor4m', 'Estado motor 4 m', { list: 'estadoMotor' })}
            {T('motilidad', 'estadoMotor3m', 'Estado motor 3 m', { list: 'estadoMotor' })}
            {T('motilidad', 'estadoMotor40cm', 'Estado motor 40 cm', { list: 'estadoMotor' })}
            {T('motilidad', 'estadoMotor20cm', 'Estado motor 20 cm', { list: 'estadoMotor' })}
            {T('motilidad', 'duccionesOd', 'Ducciones OD', { list: 'ducciones' })}
            {T('motilidad', 'duccionesOi', 'Ducciones OI', { list: 'ducciones' })}
          </div>
          <span className="block text-[11px] font-semibold text-slate-600 mb-1.5 mt-4">Punto próximo de convergencia</span>
          <div className="grid md:grid-cols-3 gap-3">
            {T('motilidad', 'ppcDefectoReal', 'Defecto real', { list: 'ppc', inputMode: 'decimal', placeholder: 'cm', help: 'Punto próximo de convergencia (PPC): distancia en cm a la que el objeto real se duplica.' })}
            {T('motilidad', 'ppcLuz', 'Luz', { list: 'ppc', inputMode: 'decimal', placeholder: 'cm', help: 'PPC usando una luz como estímulo, en vez de un objeto real.' })}
            {T('motilidad', 'ppcLuzFiltroRojo', 'Luz + filtro rojo', { list: 'ppc', inputMode: 'decimal', placeholder: 'cm', help: 'PPC con filtro rojo sobre un ojo, para diferenciar supresión de diplopía real.' })}
          </div>
          <div className="mt-3">
            <span className="block text-[11px] font-medium text-slate-500 mb-1">Versiones</span>
            <div className="grid grid-cols-2 gap-4">
              <DiagramaCanvas label="Versiones OD" value={get('motilidad', 'diagramaVersionesOd')} readOnly={view} template="versiones"
                onChange={(d) => onField?.('motilidad', 'diagramaVersionesOd', d)} />
              <DiagramaCanvas label="Versiones OI" value={get('motilidad', 'diagramaVersionesOi')} readOnly={view} template="versiones"
                onChange={(d) => onField?.('motilidad', 'diagramaVersionesOi', d)} />
            </div>
          </div>
          <div className="mt-3">{T('motilidad', 'observaciones', 'Observaciones', { area: true })}</div>
        </Sub>

        <Sub title="Fondo de ojo">
          <div className="flex justify-end mb-1">
            <BotonTodoNormal s="fondoOjo" campos={FONDO_OJO_HALLAZGOS.flatMap(([k]) => [`${k}Od`, `${k}Oi`])} />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead><tr className="text-[11px] text-slate-500"><th className="text-left py-1 pr-2">Hallazgo</th><th className="px-1 text-left">Ojo derecho</th><th className="px-1 text-left">Ojo izquierdo</th></tr></thead>
              <tbody>
                {FONDO_OJO_HALLAZGOS.map(([k, l]) => (
                  <tr key={k} className="border-t border-slate-100">
                    <td className="py-1 pr-2 text-slate-600 whitespace-nowrap">{l}</td>
                    <td className="px-1">{Cell('fondoOjo', `${k}Od`, k)}</td>
                    <td className="px-1">{Cell('fondoOjo', `${k}Oi`, k)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="grid grid-cols-2 gap-4 mt-4">
            <DiagramaCanvas label="Fondo de ojo OD" value={get('fondoOjo', 'diagramaOd')} readOnly={view} template="fondo"
              onChange={(d) => onField?.('fondoOjo', 'diagramaOd', d)} />
            <DiagramaCanvas label="Fondo de ojo OI" value={get('fondoOjo', 'diagramaOi')} readOnly={view} template="fondo"
              onChange={(d) => onField?.('fondoOjo', 'diagramaOi', d)} />
          </div>
        </Sub>
      </Folio>

      {/* ===================== FOLIO 3 ===================== */}
      <Folio n={3} title="Refracción, pruebas complementarias y fórmula">
        <Sub title="Queratometría">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {T('queratometria', 'ojoDerecho', 'Ojo derecho', { placeholder: 'Ej. 43.00 x 44.25 @ 90' })}
            {T('queratometria', 'ojoIzquierdo', 'Ojo izquierdo', { placeholder: 'Ej. 43.00 x 44.25 @ 90' })}
            {T('queratometria', 'mirasOd', 'Miras OD', { list: 'miras' })}
            {T('queratometria', 'mirasOi', 'Miras OI', { list: 'miras' })}
            {T('queratometria', 'av', 'AV', { list: 'av' })}
          </div>
          <div className="mt-3">{T('queratometria', 'observacionesEInstrumento', 'Observaciones e instrumento', { area: true })}</div>
        </Sub>

        <Sub title="Refracción">
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="text-[11px] text-slate-500">
                  <th className="text-left py-1 pr-2">Prueba</th><th className="px-1">OD</th><th className="px-1">OI</th><th className="px-1">AV OD</th><th className="px-1">AV OI</th><th className="px-1 text-left">Observaciones</th>
                </tr>
              </thead>
              <tbody>
                {[['estatica', 'Retinoscopía estática'], ['dinamica', 'Retinoscopía dinámica'], ['cicloplegia', 'Retinoscopía bajo cicloplegia'],
                  ['subjetivo', 'Subjetivo'], ['afinacion', 'Afinación'], ['balance', 'Balance y/o ecualización'],
                  ['adicion', 'Adición']].map(([k, l]) => (
                  <tr key={k} className="border-t border-slate-100">
                    <td className="py-1 pr-2 text-slate-600 whitespace-nowrap">{l}</td>
                    <td className="px-1">{Cell('refraccion', `${k}Od`)}</td>
                    <td className="px-1">{Cell('refraccion', `${k}Oi`)}</td>
                    <td className="px-1 w-16">{Cell('refraccion', `${k}OdAv`, 'av')}</td>
                    <td className="px-1 w-16">{Cell('refraccion', `${k}OiAv`, 'av')}</td>
                    <td className="px-1">{Cell('refraccion', `${k}Observaciones`)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-2 max-w-xs">{T('refraccion', 'adicionDistancia', 'Adición - distancia')}</div>
        </Sub>

        <Sub title="Pruebas complementarias">
          <div className="grid md:grid-cols-3 gap-3">
            {T('pruebasComplementarias', 'visionCromaticaTest', 'Visión cromática - test', { list: 'vcTest' })}
            {T('pruebasComplementarias', 'visionCromaticaOd', 'Visión cromática OD', { list: 'normalAlt', placeholder: 'Ej. 14/14' })}
            {T('pruebasComplementarias', 'visionCromaticaOi', 'Visión cromática OI', { list: 'normalAlt', placeholder: 'Ej. 14/14' })}
            {T('pruebasComplementarias', 'estereopsis', 'Estereopsis', { placeholder: 'Ej. 40 seg de arco' })}
            {T('pruebasComplementarias', 'estereopsisTest', 'Estereopsis - test', { list: 'estereoTest' })}
            {T('pruebasComplementarias', 'campoVisualTest', 'Campo visual - test', { list: 'cvTest' })}
            {T('pruebasComplementarias', 'campoVisualOd', 'Campo visual OD', { list: 'normalAltM' })}
            {T('pruebasComplementarias', 'campoVisualOi', 'Campo visual OI', { list: 'normalAltM' })}
            {T('pruebasComplementarias', 'sensibilidadContrasteTest', 'Sensibilidad al contraste - test', { list: 'scTest' })}
            {T('pruebasComplementarias', 'sensibilidadContrasteOd', 'Sensibilidad al contraste OD', { list: 'normalAlt' })}
            {T('pruebasComplementarias', 'sensibilidadContrasteOi', 'Sensibilidad al contraste OI', { list: 'normalAlt' })}
            {T('pruebasComplementarias', 'foriaDistancia', 'Foria inducida (6/4/3 m)', { list: 'estadoMotor', placeholder: 'Ej. Exoforia 2Δ' })}
            {T('pruebasComplementarias', 'foria40cm', 'Foria inducida 40 cm', { list: 'estadoMotor', placeholder: 'Ej. Exoforia 4Δ' })}
            {S('pruebasComplementarias', 'foriaTest', 'Foria inducida - test', ['Cover test', 'Varilla de Maddox', 'Risley', 'Krimsky'])}
          </div>
          <div className="mt-3">{T('pruebasComplementarias', 'otrasPruebas', 'Otras pruebas complementarias', { area: true })}</div>
        </Sub>

        <Sub title="Fórmula final">
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="text-[11px] text-slate-500">
                  <th className="text-left py-1 pr-2">Visión</th><th>Ojo</th>{MEDIDA_COLS.map(c => <th key={c} className="px-1">{c}</th>)}<th className="px-1">AV</th>
                </tr>
              </thead>
              <tbody>
                {[['lejos', 'Lejos'], ['media', 'Media'], ['cerca', 'Cerca'], ['adicion', 'Adición']].flatMap(([k, l]) =>
                  [['od', 'OD'], ['oi', 'OI']].map(([eye, el], i) => (
                    <tr key={k + eye} className="border-t border-slate-100">
                      {i === 0 && <td rowSpan={2} className="py-1 pr-2 text-slate-600 align-top">{l}</td>}
                      <td className="text-slate-500 text-xs">{el}</td>
                      {['Esfera', 'Cilindro', 'Eje', 'Prisma', 'Base'].map(c =>
                        <td key={c} className="px-1">{Cell('formulaFinal', `${k}${eye === 'od' ? 'Od' : 'Oi'}${c}`, c.toLowerCase())}</td>)}
                      <td className="px-1 w-16">{Cell('formulaFinal', `${k}${eye === 'od' ? 'Od' : 'Oi'}Av`, 'av')}</td>
                    </tr>
                  )))}
              </tbody>
            </table>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
            {T('formulaFinal', 'distanciaNasoPupilar', 'Distancia naso pupilar', { inputMode: 'decimal', placeholder: 'mm' })}
            {T('formulaFinal', 'tipoLenteYMarca', 'Tipo de lente y marca', { list: 'disenoLente' })}
            {T('formulaFinal', 'material', 'Material', { list: 'material' })}
            {T('formulaFinal', 'filtros', 'Filtros', { list: 'filtros' })}
            {T('formulaFinal', 'color', 'Color')}
            {T('formulaFinal', 'uso', 'Uso', { list: 'usoLente' })}
          </div>
          <div className="mt-3">{T('formulaFinal', 'observaciones', 'Observaciones', { area: true })}</div>
        </Sub>
      </Folio>

      {/* ===================== FOLIO 4 ===================== */}
      <Folio n={4} title="Diagnóstico y conducta">
        <Sub title="Diagnóstico (CIE-10)">
          <DiagnosticosEditor diagnosticos={diagnosticos} onChange={onDiagnosticos} view={view} />
        </Sub>
        <Sub title="Procedimientos (CUPS)">
          <ProcedimientosEditor procedimientos={procedimientos} onChange={onProcedimientos} view={view} />
        </Sub>
        <Sub title="Conducta">
          <div className="grid gap-3">
            {T('conducta', 'conducta', 'Conducta', { area: true })}
            {T('conducta', 'remisiones', 'Remisiones', { area: true })}
            {T('conducta', 'control', 'Control', { area: true })}
          </div>
        </Sub>

        <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 text-[11px] leading-relaxed text-slate-500">
          <p className="font-semibold text-slate-600 mb-1">Observaciones — guía de colores para los diagramas</p>
          <p>
            Los hallazgos del segmento anterior se deben expresar en forma gráfica y descriptiva. Se deben aplicar
            los siguientes colores: <strong>verde</strong>: opacidades de los medios y coloración del iris;{' '}
            <strong>negro</strong>: neoformaciones pigmentarias y pupila; <strong>café</strong>: neoformaciones
            pigmentarias y coloración del iris; <strong>amarillo</strong>: alteraciones conjuntivales y palpebrales.
          </p>
          <p className="mt-2">
            Oftalmoscopia: los hallazgos deben expresarse en forma gráfica y descriptiva utilizando los siguientes
            colores: <strong>rojo claro</strong>: fondo; <strong>rojo oscuro</strong>: arterias y mácula;{' '}
            <strong>morado</strong>: venas; <strong>amarillo</strong>: papila y fóvea; <strong>negro y café</strong>:
            neoformaciones pigmentarias; <strong>verde</strong>: opacidades de los medios.
          </p>
        </div>
      </Folio>
    </div>
  );

  // ---- helpers de tabla específicos ----
  function AvTabla({ title, p, Cell }: { title: string; p: string; Cell: (s: string, f: string, list?: string) => ReactNode }) {
    return (
      <div>
        <div className="text-xs font-semibold text-slate-600 mb-1">{title}</div>
        <table className="w-full text-sm border-collapse">
          <thead><tr className="text-[11px] text-slate-500"><th className="text-left py-1">Ojo</th><th>Lejos</th><th>Cerca</th><th>Estenopeico<HelpTip text="Agudeza visual mirando a través de un orificio pequeño (pinhole): si mejora, el defecto es probablemente refractivo." /></th></tr></thead>
          <tbody>
            {[['Od', 'OD'], ['Oi', 'OI'], ['Ao', 'AO']].map(([e, l]) => (
              <tr key={e} className="border-t border-slate-100">
                <td className="py-1 text-slate-600">{l}</td>
                <td className="px-1">{Cell('agudezaVisual', `${p}${e}Lejos`, 'av')}</td>
                <td className="px-1">{Cell('agudezaVisual', `${p}${e}Cerca`, 'av')}</td>
                <td className="px-1">{Cell('agudezaVisual', `${p}${e}Estenopeico`, 'av')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  function MedidaTabla({ section, cols, render, fields, rows, listIds }: {
    section: string; cols: string[]; render: (s: string, f: string, list?: string) => ReactNode;
    fields: (eye: string) => string[]; rows: [string, string][]; prefix: string;
    /** Lista de sugerencias por columna (mismo orden que `cols`). */
    listIds?: (string | undefined)[];
  }) {
    return (
      <div className="overflow-x-auto">
        <table className="w-full text-sm border-collapse">
          <thead><tr className="text-[11px] text-slate-500"><th className="text-left py-1 pr-2">Ojo</th>{cols.map(c => <th key={c} className="px-1">{c}</th>)}</tr></thead>
          <tbody>
            {rows.map(([label, eye]) => (
              <tr key={eye} className="border-t border-slate-100">
                <td className="py-1 pr-2 text-slate-600">{label}</td>
                {fields(eye).map((f, i) => <td key={f} className="px-1">{render(section, f, listIds?.[i])}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }
}

function Folio({ n, title, children }: { n: number; title: string; children: ReactNode }) {
  return (
    <section className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
      <div className="px-5 py-3 bg-slate-50 border-b border-slate-200 flex items-center gap-2">
        <span className="w-6 h-6 rounded-full bg-bosque text-white text-xs flex items-center justify-center font-bold">{n}</span>
        <h3 className="text-sm font-bold text-slate-700">{title}</h3>
      </div>
      <div className="p-5 space-y-5">{children}</div>
    </section>
  );
}

function Sub({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <div className="text-xs font-bold uppercase tracking-wide text-blue-600 mb-2">{title}</div>
      {children}
    </div>
  );
}

function DiagnosticosEditor({ diagnosticos, onChange, view }: {
  diagnosticos: DiagnosticoDto[]; onChange?: (l: DiagnosticoDto[]) => void; view: boolean;
}) {
  const list = diagnosticos.length ? diagnosticos : (view ? [] : [{ codigoCie10: '', descripcion: '' }]);
  if (view) {
    return list.length ? (
      <ul className="space-y-1">
        {list.map((d, i) => (
          <li key={i} className="text-sm text-slate-800"><span className="font-mono text-blue-700">{d.codigoCie10 || '—'}</span> · {d.descripcion || '—'}</li>
        ))}
      </ul>
    ) : <p className="text-sm text-slate-400">Sin diagnósticos</p>;
  }
  const pick = (i: number, item: Cie10Item) => {
    const next = [...list];
    next[i] = { ...next[i], codigoCie10: item.codigo, descripcion: item.descripcion };
    onChange?.(next);
  };
  const clear = (i: number) => {
    const next = [...list];
    next[i] = { ...next[i], codigoCie10: '', descripcion: '' };
    onChange?.(next);
  };
  return (
    <div className="space-y-2">
      {list.map((d, i) => (
        <div key={i} className="flex gap-2 items-start">
          <Cie10Picker
            codigo={d.codigoCie10 ?? ''}
            descripcion={d.descripcion ?? ''}
            onPick={(item) => pick(i, item)}
            onClear={() => clear(i)}
          />
          <button type="button" onClick={() => onChange?.(list.filter((_, j) => j !== i))}
            className="px-2 py-1.5 text-red-500 hover:bg-red-50 rounded">✕</button>
        </div>
      ))}
      <button type="button" onClick={() => onChange?.([...list, { codigoCie10: '', descripcion: '' }])}
        className="text-sm text-blue-600 hover:underline">+ Agregar diagnóstico</button>
      <p className="text-[11px] text-slate-400">Los diagnósticos deben seleccionarse del catálogo CIE-10.</p>
    </div>
  );
}

/** Buscador-autocompletar de un diagnóstico contra el catálogo CIE-10. */
function Cie10Picker({ codigo, descripcion, onPick, onClear }: {
  codigo: string; descripcion: string;
  onPick: (item: Cie10Item) => void; onClear: () => void;
}) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [opciones, setOpciones] = useState<Cie10Item[]>([]);
  const [cargando, setCargando] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);

  const seleccionado = !!codigo;

  useEffect(() => {
    if (seleccionado || query.trim().length < 2) { setOpciones([]); return; }
    const id = setTimeout(async () => {
      setCargando(true);
      try {
        const res = await cie10Service.buscar(query.trim());
        setOpciones(res.data.data ?? []);
        setOpen(true);
      } catch { /* silencioso */ } finally { setCargando(false); }
    }, 250);
    return () => clearTimeout(id);
  }, [query, seleccionado]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => { if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  if (seleccionado) {
    return (
      <div className="flex-1 flex items-center gap-2 px-2.5 py-1.5 text-sm border border-emerald-200 bg-emerald-50 rounded-md">
        <span className="font-mono text-emerald-700 font-semibold">{codigo}</span>
        <span className="text-slate-700 truncate">· {descripcion}</span>
        <button type="button" onClick={onClear} className="ml-auto text-slate-400 hover:text-red-500" title="Cambiar">✕</button>
      </div>
    );
  }

  return (
    <div ref={boxRef} className="flex-1 relative">
      <input
        placeholder="Buscar diagnóstico por código o nombre (CIE-10)…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => opciones.length && setOpen(true)}
        className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
      />
      {open && (
        <ul className="absolute z-20 mt-1 w-full max-h-56 overflow-y-auto bg-white border border-slate-200 rounded-md shadow-lg text-sm">
          {cargando && <li className="px-3 py-2 text-slate-400">Buscando…</li>}
          {!cargando && opciones.length === 0 && query.trim().length >= 2 && (
            <li className="px-3 py-2 text-slate-400">Sin coincidencias</li>
          )}
          {opciones.map((o) => (
            <li key={o.codigo}>
              <button type="button" onClick={() => { onPick(o); setOpen(false); setQuery(''); }}
                className="w-full text-left px-3 py-2 hover:bg-blue-50 flex gap-2">
                <span className="font-mono text-blue-700 shrink-0">{o.codigo}</span>
                <span className="text-slate-700">{o.descripcion}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function ProcedimientosEditor({ procedimientos, onChange, view }: {
  procedimientos: ProcedimientoDto[]; onChange?: (l: ProcedimientoDto[]) => void; view: boolean;
}) {
  const list = procedimientos.length ? procedimientos : (view ? [] : [{ codigoCups: '', descripcion: '' }]);
  if (view) {
    return list.length ? (
      <ul className="space-y-1">
        {list.map((p, i) => (
          <li key={i} className="text-sm text-slate-800"><span className="font-mono text-blue-700">{p.codigoCups || '—'}</span> · {p.descripcion || '—'}</li>
        ))}
      </ul>
    ) : <p className="text-sm text-slate-400">Sin procedimientos</p>;
  }
  const pick = (i: number, item: CupsItem) => {
    const next = [...list];
    next[i] = { ...next[i], codigoCups: item.codigo, descripcion: item.descripcion };
    onChange?.(next);
  };
  const clear = (i: number) => {
    const next = [...list];
    next[i] = { ...next[i], codigoCups: '', descripcion: '' };
    onChange?.(next);
  };
  return (
    <div className="space-y-2">
      {list.map((p, i) => (
        <div key={i} className="flex gap-2 items-start">
          <CupsPicker
            codigo={p.codigoCups ?? ''}
            descripcion={p.descripcion ?? ''}
            onPick={(item) => pick(i, item)}
            onClear={() => clear(i)}
          />
          <button type="button" onClick={() => onChange?.(list.filter((_, j) => j !== i))}
            className="px-2 py-1.5 text-red-500 hover:bg-red-50 rounded">✕</button>
        </div>
      ))}
      <button type="button" onClick={() => onChange?.([...list, { codigoCups: '', descripcion: '' }])}
        className="text-sm text-blue-600 hover:underline">+ Agregar procedimiento</button>
      <p className="text-[11px] text-slate-400">Los procedimientos deben seleccionarse del catálogo CUPS.</p>
    </div>
  );
}

/** Buscador-autocompletar de un procedimiento contra el catálogo CUPS. */
function CupsPicker({ codigo, descripcion, onPick, onClear }: {
  codigo: string; descripcion: string;
  onPick: (item: CupsItem) => void; onClear: () => void;
}) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [opciones, setOpciones] = useState<CupsItem[]>([]);
  const [cargando, setCargando] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);

  const seleccionado = !!codigo;

  useEffect(() => {
    if (seleccionado || query.trim().length < 2) { setOpciones([]); return; }
    const id = setTimeout(async () => {
      setCargando(true);
      try {
        const res = await cupsService.buscar(query.trim());
        setOpciones(res.data.data ?? []);
        setOpen(true);
      } catch { /* silencioso */ } finally { setCargando(false); }
    }, 250);
    return () => clearTimeout(id);
  }, [query, seleccionado]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => { if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  if (seleccionado) {
    return (
      <div className="flex-1 flex items-center gap-2 px-2.5 py-1.5 text-sm border border-emerald-200 bg-emerald-50 rounded-md">
        <span className="font-mono text-emerald-700 font-semibold">{codigo}</span>
        <span className="text-slate-700 truncate">· {descripcion}</span>
        <button type="button" onClick={onClear} className="ml-auto text-slate-400 hover:text-red-500" title="Cambiar">✕</button>
      </div>
    );
  }

  return (
    <div ref={boxRef} className="flex-1 relative">
      <input
        placeholder="Buscar procedimiento por código o nombre (CUPS)…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => opciones.length && setOpen(true)}
        className="w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
      />
      {open && (
        <ul className="absolute z-20 mt-1 w-full max-h-56 overflow-y-auto bg-white border border-slate-200 rounded-md shadow-lg text-sm">
          {cargando && <li className="px-3 py-2 text-slate-400">Buscando…</li>}
          {!cargando && opciones.length === 0 && query.trim().length >= 2 && (
            <li className="px-3 py-2 text-slate-400">Sin coincidencias</li>
          )}
          {opciones.map((o) => (
            <li key={o.codigo}>
              <button type="button" onClick={() => { onPick(o); setOpen(false); setQuery(''); }}
                className="w-full text-left px-3 py-2 hover:bg-blue-50 flex gap-2">
                <span className="font-mono text-blue-700 shrink-0">{o.codigo}</span>
                <span className="text-slate-700">{o.descripcion}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
