import * as Notifications from 'expo-notifications';
import { useEffect } from 'react';
import { queryClient } from '../lib/queryClient';
import { navigationRef } from '../navigation/navigationRef';
import type { CitaResponse } from '../types/cita';

/** Busca una cita por id en cualquier lista cacheada por React Query. */
function buscarCitaEnCache(idCita: number): CitaResponse | undefined {
  const queries = queryClient.getQueriesData<unknown>({});
  for (const [, data] of queries) {
    if (Array.isArray(data)) {
      const found = (data as CitaResponse[]).find(
        (c) => c && typeof c === 'object' && c.idCita === idCita,
      );
      if (found) return found;
    }
  }
  return undefined;
}

function handle(response: Notifications.NotificationResponse | null) {
  if (!response) return;
  const data = response.notification.request.content.data as { idCita?: number };
  const idCita = data?.idCita;
  if (!idCita || !navigationRef.isReady()) return;

  const cita = buscarCitaEnCache(Number(idCita));
  if (cita) {
    navigationRef.navigate('CitaDetail', { cita });
  } else {
    // Cache fria: al menos lleva al home, donde se recargan las citas.
    navigationRef.navigate('Home');
  }
}

/**
 * Navega a la cita al tocar una push. Cubre app en background y app abierta
 * desde estado cerrado (getLastNotificationResponseAsync).
 */
export function useNotificationDeepLink() {
  useEffect(() => {
    Notifications.getLastNotificationResponseAsync().then(handle);
    const sub = Notifications.addNotificationResponseReceivedListener(handle);
    return () => sub.remove();
  }, []);
}
