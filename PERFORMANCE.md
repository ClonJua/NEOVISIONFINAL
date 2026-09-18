# Pruebas de carga — NeoVision

Medido contra el backend corriendo en local (`./mvnw spring-boot:run`, Postgres local, datos de la semilla de pruebas), usando [autocannon](https://github.com/mcollina/autocannon) (`npx autocannon`). No reemplaza medir contra el hardware real de Azure App Service, pero da una línea base real en vez de solo "debería ser rápido por cómo está escrito el código".

## Cómo reproducirlo

```bash
# 1) Levantar el backend
cd neovision && ./mvnw spring-boot:run

# 2) Obtener un token (en otra terminal)
TOKEN=$(curl -s http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@neovision.com","contrasena":"admin123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 3) Correr una prueba (ejemplo: reporte agregado, 20 conexiones, 20s)
npx --yes autocannon -c 20 -d 20 -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/citas/reportes/indicadores
```

## Matriz de resultados (2026-08-23)

Cada fila es una corrida independiente de `autocannon`. Percentiles de latencia en milisegundos; Req/s como promedio de la ventana de la corrida. "Techo" en la columna de veredicto significa que subir la concurrencia no aumentó el throughput — la corrida a 50 conexiones sobre `/admin/citas` se hizo específicamente para confirmarlo.

| # | Endpoint | Operación | Concurrencia | Duración | Peticiones totales | Req/s p50 | Req/s avg | Latencia p50 | Latencia p97.5 | Latencia p99 | Latencia máx | Errores | Datos transferidos | Veredicto |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | `GET /admin/citas/reportes/indicadores` | Agregación en BD (GROUP BY) | 20 | 20 s | 29.000 | 1.440 | 1.424,7 | 105 ms | 207 ms | 224 ms | 285 ms | 0 | 17,4 MB | Sólido — sin degradación bajo concurrencia |
| 2 | `GET /admin/citas` | Listado completo sin paginar | 20 | 15 s | 7.000 | 460 | 453,6 | 93 ms | 202 ms | 230 ms | 319 ms | 0 | 761 MB | Techo por tamaño de respuesta (~112 KB c/u) |
| 3 | `GET /admin/citas` | Listado completo sin paginar | 50 | 15 s | 7.000 | 477 | 469,4 | 115 ms | 259 ms | 289 ms | 402 ms | 0 | 788 MB | Confirma el techo: 20→50 conexiones no sube el throughput |
| 4 | `POST /auth/login` | Autenticación (BCrypt) | 15 | 15 s | 475 | 30 | 30,67 | 437 ms | 639 ms | 651 ms | 670 ms | 0 | 331 kB | Lento a propósito (costo de BCrypt), no es una falla |
| 5 | `GET /historias/cie10?q=vision` | Búsqueda / autocompletar | 20 | 15 s | 24.000 | 1.615 | 1.604,1 | 78 ms | 160 ms | 173 ms | 240 ms | 1 (0,004 %) | 11,5 MB | Sólido — error transitorio aislado, no sistemático |

**Total acumulado de esta ronda de pruebas**: ~67.500 peticiones HTTP, 1 error no sistemático (0,0015 % del total), 0 timeouts.

## Conclusiones

- **Los endpoints de reportes agregados en base de datos** (los que se reescribieron para usar `GROUP BY`/`COUNT DISTINCT` en vez de traer todo el histórico al navegador) sostienen >1.400 req/s con latencia de un dígito de cientos de milisegundos incluso bajo concurrencia — la optimización aplicada por buenas prácticas de código queda validada con datos reales, no solo por lectura de código.
- **El listado completo de citas sin paginar es el cuello de botella real del sistema**: el throughput no mejora al subir de 20 a 50 conexiones concurrentes porque el límite es el tamaño de cada respuesta (~112 KB), no la capacidad de cómputo. Con el volumen de datos de la semilla de pruebas ya es visible; con datos acumulados de varios semestres reales sería peor. **Recomendación aplicable ahora**: preferir la variante paginada (`/admin/citas/paginadas`, ya existente) en las pantallas que listan muchas citas.
- **El login es intencionalmente el endpoint más lento** (~30 req/s sostenidos) por el costo computacional de BCrypt — es una decisión de seguridad deliberada, reforzada por el límite de intentos fallidos agregado por separado (`LoginAttemptService`), no una falla de rendimiento.
- Para el uso real esperado de un sistema de una clínica universitaria de optometría (decenas de estudiantes/pacientes/personal concurrentes, no miles), el sistema sostiene esa carga con margen amplio en una sola máquina de desarrollo — una instancia real de Azure App Service debería comportarse al menos igual de bien.
