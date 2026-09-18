import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { ComponentType } from 'react';
import { CitaDetailScreen } from '../screens/CitaDetailScreen';
import { DocenteHomeScreen } from '../screens/DocenteHomeScreen';
import { EstadisticasScreen } from '../screens/EstadisticasScreen';
import { EstudianteHomeScreen } from '../screens/EstudianteHomeScreen';
import { PacienteHomeScreen } from '../screens/PacienteHomeScreen';
import { PerfilScreen } from '../screens/PerfilScreen';
import { UnsupportedRoleScreen } from '../screens/UnsupportedRoleScreen';
import type { Rol } from '../types';
import type { MainStackParamList } from './types';

const Stack = createNativeStackNavigator<MainStackParamList>();

function homeForRole(rol: Rol): ComponentType {
  switch (rol) {
    case 'ROLE_PACIENTE':   return PacienteHomeScreen;
    case 'ROLE_ESTUDIANTE': return EstudianteHomeScreen;
    case 'ROLE_DOCENTE':    return DocenteHomeScreen;
    default:                return UnsupportedRoleScreen;
  }
}

export function MainNavigator({ rol }: { rol: Rol }) {
  const Home = homeForRole(rol);
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Home" component={Home} />
      <Stack.Screen name="Perfil" component={PerfilScreen} />
      <Stack.Screen name="Estadisticas" component={EstadisticasScreen} />
      <Stack.Screen name="CitaDetail" component={CitaDetailScreen} />
    </Stack.Navigator>
  );
}
