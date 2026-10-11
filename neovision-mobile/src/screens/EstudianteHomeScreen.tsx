import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMemo, useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../components/Button';
import { CitaCard } from '../components/CitaCard';
import { DataList } from '../components/DataList';
import { MotivoModal } from '../components/MotivoModal';
import { OptionPickerModal } from '../components/OptionPickerModal';
import { PacientePickerModal } from '../components/PacientePickerModal';
import { ProgresoWidget } from '../components/ProgresoWidget';
import { RoleHeader } from '../components/RoleHeader';
import { Screen } from '../components/Screen';
import { Tabs } from '../components/Tabs';
import { toast } from '../store/toastStore';
import { theme } from '../theme';
import {
  useAsignarPaciente, useCancelarEstudiante, useMisBloques, usePacientes, useProgresoEstudiante,
  useReprogramarEstudiante,
} from '../hooks/useEstudiante';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import type { MainStackParamList } from '../navigation/types';
import type { CitaResponse, EstadoCita, PacienteResponse } from '../types/cita';
import { errorMessage } from '../utils/error';

type Tab = 'agendadas' | 'disponibles' | 'historial';

const HISTORIAL_ESTADOS: EstadoCita[] = ['COMPLETADO', 'CANCELADO', 'VENCIDA'];

export function EstudianteHomeScreen() {
  const [tab, setTab] = useState<Tab>('agendadas');
  const [pickerForCita, setPickerForCita] = useState<number | null>(null);
  const [citaACancelar, setCitaACancelar] = useState<CitaResponse | null>(null);
  const [citaAReprogramar, setCitaAReprogramar] = useState<CitaResponse | null>(null);
  const online = useOnlineStatus();
  const navigation = useNavigation<NativeStackNavigationProp<MainStackParamList>>();

  const bloquesQ  = useMisBloques();
  const pacientesQ = usePacientes();
  const progresoQ = useProgresoEstudiante();
  const asignar   = useAsignarPaciente();
  const cancelar  = useCancelarEstudiante();
  const reprogramar = useReprogramarEstudiante();

  const bloques = bloquesQ.data ?? [];

  // Destinos para reprogramar: bloques propios libres, de hoy en adelante.
  const destinos = useMemo(() => {
    const d = new Date();
    const hoy = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return bloques
      .filter(b => b.estado === 'DISPONIBLE' && b.fechaSesion >= hoy)
      .sort((a, b) => `${a.fechaSesion} ${a.horaInicio}`.localeCompare(`${b.fechaSesion} ${b.horaInicio}`));
  }, [bloques]);

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

  const handleCancelar = (motivo: string) => {
    const cita = citaACancelar;
    if (!cita) return;
    cancelar.mutate({ idCita: cita.idCita, motivo }, {
      onSuccess: () => { setCitaACancelar(null); toast.success('Cita cancelada'); },
      onError: (e) => Alert.alert('Error', errorMessage(e, 'No se pudo cancelar')!),
    });
  };

  const handleReprogramar = (idCitaNueva: number) => {
    const cita = citaAReprogramar;
    setCitaAReprogramar(null);
    if (!cita) return;
    reprogramar.mutate({ idCita: cita.idCita, idCitaNueva }, {
      onSuccess: () => toast.success('Cita reprogramada'),
      onError: (e) => Alert.alert('Error', errorMessage(e, 'No se pudo reprogramar')!),
    });
  };

  const renderActions = (cita: CitaResponse) => {
    if (cita.estado === 'RESERVADO') {
      return (
        <>
          <Button
            title={online ? 'Reprogramar' : 'Sin conexion'}
            variant="secondary"
            disabled={!online}
            loading={reprogramar.isPending && reprogramar.variables?.idCita === cita.idCita}
            onPress={() => setCitaAReprogramar(cita)}
          />
          <Button
            title={online ? 'Cancelar' : 'Sin conexion'}
            variant="danger"
            disabled={!online}
            onPress={() => setCitaACancelar(cita)}
          />
        </>
      );
    }
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

      <OptionPickerModal
        visible={citaAReprogramar !== null}
        title="Nuevo bloque"
        options={destinos.map(b => ({
          key: String(b.idCita),
          label: `${b.fechaSesion} · ${b.horaInicio.slice(0, 5)}–${b.horaFin.slice(0, 5)}`,
          sublabel: `${b.nombreSalon ?? 'Sin salon'} · ${b.nombreDocente}`,
        }))}
        onSelect={(key) => handleReprogramar(Number(key))}
        onClose={() => setCitaAReprogramar(null)}
      />

      <MotivoModal
        visible={citaACancelar !== null}
        title="Cancelar cita"
        confirmLabel="Si, cancelar"
        loading={cancelar.isPending}
        onConfirm={handleCancelar}
        onClose={() => setCitaACancelar(null)}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  padded: { paddingHorizontal: 24, paddingTop: 24 },
  statsLink: { alignSelf: 'flex-end', marginTop: -8, marginBottom: 16 },
  statsLinkText: { color: theme.colors.primary, fontSize: 13, fontWeight: '600' },
});
