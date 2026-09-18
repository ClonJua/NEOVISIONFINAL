import { useState } from 'react';
import { InfoIcon } from './Icons';

/**
 * Leyenda desplegable que explica qué contiene cada pestaña y qué se hace en ella.
 *
 * <p>Las pestañas se agrupan por *eje*, no en una lista plana: es lo que resuelve la
 * confusión de fondo, porque pestañas de ejes distintos pueden mostrar la misma cita
 * o la misma historia desde ángulos diferentes y parecer duplicadas.
 */

export interface FilaGuia {
  /** Debe coincidir con la clave de pestaña de la página para resaltar la actual. */
  clave: string;
  nombre: string;
  contiene: string;
  accion: string;
}

export interface GrupoGuia {
  titulo: string;
  filas: FilaGuia[];
}

interface Props {
  grupos: GrupoGuia[];
  actual: string;
  /** Aclaración final sobre el malentendido típico entre dos pestañas. */
  nota?: React.ReactNode;
}

function Fila({ fila, activa }: { fila: FilaGuia; activa: boolean }) {
  return (
    <li className={`flex flex-col gap-0.5 sm:flex-row sm:gap-3 rounded-md px-2 py-1.5 ${activa ? 'bg-white ring-1 ring-bosque/30' : ''}`}>
      <span className={`sm:w-44 shrink-0 text-sm font-medium ${activa ? 'text-bosque' : 'text-slate-700'}`}>
        {fila.nombre}
        {activa && <span className="ml-1 text-[10px] font-normal text-slate-400">estás aquí</span>}
      </span>
      <span className="text-sm text-slate-600">
        {fila.contiene}
        <span className="block text-xs text-slate-500 mt-0.5">
          <strong className="text-slate-600">Qué haces:</strong> {fila.accion}
        </span>
      </span>
    </li>
  );
}

export function GuiaPestanas({ grupos, actual, nota }: Props) {
  const [abierto, setAbierto] = useState(false);

  return (
    <div className="mb-4 border border-slate-200 rounded-lg bg-slate-50/60">
      <button
        type="button"
        onClick={() => setAbierto(!abierto)}
        aria-expanded={abierto}
        className="w-full flex items-center gap-2 px-4 py-2.5 text-left text-sm text-slate-600 hover:text-slate-800"
      >
        <InfoIcon size={15} />
        <span className="font-medium">¿Qué va en cada pestaña?</span>
        <span className="ml-auto text-slate-400 text-xs">{abierto ? 'Ocultar' : 'Ver'}</span>
      </button>

      {abierto && (
        <div className="px-4 pb-3 space-y-3">
          {grupos.map((g) => (
            <div key={g.titulo}>
              <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-1">{g.titulo}</h4>
              <ul className="space-y-1">
                {g.filas.map((f) => <Fila key={f.clave} fila={f} activa={f.clave === actual} />)}
              </ul>
            </div>
          ))}
          {nota && (
            <p className="text-xs text-slate-500 border-t border-slate-200 pt-2">{nota}</p>
          )}
        </div>
      )}
    </div>
  );
}
