import { useState } from 'react';
import { Badge, InfoIcon } from '../ui';

/**
 * Leyenda explícita de los cuatro estados de una atención.
 *
 * <p>Los estados son el punto donde más se pierden estudiantes y docentes: el
 * nombre del estado dice *en qué* está el registro, pero no *qué puede hacer cada
 * uno* ni *a quién le toca mover*. Esto último es lo que se explica aquí, en la
 * misma pantalla, sin obligar a abrir la guía.
 */

type Rol = 'estudiante' | 'docente';

interface EstadoInfo {
  estado: string;
  tono: 'slate' | 'amber' | 'red' | 'emerald';
  significa: string;
  /** Qué puede hacer quien está mirando la pantalla. */
  puedes: Record<Rol, string>;
}

const ESTADOS: EstadoInfo[] = [
  {
    estado: 'BORRADOR',
    tono: 'slate',
    significa: 'Se está diligenciando. Nadie la ha revisado todavía.',
    puedes: {
      estudiante: 'Editar y guardar las veces que quieras. No hay guardado automático: usa "Guardar borrador".',
      docente: 'Aún no te llega: no aparece en tu bandeja hasta que el estudiante la envíe.',
    },
  },
  {
    estado: 'PENDIENTE_REVISION',
    tono: 'amber',
    significa: 'El estudiante la envió y espera al docente.',
    puedes: {
      estudiante: 'No puedes editarla mientras esté aquí: está en manos del docente. El plazo de edición se detiene.',
      docente: 'Revisarla y decidir: finalizarla (queda sellada) o devolverla con un motivo.',
    },
  },
  {
    estado: 'DEVUELTA',
    tono: 'red',
    significa: 'El docente pidió correcciones.',
    puedes: {
      estudiante: 'Corregir lo que te señalaron y volver a enviarla. La devolución te reabre el plazo de edición.',
      docente: 'Esperar: no puedes finalizarla hasta que el estudiante la reenvíe, aunque no le cambie nada.',
    },
  },
  {
    estado: 'FINALIZADA',
    tono: 'emerald',
    significa: 'Firmada por el docente y sellada. Es inmutable (Res. 1995/1999, Art. 18).',
    puedes: {
      estudiante: 'Solo consultarla. Ya no se edita nada.',
      docente: 'Solo consultarla. Cualquier corrección posterior se registra como adenda, sin tocar el original.',
    },
  },
];

export function EstadosAtencionInfo({ rol }: { rol: Rol }) {
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
        <span className="font-medium">¿Qué significa cada estado y qué te toca hacer?</span>
        <span className="ml-auto text-slate-400 text-xs">{abierto ? 'Ocultar' : 'Ver'}</span>
      </button>

      {abierto && (
        <ul className="px-4 pb-3 space-y-3">
          {ESTADOS.map((e) => (
            <li key={e.estado} className="flex flex-col gap-1 sm:flex-row sm:gap-3">
              <div className="sm:w-48 shrink-0">
                <Badge tone={e.tono}>{e.estado.replace('_', ' ')}</Badge>
              </div>
              <div className="text-sm">
                <p className="text-slate-600">{e.significa}</p>
                <p className="text-slate-500 text-xs mt-0.5">
                  <strong className="text-slate-600">Tú:</strong> {e.puedes[rol]}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
