const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const DIAS   = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];

export function formatFecha(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${DIAS[date.getDay()]} ${d} ${MESES[m - 1]}`;
}

export function formatHora(hhmmss: string): string {
  return hhmmss.slice(0, 5);
}

export function formatRangoHora(inicio: string, fin: string): string {
  return `${formatHora(inicio)} - ${formatHora(fin)}`;
}
