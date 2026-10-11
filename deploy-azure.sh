#!/usr/bin/env bash
# Despliegue manual a Azure App Service: un solo jar con backend + frontend.
#
# Requisitos: `az login` hecho, Node y Java 21. Uso desde la raiz del repo:
#   ./deploy-azure.sh            # build frontend + jar (con tests unitarios) + deploy + logs
#   SKIP_TESTS=1 ./deploy-azure.sh
#   TAIL_LOGS=0  ./deploy-azure.sh   # no quedarse siguiendo los logs al final
#
# Antes de desplegar cambios de esquema, crea un branch de respaldo en Neon.
set -euo pipefail
cd "$(dirname "$0")"

APP=neovision-demo-api
RG=neovision-demo-rg
STATIC=neovision/src/main/resources/static

echo "==> 1/4 Frontend (Vite) -> $STATIC"
(
  cd neovision-frontend
  if [ -f package-lock.json ]; then npm ci --silent --no-audit --no-fund; else npm install --silent --no-audit --no-fund; fi
  npm run build
)
rm -rf "$STATIC"
mkdir -p "$STATIC"
cp -r neovision-frontend/dist/. "$STATIC/"

echo "==> 2/4 Backend (jar)"
MVN=./mvnw
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) MVN=./mvnw.cmd ;; esac
(
  cd neovision
  if [ "${SKIP_TESTS:-0}" = "1" ]; then "$MVN" -q package -DskipTests; else "$MVN" -q package; fi
)
JAR=$(ls neovision/target/neovision-*.jar | grep -v '\.original$' | head -1)
echo "    jar: $JAR ($(du -h "$JAR" | cut -f1))"

echo "==> 3/4 Ajustes de produccion + az webapp deploy -> $APP"
# Deshabilita la documentacion de la API (Swagger / OpenAPI) en produccion.
# Sin esto, /v3/api-docs y /swagger-ui quedan accesibles sin autenticacion y
# publican el mapa completo de la API. Se fija en cada despliegue para que la
# proteccion no dependa de recordar configurarla a mano en el portal.
az webapp config appsettings set -n "$APP" -g "$RG" \
  --settings SWAGGER_ENABLED=false --only-show-errors >/dev/null

az webapp deploy -n "$APP" -g "$RG" --type jar --src-path "$JAR" --clean true --restart true --only-show-errors

if [ "${TAIL_LOGS:-1}" = "1" ]; then
  echo "==> 4/4 Logs en vivo (Ctrl+C para salir)."
  echo "    Espera a ver: 'Successfully applied N migrations' (Flyway) y 'Started NeovisionApplication'."
  az webapp log tail -n "$APP" -g "$RG"
else
  echo "==> 4/4 Listo. Logs: az webapp log tail -n $APP -g $RG"
fi
