import { Modal } from './Modal';

type Rol = 'admin' | 'docente' | 'estudiante' | 'paciente';

interface Seccion {
  titulo: string;
  items: string[];
}

const CONTENIDO: Record<Rol, { titulo: string; secciones: Seccion[] }> = {
  admin: {
    titulo: 'Guía rápida — Administrador',
    secciones: [
      {
        titulo: 'Agenda',
        items: [
          'Sesiones: al crear una sesión con estudiantes asignados, los bloques de cita se generan automáticamente — ya no hace falta crearlos uno por uno en Citas.',
          'Los cupos son el número de pacientes que admite la jornada, no de estudiantes. Cada cupo es un bloque: un paciente atendido por una pareja de estudiantes.',
          'Si vas a hacer varias rondas (las mismas parejas atienden más pacientes en la franja), sube los cupos y agrega esos bloques a mano.',
          'Citas: crea bloques adicionales, o asigna/reprograma un paciente si algo no se generó solo.',
        ],
      },
      {
        titulo: 'Usuarios',
        items: [
          'El correo es el usuario de ingreso y es único en todo el sistema. El correo institucional queda reservado para docentes y estudiantes.',
          'Un paciente puede registrarse sin correo: no tendrá cuenta ni recibirá notificaciones, y se le avisa por teléfono. Es lo normal en adultos mayores.',
          'Si un paciente olvida su contraseña, restablécela desde Pacientes — no existe recuperación automática por correo.',
          'Reportes → Distribución: revisa el progreso de cada estudiante frente a su meta y avanza de semestre a quienes ya la cumplieron.',
        ],
      },
      {
        titulo: 'Historias clínicas',
        items: [
          'Solicitudes: si un paciente sin cuenta pide su historia en la clínica, usa "Solicitud presencial" — verifica su documento, queda registrado que tú lo hiciste y se descarga el PDF para entregárselo.',
        ],
      },
    ],
  },

  docente: {
    titulo: 'Guía rápida — Docente',
    secciones: [
      {
        titulo: 'Tu bandeja de revisión',
        items: [
          'Ahí llega lo que tus estudiantes te envían. Solo aparecen las atenciones en PENDIENTE_REVISION.',
          'Sobre cada una decides: finalizarla (la firmas y queda sellada) o devolverla con un motivo para que la corrijan.',
          'Si la devuelves, deja de ser tuya hasta que el estudiante la reenvíe — aunque no le cambie nada. No puedes finalizar una atención devuelta.',
        ],
      },
      {
        titulo: 'Finalizar: es definitivo',
        items: [
          'Al finalizar, la atención queda inmutable (Res. 1995/1999, Art. 18) con un sello de integridad. No hay forma de reabrirla.',
          'Si después hay que corregir algo, se registra como adenda: queda bajo tu autoría y con fecha, sin modificar el original.',
          'Necesitas tu firma registrada en Mi perfil → Firma y sello. Sin ella el sistema no te deja finalizar.',
        ],
      },
      {
        titulo: 'Ojo con esto',
        items: [
          'Revisa pronto: mientras la atención espera en tu bandeja, el estudiante no puede tocarla. Si la devuelves tarde, el sistema le reabre el plazo 2 días para que pueda corregir.',
          'Citas: confirma la asistencia del paciente una vez atendida la consulta — esa confirmación no la hace el estudiante.',
          'Un paciente lo atiende una pareja de estudiantes, pero la atención es una sola y compartida: cualquiera de los dos puede diligenciarla.',
        ],
      },
    ],
  },

  estudiante: {
    titulo: 'Guía rápida — Estudiante',
    secciones: [
      {
        titulo: 'Cómo fluye una historia clínica',
        items: [
          '1. Disponibles: asigna un paciente (nuevo o existente) a uno de tus bloques.',
          '2. Agendadas: abre la historia clínica de la cita y diligénciala.',
          '3. "Enviar a revisión": pasa a tu docente. Desde ese momento no puedes editarla.',
          '4. El docente la firma (queda finalizada) o te la devuelve con un motivo para que corrijas y la reenvíes.',
        ],
      },
      {
        titulo: 'Plazos',
        items: [
          'Tienes 2 días desde la fecha de la cita para editar los datos clínicos (principio de oportunidad, Res. 1995/1999).',
          'El tiempo que la atención pasa esperando revisión no te consume el plazo, y cada devolución te lo reabre por 2 días más.',
          'Arriba del formulario siempre ves hasta qué día puedes editar.',
          'Si el plazo cierra, aún puedes enviarla a revisión para que el docente la finalice — lo que ya no puedes es cambiar datos.',
        ],
      },
      {
        titulo: 'Ojo con esto',
        items: [
          'No hay guardado automático: usa "Guardar borrador" (o Ctrl/Cmd+S) seguido mientras escribes.',
          'Solo puedes ver la historia del paciente dentro del horario de la cita, con 30 minutos de margen antes y después.',
          'La atención es compartida con tu pareja de práctica: los dos pueden diligenciar la misma, no hagan una cada uno.',
          'Registra tu firma en Mi perfil → Firma y sello; se aplica sola al enviar a revisión.',
          'Una vez finalizada no se edita nada: las correcciones van como adenda.',
        ],
      },
      {
        titulo: 'Tu progreso',
        items: [
          'Cada semestre tienes una meta mínima de pacientes atendidos. En Resumen ves cuántos llevas frente a la meta.',
          'Cuentan las citas marcadas como COMPLETADO — y esa confirmación de asistencia la hace el docente, no tú. Diligenciar la historia no suma por sí solo.',
        ],
      },
    ],
  },

  paciente: {
    titulo: 'Guía rápida — Paciente',
    secciones: [
      {
        titulo: 'Tus citas',
        items: [
          'Disponibles: elige un bloque de cita según la especialidad que necesitas y resérvalo.',
          'Mis citas: consulta el estado de tus citas reservadas o cancélalas si ya no puedes asistir.',
          'Solo puedes tener una cita reservada a la vez: espera a que se complete, se cancele o venza para agendar otra.',
          'Te llegan recordatorios por correo 24 horas y 1 hora antes de la cita.',
        ],
      },
      {
        titulo: 'Tu historia clínica',
        items: [
          'Puedes solicitar una copia desde "Mi historia". Se te envía en PDF al correo registrado cuando la secretaría la aprueba.',
          'Tu historia se abre en tu primera atención: si aún no te han atendido, todavía no hay nada que solicitar.',
        ],
      },
    ],
  },
};

export function AyudaModal({ open, onClose, rol }: { open: boolean; onClose: () => void; rol: Rol }) {
  const c = CONTENIDO[rol];
  return (
    <Modal open={open} onClose={onClose} title={c.titulo} size="md">
      <div className="space-y-5">
        {c.secciones.map((s) => (
          <section key={s.titulo}>
            <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-1.5">{s.titulo}</h3>
            <ul className="space-y-2 text-sm text-slate-600">
              {s.items.map((item, i) => (
                <li key={i} className="flex gap-2">
                  <span className="text-slate-300 mt-0.5">•</span>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </Modal>
  );
}
