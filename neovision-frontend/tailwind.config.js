/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Colores del Manual de Identidad Visual de la Universidad El Bosque
        // (manual_de_imagen_corporativa_universidad_el_bosque.pdf).
        // Verde institucional — pág. 4: PANTONE 5743 C, CMYK 68/51/87/48, HEX 3e4729.
        // Se usa para marca, navegación y botones principales de acción; los
        // colores de ESTADO (disponible/reservado/cancelado/vencida) no se
        // tocan, son semánticos.
        bosque: {
          DEFAULT: '#3E4729',
          dark: '#2C331D',
          light: '#5C6B3E',
        },
        // Naranja del programa de Optometría — pág. 15 ("colores programas
        // académicos"): CMYK 0/80/100/0, HEX f15a22. El manual reserva el
        // naranja institucional genérico (f5841f) para la marca Universidad a
        // secas; como NeoVision es software del programa de Optometría
        // específicamente, se usa el tono que el propio manual asigna a ese
        // programa en vez del naranja genérico.
        bosqueOrange: {
          DEFAULT: '#F15A22',
          dark: '#B5441A',
          light: '#F6946F',
        },
      },
    },
  },
  plugins: [],
}

