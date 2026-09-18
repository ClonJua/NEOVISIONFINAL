import { useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useToastStore } from '../store/toastStore';
import { theme } from '../theme';

/** Se monta una sola vez. Muestra el toast actual del store y se auto-oculta. */
export function ToastHost() {
  const { message, type, hide } = useToastStore();
  const opacity = useRef(new Animated.Value(0)).current;
  const insets = useSafeAreaInsets();

  useEffect(() => {
    if (!message) return;
    Animated.timing(opacity, { toValue: 1, duration: 200, useNativeDriver: true }).start();
    const t = setTimeout(() => {
      Animated.timing(opacity, { toValue: 0, duration: 200, useNativeDriver: true }).start(hide);
    }, 2500);
    return () => clearTimeout(t);
  }, [message, opacity, hide]);

  if (!message) return null;

  return (
    <Animated.View
      pointerEvents="none"
      style={[
        styles.toast,
        { top: insets.top + 8, opacity, backgroundColor: type === 'error' ? theme.colors.danger : theme.colors.success },
      ]}
    >
      <Text style={styles.text}>{message}</Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  toast: {
    position: 'absolute',
    left: 16,
    right: 16,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: theme.radius.md,
    zIndex: 1000,
  },
  text: { color: '#ffffff', fontSize: 14, fontWeight: '600', textAlign: 'center' },
});
