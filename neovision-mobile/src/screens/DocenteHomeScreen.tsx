import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import { Button } from '../components/Button';
import { CitaCard } from '../components/CitaCard';
import { DataList } from '../components/DataList';
import { RoleHeader } from '../components/RoleHeader';
import { Screen } from '../components/Screen';
import { SesionCard } from '../components/SesionCard';
import { Tabs } from '../components/Tabs';
import { useConfirmarAsistenciaDocente, useMisCitasDocente, useMisSesiones } from '../hooks/useDocente';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { toast } from '../store/toastStore';
import type { MainStackParamList } from '../navigation/types';
import type { CitaResponse } from '../types/cita';
import { errorMessage as errMsg } from '../utils/error';

type Tab = 'sesiones' | 'citas';

export function DocenteHomeScreen() {
  const [tab, setTab] = useState<Tab>('sesiones');
  const navigation = useNavigation<NativeStackNavigationProp<MainStackParamList>>();
  const online = useOnlineStatus();
  const sesiones = useMisSesiones();
  const citas    = useMisCitasDocente();
  const confirmar = useConfirmarAsistenciaDocente();

  const handleConfirmar = (cita: CitaResponse) => {
    Alert.alert(
      'Confirmar asistencia',
      `¿Marcar como completada la cita de ${cita.nombrePaciente ?? 'este paciente'}?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Confirmar',
          onPress: () => confirmar.mutate(cita.idCita, {
            onSuccess: () => toast.success('Asistencia confirmada'),
            onError: (e) => Alert.alert('Error', errMsg(e, 'No se pudo confirmar')!),
          }),
        },
      ],
    );
  };

  return (
    <Screen padded={false}>
      <View style={styles.padded}>
        <RoleHeader title="Docente" />
        <Tabs
          current={tab}
          onChange={setTab}
          items={[
            { key: 'sesiones', label: 'Mis sesiones', count: sesiones.data?.length ?? 0 },
            { key: 'citas',    label: 'Citas',        count: citas.data?.length ?? 0 },
          ]}
        />
      </View>

      {tab === 'sesiones'
        ? (
          <DataList
            data={sesiones.data ?? []}
            keyExtractor={(s) => String(s.idSesion)}
            renderItem={(s) => <SesionCard sesion={s} />}
            loading={sesiones.isLoading}
            refreshing={sesiones.isRefetching}
            onRefresh={sesiones.refetch}
            error={errMsg(sesiones.error)}
            emptyText="No tienes sesiones asignadas."
          />
        )
        : (
          <DataList
            data={citas.data ?? []}
            keyExtractor={(c) => String(c.idCita)}
            renderItem={(c) => (
              <CitaCard
                cita={c}
                onPress={() => navigation.navigate('CitaDetail', { cita: c })}
                actions={c.estado === 'RESERVADO' ? (
                  <Button
                    title={online ? 'Confirmar asistencia' : 'Sin conexion'}
                    disabled={!online}
                    loading={confirmar.isPending && confirmar.variables === c.idCita}
                    onPress={() => handleConfirmar(c)}
                  />
                ) : null}
              />
            )}
            loading={citas.isLoading}
            refreshing={citas.isRefetching}
            onRefresh={citas.refetch}
            error={errMsg(citas.error)}
            emptyText="No hay citas en tus sesiones."
          />
        )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  padded: { paddingHorizontal: 24, paddingTop: 24 },
});
