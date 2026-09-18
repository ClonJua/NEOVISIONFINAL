import { useState, useEffect, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMsal } from '@azure/msal-react';
import { authService } from '../../api/authService';
import { useAuth } from '../../auth/context/AuthContext';
import { loginRequest } from '../../auth/msalConfig';
import { Button, Input, useToast, AlertCircleIcon } from '../../components/ui';

export default function LoginPage() {
  const [modo, setModo] = useState<'paciente' | 'institucional'>('paciente');
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, isAuthenticated, rol } = useAuth();
  const navigate = useNavigate();
  const { instance } = useMsal();
  const toast = useToast();

  const redirectByRole = (r: string) => {
    switch (r) {
      case 'ROLE_ADMIN':      navigate('/admin'); break;
      case 'ROLE_ESTUDIANTE': navigate('/estudiante'); break;
      case 'ROLE_DOCENTE':    navigate('/docente'); break;
      default:                navigate('/dashboard'); break;
    }
  };

  useEffect(() => {
    if (isAuthenticated && rol) {
      redirectByRole(rol);
      return;
    }
    const msalErr = sessionStorage.getItem('msalError');
    if (msalErr) {
      setError(msalErr);
      toast.error(msalErr);
      setModo('institucional');
      sessionStorage.removeItem('msalError');
    }
  }, [isAuthenticated, rol]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await authService.login({ correo, contrasena });
      const { token, rol, correo: email } = res.data.data;
      login(token, rol, email);
      toast.success('Bienvenido a NeoVision');
      redirectByRole(rol);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Credenciales incorrectas.';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleMicrosoftLogin = async () => {
    setError('');
    setLoading(true);
    try {
      await instance.loginRedirect(loginRequest);
    } catch (err: any) {
      const msg = 'Error al iniciar con Microsoft: ' + (err.message || '');
      setError(msg);
      toast.error(msg);
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-bosque/5 to-bosqueOrange/5 px-4 py-8">
      {/* Decorative background */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 -left-32 w-96 h-96 bg-bosque/20 rounded-full blur-3xl opacity-30" />
        <div className="absolute bottom-0 -right-32 w-96 h-96 bg-bosqueOrange/20 rounded-full blur-3xl opacity-30" />
      </div>

      <div className="relative w-full max-w-md">
        <div className="bg-white/90 backdrop-blur-xl rounded-3xl shadow-xl ring-1 ring-slate-200/50 p-8">
          {/* Logo */}
          <div className="flex flex-col items-center mb-6">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-bosque to-bosqueOrange flex items-center justify-center text-white shadow-lg shadow-bosque/30 mb-4">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-slate-800">NeoVision</h1>
            <p className="text-sm text-slate-500 mt-1">Clínica de Optometría · Universidad El Bosque</p>
          </div>

          {/* Selector de modo */}
          <div className="flex rounded-xl bg-slate-100 p-1 mb-6" role="tablist">
            <button
              role="tab"
              aria-selected={modo === 'paciente'}
              onClick={() => { setModo('paciente'); setError(''); }}
              className={`flex-1 py-2.5 text-sm font-medium rounded-lg transition ${
                modo === 'paciente' ? 'bg-white text-bosque shadow-sm' : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              Soy Paciente
            </button>
            <button
              role="tab"
              aria-selected={modo === 'institucional'}
              onClick={() => { setModo('institucional'); setError(''); }}
              className={`flex-1 py-2.5 text-sm font-medium rounded-lg transition ${
                modo === 'institucional' ? 'bg-white text-bosque shadow-sm' : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              Institucional
            </button>
          </div>

          {error && (
            <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 px-3.5 py-2.5 rounded-lg mb-4 text-sm">
              <AlertCircleIcon size={16} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {modo === 'paciente' ? (
            <>
              <form onSubmit={handleSubmit} className="space-y-4">
                <Input
                  label="Correo electrónico"
                  type="email"
                  value={correo}
                  onChange={(e) => setCorreo(e.target.value)}
                  required
                  placeholder="tu@correo.com"
                  autoComplete="email"
                />
                <Input
                  label="Contraseña"
                  type="password"
                  value={contrasena}
                  onChange={(e) => setContrasena(e.target.value)}
                  required
                  placeholder="••••••••"
                  autoComplete="current-password"
                />
                <Button type="submit" loading={loading} fullWidth size="lg">
                  Iniciar Sesion
                </Button>
              </form>
              <p className="text-center text-sm text-slate-500 mt-6">
                No tienes cuenta?{' '}
                <Link to="/registro" className="text-bosque hover:text-bosque-dark font-semibold hover:underline">
                  Registrate aqui
                </Link>
              </p>
            </>
          ) : (
            <div className="text-center space-y-4">
              <p className="text-sm text-slate-600 px-4">
                Docentes, estudiantes y personal administrativo acceden con su cuenta de <strong>Microsoft 365</strong>.
              </p>
              <button
                onClick={handleMicrosoftLogin}
                disabled={loading}
                className="w-full flex items-center justify-center gap-3 bg-[#2f2f2f] hover:bg-[#1a1a1a] active:bg-black text-white font-medium py-3 rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed shadow-sm hover:shadow"
              >
                <svg width="20" height="20" viewBox="0 0 21 21">
                  <rect x="1" y="1" width="9" height="9" fill="#f25022" />
                  <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
                  <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
                  <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
                </svg>
                {loading ? 'Redirigiendo...' : 'Continuar con Microsoft'}
              </button>
              <p className="text-xs text-slate-400">
                Debes estar registrado previamente por la secretaria del programa.
              </p>
            </div>
          )}
        </div>

        <p className="text-center text-xs text-slate-400 mt-6">
          &copy; {new Date().getFullYear()} NeoVision · Sistema de Gestion Clinica
        </p>
      </div>
    </div>
  );
}
