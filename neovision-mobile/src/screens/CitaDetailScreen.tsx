import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { EstadoBadge } from '../components/EstadoBadge';
import { Screen } from '../components/Screen';
import type { MainStackParamList } from '../navigation/types';
import { theme } from '../theme';
import { formatFecha, formatRangoHora } from '../utils/format';

type Props = NativeStackScreenProps<MainStackParamList, 'CitaDetail'>;

export function CitaDetailScreen({ route, navigation }: Props) {
  const { cita } = route.params;

  const rows: { label: string; value: string }[] = [
    { label: 'Fecha',        value: formatFecha(cita.fechaSesion) },
    { label: 'Horario',      value: formatRangoHora(cita.horaInicio, cita.horaFin) },
    { label: 'Paciente',     value: cita.nombrePaciente ?? 'Sin asignar' },
    { label: cita.nombreEstudiante2 ? 'Estudiantes' : 'Estudiante',
      value: cita.nombreEstudiante2 ? `${cita.nombreEstudiante} y ${cita.nombreEstudiante2}` : cita.nombreEstudiante },
    { label: 'Docente',      value: cita.nombreDocente },
    { label: 'Especialidad', value: cita.nombreEspecialidad ?? '-' },
    { label: 'Subtipo',      value: cita.subtipoControl ?? '-' },
    { label: 'Espacio',      value: cita.nombreEspacio ?? '-' },
    { label: 'Salon',        value: cita.nombreSalon ?? '-' },
    { label: 'Reservada',    value: cita.fechaReserva ?? '-' },
  ];

  return (
    <Screen>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹ Volver</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Detalle de cita</Text>
        <View style={styles.backSpacer} />
      </View>

      <ScrollView>
        <View style={styles.estadoRow}>
          <EstadoBadge estado={cita.estado} />
        </View>

        <View style={styles.card}>
          {rows.map(r => (
            <View key={r.label} style={styles.row}>
              <Text style={styles.label}>{r.label}</Text>
              <Text style={styles.value}>{r.value}</Text>
            </View>
          ))}
        </View>

        {cita.observaciones ? (
          <View style={styles.card}>
            <Text style={styles.label}>Observaciones</Text>
            <Text style={styles.obs}>{cita.observaciones}</Text>
          </View>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header:     { flexDirection: 'row', alignItems: 'center', marginBottom: 16 },
  back:       { color: theme.colors.primary, fontSize: 16, fontWeight: '600', width: 80 },
  backSpacer: { width: 80 },
  title:      { flex: 1, color: theme.colors.text, fontSize: 20, fontWeight: '700', textAlign: 'center' },
  estadoRow:  { marginBottom: 12 },
  card:       { backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: theme.colors.border },
  row:        { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  label:      { color: theme.colors.textMuted, fontSize: 13 },
  value:      { color: theme.colors.text, fontSize: 13, fontWeight: '600', flex: 1, textAlign: 'right', marginLeft: 12 },
  obs:        { color: theme.colors.text, fontSize: 14, marginTop: 6, lineHeight: 20 },
});
