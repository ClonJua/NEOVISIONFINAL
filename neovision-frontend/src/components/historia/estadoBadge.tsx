import { Badge } from '../../components/ui';
import type { EstadoAtencion } from '../../api/historiaClinicaService';

const MAP: Record<EstadoAtencion, { tone: 'slate' | 'amber' | 'red' | 'emerald'; label: string }> = {
  BORRADOR:           { tone: 'slate',   label: 'Borrador' },
  PENDIENTE_REVISION: { tone: 'amber',   label: 'Pendiente revisión' },
  DEVUELTA:           { tone: 'red',     label: 'Devuelta' },
  FINALIZADA:         { tone: 'emerald', label: 'Finalizada' },
};

export function EstadoAtencionBadge({ estado }: { estado: EstadoAtencion }) {
  const m = MAP[estado];
  return <Badge tone={m.tone}>{m.label}</Badge>;
}
