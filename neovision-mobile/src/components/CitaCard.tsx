import { ReactNode } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { theme } from '../theme';
import { textoCancelacion, type CitaResponse } from '../types/cita';
import { formatFecha, formatRangoHora } from '../utils/format';
import { EstadoBadge } from './EstadoBadge';

interface Props {
  cita: CitaResponse;
  actions?: ReactNode;
  onPress?: () => void;
}

export function CitaCard({ cita, actions, onPress }: Props) {
  const Info = (
    <>
      <View style={styles.header}>
        <View style={styles.flex}>
          <Text style={styles.fecha}>{formatFecha(cita.fechaSesion)}</Text>
          <Text style={styles.hora}>{formatRangoHora(cita.horaInicio, cita.horaFin)}</Text>
        </View>
        <EstadoBadge estado={cita.estado} />
      </View>

      <View style={styles.divider} />

      <Row label="Paciente" value={cita.nombrePaciente ?? 'Sin asignar'} />
      <Row
        label={cita.nombreEstudiante2 ? 'Estudiantes' : 'Estudiante'}
        value={cita.nombreEstudiante2 ? `${cita.nombreEstudiante} y ${cita.nombreEstudiante2}` : cita.nombreEstudiante}
      />
      <Row label="Especialidad" value={cita.nombreEspecialidad ?? '-'} />
      <Row label="Docente" value={cita.nombreDocente} />
      <Row label="Espacio" value={cita.nombreEspacio ?? '-'} />
      {cita.nombreSalon ? <Row label="Salon" value={cita.nombreSalon} /> : null}
      {textoCancelacion(cita) ? <Text style={styles.cancelacion}>{textoCancelacion(cita)}</Text> : null}
    </>
  );

  return (
    <View style={styles.card}>
      {onPress
        ? <TouchableOpacity onPress={onPress} activeOpacity={0.7}>{Info}</TouchableOpacity>
        : Info}
      {actions ? <View style={styles.actions}>{actions}</View> : null}
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
  card:    { backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: theme.colors.border },
  header:  { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 12 },
  flex:    { flex: 1 },
  fecha:   { color: theme.colors.text, fontSize: 16, fontWeight: '700', textTransform: 'capitalize' },
  hora:    { color: theme.colors.textMuted, fontSize: 13, marginTop: 2 },
  divider: { height: 1, backgroundColor: theme.colors.border, marginBottom: 12 },
  row:     { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  label:   { color: theme.colors.textMuted, fontSize: 13 },
  value:   { color: theme.colors.text, fontSize: 13, fontWeight: '600', flex: 1, textAlign: 'right', marginLeft: 12 },
  actions: { flexDirection: 'row', gap: 8, marginTop: 12 },
  cancelacion: { color: theme.colors.textMuted, fontSize: 12, fontStyle: 'italic', marginTop: 6 },
});
