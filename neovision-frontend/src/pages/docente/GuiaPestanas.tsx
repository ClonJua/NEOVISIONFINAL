import { GuiaPestanas as Guia, type GrupoGuia } from '../../components/ui';

/**
 * Contenido de la leyenda de pestañas del docente.
 *
 * <p>Dos ejes que se confunden: por un lado la agenda, donde "Mis sesiones" es la
 * jornada completa y "Citas" son los bloques que viven dentro de ella; por otro las
 * historias clínicas, donde "Bandeja" es solo lo que espera tu firma y "Historias de
 * pacientes" es el archivo completo. La misma atención puede verse en las dos.
 */

export type PestanaDocente = 'bandeja' | 'sesiones' | 'citas' | 'pacientes';

const GRUPOS: GrupoGuia[] = [
  {
    titulo: 'Tu agenda — la sesión es la jornada; las citas son sus bloques',
    filas: [
      {
        clave: 'sesiones',
        nombre: 'Mis sesiones',
        contiene: 'Las jornadas de práctica que supervisas: fecha, horario, salón y estudiantes asignados.',
        accion: 'Consultar cuándo y con quién te toca. Los cupos son pacientes, no estudiantes.',
      },
      {
        clave: 'citas',
        nombre: 'Citas',
        contiene: 'Cada bloque de todas tus sesiones, con su estado y su paciente.',
        accion: 'Confirmar la asistencia del paciente — eso es lo que suma a la meta del estudiante.',
      },
    ],
  },
  {
    titulo: 'Historias clínicas — una espera tu acción, la otra es consulta',
    filas: [
      {
        clave: 'bandeja',
        nombre: 'Bandeja de revisión',
        contiene: 'Solo las atenciones que tus estudiantes te enviaron y están esperándote.',
        accion: 'Finalizarlas (las firmas y quedan selladas) o devolverlas con un motivo.',
      },
      {
        clave: 'pacientes',
        nombre: 'Historias de pacientes',
        contiene: 'El archivo completo de los pacientes atendidos en tus sesiones, con su auditoría.',
        accion: 'Consultar. Aquí no se firma ni se devuelve nada.',
      },
    ],
  },
];

const NOTA = (
  <>
    Si una atención ya no está en <strong>Bandeja</strong>, no se perdió: salió de ahí
    porque la finalizaste o la devolviste. La sigues viendo en{' '}
    <strong>Historias de pacientes</strong>, que muestra todo sin importar su estado.
  </>
);

export function GuiaPestanas({ actual }: { actual: PestanaDocente }) {
  return <Guia grupos={GRUPOS} actual={actual} nota={NOTA} />;
}
