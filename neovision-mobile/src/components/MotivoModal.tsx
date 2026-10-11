import { useEffect, useState } from 'react';
import { Modal, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { theme } from '../theme';
import { Button } from './Button';
import { Input } from './Input';

interface Props {
  visible: boolean;
  title: string;
  confirmLabel: string;
  loading?: boolean;
  onConfirm: (motivo: string) => void;
  onClose: () => void;
}

/** Pide un motivo obligatorio antes de confirmar (Alert.prompt solo existe en iOS). */
export function MotivoModal({ visible, title, confirmLabel, loading = false, onConfirm, onClose }: Props) {
  const [motivo, setMotivo] = useState('');

  useEffect(() => { if (visible) setMotivo(''); }, [visible]);

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>{title}</Text>
          <TouchableOpacity onPress={onClose}><Text style={styles.close}>Cerrar</Text></TouchableOpacity>
        </View>
        <Input
          label="Motivo"
          placeholder="Ej: tengo un imprevisto"
          value={motivo}
          onChangeText={setMotivo}
          multiline
          maxLength={255}
          style={styles.textarea}
        />
        <Button
          title={confirmLabel}
          variant="danger"
          loading={loading}
          disabled={!motivo.trim()}
          onPress={() => onConfirm(motivo.trim())}
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
  textarea:  { minHeight: 96, textAlignVertical: 'top' },
});
