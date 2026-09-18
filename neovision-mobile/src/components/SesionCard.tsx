import { StyleSheet, Text, View } from 'react-native';
import { theme } from '../theme';
import type { SesionResponse } from '../types/sesion';
import { formatFecha, formatRangoHora } from '../utils/format';

interface Props { sesion: SesionResponse; }

export function SesionCard({ sesion }: Props) {
  const horario = sesion.horaInicio && sesion.horaFin
    ? formatRangoHora(sesion.horaInicio, sesion.horaFin)
    : 'Sin horario';

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <View style={styles.flex}>
          <Text style={styles.fecha}>{formatFecha(sesion.fecha)}</Text>
          <Text style={styles.hora}>{horario}</Text>
        </View>
        <View style={styles.cupos}>
          <Text style={styles.cuposNum}>{sesion.cuposDisponibles}/{sesion.cuposTotales}</Text>
          <Text style={styles.cuposLabel}>cupos</Text>
        </View>
      </View>

      <View style={styles.divider} />

      <Row label="Espacio" value={sesion.nombreEspacio ?? '-'} />
      {sesion.nombreSalon ? <Row label="Salon" value={sesion.nombreSalon} /> : null}

      <Text style={styles.subtitle}>Estudiantes ({sesion.estudiantes.length})</Text>
      {sesion.estudiantes.length === 0
        ? <Text style={styles.empty}>Ninguno asignado</Text>
        : sesion.estudiantes.map(e => (
            <Text key={e.idEstudiante} style={styles.estudiante}>• {e.nombre}</Text>
          ))}
    </View>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value} numberOfLines={1}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card:       { backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: theme.colors.border },
  header:     { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 12 },
  flex:       { flex: 1 },
  fecha:      { color: theme.colors.text, fontSize: 16, fontWeight: '700', textTransform: 'capitalize' },
  hora:       { color: theme.colors.textMuted, fontSize: 13, marginTop: 2 },
  cupos:      { alignItems: 'flex-end' },
  cuposNum:   { color: theme.colors.primary, fontSize: 16, fontWeight: '700' },
  cuposLabel: { color: theme.colors.textMuted, fontSize: 11 },
  divider:    { height: 1, backgroundColor: theme.colors.border, marginBottom: 12 },
  row:        { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  label:      { color: theme.colors.textMuted, fontSize: 13 },
  value:      { color: theme.colors.text, fontSize: 13, fontWeight: '600', flex: 1, textAlign: 'right', marginLeft: 12 },
  subtitle:   { color: theme.colors.textMuted, fontSize: 12, fontWeight: '700', marginTop: 8, marginBottom: 4, textTransform: 'uppercase' },
  estudiante: { color: theme.colors.text, fontSize: 13, marginBottom: 2 },
  empty:      { color: theme.colors.textMuted, fontSize: 13, fontStyle: 'italic' },
});
