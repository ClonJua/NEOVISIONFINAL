import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import { QueryClient, onlineManager } from '@tanstack/react-query';

const DAY = 1000 * 60 * 60 * 24;

// React Query escucha el estado de red real del dispositivo (NetInfo).
// Sin esto no pausa los fetch ni sabe que esta offline.
onlineManager.setEventListener((setOnline) =>
  NetInfo.addEventListener((state) => {
    setOnline(Boolean(state.isConnected));
  })
);

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // gcTime largo: las entradas deben sobrevivir para persistirse a disco.
      gcTime: 7 * DAY,
      staleTime: 1000 * 30,
      retry: 2,
      refetchOnReconnect: true,
    },
  },
});

export const asyncStoragePersister = createAsyncStoragePersister({
  storage: AsyncStorage,
  key: 'neovision.query-cache',
});
