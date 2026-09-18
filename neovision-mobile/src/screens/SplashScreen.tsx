import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { theme } from '../theme';

export function SplashScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>NeoVision</Text>
      <ActivityIndicator color={theme.colors.primary} size="large" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: theme.colors.background },
  title:     { color: theme.colors.text, fontSize: 32, fontWeight: '700', marginBottom: 24 },
});
