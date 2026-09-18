import { useState } from 'react';
import { InfoIcon } from './Icons';

/**
 * Heurística de Nielsen #10 (ayuda y documentación): un "?" contextual junto a
 * abreviaturas clínicas crípticas (PPC, Kappa, BRUT...) que explica qué
 * significan sin obligar a salir de la pantalla a buscarlo.
 */
export function HelpTip({ text }: { text: string }) {
  const [open, setOpen] = useState(false);

  return (
    <span
      className="relative inline-flex align-middle ml-1"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        type="button"
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        className="text-slate-400 hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-300 rounded-full"
        aria-label="Ayuda"
      >
        <InfoIcon size={13} />
      </button>
      {open && (
        <span
          role="tooltip"
          className="absolute z-30 left-1/2 -translate-x-1/2 bottom-full mb-1.5 w-56 text-[11px] leading-snug text-left normal-case font-normal bg-slate-800 text-white px-2.5 py-1.5 rounded-md shadow-lg pointer-events-none"
        >
          {text}
        </span>
      )}
    </span>
  );
}
