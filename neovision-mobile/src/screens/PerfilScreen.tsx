import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { Screen } from '../components/Screen';
import { useEditarPerfil, usePerfil } from '../hooks/usePerfil';
import type { MainStackParamList } from '../navigation/types';
import { useAuthStore } from '../store/authStore';
import { toast } from '../store/toastStore';
import { theme } from '../theme';
import type { Perfil, PerfilDocente, PerfilEstudiante, PerfilPaciente } from '../types/perfil';
import { errorMessage } from '../utils/error';

type Props = NativeStackScreenProps<MainStackParamList, 'Perfil'>;
type RolNoAdmin = 'ROLE_PACIENTE' | 'ROLE_ESTUDIANTE' | 'ROLE_DOCENTE';

function camposExtra(p: Perfil): { label: string; value: string }[] {
  if ('numeroDocumento' in p) {
    const pac = p as PerfilPaciente;
    return [
      { label: 'Documento', value: `${pac.tipoDocumento} ${pac.numeroDocumento}` },
      { label: 'Nacimiento', value: pac.fechaNacimiento },
    ];
  }
  if ('semestre' in p) {
    const est = p as PerfilEstudiante;
    return [
      { label: 'Cedula', value: est.cedula },
      { label: 'Semestre', value: String(est.semestre) },
    ];
  }
  const doc = p as PerfilDocente;
  return [
    { label: 'Cedula', value: doc.cedula },
    { label: 'Especialidades', value: doc.especialidades.map(e => e.nombre).join(', ') || '-' },
  ];
}

export function PerfilScreen({ navigation }: Props) {
  const rol = useAuthStore((s) => s.auth?.rol) as RolNoAdmin;
  const { data, isLoading, error } = usePerfil(rol);
  const editar = useEditarPerfil(rol);

  const [nombre, setNombre]     = useState('');
  const [apellido, setApellido] = useState('');
  const [telefono, setTelefono] = useState('');

  useEffect(() => {
    if (data) {
      setNombre(data.nombre);
      setApellido(data.apellido);
      setTelefono(data.telefono);
    }
  }, [data]);

  const handleGuardar = () => {
    if (!nombre || !apellido || !telefono) {
      Alert.alert('Datos incompletos', 'Nombre, apellido y teléfono son obligatorios');
      return;
    }
    if (!/^3\d{9}$/.test(telefono)) {
      Alert.alert('Teléfono inválido', 'Debe ser celular colombiano (10 dígitos, inicia con 3)');
      return;
    }
    editar.mutate(
      { nombre, apellido, telefono },
      {
        onSuccess: () => toast.success('Perfil actualizado'),
        onError: (e) => Alert.alert('Error', errorMessage(e, 'No se pudo actualizar')!),
      },
    );
  };

  return (
    <Screen>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹ Volver</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Mi perfil</Text>
        <View style={styles.backSpacer} />
      </View>

      {isLoading || !data
        ? <Text style={styles.muted}>{error ? errorMessage(error) : 'Cargando...'}</Text>
        : (
          <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
            <ScrollView keyboardShouldPersistTaps="handled">
              <View style={styles.readonly}>
                <Field label="Correo" value={data.correo} />
                {camposExtra(data).map(c => <Field key={c.label} label={c.label} value={c.value} />)}
              </View>

              <Input label="Nombre"   value={nombre}   onChangeText={setNombre} />
              <Input label="Apellido" value={apellido} onChangeText={setApellido} />
              <Input
                label="Celular"
                value={telefono}
                onChangeText={(t) => /^\d{0,10}$/.test(t) && setTelefono(t)}
                keyboardType="number-pad"
                placeholder="3001234567"
              />

              <Button title="Guardar cambios" onPress={handleGuardar} loading={editar.isPending} />
            </ScrollView>
          </KeyboardAvoidingView>
        )}
    </Screen>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <Text style={styles.fieldValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  flex:       { flex: 1 },
  header:     { flexDirection: 'row', alignItems: 'center', marginBottom: 20 },
  back:       { color: theme.colors.primary, fontSize: 16, fontWeight: '600', width: 80 },
  backSpacer: { width: 80 },
  title:      { flex: 1, color: theme.colors.text, fontSize: 20, fontWeight: '700', textAlign: 'center' },
  muted:      { color: theme.colors.textMuted, textAlign: 'center', marginTop: 32 },
  readonly:   { backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 14, marginBottom: 20, borderWidth: 1, borderColor: theme.colors.border },
  field:      { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  fieldLabel: { color: theme.colors.textMuted, fontSize: 13 },
  fieldValue: { color: theme.colors.text, fontSize: 13, fontWeight: '600', flex: 1, textAlign: 'right', marginLeft: 12 },
});
