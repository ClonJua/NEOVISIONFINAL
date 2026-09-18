import { StyleSheet, Text, View } from 'react-native';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { theme } from '../theme';

/**
 * Banner que aparece solo cuando no hay conexion. Avisa al usuario
 * que los datos pueden estar desactualizados y las acciones deshabilitadas.
 */
export function OfflineBanner() {
  const online = useOnlineStatus();
  if (online) return null;

  return (
    <View style={styles.banner}>
      <Text style={styles.text}>Sin conexion · mostrando datos guardados</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { backgroundColor: theme.colors.danger, paddingVertical: 6, paddingHorizontal: 16, alignItems: 'center' },
  text:   { color: theme.colors.text, fontSize: 12, fontWeight: '600' },
});
