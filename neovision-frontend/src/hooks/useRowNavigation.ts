import { useEffect, useState, type RefObject } from 'react';

interface Options<T> {
  items: T[];
  onSelect: (item: T) => void;
  enabled?: boolean;
  /** El buscador de la tabla: ArrowUp/ArrowDown/Enter funcionan aunque tenga el foco. */
  searchRef?: RefObject<HTMLElement | null>;
}

/**
 * Navegación de tabla por teclado (heurística de Nielsen #7): ArrowUp/ArrowDown
 * mueven una fila "activa" resaltada sobre `items`, Enter dispara `onSelect`
 * con esa fila (normalmente la misma acción que el botón "Editar"), Escape
 * quita el resaltado. No interfiere con selects nativos ni con escribir en
 * otros campos de texto del formulario de filtros.
 */
export function useRowNavigation<T>({ items, onSelect, enabled = true, searchRef }: Options<T>) {
  const [activeIndex, setActiveIndex] = useState(-1);

  useEffect(() => {
    setActiveIndex(-1);
  }, [items.length]);

  useEffect(() => {
    if (!enabled) return;

    const isOtroCampoDeTexto = () => {
      const el = document.activeElement as HTMLElement | null;
      if (!el) return false;
      if (searchRef?.current && el === searchRef.current) return false;
      const tag = el.tagName;
      return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable;
    };

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowDown') {
        if (isOtroCampoDeTexto()) return;
        e.preventDefault();
        setActiveIndex((i) => Math.min(items.length - 1, i + 1));
      } else if (e.key === 'ArrowUp') {
        if (isOtroCampoDeTexto()) return;
        e.preventDefault();
        setActiveIndex((i) => Math.max(0, i - 1));
      } else if (e.key === 'Enter') {
        if (isOtroCampoDeTexto()) return;
        if (activeIndex < 0 || activeIndex >= items.length) return;
        e.preventDefault();
        onSelect(items[activeIndex]);
      } else if (e.key === 'Escape') {
        setActiveIndex(-1);
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [items, activeIndex, onSelect, enabled, searchRef]);

  return { activeIndex, setActiveIndex };
}
