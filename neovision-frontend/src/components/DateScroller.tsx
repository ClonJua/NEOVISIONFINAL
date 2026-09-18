import { useEffect, useRef, useState } from 'react';

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
const DIAS_SEMANA = ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa'];

interface DateScrollerProps {
  value: string; // "YYYY-MM-DD"
  onChange: (date: string) => void;
  minDate?: string; // "YYYY-MM-DD"
  maxDate?: string; // "YYYY-MM-DD"
}

/** Selector de fecha en forma de calendario (grilla de mes), con navegación rápida de mes/año. */
export default function DateScroller({ value, onChange, minDate, maxDate }: DateScrollerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);

  const parseDate = (dateStr: string) => {
    if (!dateStr) return new Date();
    const [y, m, d] = dateStr.split('-').map(Number);
    return new Date(y, m - 1, d);
  };

  const formatDate = (d: Date): string => {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  };

  const selectedDate = parseDate(value);
  const todayStr = formatDate(new Date());

  // Mes/año que se está viendo en la grilla (puede diferir del seleccionado
  // mientras se navega). Se sincroniza con el valor real cada vez que se abre.
  const [viewYear, setViewYear] = useState(selectedDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(selectedDate.getMonth());

  useEffect(() => {
    if (isOpen) {
      setViewYear(selectedDate.getFullYear());
      setViewMonth(selectedDate.getMonth());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => { if (boxRef.current && !boxRef.current.contains(e.target as Node)) setIsOpen(false); };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  const today = new Date();
  const minYear = minDate ? parseDate(minDate).getFullYear() : today.getFullYear() - 100;
  const maxYear = maxDate ? parseDate(maxDate).getFullYear() : today.getFullYear() + 10;
  const years = Array.from({ length: maxYear - minYear + 1 }, (_, i) => minYear + i);

  const firstDayOfMonth = new Date(viewYear, viewMonth, 1).getDay();
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();

  const irMesAnterior = () => {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  };
  const irMesSiguiente = () => {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  };

  const elegirDia = (day: number) => {
    onChange(formatDate(new Date(viewYear, viewMonth, day)));
    setIsOpen(false);
  };

  return (
    <div className="relative" ref={boxRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 bg-white hover:border-slate-400 transition text-left"
      >
        {selectedDate.getDate()} {MESES[selectedDate.getMonth()]} {selectedDate.getFullYear()}
      </button>

      {isOpen && (
        <div className="absolute top-full left-0 mt-2 bg-white border border-slate-200 rounded-lg shadow-lg p-3 z-50 w-64">
          {/* Navegación de mes/año */}
          <div className="flex items-center gap-1 mb-2">
            <button type="button" onClick={irMesAnterior}
              className="w-7 h-7 flex items-center justify-center rounded text-slate-500 hover:bg-slate-100 transition">
              ‹
            </button>
            <select value={viewMonth} onChange={e => setViewMonth(Number(e.target.value))}
              className="flex-1 text-xs font-medium text-slate-700 border border-slate-200 rounded px-1.5 py-1 outline-none">
              {MESES.map((m, i) => <option key={m} value={i}>{m}</option>)}
            </select>
            <select value={viewYear} onChange={e => setViewYear(Number(e.target.value))}
              className="text-xs font-medium text-slate-700 border border-slate-200 rounded px-1.5 py-1 outline-none">
              {years.map(y => <option key={y} value={y}>{y}</option>)}
            </select>
            <button type="button" onClick={irMesSiguiente}
              className="w-7 h-7 flex items-center justify-center rounded text-slate-500 hover:bg-slate-100 transition">
              ›
            </button>
          </div>

          {/* Grilla del mes */}
          <div className="grid grid-cols-7 gap-y-1 place-items-center">
            {DIAS_SEMANA.map(d => (
              <span key={d} className="text-[10px] font-medium text-slate-400">{d}</span>
            ))}
            {Array.from({ length: firstDayOfMonth }).map((_, i) => <span key={`pad-${i}`} />)}
            {Array.from({ length: daysInMonth }, (_, i) => i + 1).map(day => {
              const dateStr = formatDate(new Date(viewYear, viewMonth, day));
              const seleccionado = dateStr === value;
              const deshabilitado = (!!minDate && dateStr < minDate) || (!!maxDate && dateStr > maxDate);
              const esHoy = dateStr === todayStr;
              return (
                <button
                  key={day}
                  type="button"
                  disabled={deshabilitado}
                  onClick={() => elegirDia(day)}
                  className={`w-8 h-8 text-xs rounded-full transition ${
                    seleccionado
                      ? 'bg-bosque text-white font-semibold'
                      : deshabilitado
                      ? 'text-slate-300 cursor-not-allowed'
                      : esHoy
                      ? 'text-bosque font-semibold hover:bg-slate-100'
                      : 'text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  {day}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
