import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMemo, useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../components/Button';
import { CitaCard } from '../components/CitaCard';
import { DataList } from '../components/DataList';
import { PacientePickerModal } from '../components/PacientePickerModal';
import { ProgresoWidget } from '../components/ProgresoWidget';
import { RoleHeader } from '../components/RoleHeader';
import { Screen } from '../components/Screen';
import { Tabs } from '../components/Tabs';
import { toast } from '../store/toastStore';
import { theme } from '../theme';
import { useAsignarPaciente, useMisBloques, usePacientes, useProgresoEstudiante } from '../hooks/useEstudiante';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import type { MainStackParamList } from '../navigation/types';
import type { CitaResponse, EstadoCita, PacienteResponse } from '../types/cita';
import { errorMessage } from '../utils/error';

type Tab = 'agendadas' | 'disponibles' | 'historial';

const HISTORIAL_ESTADOS: EstadoCita[] = ['COMPLETADO', 'CANCELADO', 'VENCIDA'];

export function EstudianteHomeScreen() {
  const [tab, setTab] = useState<Tab>('agendadas');
  const [pickerForCita, setPickerForCita] = useState<number | null>(null);
  const online = useOnlineStatus();
  const navigation = useNavigation<NativeStackNavigationProp<MainStackParamList>>();

  const bloquesQ  = useMisBloques();
  const pacientesQ = usePacientes();
  const progresoQ = useProgresoEstudiante();
  const asignar   = useAsignarPaciente();

  const bloques = bloquesQ.data ?? [];

  const visibles = useMemo(() => {
    if (tab === 'agendadas')   return bloques.filter(b => b.estado === 'RESERVADO');
    if (tab === 'disponibles') return bloques.filter(b => b.estado === 'DISPONIBLE');
    return bloques.filter(b => HISTORIAL_ESTADOS.includes(b.estado));
  }, [bloques, tab]);

  const handleAsignarPaciente = (paciente: PacienteResponse) => {
    if (pickerForCita == null) return;
    const idCita = pickerForCita;
    setPickerForCita(null);
    asignar.mutate(
      { idCita, idPaciente: paciente.idPaciente },
      {
        onSuccess: () => toast.success('Paciente asignado'),
        onError: (e) => Alert.alert('Error', errorMessage(e, 'No se pudo asignar')!),
      },
    );
  };

  const renderActions = (cita: CitaResponse) => {
    if (cita.estado === 'DISPONIBLE') {
      return (
        <Button
          title={online ? 'Asignar paciente' : 'Sin conexion'}
          disabled={!online}
          loading={asignar.isPending && asignar.variables?.idCita === cita.idCita}
          onPress={() => setPickerForCita(cita.idCita)}
        />
      );
    }
    return null;
  };

  return (
    <Screen padded={false}>
      <View style={styles.padded}>
        <RoleHeader title="Mis bloques" />
        {progresoQ.data ? <ProgresoWidget progreso={progresoQ.data} /> : null}
        <TouchableOpacity style={styles.statsLink} onPress={() => navigation.navigate('Estadisticas')}>
          <Text style={styles.statsLinkText}>Ver mis estadísticas ›</Text>
        </TouchableOpacity>
        <Tabs
          current={tab}
          onChange={setTab}
          items={[
            { key: 'agendadas',   label: 'Agendadas',   count: bloques.filter(b => b.estado === 'RESERVADO').length },
            { key: 'disponibles', label: 'Disponibles', count: bloques.filter(b => b.estado === 'DISPONIBLE').length },
            { key: 'historial',   label: 'Historial',   count: bloques.filter(b => HISTORIAL_ESTADOS.includes(b.estado)).length },
          ]}
        />
      </View>

      <DataList
        data={visibles}
        keyExtractor={(c) => String(c.idCita)}
        renderItem={(item) => (
          <CitaCard
            cita={item}
            actions={renderActions(item)}
            onPress={() => navigation.navigate('CitaDetail', { cita: item })}
          />
        )}
        loading={bloquesQ.isLoading}
        refreshing={bloquesQ.isRefetching}
        onRefresh={bloquesQ.refetch}
        error={errorMessage(bloquesQ.error)}
        emptyText="Sin bloques en esta categoria."
      />

      <PacientePickerModal
        visible={pickerForCita !== null}
        pacientes={pacientesQ.data ?? []}
        onSelect={handleAsignarPaciente}
        onClose={() => setPickerForCita(null)}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  padded: { paddingHorizontal: 24, paddingTop: 24 },
  statsLink: { alignSelf: 'flex-end', marginTop: -8, marginBottom: 16 },
  statsLinkText: { color: theme.colors.primary, fontSize: 13, fontWeight: '600' },
});
