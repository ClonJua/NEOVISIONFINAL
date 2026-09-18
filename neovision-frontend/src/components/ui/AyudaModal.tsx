import { Modal } from './Modal';

type Rol = 'admin' | 'docente' | 'estudiante' | 'paciente';

const CONTENIDO: Record<Rol, { titulo: string; items: string[] }> = {
  admin: {
    titulo: 'Guía rápida — Administrador',
    items: [
      'Sesiones: al crear una sesión con estudiantes asignados, los bloques de cita se generan automáticamente según los cupos — ya no hace falta crearlos uno por uno en Citas.',
      'Citas: crea bloques adicionales a mano, o asigna/reprograma un paciente si algo no se generó solo.',
      'Reportes → Distribución: revisa el progreso de cada estudiante frente a su meta de pacientes y avanza de semestre a quienes ya la cumplieron, sin editarlos uno por uno.',
      'Pacientes: si un paciente olvida su contraseña, restablécela ahí mismo — no existe recuperación automática por correo.',
    ],
  },
  docente: {
    titulo: 'Guía rápida — Docente',
    items: [
      'Bandeja de revisión: revisa lo que tus estudiantes enviaron; fírmalo para finalizarlo o devuélvelo con un motivo para que lo corrijan.',
      'Citas: confirma la asistencia del paciente una vez atendida la consulta — esa confirmación ya no la hace el estudiante.',
      'Historias de pacientes: consulta el historial de los pacientes atendidos en tus sesiones.',
    ],
  },
  estudiante: {
    titulo: 'Guía rápida — Estudiante',
    items: [
      'Disponibles: asigna un paciente (nuevo o existente) a uno de tus bloques disponibles.',
      'Agendadas: abre la historia clínica de tu cita para diligenciarla. Solo puedes verla dentro del horario de la cita, con 30 minutos de margen antes y después.',
      'No hay guardado automático: usa "Guardar borrador" (o Ctrl/Cmd+S) seguido mientras escribes.',
      'Cuando termines, usa "Enviar a revisión" para que tu docente la firme y la finalice.',
    ],
  },
  paciente: {
    titulo: 'Guía rápida — Paciente',
    items: [
      'Disponibles: elige un bloque de cita según la especialidad que necesitas y resérvalo.',
      'Mis citas: consulta el estado de tus citas reservadas o cancélalas si ya no puedes asistir.',
    ],
  },
};

export function AyudaModal({ open, onClose, rol }: { open: boolean; onClose: () => void; rol: Rol }) {
  const c = CONTENIDO[rol];
  return (
    <Modal open={open} onClose={onClose} title={c.titulo} size="md">
      <ul className="space-y-2.5 text-sm text-slate-600">
        {c.items.map((item, i) => (
          <li key={i} className="flex gap-2">
            <span className="text-slate-300 mt-0.5">•</span>
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </Modal>
  );
}
