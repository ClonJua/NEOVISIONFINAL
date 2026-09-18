import { GuiaPestanas as Guia, type GrupoGuia } from '../../components/ui';

/**
 * Contenido de la leyenda de pestañas del estudiante.
 *
 * <p>Tres pestañas siguen **la cita** (un bloque de agenda) y se diferencian por su
 * estado — sin paciente, con paciente, o ya pasada. La cuarta sigue **la historia
 * clínica**, que es otro objeto con su propio ciclo. Una misma cita aparece en
 * "Agendadas" y a la vez genera una atención que vive en "Mis atenciones"; sin
 * explicarlo, parecen duplicados.
 */

export type PestanaEstudiante = 'disponibles' | 'agendadas' | 'historial' | 'atenciones';

const GRUPOS: GrupoGuia[] = [
  {
    titulo: 'Tus citas — se diferencian por el estado del bloque',
    filas: [
      {
        clave: 'disponibles',
        nombre: 'Disponibles',
        contiene: 'Tus bloques que todavía no tienen paciente.',
        accion: 'Asignarles un paciente, nuevo o ya registrado.',
      },
      {
        clave: 'agendadas',
        nombre: 'Agendadas',
        contiene: 'Bloques con paciente asignado que aún no se han atendido.',
        accion: 'Atender y abrir la historia clínica (solo dentro del horario de la cita, ±30 min).',
      },
      {
        clave: 'historial',
        nombre: 'Historial',
        contiene: 'Citas que ya terminaron: completadas, canceladas o vencidas.',
        accion: 'Consultar. Las completadas son las que suman a tu meta.',
      },
    ],
  },
  {
    titulo: 'Tus historias clínicas — es otra cosa',
    filas: [
      {
        clave: 'atenciones',
        nombre: 'Mis atenciones',
        contiene: 'Las historias clínicas que has diligenciado, con su estado de revisión.',
        accion: 'Ver en qué va cada una: borrador, en revisión, devuelta o finalizada.',
      },
    ],
  },
];

const NOTA = (
  <>
    Una misma cita está en <strong>Agendadas</strong> y, apenas abres su historia, aparece
    también en <strong>Mis atenciones</strong>. No están duplicadas: una te dice el estado
    de la <em>cita</em>, la otra el del <em>documento</em>.
  </>
);

export function GuiaPestanas({ actual }: { actual: PestanaEstudiante }) {
  return <Guia grupos={GRUPOS} actual={actual} nota={NOTA} />;
}
