import type { ReactNode, HTMLAttributes } from 'react';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  hover?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

const paddings = {
  none: '',
  sm: 'p-3',
  md: 'p-5',
  lg: 'p-6',
};

export function Card({ children, hover = false, padding = 'md', className = '', ...rest }: CardProps) {
  return (
    <div
      className={`
        bg-white border border-slate-200 rounded-xl shadow-sm
        ${hover ? 'hover:shadow-md hover:border-slate-300 transition-all duration-200' : ''}
        ${paddings[padding]} ${className}
      `}
      {...rest}
    >
      {children}
    </div>
  );
}

export function StatCard({
  label,
  value,
  icon,
  tone = 'blue',
}: {
  label: string;
  value: string | number;
  icon?: ReactNode;
  tone?: 'blue' | 'emerald' | 'amber' | 'slate' | 'indigo';
}) {
  const tones = {
    blue: 'bg-blue-50 text-blue-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    slate: 'bg-slate-100 text-slate-600',
    indigo: 'bg-indigo-50 text-indigo-600',
  };
  return (
    <Card padding="md" className="flex items-center gap-4">
      {icon && (
        <div className={`flex items-center justify-center w-11 h-11 rounded-lg ${tones[tone]}`}>
          {icon}
        </div>
      )}
      <div>
        <div className="text-xs uppercase tracking-wide text-slate-500 font-medium">{label}</div>
        <div className="text-2xl font-bold text-slate-800 leading-tight">{value}</div>
      </div>
    </Card>
  );
}
