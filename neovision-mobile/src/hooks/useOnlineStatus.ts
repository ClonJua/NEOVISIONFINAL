import NetInfo from '@react-native-community/netinfo';
import { useEffect, useState } from 'react';

/**
 * true si hay conexion a internet. Refleja el estado real del dispositivo.
 *
 * "isConnected" solo indica que hay un transporte (wifi/datos) activo, no que
 * ese enlace llegue a internet, y en Android es comun que fluctue a false por
 * un instante sin motivo real. Por eso solo se marca "sin conexion" cuando hay
 * una senal explicita de desconexion (isConnected o isInternetReachable en
 * false); un estado ambiguo (p. ej. isInternetReachable en null mientras se
 * determina) no bloquea al usuario.
 */
export function useOnlineStatus(): boolean {
  const [online, setOnline] = useState(true);

  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener((state) => {
      const desconectado = state.isConnected === false || state.isInternetReachable === false;
      setOnline(!desconectado);
    });
    return unsubscribe;
  }, []);

  return online;
}
