import { StyleSheet, Text, View } from 'react-native';
import { Button } from '../components/Button';
import { Screen } from '../components/Screen';
import { useAuthStore } from '../store/authStore';
import { theme } from '../theme';

export function UnsupportedRoleScreen() {
  const logout = useAuthStore((s) => s.logout);
  return (
    <Screen>
      <View style={styles.center}>
        <Text style={styles.title}>App no disponible para tu rol</Text>
        <Text style={styles.body}>
          La aplicacion movil esta destinada a pacientes, estudiantes y docentes. Por favor usa el portal web.
        </Text>
        <Button title="Cerrar sesión" variant="secondary" onPress={() => { logout(); }} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', gap: 16 },
  title:  { color: theme.colors.text, fontSize: 22, fontWeight: '700', textAlign: 'center' },
  body:   { color: theme.colors.textMuted, textAlign: 'center', marginBottom: 16 },
});
