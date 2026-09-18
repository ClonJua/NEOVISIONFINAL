import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useMemo } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { ProgresoWidget } from '../components/ProgresoWidget';
import { Screen } from '../components/Screen';
import { useMisBloques, useProgresoEstudiante } from '../hooks/useEstudiante';
import type { MainStackParamList } from '../navigation/types';
import { theme } from '../theme';
import { errorMessage } from '../utils/error';

type Props = NativeStackScreenProps<MainStackParamList, 'Estadisticas'>;

export function EstadisticasScreen({ navigation }: Props) {
  const bloquesQ = useMisBloques();
  const progresoQ = useProgresoEstudiante();
  const bloques = bloquesQ.data ?? [];

  const resumen = useMemo(() => {
    const completadas = bloques.filter(b => b.estado === 'COMPLETADO').length;
    const canceladas = bloques.filter(b => b.estado === 'CANCELADO').length;
    const vencidas = bloques.filter(b => b.estado === 'VENCIDA').length;
    const cerradas = completadas + canceladas + vencidas;
    const tasaFinalizacion = cerradas > 0 ? Math.round((completadas / cerradas) * 100) : 0;
    return { completadas, canceladas, vencidas, cerradas, tasaFinalizacion };
  }, [bloques]);

  const porEspecialidad = useMemo(() => {
    const conteo = new Map<string, number>();
    for (const b of bloques) {
      if (b.estado !== 'COMPLETADO') continue;
      const nombre = b.nombreEspecialidad ?? 'Sin especialidad';
      conteo.set(nombre, (conteo.get(nombre) ?? 0) + 1);
    }
    const max = Math.max(1, ...conteo.values());
    return [...conteo.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([nombre, cantidad]) => ({ nombre, cantidad, pct: Math.round((cantidad / max) * 100) }));
  }, [bloques]);

  return (
    <Screen>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹ Volver</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Mis estadísticas</Text>
        <View style={styles.backSpacer} />
      </View>

      {bloquesQ.isLoading ? (
        <Text style={styles.muted}>Cargando...</Text>
      ) : bloquesQ.error ? (
        <Text style={styles.muted}>{errorMessage(bloquesQ.error)}</Text>
      ) : (
        <ScrollView showsVerticalScrollIndicator={false}>
          {progresoQ.data ? <ProgresoWidget progreso={progresoQ.data} /> : null}

          <View style={styles.grid}>
            <StatTile label="Completadas" value={resumen.completadas} tone={theme.colors.success} />
            <StatTile label="Canceladas" value={resumen.canceladas} tone={theme.colors.danger} />
            <StatTile label="Vencidas" value={resumen.vencidas} tone={theme.colors.textMuted} />
            <StatTile label="Tasa de finalización" value={`${resumen.tasaFinalizacion}%`} tone={theme.colors.primary} />
          </View>

          <Text style={styles.sectionTitle}>Atenciones completadas por especialidad</Text>
          {porEspecialidad.length === 0 ? (
            <Text style={styles.muted}>Aún no tienes atenciones completadas.</Text>
          ) : (
            <View style={styles.card}>
              {porEspecialidad.map(item => (
                <View key={item.nombre} style={styles.barRow}>
                  <View style={styles.barLabelRow}>
                    <Text style={styles.barLabel} numberOfLines={1}>{item.nombre}</Text>
                    <Text style={styles.barCount}>{item.cantidad}</Text>
                  </View>
                  <View style={styles.barTrack}>
                    <View style={[styles.barFill, { width: `${item.pct}%` }]} />
                  </View>
                </View>
              ))}
            </View>
          )}
        </ScrollView>
      )}
    </Screen>
  );
}

function StatTile({ label, value, tone }: { label: string; value: number | string; tone: string }) {
  return (
    <View style={styles.tile}>
      <Text style={[styles.tileValue, { color: tone }]}>{value}</Text>
      <Text style={styles.tileLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  header:     { flexDirection: 'row', alignItems: 'center', marginBottom: 20 },
  back:       { color: theme.colors.primary, fontSize: 16, fontWeight: '600', width: 80 },
  backSpacer: { width: 80 },
  title:      { flex: 1, color: theme.colors.text, fontSize: 20, fontWeight: '700', textAlign: 'center' },
  muted:      { color: theme.colors.textMuted, textAlign: 'center', marginTop: 32 },

  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: theme.spacing.sm, marginBottom: theme.spacing.lg },
  tile: {
    flexBasis: '47%', flexGrow: 1, backgroundColor: theme.colors.surface, borderRadius: theme.radius.md,
    borderWidth: 1, borderColor: theme.colors.border, padding: 14, alignItems: 'flex-start',
  },
  tileValue: { fontSize: 24, fontWeight: '800', marginBottom: 2 },
  tileLabel: { color: theme.colors.textMuted, fontSize: 12, fontWeight: '600' },

  sectionTitle: { color: theme.colors.text, fontSize: 15, fontWeight: '700', marginBottom: 10 },
  card: {
    backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, borderWidth: 1,
    borderColor: theme.colors.border, padding: 14, marginBottom: 24,
  },
  barRow: { marginBottom: 12 },
  barLabelRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  barLabel: { color: theme.colors.text, fontSize: 13, fontWeight: '600', flex: 1, marginRight: 8 },
  barCount: { color: theme.colors.textMuted, fontSize: 13, fontWeight: '700' },
  barTrack: { height: 8, backgroundColor: theme.colors.background, borderRadius: 999, overflow: 'hidden' },
  barFill:  { height: 8, backgroundColor: theme.colors.primary, borderRadius: 999 },
});
