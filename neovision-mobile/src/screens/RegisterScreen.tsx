import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { RegistroRequest } from '../api/authService';
import { Button } from '../components/Button';
import { DateField } from '../components/DateField';
import { Input } from '../components/Input';
import { Screen } from '../components/Screen';
import type { AuthStackParamList } from '../navigation/types';
import { useAuthStore } from '../store/authStore';
import { theme } from '../theme';

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;
type Errores = Partial<Record<keyof RegistroRequest, string>>;

const TIPOS_DOCUMENTO = ['CC', 'TI', 'RC', 'CE', 'PA'];

const EMPTY: RegistroRequest = {
  nombre: '', apellido: '', correo: '', contrasena: '',
  tipoDocumento: 'CC', numeroDocumento: '', fechaNacimiento: '', telefono: '',
  autorizaTratamientoDatos: false,
  representanteNombre: '', representanteDocumento: '', representanteParentesco: '',
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function esMenorDeEdad(tipoDocumento: string): boolean {
  return tipoDocumento === 'TI' || tipoDocumento === 'RC';
}

function validar(form: RegistroRequest): Errores {
  const e: Errores = {};
  if (!form.nombre.trim())   e.nombre = 'Requerido';
  if (!form.apellido.trim()) e.apellido = 'Requerido';
  if (!form.correo.trim())   e.correo = 'Requerido';
  else if (!EMAIL_RE.test(form.correo.trim())) e.correo = 'Correo inválido';
  if (!form.contrasena)            e.contrasena = 'Requerido';
  else if (form.contrasena.length < 12) e.contrasena = 'Mínimo 12 caracteres';
  if (!form.numeroDocumento)       e.numeroDocumento = 'Requerido';
  else if (form.numeroDocumento.length < 5) e.numeroDocumento = 'Documento muy corto';
  if (!form.fechaNacimiento)       e.fechaNacimiento = 'Requerido';
  if (!form.telefono)              e.telefono = 'Requerido';
  else if (!/^3\d{9}$/.test(form.telefono)) e.telefono = 'Celular: 10 dígitos, inicia con 3';
  if (esMenorDeEdad(form.tipoDocumento)) {
    if (!form.representanteNombre?.trim()) e.representanteNombre = 'Requerido';
    if (!form.representanteDocumento?.trim()) e.representanteDocumento = 'Requerido';
    if (!form.representanteParentesco?.trim()) e.representanteParentesco = 'Requerido';
  }
  if (!form.autorizaTratamientoDatos) e.autorizaTratamientoDatos = 'Debes autorizar el tratamiento de datos';
  return e;
}

export function RegisterScreen({ navigation }: Props) {
  const register   = useAuthStore((s) => s.register);
  const storeError = useAuthStore((s) => s.error);
  const clearError = useAuthStore((s) => s.clearError);

  const [form, setForm]     = useState<RegistroRequest>(EMPTY);
  const [errores, setErr]   = useState<Errores>({});
  const [loading, setLoading] = useState(false);

  const set = (key: keyof RegistroRequest, value: string) => {
    setForm(prev => ({ ...prev, [key]: value }));
    if (errores[key]) setErr(prev => ({ ...prev, [key]: undefined }));
  };

  const toggleAutorizacion = () => {
    setForm(prev => ({ ...prev, autorizaTratamientoDatos: !prev.autorizaTratamientoDatos }));
    if (errores.autorizaTratamientoDatos) setErr(prev => ({ ...prev, autorizaTratamientoDatos: undefined }));
  };

  const handleSubmit = async () => {
    clearError();
    const e = validar(form);
    setErr(e);
    if (Object.keys(e).length > 0) return;
    setLoading(true);
    try {
      await register({ ...form, correo: form.correo.trim() });
    } catch { /* error en store */ }
    finally { setLoading(false); }
  };

  return (
    <Screen>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <Text style={styles.title}>Registro de Paciente</Text>

          {storeError ? <Text style={styles.error}>{storeError}</Text> : null}

          <Input label="Nombre" value={form.nombre} onChangeText={(t) => set('nombre', t)}
            placeholder="Juan" error={errores.nombre} />
          <Input label="Apellido" value={form.apellido} onChangeText={(t) => set('apellido', t)}
            placeholder="Perez" error={errores.apellido} />
          <Input label="Correo electrónico" value={form.correo} onChangeText={(t) => set('correo', t)}
            placeholder="correo@ejemplo.com" keyboardType="email-address" autoCapitalize="none"
            error={errores.correo} />
          <Input label="Contraseña (mínimo 12)" value={form.contrasena} onChangeText={(t) => set('contrasena', t)}
            placeholder="********" secureTextEntry error={errores.contrasena} />

          <Text style={styles.label}>Tipo de documento</Text>
          <View style={styles.chips}>
            {TIPOS_DOCUMENTO.map(td => {
              const active = form.tipoDocumento === td;
              return (
                <TouchableOpacity
                  key={td}
                  style={[styles.chip, active ? styles.chipActive : null]}
                  onPress={() => set('tipoDocumento', td)}
                >
                  <Text style={[styles.chipText, active ? styles.chipTextActive : null]}>{td}</Text>
                </TouchableOpacity>
              );
            })}
          </View>

          <Input label="Numero de documento" value={form.numeroDocumento}
            onChangeText={(t) => /^\d{0,15}$/.test(t) && set('numeroDocumento', t)}
            placeholder="1234567890" keyboardType="number-pad" error={errores.numeroDocumento} />

          <DateField label="Fecha de nacimiento" value={form.fechaNacimiento}
            onChange={(d) => set('fechaNacimiento', d)} error={errores.fechaNacimiento} />

          <Input label="Celular" value={form.telefono}
            onChangeText={(t) => /^\d{0,10}$/.test(t) && set('telefono', t)}
            placeholder="3001234567" keyboardType="number-pad" error={errores.telefono} />

          {esMenorDeEdad(form.tipoDocumento) && (
            <View style={styles.menorBox}>
              <Text style={styles.menorText}>
                El paciente es menor de edad. Por la Ley 1581 de 2012, la autorización de tratamiento de datos
                debe darla su representante legal (padre, madre o acudiente), no el menor.
              </Text>
              <Input label="Nombre del representante legal" value={form.representanteNombre}
                onChangeText={(t) => set('representanteNombre', t)}
                placeholder="Nombre completo" error={errores.representanteNombre} />
              <Input label="Documento del representante" value={form.representanteDocumento}
                onChangeText={(t) => /^\d{0,15}$/.test(t) && set('representanteDocumento', t)}
                placeholder="1234567890" keyboardType="number-pad" error={errores.representanteDocumento} />
              <Input label="Parentesco" value={form.representanteParentesco}
                onChangeText={(t) => set('representanteParentesco', t)}
                placeholder="Madre, padre, acudiente..." error={errores.representanteParentesco} />
            </View>
          )}

          <TouchableOpacity style={styles.consentRow} onPress={toggleAutorizacion}>
            <View style={[styles.checkbox, form.autorizaTratamientoDatos ? styles.checkboxChecked : null]}>
              {form.autorizaTratamientoDatos ? <Text style={styles.checkboxMark}>✓</Text> : null}
            </View>
            <Text style={styles.consentText}>
              Autorizo el tratamiento de mis datos personales y clínicos conforme a la Ley 1581 de 2012 y la
              Resolución 1995 de 1999, con fines de atención optométrica y académica en el Programa de Optometría
              de la Universidad El Bosque.
            </Text>
          </TouchableOpacity>
          {errores.autorizaTratamientoDatos ? <Text style={styles.error}>{errores.autorizaTratamientoDatos}</Text> : null}

          <Button title="Registrarme" onPress={handleSubmit} loading={loading} />
          <TouchableOpacity style={styles.linkWrap} onPress={() => navigation.goBack()}>
            <Text style={styles.link}>Ya tienes cuenta? <Text style={styles.linkStrong}>Inicia sesion</Text></Text>
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex:           { flex: 1 },
  scroll:         { flexGrow: 1, paddingVertical: 16 },
  title:          { color: theme.colors.text, fontSize: 26, fontWeight: '800', textAlign: 'center', marginBottom: 20 },
  error:          { color: theme.colors.danger, textAlign: 'center', marginBottom: 12 },
  label:          { color: theme.colors.textMuted, fontSize: 13, fontWeight: '600', marginBottom: 6 },
  chips:          { flexDirection: 'row', gap: 8, marginBottom: 14 },
  chip:           { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: theme.radius.sm, backgroundColor: theme.colors.surface, borderWidth: 1, borderColor: theme.colors.border },
  chipActive:     { backgroundColor: theme.colors.primary, borderColor: theme.colors.primary },
  chipText:       { color: theme.colors.textMuted, fontSize: 13, fontWeight: '700' },
  chipTextActive: { color: theme.colors.text },
  linkWrap:       { marginTop: 16, alignItems: 'center' },
  link:           { color: theme.colors.textMuted, fontSize: 14 },
  linkStrong:     { color: theme.colors.primary, fontWeight: '700' },
  menorBox:       { backgroundColor: theme.colors.surface, borderRadius: theme.radius.sm, borderWidth: 1, borderColor: theme.colors.border, padding: 12, marginBottom: 14, gap: 4 },
  menorText:      { color: theme.colors.textMuted, fontSize: 12, lineHeight: 17, marginBottom: 6 },
  consentRow:     { flexDirection: 'row', alignItems: 'flex-start', gap: 10, marginBottom: 4 },
  checkbox:       { width: 20, height: 20, borderRadius: 4, borderWidth: 1, borderColor: theme.colors.border, alignItems: 'center', justifyContent: 'center', marginTop: 2 },
  checkboxChecked:{ backgroundColor: theme.colors.primary, borderColor: theme.colors.primary },
  checkboxMark:   { color: theme.colors.text, fontSize: 13, fontWeight: '700' },
  consentText:    { flex: 1, color: theme.colors.textMuted, fontSize: 12, lineHeight: 17 },
});
