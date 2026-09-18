import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { theme } from '../theme';

export interface TabItem<K extends string> {
  key: K;
  label: string;
  count?: number;
}

interface Props<K extends string> {
  items: TabItem<K>[];
  current: K;
  onChange: (key: K) => void;
}

export function Tabs<K extends string>({ items, current, onChange }: Props<K>) {
  return (
    <View style={styles.tabs}>
      {items.map(item => {
        const active = current === item.key;
        const label = item.count != null ? `${item.label} (${item.count})` : item.label;
        return (
          <TouchableOpacity
            key={item.key}
            style={[styles.tab, active ? styles.tabActive : null]}
            onPress={() => onChange(item.key)}
          >
            <Text style={[styles.tabText, active ? styles.tabTextActive : null]}>{label}</Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  tabs:          { flexDirection: 'row', backgroundColor: theme.colors.surface, borderRadius: theme.radius.md, padding: 4, marginBottom: 16 },
  tab:           { flex: 1, paddingVertical: 8, alignItems: 'center', borderRadius: theme.radius.sm },
  tabActive:     { backgroundColor: theme.colors.primary },
  tabText:       { color: theme.colors.textMuted, fontSize: 13, fontWeight: '600' },
  tabTextActive: { color: theme.colors.text },
});
