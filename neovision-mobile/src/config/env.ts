// EXPO_PUBLIC_BACKEND_URL se lee al construir el bundle (Expo SDK 49+ lo
// soporta de forma nativa, sin config adicional). En desarrollo local, si no
// se define, cae en la IP de red local de abajo — cámbiala por la IP actual
// de tu máquina si ya no es esa. En un build de producción, defínela apuntando
// al backend desplegado (ej. https://neovision-api.azurewebsites.net/api).
const BACKEND_URL = process.env.EXPO_PUBLIC_BACKEND_URL || 'http://192.168.2.13:8080/api';

// Datos clínicos: fuera de desarrollo el backend tiene que ir cifrado. Si una
// build de producción sale sin EXPO_PUBLIC_BACKEND_URL (o con http://), es
// preferible que falle al arrancar a que envíe historias clínicas en claro.
if (!__DEV__ && !BACKEND_URL.startsWith('https://')) {
  throw new Error('EXPO_PUBLIC_BACKEND_URL debe ser https:// en builds de producción');
}

export const ENV = {
  BACKEND_URL,
  CLIENT_ID:    'c441594b-ed0a-490c-abee-138cb21acef1',
  REDIRECT_URI: 'neovision://auth',
  AUTH_URL:     'https://login.microsoftonline.com/common/oauth2/v2.0/authorize',
  TOKEN_URL:    'https://login.microsoftonline.com/common/oauth2/v2.0/token',
  SCOPES:       'openid profile email User.Read',
} as const;
