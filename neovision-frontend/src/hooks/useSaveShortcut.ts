import { useEffect } from 'react';

/**
 * Ctrl/Cmd+S dispara `onSave` mientras `enabled` es true (ej. un formulario
 * abierto). Acelerador para uso experto (heurística de Nielsen #7): quien usa
 * el sistema a diario no debería tener que ir al mouse cada vez a guardar.
 */
export function useSaveShortcut(onSave: () => void, enabled: boolean) {
  useEffect(() => {
    if (!enabled) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        onSave();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onSave, enabled]);
}
