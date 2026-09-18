import { useEffect, type RefObject } from 'react';

/**
 * Presiona `key` (por defecto "/") para enfocar `ref` —el buscador de una
 * tabla, normalmente— sin tener que ir a buscarlo con el mouse. No dispara
 * mientras ya se está escribiendo en otro campo.
 */
export function useFocusShortcut(ref: RefObject<HTMLElement | null>, key = '/') {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== key) return;
      const el = document.activeElement as HTMLElement | null;
      const tag = el?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el?.isContentEditable) return;
      e.preventDefault();
      ref.current?.focus();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [ref, key]);
}
