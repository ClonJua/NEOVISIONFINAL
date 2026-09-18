// Config dinamica sobre app.json. Solo habilita trafico HTTP sin cifrar (ATS en
// iOS, usesCleartextTraffic en Android) cuando el backend configurado para la
// build es http://, es decir, desarrollo contra la maquina local. Las builds
// preview/production reciben una URL https desde eas.json y quedan con la
// politica estricta por defecto: en una app de salud nada viaja en claro.
const backendUrl = process.env.EXPO_PUBLIC_BACKEND_URL || '';
const permitirHttp = !backendUrl.startsWith('https://');

module.exports = ({ config }) => ({
  ...config,
  ios: {
    ...config.ios,
    ...(permitirHttp && {
      infoPlist: {
        ...config.ios.infoPlist,
        NSAppTransportSecurity: { NSAllowsArbitraryLoads: true },
      },
    }),
  },
  plugins: config.plugins.map((plugin) =>
    Array.isArray(plugin) && plugin[0] === 'expo-build-properties'
      ? [plugin[0], { ...plugin[1], android: { ...plugin[1].android, usesCleartextTraffic: permitirHttp } }]
      : plugin,
  ),
});
