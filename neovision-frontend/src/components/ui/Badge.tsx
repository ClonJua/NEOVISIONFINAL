import type { ReactNode } from 'react';

type Tone = 'blue' | 'emerald' | 'amber' | 'red' | 'slate' | 'indigo' | 'purple';

const tones: Record<Tone, string> = {
  blue: 'bg-blue-50 text-blue-700 ring-blue-200',
  emerald: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  slate: 'bg-slate-100 text-slate-600 ring-slate-200',
  indigo: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  purple: 'bg-purple-50 text-purple-700 ring-purple-200',
};

export function Badge({
  tone = 'slate',
  children,
  icon,
  className = '',
}: {
  tone?: Tone;
  children: ReactNode;
  icon?: ReactNode;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ring-1 ring-inset ${tones[tone]} ${className}`}
    >
      {icon}
      {children}
    </span>
  );
}

const estadoMap: Record<string, { tone: Tone; label: string }> = {
  DISPONIBLE: { tone: 'emerald', label: 'Disponible' },
  RESERVADO: { tone: 'blue', label: 'Reservado' },
  COMPLETADO: { tone: 'slate', label: 'Completado' },
  CANCELADO: { tone: 'red', label: 'Cancelado' },
  VENCIDA: { tone: 'amber', label: 'Vencida' },
};

export function EstadoBadge({ estado }: { estado: string }) {
  const cfg = estadoMap[estado] || { tone: 'slate' as Tone, label: estado };
  return <Badge tone={cfg.tone}>{cfg.label}</Badge>;
}
