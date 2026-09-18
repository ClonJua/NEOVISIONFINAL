import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { theme } from '../theme';
import { OfflineBanner } from './OfflineBanner';

interface Props {
  children: ReactNode;
  padded?: boolean;
}

export function Screen({ children, padded = true }: Props) {
  return (
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      <OfflineBanner />
      <View style={padded ? styles.padded : styles.flex}>{children}</View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe:   { flex: 1, backgroundColor: theme.colors.background },
  flex:   { flex: 1 },
  padded: { flex: 1, padding: 24 },
});
