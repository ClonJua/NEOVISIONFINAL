import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useEffect } from 'react';
import { setUnauthorizedHandler } from '../api/axios';
import { SplashScreen } from '../screens/SplashScreen';
import { useAuthStore } from '../store/authStore';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';

const Stack = createNativeStackNavigator();

export function RootNavigator() {
  const status    = useAuthStore((s) => s.status);
  const auth      = useAuthStore((s) => s.auth);
  const bootstrap = useAuthStore((s) => s.bootstrap);
  const logout    = useAuthStore((s) => s.logout);

  useEffect(() => {
    bootstrap();
    setUnauthorizedHandler(() => { logout(); });
  }, [bootstrap, logout]);

  if (status === 'loading') {
    return (
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Splash" component={SplashScreen} />
      </Stack.Navigator>
    );
  }

  if (status === 'unauthenticated' || !auth) {
    return <AuthNavigator />;
  }

  return <MainNavigator rol={auth.rol} />;
}
