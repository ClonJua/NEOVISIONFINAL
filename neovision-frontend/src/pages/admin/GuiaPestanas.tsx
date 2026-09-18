import { GuiaPestanas as Guia, type GrupoGuia } from '../../components/ui';

/**
 * Contenido de la leyenda de pestañas del administrador.
 *
 * <p>Lo que más se pierde aquí es que la agenda **se construye en cadena**: un espacio
 * contiene salones, un salón aloja una sesión (la jornada), y la sesión genera las
 * citas (los bloques). Las citas no se crean sueltas. Y "Calendario" no es otra cosa:
 * son esas mismas citas vistas en el mes.
 */

export type PestanaAdmin =
  | 'citas' | 'sesiones' | 'calendario' | 'espacios'
  | 'historias' | 'solicitudes'
  | 'docentes' | 'estudiantes' | 'pacientes'
  | 'reportes';

const GRUPOS: GrupoGuia[] = [
  {
    titulo: 'Agendamiento — se construye en cadena: espacio → sesión → cita',
    filas: [
      {
        clave: 'espacios',
        nombre: 'Espacios',
        contiene: 'Los lugares físicos y los salones (consultorios) que hay en cada uno.',
        accion: 'Crearlos una sola vez. Son el lugar donde después ocurre la práctica.',
      },
      {
        clave: 'sesiones',
        nombre: 'Sesiones',
        contiene: 'La jornada de práctica: docente, salón, horario, cupos y estudiantes asignados.',
        accion: 'Crearla. Al guardarla, los bloques de cita se generan solos según los cupos.',
      },
      {
        clave: 'citas',
        nombre: 'Citas',
        contiene: 'Cada bloque que vive dentro de una sesión, con su paciente y su estado.',
        accion: 'Asignar o reprogramar pacientes, y crear bloques extra si algo no se generó solo.',
      },
      {
        clave: 'calendario',
        nombre: 'Calendario',
        contiene: 'Las mismas citas, pero vistas sobre el mes.',
        accion: 'Ver la ocupación de un vistazo y detectar días vacíos o saturados.',
      },
    ],
  },
  {
    titulo: 'Historias clínicas — una es el archivo, la otra son trámites',
    filas: [
      {
        clave: 'historias',
        nombre: 'Historias Clínicas',
        contiene: 'El archivo de todos los pacientes, con sus atenciones y la auditoría de accesos.',
        accion: 'Consultar. Desde aquí no se firma ni se edita nada.',
      },
      {
        clave: 'solicitudes',
        nombre: 'Solicitudes HC',
        contiene: 'Pacientes que pidieron una copia de su historia (derecho de acceso).',
        accion: 'Aprobarlas (se envía el PDF al correo) o registrar una presencial para quien no tiene correo.',
      },
    ],
  },
  {
    titulo: 'Personas — el correo es el usuario de ingreso',
    filas: [
      {
        clave: 'docentes',
        nombre: 'Docentes',
        contiene: 'Quienes supervisan las sesiones y firman las historias.',
        accion: 'Crearlos con correo institucional. Ingresan solo con Microsoft, sin contraseña.',
      },
      {
        clave: 'estudiantes',
        nombre: 'Estudiantes',
        contiene: 'Quienes atienden a los pacientes, organizados por semestre.',
        accion: 'Crearlos uno a uno o por carga masiva desde Excel.',
      },
      {
        clave: 'pacientes',
        nombre: 'Pacientes',
        contiene: 'Las personas que se atienden en la clínica.',
        accion: 'Registrarlos. Si no tienen correo, quedan sin cuenta y se les avisa por teléfono.',
      },
    ],
  },
  {
    titulo: 'Control académico',
    filas: [
      {
        clave: 'reportes',
        nombre: 'Reportes',
        contiene: 'Indicadores de ocupación e inasistencia, y el avance de cada estudiante frente a su meta.',
        accion: 'Ajustar las metas por semestre y avanzar de semestre a quienes ya la cumplieron.',
      },
    ],
  },
];

const NOTA = (
  <>
    Las citas no se crean sueltas: nacen de una <strong>sesión</strong>. Si no ves bloques
    en <strong>Citas</strong>, casi siempre es porque la sesión no tiene estudiantes
    asignados o se quedó corta de cupos — revísala ahí primero.
  </>
);

export function GuiaPestanas({ actual }: { actual: PestanaAdmin }) {
  return <Guia grupos={GRUPOS} actual={actual} nota={NOTA} />;
}
