import { NavigationContainer } from '@react-navigation/native';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ToastHost } from './src/components/ToastHost';
import { asyncStoragePersister, queryClient } from './src/lib/queryClient';
import { navigationRef } from './src/navigation/navigationRef';
import { RootNavigator } from './src/navigation/RootNavigator';
import { useNotificationDeepLink } from './src/notifications/useNotificationDeepLink';

export default function App() {
  useNotificationDeepLink();

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister: asyncStoragePersister, maxAge: 1000 * 60 * 60 * 24 * 7 }}
    >
      <SafeAreaProvider>
        <StatusBar style="light" />
        <NavigationContainer ref={navigationRef}>
          <RootNavigator />
        </NavigationContainer>
        <ToastHost />
      </SafeAreaProvider>
    </PersistQueryClientProvider>
  );
}
