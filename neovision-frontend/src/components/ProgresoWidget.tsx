interface ProgresoWidgetProps {
  semestre: number;
  atendidos: number;
  meta: number;
}

export default function ProgresoWidget({ semestre, atendidos, meta }: ProgresoWidgetProps) {
  const pct = meta > 0 ? Math.min(Math.round((atendidos / meta) * 100), 100) : 0;

  return (
    <div
      style={{ background: '#E6F4EF', border: '1px solid #A7D7C5', borderRadius: '12px' }}
      className="p-5 mb-6"
    >
      <div className="flex items-center justify-between mb-3">
        <div>
          <p style={{ color: '#14532D', fontWeight: 700, fontSize: '15px', margin: 0 }}>
            Progreso del semestre {semestre}
          </p>
          <p style={{ color: '#1F7A63', fontSize: '13px', margin: '2px 0 0' }}>
            Has atendido{' '}
            <strong>{atendidos}</strong> de <strong>{meta}</strong> pacientes este semestre
          </p>
        </div>
        <div
          style={{
            width: '56px', height: '56px', borderRadius: '50%',
            background: '#1F7A63', color: 'white',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 700, fontSize: '15px', flexShrink: 0,
          }}
        >
          {pct}%
        </div>
      </div>

      {/* Barra de progreso */}
      <div style={{ background: '#A7D7C5', borderRadius: '999px', height: '10px', overflow: 'hidden' }}>
        <div
          style={{
            width: `${pct}%`,
            height: '100%',
            background: pct >= 100 ? '#166534' : '#1F7A63',
            borderRadius: '999px',
            transition: 'width 0.6s ease',
          }}
        />
      </div>

      {/* Etiquetas de rango */}
      <div className="flex justify-between mt-1" style={{ fontSize: '11px', color: '#1F7A63' }}>
        <span>0</span>
        <span>{Math.round(meta / 2)}</span>
        <span>{meta}</span>
      </div>

      {pct >= 100 && (
        <p style={{ color: '#166534', fontWeight: 600, fontSize: '13px', marginTop: '8px', textAlign: 'center' }}>
          ¡Meta alcanzada! Excelente trabajo este semestre.
        </p>
      )}
    </div>
  );
}
