import { useMemo, useState } from 'react';
import { FlatList, Modal, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { theme } from '../theme';
import type { PacienteResponse } from '../types/cita';

interface Props {
  visible: boolean;
  pacientes: PacienteResponse[];
  onSelect: (paciente: PacienteResponse) => void;
  onClose: () => void;
}

export function PacientePickerModal({ visible, pacientes, onSelect, onClose }: Props) {
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return pacientes;
    return pacientes.filter(p =>
      `${p.nombre} ${p.apellido} ${p.numeroDocumento} ${p.correo}`.toLowerCase().includes(q)
    );
  }, [pacientes, query]);

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Asignar paciente</Text>
          <TouchableOpacity onPress={onClose}><Text style={styles.close}>Cerrar</Text></TouchableOpacity>
        </View>

        <TextInput
          value={query}
          onChangeText={setQuery}
          placeholder="Buscar por nombre, cedula o correo"
          placeholderTextColor={theme.colors.textMuted}
          style={styles.search}
        />

        <FlatList
          data={filtered}
          keyExtractor={(p) => String(p.idPaciente)}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.row} onPress={() => onSelect(item)}>
              <View style={styles.flex}>
                <Text style={styles.name}>{item.nombre} {item.apellido}</Text>
                <Text style={styles.meta}>{item.tipoDocumento} {item.numeroDocumento}</Text>
              </View>
              <Text style={styles.arrow}>›</Text>
            </TouchableOpacity>
          )}
          ListEmptyComponent={<Text style={styles.empty}>No se encontraron pacientes</Text>}
        />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.background, padding: 24 },
  header:    { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
  title:     { color: theme.colors.text, fontSize: 22, fontWeight: '700' },
  close:     { color: theme.colors.primary, fontSize: 16, fontWeight: '600' },
  search:    { backgroundColor: theme.colors.surface, color: theme.colors.text, padding: 12, borderRadius: theme.radius.md, marginBottom: 12, borderWidth: 1, borderColor: theme.colors.border },
  row:       { flexDirection: 'row', alignItems: 'center', backgroundColor: theme.colors.surface, padding: 14, borderRadius: theme.radius.md, marginBottom: 8, borderWidth: 1, borderColor: theme.colors.border },
  flex:      { flex: 1 },
  name:      { color: theme.colors.text, fontSize: 15, fontWeight: '600' },
  meta:      { color: theme.colors.textMuted, fontSize: 12, marginTop: 2 },
  arrow:     { color: theme.colors.textMuted, fontSize: 24 },
  empty:     { color: theme.colors.textMuted, textAlign: 'center', marginTop: 32 },
});
