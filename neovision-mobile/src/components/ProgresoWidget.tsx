import { StyleSheet, Text, View } from 'react-native';
import { theme } from '../theme';
import type { ProgresoEstudiante } from '../types/perfil';

interface Props { progreso: ProgresoEstudiante; }

export function ProgresoWidget({ progreso }: Props) {
  const { atendidos, meta, semestre } = progreso;
  const pct = meta > 0 ? Math.min(100, Math.round((atendidos / meta) * 100)) : 0;

  return (
    <View style={styles.card}>
      <View style={styles.row}>
        <Text style={styles.label}>Progreso semestre {semestre}</Text>
        <Text style={styles.count}>{atendidos}/{meta}</Text>
      </View>
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${pct}%` }]} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card:  { backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 14, marginBottom: 16, borderWidth: 1, borderColor: theme.colors.border },
  row:   { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  label: { color: theme.colors.textMuted, fontSize: 13, fontWeight: '600' },
  count: { color: theme.colors.text, fontSize: 13, fontWeight: '700' },
  track: { height: 8, backgroundColor: theme.colors.background, borderRadius: 999, overflow: 'hidden' },
  fill:  { height: 8, backgroundColor: theme.colors.primary, borderRadius: 999 },
});
