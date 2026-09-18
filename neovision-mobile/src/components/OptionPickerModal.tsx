import { FlatList, Modal, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { theme } from '../theme';

export interface PickerOption {
  key: string;
  label: string;
  sublabel?: string;
}

interface Props {
  visible: boolean;
  title: string;
  options: PickerOption[];
  onSelect: (key: string) => void;
  onClose: () => void;
}

/** Modal generico de seleccion de una opcion de una lista. */
export function OptionPickerModal({ visible, title, options, onSelect, onClose }: Props) {
  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>{title}</Text>
          <TouchableOpacity onPress={onClose}><Text style={styles.close}>Cerrar</Text></TouchableOpacity>
        </View>
        <FlatList
          data={options}
          keyExtractor={(o) => o.key}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.row} onPress={() => onSelect(item.key)}>
              <View style={styles.flex}>
                <Text style={styles.label}>{item.label}</Text>
                {item.sublabel ? <Text style={styles.sub}>{item.sublabel}</Text> : null}
              </View>
              <Text style={styles.arrow}>›</Text>
            </TouchableOpacity>
          )}
          ListEmptyComponent={<Text style={styles.empty}>Sin opciones</Text>}
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
  row:       { flexDirection: 'row', alignItems: 'center', backgroundColor: theme.colors.surface, padding: 14, borderRadius: theme.radius.md, marginBottom: 8, borderWidth: 1, borderColor: theme.colors.border },
  flex:      { flex: 1 },
  label:     { color: theme.colors.text, fontSize: 15, fontWeight: '600' },
  sub:       { color: theme.colors.textMuted, fontSize: 12, marginTop: 2 },
  arrow:     { color: theme.colors.textMuted, fontSize: 24 },
  empty:     { color: theme.colors.textMuted, textAlign: 'center', marginTop: 32 },
});
