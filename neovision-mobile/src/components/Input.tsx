import { StyleSheet, Text, TextInput, TextInputProps, View } from 'react-native';
import { theme } from '../theme';

interface Props extends TextInputProps {
  label: string;
  error?: string;
}

export function Input({ label, error, style, ...rest }: Props) {
  return (
    <View style={styles.wrapper}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        placeholderTextColor={theme.colors.textMuted}
        style={[styles.input, error ? styles.inputError : null, style]}
        {...rest}
      />
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper:    { marginBottom: 14 },
  label:      { color: theme.colors.textMuted, fontSize: 13, fontWeight: '600', marginBottom: 6 },
  input:      {
    backgroundColor: theme.colors.surface,
    color: theme.colors.text,
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.colors.border,
    fontSize: 15,
  },
  inputError: { borderColor: theme.colors.danger },
  errorText:  { color: theme.colors.danger, fontSize: 12, marginTop: 4 },
});
