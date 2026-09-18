import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { Screen } from '../components/Screen';
import type { AuthStackParamList } from '../navigation/types';
import { useAuthStore } from '../store/authStore';
import { theme } from '../theme';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;
type Modo = 'paciente' | 'institucional';

export function LoginScreen({ navigation }: Props) {
  const login              = useAuthStore((s) => s.login);
  const loginWithMicrosoft = useAuthStore((s) => s.loginWithMicrosoft);
  const error              = useAuthStore((s) => s.error);
  const clearError         = useAuthStore((s) => s.clearError);

  const [modo, setModo]             = useState<Modo>('paciente');
  const [correo, setCorreo]         = useState('');
  const [contrasena, setContrasena] = useState('');
  const [loading, setLoading]       = useState(false);

  const cambiarModo = (m: Modo) => { clearError(); setModo(m); };

  const handlePacienteLogin = async () => {
    if (!correo || !contrasena) return;
    setLoading(true);
    try {
      await login({ correo: correo.trim(), contrasena });
    } catch { /* error en store */ }
    finally { setLoading(false); }
  };

  const handleMicrosoftLogin = async () => {
    setLoading(true);
    try {
      await loginWithMicrosoft();
    } catch { /* error en store */ }
    finally { setLoading(false); }
  };

  return (
    <Screen>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <Text style={styles.brand}>NeoVision</Text>
          <Text style={styles.subtitle}>Clínica de Optometría - Universidad El Bosque</Text>

          <View style={styles.modoTabs}>
            <ModoTab label="Soy Paciente"  active={modo === 'paciente'}      onPress={() => cambiarModo('paciente')} />
            <ModoTab label="Institucional" active={modo === 'institucional'} onPress={() => cambiarModo('institucional')} />
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          {modo === 'paciente' ? (
            <View>
              <Input
                label="Correo electrónico"
                value={correo}
                onChangeText={setCorreo}
                placeholder="tu@correo.com"
                keyboardType="email-address"
                autoCapitalize="none"
                autoComplete="email"
              />
              <Input
                label="Contraseña"
                value={contrasena}
                onChangeText={setContrasena}
                placeholder="********"
                secureTextEntry
                autoComplete="current-password"
              />
              <Button title="Iniciar sesion" onPress={handlePacienteLogin} loading={loading} />
              <TouchableOpacity style={styles.linkWrap} onPress={() => navigation.navigate('Register')}>
                <Text style={styles.link}>No tienes cuenta? <Text style={styles.linkStrong}>Registrate aqui</Text></Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View>
              <Text style={styles.institucionalText}>
                Docentes, estudiantes y personal administrativo acceden con su cuenta de Microsoft 365.
              </Text>
              <Button title="Continuar con Microsoft" onPress={handleMicrosoftLogin} loading={loading} />
              <Text style={styles.hint}>Debes estar registrado previamente por la secretaria del programa.</Text>
            </View>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}

function ModoTab({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <TouchableOpacity style={[styles.modoTab, active ? styles.modoTabActive : null]} onPress={onPress}>
      <Text style={[styles.modoTabText, active ? styles.modoTabTextActive : null]}>{label}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  flex:             { flex: 1 },
  scroll:           { flexGrow: 1, justifyContent: 'center' },
  brand:            { color: theme.colors.text, fontSize: 36, fontWeight: '800', textAlign: 'center' },
  subtitle:         { color: theme.colors.textMuted, fontSize: 14, textAlign: 'center', marginTop: 8, marginBottom: 32 },
  modoTabs:         { flexDirection: 'row', backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 4, marginBottom: 20 },
  modoTab:          { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: theme.radius.sm },
  modoTabActive:    { backgroundColor: theme.colors.primary },
  modoTabText:      { color: theme.colors.textMuted, fontSize: 14, fontWeight: '600' },
  modoTabTextActive:{ color: theme.colors.text },
  error:            { color: theme.colors.danger, textAlign: 'center', marginBottom: 12 },
  linkWrap:         { marginTop: 16, alignItems: 'center' },
  link:             { color: theme.colors.textMuted, fontSize: 14 },
  linkStrong:       { color: theme.colors.primary, fontWeight: '700' },
  institucionalText:{ color: theme.colors.textMuted, fontSize: 14, textAlign: 'center', marginBottom: 20, lineHeight: 20 },
  hint:             { color: theme.colors.textMuted, fontSize: 12, textAlign: 'center', marginTop: 12 },
});
