// Colores del Manual de Identidad Visual de la Universidad El Bosque.
// `primary` usa el naranja asignado al programa de Optometría (pág. 15,
// "colores programas académicos": HEX f15a22) en vez del genérico azul de
// Microsoft que traía por defecto — sobre el fondo oscuro de la app este
// tono tiene mejor contraste que el verde institucional (HEX 3e4729, muy
// oscuro), que se reserva para `institutional`.
export const theme = {
  colors: {
    background:   '#0f172a',
    surface:      '#1e293b',
    primary:      '#F15A22',
    primaryDim:   '#F15A2280',
    institutional:'#3E4729',
    text:         '#ffffff',
    textMuted:    '#94a3b8',
    border:       '#334155',
    danger:       '#ef4444',
    success:      '#22c55e',
  },
  spacing: { xs: 4, sm: 8, md: 16, lg: 24, xl: 32 },
  radius:  { sm: 6, md: 10, lg: 16 },
} as const;
