import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { theme } from '../theme';

interface Props<T> {
  data: T[];
  keyExtractor: (item: T) => string;
  renderItem: (item: T) => React.ReactElement;
  loading: boolean;
  refreshing: boolean;
  onRefresh: () => void;
  error: string | null;
  emptyText: string;
}

/**
 * Lista con manejo unificado de los 4 estados: cargando, error, vacio y datos.
 * Centraliza el scaffold que compartian las pantallas de cada rol.
 */
export function DataList<T>({
  data, keyExtractor, renderItem, loading, refreshing, onRefresh, error, emptyText,
}: Props<T>) {
  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={theme.colors.primary} size="large" />
      </View>
    );
  }

  return (
    <FlatList
      data={data}
      keyExtractor={keyExtractor}
      contentContainerStyle={styles.list}
      renderItem={({ item }) => renderItem(item)}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.primary} />
      }
      ListEmptyComponent={
        error
          ? <Text style={styles.error}>{error}</Text>
          : <Text style={styles.empty}>{emptyText}</Text>
      }
    />
  );
}

const styles = StyleSheet.create({
  list:   { paddingHorizontal: 24, paddingBottom: 24 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  empty:  { color: theme.colors.textMuted, textAlign: 'center', marginTop: 48 },
  error:  { color: theme.colors.danger, textAlign: 'center', marginTop: 48 },
});
