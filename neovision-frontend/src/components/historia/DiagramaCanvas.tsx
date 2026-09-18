import { useEffect, useRef, useState } from 'react';

export type DiagramaTemplate = 'ojo' | 'fondo' | 'versiones';

interface Props {
  label: string;
  value?: string;            // PNG en base64 (data URL)
  onChange?: (dataUrl: string) => void;
  readOnly?: boolean;
  /** Figura guía impresa en el papel, para dibujar encima (igual que el formato físico). */
  template?: DiagramaTemplate;
}

/** Dibuja la figura guía del papel (óvalo del ojo, círculo de fondo, o cruz de 9 posiciones de mirada). */
function drawTemplate(ctx: CanvasRenderingContext2D, w: number, h: number, template?: DiagramaTemplate) {
  if (!template) return;
  ctx.save();
  ctx.strokeStyle = '#94a3b8';
  ctx.fillStyle = '#94a3b8';
  ctx.lineWidth = 1.5;
  const cx = w / 2;
  const cy = h / 2;

  if (template === 'ojo') {
    // Óvalo del ojo (segmento anterior) con la pupila al centro, como en el papel.
    ctx.beginPath();
    ctx.ellipse(cx, cy, w * 0.42, h * 0.34, 0, 0, Math.PI * 2);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(cx, cy, Math.min(w, h) * 0.12, 0, Math.PI * 2);
    ctx.stroke();
  } else if (template === 'fondo') {
    // Círculo en blanco del fondo de ojo, igual al del papel.
    const r = Math.min(w, h) * 0.4;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.stroke();
  } else if (template === 'versiones') {
    // Figura de versiones oculares: línea horizontal con un tope vertical en cada extremo, igual al papel.
    const halfWidth = w * 0.35;
    const halfHeight = h * 0.3;
    ctx.beginPath();
    ctx.moveTo(cx - halfWidth, cy);
    ctx.lineTo(cx + halfWidth, cy);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx - halfWidth, cy - halfHeight);
    ctx.lineTo(cx - halfWidth, cy + halfHeight);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx + halfWidth, cy - halfHeight);
    ctx.lineTo(cx + halfWidth, cy + halfHeight);
    ctx.stroke();
  }
  ctx.restore();
}

/**
 * Lienzo para los diagramas clínicos (segmento anterior / fondo de ojo / versiones). En modo
 * edición permite dibujar con el mouse y exporta PNG base64; en solo lectura
 * muestra la imagen guardada. Reemplaza los esquemas a mano del formato en papel,
 * incluyendo la figura guía impresa (óvalo, círculo o cruz) sobre la que se dibuja.
 */
export function DiagramaCanvas({ label, value, onChange, readOnly = false, template }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const drawing = useRef(false);
  const [hasContent, setHasContent] = useState(!!value);

  // Pinta la figura guía y, encima, la imagen guardada al montar / cambiar el valor.
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    drawTemplate(ctx, canvas.width, canvas.height, template);
    if (value) {
      const img = new Image();
      img.onload = () => ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      img.src = value;
      setHasContent(true);
    } else {
      setHasContent(false);
    }
  }, [value, template]);

  const pos = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const rect = canvasRef.current!.getBoundingClientRect();
    return {
      x: (e.clientX - rect.left) * (canvasRef.current!.width / rect.width),
      y: (e.clientY - rect.top) * (canvasRef.current!.height / rect.height),
    };
  };

  const start = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (readOnly) return;
    drawing.current = true;
    const ctx = canvasRef.current!.getContext('2d')!;
    const p = pos(e);
    ctx.beginPath();
    ctx.moveTo(p.x, p.y);
  };

  const move = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (readOnly || !drawing.current) return;
    const ctx = canvasRef.current!.getContext('2d')!;
    const p = pos(e);
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1e293b';
    ctx.lineTo(p.x, p.y);
    ctx.stroke();
  };

  const end = () => {
    if (readOnly || !drawing.current) return;
    drawing.current = false;
    setHasContent(true);
    onChange?.(canvasRef.current!.toDataURL('image/png'));
  };

  const limpiar = () => {
    const canvas = canvasRef.current!;
    const ctx = canvas.getContext('2d')!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    drawTemplate(ctx, canvas.width, canvas.height, template);
    setHasContent(false);
    onChange?.('');
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs font-medium text-slate-600">{label}</span>
        {!readOnly && hasContent && (
          <button type="button" onClick={limpiar} className="text-[11px] text-red-600 hover:underline">
            Limpiar
          </button>
        )}
      </div>
      <canvas
        ref={canvasRef}
        width={300}
        height={200}
        onPointerDown={start}
        onPointerMove={move}
        onPointerUp={end}
        onPointerLeave={end}
        className={`w-full rounded-lg border border-slate-300 bg-white touch-none ${readOnly ? '' : 'cursor-crosshair'}`}
      />
      {readOnly && !hasContent && (
        <p className="text-[11px] text-slate-400 mt-1">Sin diagrama</p>
      )}
    </div>
  );
}
