import { StyleSheet, Text, View } from 'react-native';
import type { EstadoCita } from '../types/cita';

interface Props { estado: EstadoCita; }

const STYLES: Record<EstadoCita, { bg: string; fg: string; label: string }> = {
  DISPONIBLE: { bg: '#064e3b', fg: '#6ee7b7', label: 'Disponible' },
  RESERVADO:  { bg: '#1e3a8a', fg: '#93c5fd', label: 'Reservado' },
  COMPLETADO: { bg: '#334155', fg: '#cbd5e1', label: 'Completado' },
  CANCELADO:  { bg: '#7f1d1d', fg: '#fca5a5', label: 'Cancelado' },
  VENCIDA:    { bg: '#78350f', fg: '#fcd34d', label: 'Vencida' },
};

export function EstadoBadge({ estado }: Props) {
  const s = STYLES[estado];
  return (
    <View style={[styles.badge, { backgroundColor: s.bg }]}>
      <Text style={[styles.label, { color: s.fg }]}>{s.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, alignSelf: 'flex-start' },
  label: { fontSize: 11, fontWeight: '700' },
});
