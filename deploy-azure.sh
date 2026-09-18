#!/usr/bin/env bash
# Despliegue manual a Azure App Service: un solo jar con backend + frontend.
#
# Requisitos: `az login` hecho, Node y Java 21. Uso desde la raiz del repo:
#   ./deploy-azure.sh            # build frontend + jar (con tests unitarios) + deploy
#   SKIP_TESTS=1 ./deploy-azure.sh
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

echo "==> 3/4 az webapp deploy -> $APP"
az webapp deploy -n "$APP" -g "$RG" --type jar --src-path "$JAR" --clean true --restart true --only-show-errors

echo "==> 4/4 Logs en vivo (Ctrl+C para salir)."
echo "    Espera a ver: 'Successfully applied N migrations' (Flyway) y 'Started NeovisionApplication'."
az webapp log tail -n "$APP" -g "$RG"
