import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { MainStackParamList } from '../navigation/types';
import { useAuthStore } from '../store/authStore';
import { theme } from '../theme';

interface Props { title: string; }

export function RoleHeader({ title }: Props) {
  const correo = useAuthStore((s) => s.auth?.correo);
  const logout = useAuthStore((s) => s.logout);
  const navigation = useNavigation<NativeStackNavigationProp<MainStackParamList>>();

  const confirmLogout = () => {
    Alert.alert('Cerrar sesión', '¿Seguro que quieres salir?', [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Salir', style: 'destructive', onPress: () => { logout(); } },
    ]);
  };

  return (
    <View style={styles.header}>
      <View style={styles.flex}>
        <Text style={styles.title}>{title}</Text>
        {correo ? <Text style={styles.email} numberOfLines={1}>{correo}</Text> : null}
      </View>
      <TouchableOpacity onPress={() => navigation.navigate('Perfil')} style={styles.btn}>
        <Text style={styles.btnText}>Perfil</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={confirmLogout} style={[styles.btn, styles.btnDanger]}>
        <Text style={styles.btnText}>Salir</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  header:    { flexDirection: 'row', alignItems: 'center', marginBottom: 24 },
  flex:      { flex: 1 },
  title:     { color: theme.colors.text, fontSize: 22, fontWeight: '700' },
  email:     { color: theme.colors.textMuted, fontSize: 12, marginTop: 2 },
  btn:       { paddingHorizontal: 12, paddingVertical: 8, borderRadius: theme.radius.sm, borderWidth: 1, borderColor: theme.colors.border, marginLeft: 8 },
  btnDanger: { borderColor: theme.colors.danger },
  btnText:   { color: theme.colors.text, fontSize: 13, fontWeight: '600' },
});
