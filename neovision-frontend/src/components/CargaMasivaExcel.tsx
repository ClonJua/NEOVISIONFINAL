import { useRef, useState } from 'react';
import type { AxiosResponse } from 'axios';
import type { ApiResponse } from '../api/authService';
import type { CargaMasivaResponse } from '../api/adminService';
import { useToast } from './ui/Toast';

interface Props {
  /** Plural en minúsculas, se usa en los textos: "docentes" | "estudiantes". */
  entidad: string;
  onDescargarPlantilla: () => Promise<AxiosResponse<Blob>>;
  onSubir: (archivo: File) => Promise<AxiosResponse<ApiResponse<CargaMasivaResponse>>>;
  /** Se llama cuando al menos un registro se creó, para refrescar la lista. */
  onExito: () => void;
}

/** Panel para crear muchos docentes o estudiantes a la vez desde un Excel. */
export default function CargaMasivaExcel({ entidad, onDescargarPlantilla, onSubir, onExito }: Props) {
  const toast = useToast();
  const [mostrar, setMostrar] = useState(false);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [subiendo, setSubiendo] = useState(false);
  const [resultado, setResultado] = useState<CargaMasivaResponse | null>(null);
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const descargarPlantilla = async () => {
    try {
      const res = await onDescargarPlantilla();
      const url = window.URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `plantilla_${entidad}.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error('No se pudo descargar la plantilla');
    }
  };

  const subir = async () => {
    if (!archivo) return;
    setSubiendo(true);
    setError('');
    setResultado(null);
    try {
      const res = await onSubir(archivo);
      const data = res.data.data;
      setResultado(data);
      if (data.creados > 0) onExito();
      const resumen = `Se crearon ${data.creados} de ${data.totalFilas} ${entidad}.`;
      if (data.errores.length === 0) toast.success(resumen);
      else toast.error(`${resumen} Revisa las filas con error.`);
      setArchivo(null);
      if (inputRef.current) inputRef.current.value = '';
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error al procesar el archivo';
      setError(msg);
      toast.error(msg);
    } finally {
      setSubiendo(false);
    }
  };

  return (
    <div className="mb-6">
      <button type="button" onClick={() => setMostrar(!mostrar)}
        className="text-sm font-medium text-bosque hover:text-bosque-dark underline underline-offset-2">
        {mostrar ? 'Ocultar carga masiva' : `Cargar varios ${entidad} desde Excel`}
      </button>

      {mostrar && (
        <div className="bg-white border border-slate-200 rounded-lg p-6 mt-3 space-y-4">
          <div className="flex items-start justify-between flex-wrap gap-3">
            <p className="text-sm text-slate-600 max-w-2xl">
              Sube un Excel (.xlsx) con un {entidad.slice(0, -1)} por fila, con las columnas de la plantilla.
              Cada fila se crea por separado: si alguna tiene un error, las demás igual se guardan y aquí
              verás qué fila falló y por qué.
            </p>
            <button type="button" onClick={descargarPlantilla}
              className="text-sm text-blue-600 hover:text-blue-700 font-medium whitespace-nowrap">
              Descargar plantilla
            </button>
          </div>

          <div className="flex items-center gap-3 flex-wrap">
            <input ref={inputRef} type="file" accept=".xlsx,.xls"
              onChange={e => { setArchivo(e.target.files?.[0] || null); setResultado(null); setError(''); }}
              className="text-sm text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-slate-100 file:text-slate-700 file:text-sm file:font-medium hover:file:bg-slate-200" />
            <button type="button" onClick={subir} disabled={!archivo || subiendo}
              className="bg-bosque hover:bg-bosque-dark text-white text-sm font-medium px-4 py-2 rounded-lg transition disabled:opacity-50">
              {subiendo ? 'Procesando...' : 'Cargar archivo'}
            </button>
          </div>

          {error && <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>}

          {resultado && (
            <div className="space-y-3">
              <div className={`px-4 py-3 rounded-lg text-sm border ${resultado.errores.length === 0
                ? 'bg-green-50 border-green-200 text-green-700'
                : 'bg-amber-50 border-amber-200 text-amber-800'}`}>
                Se crearon <strong>{resultado.creados}</strong> de <strong>{resultado.totalFilas}</strong> {entidad}.
              </div>
              {resultado.errores.length > 0 && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm">
                  <p className="font-medium text-red-700 mb-1">Filas con error ({resultado.errores.length}) — corrígelas en el Excel y vuelve a subir solo esas:</p>
                  <ul className="space-y-0.5 text-red-700 max-h-48 overflow-y-auto">
                    {resultado.errores.map((e, i) => (
                      <li key={i}><span className="font-medium">Fila {e.fila}:</span> {e.mensaje}</li>
                    ))}
                  </ul>
                </div>
              )}
              {resultado.advertencias.length > 0 && (
                <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm">
                  <p className="font-medium text-amber-800 mb-1">Advertencias ({resultado.advertencias.length}):</p>
                  <ul className="space-y-0.5 text-amber-800 max-h-48 overflow-y-auto">
                    {resultado.advertencias.map((a, i) => (
                      <li key={i}><span className="font-medium">Fila {a.fila}:</span> {a.mensaje}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
