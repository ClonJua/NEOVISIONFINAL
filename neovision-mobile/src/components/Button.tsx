import { ActivityIndicator, StyleSheet, Text, TouchableOpacity, ViewStyle } from 'react-native';
import { theme } from '../theme';

interface Props {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
}

const variantStyles: Record<NonNullable<Props['variant']>, ViewStyle> = {
  primary:   { backgroundColor: theme.colors.primary },
  secondary: { backgroundColor: theme.colors.surface, borderWidth: 1, borderColor: theme.colors.border },
  danger:    { backgroundColor: theme.colors.danger },
};

export function Button({ title, onPress, loading = false, disabled = false, variant = 'primary' }: Props) {
  const isDisabled = disabled || loading;
  return (
    <TouchableOpacity
      style={[styles.base, variantStyles[variant], isDisabled ? styles.disabled : null]}
      onPress={onPress}
      disabled={isDisabled}
      activeOpacity={0.8}
    >
      {loading
        ? <ActivityIndicator color={theme.colors.text} />
        : <Text style={styles.label}>{title}</Text>}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  base:     { padding: 16, borderRadius: theme.radius.md, alignItems: 'center', justifyContent: 'center' },
  disabled: { opacity: 0.5 },
  label:    { color: theme.colors.text, fontWeight: '700', fontSize: 16 },
});
