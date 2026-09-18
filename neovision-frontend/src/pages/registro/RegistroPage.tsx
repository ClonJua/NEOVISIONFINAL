import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authService } from '../../api/authService';
import { useAuth } from '../../auth/context/AuthContext';
import { useToast } from '../../components/ui';

const EMOJI_RE = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/u;

export default function RegistroPage() {
  const toast = useToast();
  const [form, setForm] = useState({
    nombre: '',
    apellido: '',
    correo: '',
    contrasena: '',
    tipoDocumento: 'CC',
    numeroDocumento: '',
    fechaNacimiento: '',
    telefono: '',
    autorizaTratamientoDatos: false,
    representanteNombre: '',
    representanteDocumento: '',
    representanteParentesco: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const esMenor = form.tipoDocumento === 'TI' || form.tipoDocumento === 'RC';

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (EMOJI_RE.test(form.contrasena)) {
      setError('La contraseña no puede contener emojis');
      toast.error('La contraseña no puede contener emojis');
      return;
    }
    setLoading(true);
    try {
      const res = await authService.registro(form);
      const { token, rol, correo } = res.data.data;
      login(token, rol, correo);
      navigate('/dashboard');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Error en el registro. Intente de nuevo.';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-lg bg-white rounded-2xl shadow-lg p-8">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-slate-800">NeoVision</h1>
          <p className="text-slate-500 mt-1">Registro de Paciente</p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Nombre</label>
              <input type="text" name="nombre" value={form.nombre} onChange={handleChange} required
                className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Apellido</label>
              <input type="text" name="apellido" value={form.apellido} onChange={handleChange} required
                className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Correo electronico</label>
            <input type="email" name="correo" value={form.correo} onChange={handleChange} required
              placeholder="correo@ejemplo.com"
              className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Contrasena</label>
            <input type="password" name="contrasena" value={form.contrasena} onChange={handleChange} required
              minLength={12} placeholder="Minimo 12 caracteres"
              className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Tipo de documento</label>
              <select name="tipoDocumento" value={form.tipoDocumento} onChange={handleChange}
                className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition bg-white">
                <option value="CC">Cedula de Ciudadania</option>
                <option value="TI">Tarjeta de Identidad</option>
                <option value="RC">Registro Civil</option>
                <option value="CE">Cedula de Extranjeria</option>
                <option value="PA">Pasaporte</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Numero de documento</label>
              <input type="text" name="numeroDocumento" value={form.numeroDocumento}
                onChange={e => { if (/^\d{0,15}$/.test(e.target.value)) setForm({...form, numeroDocumento: e.target.value}); }}
                required placeholder="1234567890"
                className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Fecha de nacimiento</label>
              <input type="date" name="fechaNacimiento" value={form.fechaNacimiento} onChange={handleChange} required
                className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Celular</label>
              <input type="tel" name="telefono" value={form.telefono}
                onChange={e => { if (/^\d{0,10}$/.test(e.target.value)) setForm({...form, telefono: e.target.value}); }}
                required pattern="3\d{9}" maxLength={10}
                placeholder="3001234567" title="Celular colombiano: 10 digitos, inicia con 3"
                className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
            </div>
          </div>

          {esMenor && (
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-3">
              <p className="text-xs text-amber-800">
                El paciente es menor de edad. Por la <strong>Ley 1581 de 2012</strong>, la autorización de tratamiento
                de datos debe darla su representante legal (padre, madre o acudiente), no el menor.
              </p>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nombre del representante legal</label>
                <input type="text" name="representanteNombre" value={form.representanteNombre} onChange={handleChange} required
                  className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Documento del representante</label>
                  <input type="text" name="representanteDocumento" value={form.representanteDocumento}
                    onChange={e => { if (/^\d{0,15}$/.test(e.target.value)) setForm({ ...form, representanteDocumento: e.target.value }); }}
                    required placeholder="1234567890"
                    className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Parentesco</label>
                  <input type="text" name="representanteParentesco" value={form.representanteParentesco} onChange={handleChange}
                    required placeholder="Madre, padre, acudiente..."
                    className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-bosque focus:border-bosque outline-none transition" />
                </div>
              </div>
            </div>
          )}

          <label className="flex items-start gap-2 text-sm text-slate-600 bg-slate-50 border border-slate-200 rounded-lg p-3">
            <input type="checkbox" checked={form.autorizaTratamientoDatos} required
              onChange={e => setForm({ ...form, autorizaTratamientoDatos: e.target.checked })}
              className="mt-0.5 rounded" />
            <span>Autorizo el tratamiento de mis datos personales y clinicos conforme a la
              <strong> Ley 1581 de 2012</strong> y la <strong>Resolucion 1995 de 1999</strong>, con fines de
              atencion optometrica y academica en el Programa de Optometria de la Universidad El Bosque.</span>
          </label>

          <button type="submit" disabled={loading || !form.autorizaTratamientoDatos}
            className="w-full bg-bosque hover:bg-bosque-dark text-white font-medium py-2.5 rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed">
            {loading ? 'Registrando...' : 'Registrarse'}
          </button>
        </form>

        <p className="text-center text-sm text-slate-500 mt-6">
          Ya tienes cuenta?{' '}
          <Link to="/login" className="text-bosque hover:underline font-medium">
            Inicia sesion
          </Link>
        </p>
      </div>
    </div>
  );
}
