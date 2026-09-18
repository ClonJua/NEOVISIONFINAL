# NeoVision — Briefing completo para agente de desarrollo

## Contexto del proyecto

Proyecto de grado universitario. Sistema de gestión de citas clínicas para una clínica odontológica universitaria donde estudiantes realizan prácticas supervisadas por docentes. El sistema se llama **NeoVision**.

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 4.0.5, Java 21, Maven |
| Frontend | React + TypeScript, Vite |
| Base de datos | PostgreSQL (local) |
| Seguridad | Spring Security + JWT + OAuth2 (Microsoft) |
| IDE Backend | IntelliJ IDEA |
| IDE Frontend | VS Code |

---

## Arquitectura

**Monolito modular** — NO microservicios. Un solo backend REST que servirá web (y posiblemente móvil en el futuro).

### Capas backend (MVC adaptado a REST)
```
@RestController → @Service → @Repository → PostgreSQL
(Controller)      (Lógica)   (Datos)
```

No hay View — todo es JSON. Jackson lo maneja automáticamente.

### Estructura de paquetes backend
```
com.neovision/
├── auth/               # JWT, login, registro, OAuth2
├── usuario/            # Usuario, Rol
├── academico/          # Profesor, Estudiante, PracticaSesion, Cupo
├── clinico/            # Paciente, Cita, Patologia, PacientePatologia
├── infraestructura/    # Espacio, Salon
└── common/
    ├── dto/            # ApiResponse<T> genérico
    ├── exception/      # GlobalExceptionHandler (@RestControllerAdvice)
    └── config/         # SecurityConfig, CorsConfig, SwaggerConfig
```

### Estructura frontend (Feature-based)
```
src/
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── citas/
│   ├── sesiones/
│   └── pacientes/
├── components/         # UI genérica reutilizable
├── hooks/              # useAuth, useFetch
├── services/           # llamadas Axios
├── router/             # rutas protegidas por rol
└── store/              # Context API para estado global
```

---

## Principios SOLID — aplicación obligatoria

El proyecto debe cumplir SOLID de forma explícita y documentada. No es opcional.

- **S**: Cada clase tiene una sola responsabilidad. Controllers solo reciben HTTP y delegan. Services tienen la lógica. Repositories solo acceden a datos.
- **O**: Usar Strategy/polimorfismo para comportamientos que cambian (ej: estados de Cita). No switches gigantes.
- **L**: La jerarquía Usuario/Profesor/Estudiante/Paciente debe ser sustituible sin romper comportamiento. No usar `instanceof`.
- **I**: Interfaces específicas por módulo/rol. No una interfaz gigante de servicio.
- **D**: Siempre inyección por constructor (nunca `@Autowired` en campo). Depender de interfaces, no implementaciones.

---

## Modelo de datos (E-R)

### Entidades principales

```
Rol
- idRol (PK, int)
- nombreRol (string)

Usuario
- idUsuario (PK, int)
- correo (string)
- contrasena (string)       ← solo para PACIENTE
- estado (boolean)
- idRol (FK → Rol)

Profesor
- idDocente (PK, int)
- nombre (string)
- apellido (string)
- estaDisponible (boolean)
- correo (string)           ← correo institucional @universidad.edu.co
- IdUsuario (FK → Usuario)

Estudiante
- idEstudiante (PK, int)
- nombre (string)
- apellido (string)
- semestre (int)
- correo (string)           ← correo institucional @universidad.edu.co
- IdUsuario (FK → Usuario)

Paciente
- idPaciente (PK, int)
- nombre (string)
- apellido (string)
- tipoDocumento (enum)
- fechaNacimiento (date)
- telefono (string)
- correo (string)           ← correo personal
- IdUsuario (FK → Usuario)

Patologia
- idPatologia (PK, int)
- nombre (string)
- tipo (string)

PacientePatologia          ← tabla pivot
- idPaciente (FK)
- idPatologia (FK)

Espacio
- idEspacio (PK, int)
- sede (enum)
- estado (string)
- capacidad (string)

Salon
- idSalon (PK, int)
- nombreSalon (string)
- estado (enum)
- idEspacio (FK → Espacio)

PracticaSesion
- idSesion (PK, int)
- semana (int)
- tipoActividad (string)
- fecha (date)
- horaInicio (time)
- horaFin (time)
- estado (boolean)
- idDocente (FK → Profesor)

Cupo
- idCupo (PK, int)
- idSesion (FK → PracticaSesion)
- idSalon (FK → Salon)
- idEstudiante (FK → Estudiante)
- estado (enum)

Cita
- idCita (PK, int)
- fechaRegistro (date)
- motivo (string)
- estadoCita (enum)         ← pendiente, confirmada, cancelada, reprogramada
- idPaciente (FK → Paciente)
- idCupo (FK → Cupo)
- idCitaAnterior (FK → Cita) ← para reprogramaciones (self-reference)
```

### Relaciones clave
- `Rol` → `Usuario` (1:N) — define acceso
- `Usuario` → `Profesor` (1:1)
- `Usuario` → `Estudiante` (1:1)
- `Usuario` → `Paciente` (1:1)
- `Espacio` → `Salon` (1:N)
- `Profesor` → `PracticaSesion` (1:N) — supervisa
- `PracticaSesion` + `Salon` + `Estudiante` → `Cupo` (N:N resuelta)
- `Paciente` + `Cupo` → `Cita`
- `Cita` → `Cita` (self-reference para reprogramación)
- `Paciente` ↔ `Patologia` via `PacientePatologia`

---

## Roles y permisos

| Rol | Código | Autenticación | Puede hacer |
|-----|--------|--------------|-------------|
| Secretaria | `ROLE_ADMIN` | OAuth2 Microsoft | CRUD general, aceptar/rechazar citas, gestionar agenda |
| Docente | `ROLE_DOCENTE` | OAuth2 Microsoft | Ver y supervisar sesiones de práctica |
| Estudiante | `ROLE_ESTUDIANTE` | OAuth2 Microsoft | Enviar solicitud de cita para su paciente |
| Paciente | `ROLE_PACIENTE` | Email + contraseña | Enviar solicitud de cita, ver historial |

**Regla crítica de negocio**: Las citas NUNCA quedan confirmadas automáticamente. Siempre son solicitudes que la secretaria (ADMIN) acepta o rechaza.

---

## Autenticación — diseño detallado

### Flujo PACIENTE (email + password)
```
POST /api/auth/registro   { nombre, apellido, correo, contrasena, ... }
POST /api/auth/login      { correo, contrasena }
         ↓
JWT { rol: "ROLE_PACIENTE", idPaciente, correo }
```

### Flujo INSTITUCIONAL (OAuth2 Microsoft)
```
GET /api/auth/oauth2/microsoft
         ↓ redirige a Microsoft login
Usuario se autentica con @universidad.edu.co
         ↓ Microsoft hace callback con email
Sistema busca correo en BD:
  - Profesor.correo   → ROLE_DOCENTE
  - Estudiante.correo → ROLE_ESTUDIANTE
  - Usuario con ADMIN → ROLE_ADMIN
  - No encontrado     → 403 Acceso denegado
         ↓
JWT { rol, idUsuario, correo }
```

### Lógica clave del OAuth2
Los correos de profesores y estudiantes ya están pre-cargados en sus respectivas tablas por la secretaria. El sistema NO crea usuarios nuevos al hacer OAuth2 — solo valida que el correo exista y determina el rol por la tabla donde aparece.

### Credenciales Azure AD necesarias (pendiente de TI universitario)
- `client-id`
- `client-secret`
- `tenant-id`
- Redirect URI registrada: `http://localhost:8080/login/oauth2/code/microsoft`

---

## Dependencias pom.xml

```xml
<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Microsoft -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- DevTools -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## application.properties base

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/neovision_db
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# OAuth2 Microsoft (completar con datos de TI universitario)
spring.security.oauth2.client.registration.microsoft.client-id=PENDIENTE
spring.security.oauth2.client.registration.microsoft.client-secret=PENDIENTE
spring.security.oauth2.client.registration.microsoft.scope=openid,profile,email
spring.security.oauth2.client.provider.microsoft.authorization-uri=https://login.microsoftonline.com/TENANT_ID/oauth2/v2.0/authorize
spring.security.oauth2.client.provider.microsoft.token-uri=https://login.microsoftonline.com/TENANT_ID/oauth2/v2.0/token
spring.security.oauth2.client.provider.microsoft.user-info-uri=https://graph.microsoft.com/oidc/userinfo
spring.security.oauth2.client.provider.microsoft.jwk-set-uri=https://login.microsoftonline.com/TENANT_ID/discovery/v2.0/keys

# JWT
jwt.secret=CLAVE_SECRETA_MIN_256_BITS
jwt.expiration=86400000
```

---

## Orden de desarrollo acordado

```
1. Módulo Auth       ← EMPEZAMOS AQUÍ
   - Entidades: Usuario, Rol
   - Flujo paciente: registro + login + JWT
   - Flujo institucional: OAuth2 Microsoft + JWT
   - Filtro JWT para rutas protegidas
   - Rutas públicas vs protegidas por rol

2. Gestión de Usuarios (CRUD admin)

3. Profesor / Estudiante / Paciente (perfiles)

4. Espacio → Salon

5. PracticaSesion + Cupos

6. Citas (solicitud → aprobación → confirmación)

7. Patologías / PacientePatología

8. Dashboard por rol
```

---

## Notas importantes

- Un paciente puede ser estudiante de otra carrera en la vida real, pero en el sistema son cuentas completamente separadas (correo personal vs correo institucional).
- No usar `@Autowired` en campos — siempre inyección por constructor.
- No exponer entidades JPA directamente en Controllers — siempre usar DTOs.
- `GlobalExceptionHandler` con `@RestControllerAdvice` desde el inicio.
- Respuestas de API estandarizadas con `ApiResponse<T>`.
- Springdoc OpenAPI (Swagger) desde el día 1.
- El proyecto vive en: `C:\Users\jodan\OneDrive\Desktop\ThesisClaude\NeoVision`
