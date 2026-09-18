import DateTimePicker from '@react-native-community/datetimepicker';
import { useState } from 'react';
import { Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { theme } from '../theme';

interface Props {
  label: string;
  /** Valor en formato YYYY-MM-DD (o '' si no hay). */
  value: string;
  onChange: (yyyyMmDd: string) => void;
  error?: string;
}

function toYmd(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

export function DateField({ label, value, onChange, error }: Props) {
  const [show, setShow] = useState(false);
  const current = value ? new Date(value + 'T00:00:00') : new Date(2000, 0, 1);

  return (
    <View style={styles.wrapper}>
      <Text style={styles.label}>{label}</Text>
      <TouchableOpacity
        style={[styles.field, error ? styles.fieldError : null]}
        onPress={() => setShow(true)}
        activeOpacity={0.7}
      >
        <Text style={value ? styles.value : styles.placeholder}>
          {value || 'AAAA-MM-DD'}
        </Text>
      </TouchableOpacity>
      {error ? <Text style={styles.errorText}>{error}</Text> : null}

      {show ? (
        <DateTimePicker
          value={current}
          mode="date"
          display={Platform.OS === 'ios' ? 'spinner' : 'default'}
          maximumDate={new Date()}
          onChange={(event, selected) => {
            setShow(Platform.OS === 'ios');
            if (event.type === 'set' && selected) onChange(toYmd(selected));
          }}
        />
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper:     { marginBottom: 14 },
  label:       { color: theme.colors.textMuted, fontSize: 13, fontWeight: '600', marginBottom: 6 },
  field:       {
    backgroundColor: theme.colors.surface,
    paddingHorizontal: 14,
    paddingVertical: 14,
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.colors.border,
  },
  fieldError:  { borderColor: theme.colors.danger },
  value:       { color: theme.colors.text, fontSize: 15 },
  placeholder: { color: theme.colors.textMuted, fontSize: 15 },
  errorText:   { color: theme.colors.danger, fontSize: 12, marginTop: 4 },
});
