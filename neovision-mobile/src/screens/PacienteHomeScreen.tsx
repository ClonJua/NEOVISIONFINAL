import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMemo, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../components/Button';
import { CitaCard } from '../components/CitaCard';
import { DataList } from '../components/DataList';
import { MotivoModal } from '../components/MotivoModal';
import { OptionPickerModal } from '../components/OptionPickerModal';
import { RoleHeader } from '../components/RoleHeader';
import { Screen } from '../components/Screen';
import { Tabs } from '../components/Tabs';
import { toast } from '../store/toastStore';
import { useEspecialidades } from '../hooks/useEspecialidades';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { useBloquesDisponibles, useCancelar, useMisCitas, useReprogramar, useReservar } from '../hooks/usePaciente';
import type { MainStackParamList } from '../navigation/types';
import type { CitaResponse, EstadoCita } from '../types/cita';
import { SUBTIPOS_CONTROL } from '../types/especialidad';
import { errorMessage } from '../utils/error';

type Tab = 'disponibles' | 'miscitas';

const HISTORIAL_ESTADOS: EstadoCita[] = ['COMPLETADO', 'CANCELADO', 'VENCIDA'];

export function PacienteHomeScreen() {
  const [tab, setTab] = useState<Tab>('disponibles');
  const [idEspFiltro, setIdEspFiltro] = useState<number | undefined>(undefined);
  const [subtipoParaCita, setSubtipoParaCita] = useState<CitaResponse | null>(null);
  const [citaACancelar, setCitaACancelar] = useState<CitaResponse | null>(null);
  const [citaAReprogramar, setCitaAReprogramar] = useState<CitaResponse | null>(null);
  const online = useOnlineStatus();
  const navigation = useNavigation<NativeStackNavigationProp<MainStackParamList>>();

  const especialidadesQ = useEspecialidades();
  const disponibles = useBloquesDisponibles(idEspFiltro);
  const misCitas    = useMisCitas();
  const reservar    = useReservar();
  const cancelar    = useCancelar();
  const reprogramar = useReprogramar();
  // Destinos para reprogramar: bloques libres de la misma especialidad que la cita.
  const destinosQ   = useBloquesDisponibles(citaAReprogramar?.idEspecialidad ?? undefined);

  const especialidades = especialidadesQ.data ?? [];
  const espSeleccionada = useMemo(
    () => especialidades.find(e => e.idEspecialidad === idEspFiltro) ?? null,
    [especialidades, idEspFiltro],
  );

  const citas = misCitas.data ?? [];
  const activas = useMemo(() => citas.filter(c => c.estado === 'RESERVADO'), [citas]);
  const historial = useMemo(
    () => citas.filter(c => HISTORIAL_ESTADOS.includes(c.estado)),
    [citas],
  );

  const ejecutarReserva = (cita: CitaResponse, subtipoControl?: string) => {
    reservar.mutate(
      {
        idCita: cita.idCita,
        body: { idEspecialidad: idEspFiltro, subtipoControl },
      },
      {
        onSuccess: () => { setTab('miscitas'); toast.success('Cita reservada'); },
        onError: (e) => Alert.alert('Error', errorMessage(e, 'No se pudo reservar')!),
      },
    );
  };

  const handleReservar = (cita: CitaResponse) => {
    Alert.alert(
      'Confirmar reserva',
      `¿Reservar el bloque del ${cita.fechaSesion} (${cita.horaInicio.slice(0, 5)})?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Reservar',
          onPress: () => {
            // Si la especialidad elegida requiere subtipo, pedirlo primero.
            if (espSeleccionada?.requiereSubtipo) {
              setSubtipoParaCita(cita);
            } else {
              ejecutarReserva(cita);
            }
          },
        },
      ],
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

  const renderDisponible = (cita: CitaResponse) => (
    <CitaCard
      cita={cita}
      onPress={() => navigation.navigate('CitaDetail', { cita })}
      actions={
        <Button
          title={online ? 'Reservar' : 'Sin conexion'}
          disabled={!online}
          loading={reservar.isPending && reservar.variables?.idCita === cita.idCita}
          onPress={() => handleReservar(cita)}
        />
      }
    />
  );

  const renderMiCita = (cita: CitaResponse) => (
    <CitaCard
      cita={cita}
      onPress={() => navigation.navigate('CitaDetail', { cita })}
      actions={
        cita.estado === 'RESERVADO'
          ? <>
              <Button
                title={!online ? 'Sin conexion' : cita.reprogramacionesRestantes === 0 ? 'Sin reprogramaciones' : 'Reprogramar'}
                variant="secondary"
                disabled={!online || cita.reprogramacionesRestantes === 0}
                loading={reprogramar.isPending && reprogramar.variables?.idCita === cita.idCita}
                onPress={() => setCitaAReprogramar(cita)}
              />
              <Button
                title={online ? 'Cancelar cita' : 'Sin conexion'}
                variant="danger"
                disabled={!online}
                onPress={() => setCitaACancelar(cita)}
              />
            </>
          : null
      }
    />
  );

  const isDisponibles = tab === 'disponibles';
  const data = isDisponibles ? (disponibles.data ?? []) : [...activas, ...historial];
  const active = isDisponibles ? disponibles : misCitas;

  return (
    <Screen padded={false}>
      <View style={styles.padded}>
        <RoleHeader title="Paciente" />
        <Tabs
          current={tab}
          onChange={setTab}
          items={[
            { key: 'disponibles', label: 'Disponibles', count: disponibles.data?.length ?? 0 },
            { key: 'miscitas',    label: 'Mis citas',   count: citas.length },
          ]}
        />

        {isDisponibles && especialidades.length > 0 ? (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.chipsRow}
            contentContainerStyle={styles.chipsContent}
          >
            <EspChip label="Todas" active={idEspFiltro === undefined} onPress={() => setIdEspFiltro(undefined)} />
            {especialidades.map(e => (
              <EspChip
                key={e.idEspecialidad}
                label={e.nombre}
                active={idEspFiltro === e.idEspecialidad}
                onPress={() => setIdEspFiltro(e.idEspecialidad)}
              />
            ))}
          </ScrollView>
        ) : null}
      </View>

      <DataList
        data={data}
        keyExtractor={(c) => String(c.idCita)}
        renderItem={(item) => (isDisponibles ? renderDisponible(item) : renderMiCita(item))}
        loading={active.isLoading}
        refreshing={active.isRefetching}
        onRefresh={active.refetch}
        error={errorMessage(active.error)}
        emptyText={isDisponibles ? 'No hay bloques disponibles.' : 'No tienes citas.'}
      />

      <OptionPickerModal
        visible={subtipoParaCita !== null}
        title="Tipo de control"
        options={SUBTIPOS_CONTROL.map(s => ({ key: s, label: s }))}
        onSelect={(key) => {
          const cita = subtipoParaCita;
          setSubtipoParaCita(null);
          if (cita) ejecutarReserva(cita, key);
        }}
        onClose={() => setSubtipoParaCita(null)}
      />

      <OptionPickerModal
        visible={citaAReprogramar !== null}
        title="Nuevo horario"
        options={(destinosQ.data ?? []).map(b => ({
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

function EspChip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <TouchableOpacity style={[styles.chip, active ? styles.chipActive : null]} onPress={onPress}>
      <Text style={[styles.chipText, active ? styles.chipTextActive : null]}>{label}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  padded:        { paddingHorizontal: 24, paddingTop: 24 },
  chipsRow:      { marginBottom: 12 },
  chipsContent:  { gap: 8, paddingRight: 24 },
  chip:          { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 999, backgroundColor: '#1e293b', borderWidth: 1, borderColor: '#334155' },
  chipActive:    { backgroundColor: '#F15A22', borderColor: '#F15A22' },
  chipText:      { color: '#94a3b8', fontSize: 13, fontWeight: '600' },
  chipTextActive:{ color: '#ffffff' },
});
