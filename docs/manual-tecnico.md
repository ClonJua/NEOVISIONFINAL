# MANUAL TÉCNICO Y DE OPERACIÓN
# Sistema de Información NeoVisión

**Gestión de práctica clínica y agendamiento — Programa de Optometría**
Universidad El Bosque

| | |
|---|---|
| Versión del documento | 1.0 |
| Fecha | 14 de septiembre de 2026 |
| Sistema | NeoVisión |
| Estructura | Guía para la Elaboración del Manual Técnico y de Operación de los Sistemas de Información (DNP, 2020) — lineamiento LI.SIS.16 de MinTIC |
| Documento complementario | Manual de Usuario (no reemplaza a este documento) |

---

## 1. DESCRIPCIÓN DEL SISTEMA DE INFORMACIÓN DESARROLLADO

### 1.1 Índice del contenido

| Sección | Contenido | 
|---|---|
| 1 | Descripción del sistema de información desarrollado |
| 1.1 | Índice del contenido |
| 1.2 | Introducción |
| 1.3 | Objetivos del sistema |
| 2 | Diseño técnico del sistema de información |
| 2.1 | Esquema o modelo de requerimientos |
| 2.2 | Software base del sistema y prerrequisitos |
| 2.3 | Componentes y estándares |
| 2.4 | Modelo de datos |
| 2.5 | Funcionalidad y servicios ofrecidos |
| 3 | Despliegue y configuración de los componentes |
| 3.1 | Organización de componentes |
| 3.2 | Instalación |
| 3.3 | Configuración |
| 3.4 | Despliegue |
| 4 | Resolución de problemas |
| 5 | Anexos — índice de diagramas |

### 1.2 Introducción

NeoVisión es un sistema de información web y móvil que soporta la operación de la clínica de práctica de Optometría: programa las **sesiones de práctica clínica**, gestiona el **agendamiento de citas** entre pacientes y estudiantes supervisados por un docente, y administra la **historia clínica electrónica** de cada paciente con los controles de integridad, trazabilidad y consentimiento que exige la normativa colombiana.

El sistema sustituye un proceso previamente manual (agendas en papel/hojas de cálculo e historias clínicas físicas), cuyo estado inicial está documentado en los diagramas BPMN AS-IS y cuyo estado objetivo corresponde a los BPMN TO-BE (ver Anexos).

**Módulos principales:**

| Módulo | Responsabilidad |
|---|---|
| Autenticación y usuarios | Registro de pacientes, inicio de sesión local y federado con Microsoft, gestión de roles |
| Administración | Alta/edición/inactivación de docentes, estudiantes y pacientes; carga masiva por Excel |
| Gestión de agenda | Espacios, salones, sesiones de práctica (puntuales y recurrentes), bloques de cita, reservas, cancelaciones y reprogramaciones |
| Historia clínica (EMR) | Apertura de historia, atenciones clínicas, flujo de revisión estudiante→docente, sellado por integridad, adendas, solicitudes de copia |
| Reportes y control académico | Indicadores de ocupación e inasistencia, distribución por estudiante, metas de pacientes por semestre, avance de semestre, exportación CSV/Excel/PDF |
| Notificaciones | Correo transaccional (confirmaciones y recordatorios) y notificaciones push a la app móvil |

### 1.3 Objetivos del sistema

**Objetivo general**

Proveer una plataforma que centralice la programación de la práctica clínica de Optometría, el agendamiento de pacientes y el registro de la historia clínica electrónica, garantizando la integridad, trazabilidad y confidencialidad de la información clínica conforme a la normativa vigente.

**Objetivos específicos**

1. Automatizar la creación de sesiones de práctica y la generación de bloques de cita, eliminando la programación manual y los cruces de horario.
2. Permitir a los pacientes reservar, cancelar y consultar sus citas desde la web o la aplicación móvil.
3. Digitalizar la historia clínica optométrica completa, con flujo de revisión y aprobación del docente supervisor.
4. Garantizar la integridad de los registros clínicos mediante sellado criptográfico y registro de auditoría de solo-anexado (Res. 1995/1999).
5. Asegurar el cumplimiento del régimen de protección de datos personales (Ley 1581/2012 y Dec. 1377/2013) mediante consentimiento informado explícito y trazabilidad de accesos.
6. Entregar a la coordinación académica indicadores de cumplimiento de metas de pacientes por estudiante y semestre.

---

## 2. DISEÑO TÉCNICO DEL SISTEMA DE INFORMACIÓN

### 2.1 Esquema o modelo de requerimientos

El resumen gráfico de requerimientos corresponde al **diagrama de casos de uso** (`docs/use-case-diagram.html`), que organiza las funcionalidades por actor. Los procesos de negocio están especificados en los **diagramas BPMN** (`docs/diagramas.pdf`).

**Actores del sistema**

| Actor | Descripción | Rol técnico |
|---|---|---|
| Administrador / Secretaría | Gestiona usuarios, agenda y reportes | `ROLE_ADMIN` |
| Docente | Supervisa sesiones, revisa y aprueba atenciones clínicas | `ROLE_DOCENTE` |
| Estudiante | Atiende pacientes y diligencia la historia clínica | `ROLE_ESTUDIANTE` |
| Paciente | Reserva y consulta sus citas, solicita copia de su historia | `ROLE_PACIENTE` |
| Microsoft Identity Platform | Sistema externo de autenticación federada | — |

**Reglas de negocio implementadas**

*Gestión de usuarios*

| # | Regla | Dónde se aplica |
|---|---|---|
| RN-01 | Docentes y estudiantes deben tener correo institucional `@unbosque.edu.co` | `ValidacionesUsuario.validarCorreoInstitucional` |
| RN-02 | Los correos se normalizan a minúsculas al guardarse (identificador de acceso) | `ValidacionesUsuario.normalizarCorreo` |
| RN-03 | La cédula es única dentro de cada rol | `AdminServiceImpl.validarCedulaUnica` |
| RN-04 | El teléfono debe ser celular colombiano: 10 dígitos que inicien en 3 | `ValidacionesUsuario.validarTelefono` |
| RN-05 | El tipo de documento debe corresponder a la edad: CC y CE ≥ 18 años, TI entre 7 y 17, RC < 7, PA cualquier edad | `ValidacionesUsuario.validarTipoDocumentoEdad` |
| RN-06 | Si el paciente es menor (TI o RC), el consentimiento lo otorga su representante legal, cuyos datos son obligatorios | `ValidacionesUsuario.validarRepresentanteLegalSiMenor` |
| RN-07 | El consentimiento de tratamiento de datos es obligatorio para registrarse y deja constancia de fecha y hora | `AuthServiceImpl.registro` |
| RN-08 | Los campos de texto no admiten caracteres peligrosos (`< > { } [ ] \`) ni emojis | `ValidacionesUsuario.validarTextoSeguro` |
| RN-09 | El semestre del estudiante debe estar entre 1 y 10 | CHECK `chk_estudiante_semestre` |
| RN-10 | Tras 5 intentos fallidos de inicio de sesión, la cuenta se bloquea 15 minutos | `LoginAttemptService` |

*Gestión de agenda y citas*

| # | Regla | Dónde se aplica |
|---|---|---|
| RN-11 | Una sesión de práctica no puede tener hora fin anterior o igual a la hora inicio | CHECK `chk_sesion_horario` |
| RN-12 | Una sesión debe tener al menos un cupo | CHECK `chk_sesion_cupos` |
| RN-13 | El número de bloques activos de una sesión no puede superar sus cupos totales | `CitaServiceImpl.crearBloque` |
| RN-14 | Un paciente es atendido por una pareja de estudiantes; la generación automática crea `min(cuposTotales, ⌈nEstudiantes/2⌉)` bloques | `SesionServiceImpl.generarBloquesAutomaticos` |
| RN-15 | Los dos estudiantes de un bloque deben ser distintos | CHECK `chk_cita_estudiantes_distintos` |
| RN-16 | No se pueden crear citas en sesiones de fechas pasadas | `CitaServiceImpl.crearBloque` |
| RN-17 | Un estudiante no puede tener dos citas en horarios que se crucen | `CitaServiceImpl.requireSinConflictoHorario` |
| RN-18 | Un paciente no puede tener más de una cita en estado `RESERVADO` simultáneamente | `CitaServiceImpl.requireSinCitaReservada` |
| RN-19 | Estados válidos de cita: `DISPONIBLE`, `RESERVADO`, `COMPLETADO`, `CANCELADO`, `VENCIDA` | CHECK `chk_cita_estado` |
| RN-20 | Un bloque `DISPONIBLE` no tiene paciente; uno `RESERVADO` o `COMPLETADO` siempre lo tiene | CHECK `chk_cita_paciente_segun_estado` |
| RN-21 | Las sesiones recurrentes no pueden programarse a más de 6 meses a futuro | `SesionServiceImpl.MAX_MESES_A_FUTURO` |
| RN-22 | Las citas cuya fecha ya pasó se marcan `VENCIDA` automáticamente cada 5 minutos | `CitaScheduler` |
| RN-23 | Se envían recordatorios automáticos 24 h y 1 h antes de la cita (una sola vez cada uno) | `RecordatorioScheduler` |
| RN-24 | Todo docente recibe automáticamente la especialidad marcada como `es_default` | `AdminServiceImpl.aplicarEspecialidades` |

*Historia clínica*

| # | Regla | Dónde se aplica |
|---|---|---|
| RN-25 | Estados válidos de atención: `BORRADOR`, `PENDIENTE_REVISION`, `DEVUELTA`, `FINALIZADA` | CHECK `chk_atencion_estado` |
| RN-26 | Una atención `FINALIZADA` debe tener hash de integridad y fecha de finalización (queda sellada) | CHECK `chk_atencion_finalizada_sellada` |
| RN-27 | El estudiante solo puede editar la atención hasta 2 días después de la fecha de la cita (principio de oportunidad, Res. 1995/1999) | `AtencionService.VENTANA_GRACIA_DIAS` |
| RN-28 | Una cita origina como máximo una atención | UNIQUE `uk_atencion_cita` |
| RN-29 | Un paciente tiene una sola historia clínica | UNIQUE `uk_historia_paciente` |
| RN-30 | Las correcciones posteriores al sellado se registran como adenda, nunca sobrescribiendo el original | `AtencionService` / entidad `Adenda` |
| RN-31 | El registro de auditoría es de solo-anexado: no admite `UPDATE` ni `DELETE` | Trigger `trg_auditoria_solo_anexar` |
| RN-32 | El acceso a una historia se restringe a su estudiante autor, el docente supervisor y el administrador | `HistoriaAccessGuard` |
| RN-33 | Los diagnósticos se codifican contra CIE-10 y los procedimientos contra CUPS | FK `fk_diagnostico_cie10`, `fk_procedimiento_cups` |
| RN-34 | No se puede eliminar una cita que ya tenga una atención asociada | FK `fk_atencion_cita` (RESTRICT) |

### 2.2 Software base del sistema y prerrequisitos

**Requerimientos de hardware — servidor de aplicaciones**

| Recurso | Mínimo | Recomendado (producción) |
|---|---|---|
| CPU | 1 vCPU compartida | 1–2 vCPU dedicadas |
| Memoria RAM | 1 GB | 1,75–3,5 GB |
| Almacenamiento | 1 GB (el artefacto pesa ~92 MB) | 10 GB |
| Equivalente en Azure App Service | F1 (Free) — **solo desarrollo/demo** | B1 o superior |

> **Nota de operación:** el plan F1 tiene un tope de 60 minutos de CPU por día, no admite *Always On* (la aplicación se descarga tras 20 minutos de inactividad y el siguiente arranque tarda 25–70 s) y su cuota de memoria de 1 GB queda al límite del heap configurado (`-Xmx1024M`). No es apto para uso concurrente real.

**Requerimientos de hardware — cliente**

| Recurso | Mínimo |
|---|---|
| Equipo de escritorio | Cualquiera capaz de ejecutar un navegador moderno; 4 GB RAM |
| Dispositivo móvil | Android con soporte para Expo SDK 54 (Android 7.0+) |
| Conexión | Banda ancha estándar; no se transfieren archivos pesados |

**Sistema operativo de los servidores**

| Nodo | Sistema operativo |
|---|---|
| Servidor de aplicaciones | Linux (contenedor oficial Azure App Service `java:21.0.9`) |
| Servidor de base de datos | Administrado por Neon (PostgreSQL 16 sobre infraestructura AWS, región `us-east-2`) |

**Servidores de base de datos admitidos**

| Motor | Versión | Observación |
|---|---|---|
| PostgreSQL | 16 (probado con 16.15) | Motor soportado oficialmente |
| PostgreSQL | 14 o superior | Compatible; las migraciones usan `GENERATED BY DEFAULT AS IDENTITY`, PL/pgSQL y `ALTER TABLE ... NOT VALID` |

> El sistema **no es portable a MySQL/MariaDB** sin reescribir las migraciones: usan funciones PL/pgSQL y restricciones `NOT VALID`/`VALIDATE CONSTRAINT`, exclusivas de PostgreSQL.

**Servidor de aplicaciones y servidor web**

| Componente | Versión | Observación |
|---|---|---|
| Servidor de aplicaciones | Apache Tomcat 11 embebido | Empaquetado dentro del jar ejecutable; no requiere instalación aparte |
| Servidor web / contenedor | Azure App Service para Linux | Sirve HTTPS en el puerto 443 y enruta al puerto interno de la aplicación |
| Runtime | OpenJDK / Microsoft Build of OpenJDK 21 | — |

**Navegadores compatibles**

Compilación con Vite 6 (objetivo ES2020+). Se soportan las dos últimas versiones estables de:

| Navegador | Versión mínima |
|---|---|
| Google Chrome | 87+ |
| Microsoft Edge | 88+ |
| Mozilla Firefox | 78+ |
| Safari | 14+ |

> No se soporta Internet Explorer. La autenticación con Microsoft (MSAL) requiere cookies de terceros y almacenamiento local habilitados.

**Lenguajes de programación**

| Lenguaje | Uso |
|---|---|
| Java 21 | Backend (API REST, lógica de negocio, persistencia) |
| TypeScript 5.9 | Frontend web y aplicación móvil |
| SQL (PostgreSQL) / PL/pgSQL | Migraciones de esquema, restricciones y triggers |
| Bash | Script de despliegue |

### 2.3 Componentes y estándares

**Librerías y frameworks — Backend**

| Componente | Versión | Propósito |
|---|---|---|
| Spring Boot | 4.0.5 | Framework base, inyección de dependencias, autoconfiguración |
| Spring Web MVC | 7.0.6 | API REST |
| Spring Data JPA | 4.0.4 | Repositorios y acceso a datos |
| Hibernate ORM | 7.2.7 | Mapeo objeto-relacional |
| Spring Security | 7.0.4 | Autenticación, autorización, cabeceras de seguridad |
| jjwt | 0.12.6 | Emisión y validación de JSON Web Tokens |
| Flyway | 11.14.1 | Versionado y migración del esquema de base de datos |
| HikariCP | 7.0.2 | Pool de conexiones JDBC |
| PostgreSQL JDBC Driver | 42.7.10 | Conectividad con la base de datos |
| ShedLock | 5.13.0 | Bloqueo distribuido para tareas programadas |
| Apache POI | 5.3.0 | Lectura/escritura de archivos Excel (carga masiva y exportación) |
| OpenPDF / iText | 1.3.30 / 5.5.13 | Generación de PDF (historia clínica, reportes) |
| springdoc-openapi | 2.8.6 | Documentación interactiva de la API (Swagger UI) |
| Caffeine | 3.2.3 | Caché en memoria |
| Jakarta Mail (Angus) | 2.0.5 | Envío de correo SMTP |
| Lombok | — | Reducción de código repetitivo en entidades y DTOs |

**Librerías y frameworks — Frontend web**

| Componente | Versión | Propósito |
|---|---|---|
| React | 19.2 | Biblioteca de interfaz de usuario |
| Vite | 6.4 | Empaquetador y servidor de desarrollo |
| TypeScript | 5.9 | Tipado estático |
| React Router DOM | 7.13 | Enrutamiento del lado del cliente |
| Axios | 1.14 | Cliente HTTP |
| MSAL Browser / MSAL React | 5.6 / 5.2 | Autenticación con Microsoft |
| Tailwind CSS | 3.4 / 4.2 (plugin Vite) | Sistema de estilos utilitario |
| react-big-calendar | 1.19 | Vista de calendario de la agenda |
| date-fns / moment | 4.1 / 2.29 | Manejo de fechas |
| ESLint | 9.39 | Análisis estático de código |

**Librerías y frameworks — Aplicación móvil**

| Componente | Versión | Propósito |
|---|---|---|
| React Native | 0.81.5 | Framework de aplicación móvil nativa |
| Expo SDK | ~54.0 | Plataforma de desarrollo y despliegue |
| EAS (Expo Application Services) | — | Compilación y distribución de binarios |

**Estándares de codificación y patrones de diseño**

| Estándar / patrón | Aplicación |
|---|---|
| Arquitectura en capas | `Controller → Service → Repository`, con separación estricta de responsabilidades |
| Repository | Repositorios Spring Data JPA por agregado |
| DTO (Data Transfer Object) | Objetos `*Request` / `*Response` separados de las entidades; las entidades nunca se exponen por la API |
| Inyección de dependencias por constructor | Todos los servicios y controladores |
| Value Object / `@Embedded` | El formulario clínico se compone de ~13 objetos embebidos (`Anamnesis`, `AgudezaVisual`, `Refraccion`, ...) |
| Bloqueo optimista | `@Version` en `Cita` y `Atencion` para evitar escrituras concurrentes perdidas |
| Respuesta uniforme de API | Envoltorio `ApiResponse<T>` con `mensaje` + `data` en todos los endpoints |
| Manejo centralizado de errores | `@RestControllerAdvice` en `GlobalExceptionHandler` |
| Migraciones versionadas e inmutables | Convención Flyway `V{n}__descripcion.sql`; una migración aplicada nunca se edita |
| Nomenclatura de base de datos | `snake_case` singular para tablas, prefijo `id_` para claves, prefijo por sección en columnas del formulario clínico |
| Convención de código Java | Convenciones estándar de Oracle; paquetes por dominio, no por capa técnica |
| Convención de código TypeScript | ESLint con `typescript-eslint` y `react-hooks` |
| REST | Sustantivos en plural, verbos HTTP semánticos, códigos de estado estándar |

**Estándares de identidad corporativa y *look & feel***

La interfaz sigue la identidad visual institucional de la Universidad El Bosque. La capa de estilos se implementa con Tailwind CSS mediante clases utilitarias, sin hojas de estilo ad-hoc por pantalla, lo que mantiene consistencia tipográfica, de color y de espaciado en toda la aplicación. La aplicación móvil replica la misma paleta y jerarquía visual.

**Puertos de comunicación**

| Origen | Destino | Puerto | Protocolo |
|---|---|---|---|
| Navegador / App móvil | Azure App Service | 443 | HTTPS · REST/JSON · Bearer JWT |
| Navegador / App móvil | `login.microsoftonline.com` | 443 | HTTPS · OAuth 2.0 / OIDC |
| Backend | Neon PostgreSQL | 5432 | JDBC sobre TLS (`sslmode=require`) |
| Backend | `smtp.gmail.com` | 587 | SMTP con STARTTLS |
| Backend | Expo Push Service | 443 | HTTPS |
| Desarrollo local | Backend | 8080 | HTTP (`server.port`) |
| Desarrollo local | Vite dev server | 5173 | HTTP con proxy hacia `:8080` |

**Protocolos de seguridad**

| Control | Implementación |
|---|---|
| Cifrado en tránsito | TLS 1.2 mínimo en todos los saltos (`minTlsVersion=1.2`) |
| HSTS | `Strict-Transport-Security`, `max-age=31536000`, `includeSubDomains` |
| Content Security Policy | `default-src 'self'`; `connect-src` incluye `login.microsoftonline.com`; `frame-ancestors 'none'` |
| Autenticación local | JWT firmado (HS256), vigencia 8 horas; el secreto debe tener ≥ 32 caracteres o la aplicación no arranca |
| Autenticación federada | OAuth 2.0 / OpenID Connect vía MSAL contra Microsoft Identity Platform |
| Autorización | RBAC por rol, evaluado por `SecurityConfig` y reforzado a nivel de servicio |
| Contraseñas | Hash BCrypt; nunca se almacenan ni se registran en texto plano |
| Sesión | Sin estado (`SessionCreationPolicy.STATELESS`); no se usan cookies de sesión |
| Bloqueo por fuerza bruta | 5 intentos fallidos → bloqueo de 15 minutos |
| Revocación de token | Lista negra de tokens al cerrar sesión (`TokenBlacklistService`) |
| CORS | Orígenes explícitos por variable de entorno; sin comodines |
| Carga de archivos | Límite de 5 MB por archivo y por petición |

**Interfaces con otros sistemas**

| Sistema externo | Tipo de interfaz | Uso |
|---|---|---|
| Microsoft Identity Platform | OAuth 2.0 / OIDC (MSAL, `authority` `/common`) | Inicio de sesión de docentes y estudiantes con cuenta institucional |
| Gmail SMTP | SMTP autenticado, puerto 587 | Confirmaciones de reserva, recordatorios, envío de reportes |
| Expo Push Service | API HTTPS | Notificaciones push a la aplicación móvil |
| Catálogo CIE-10 | Carga interna (`Cie10Seeder`) | Codificación de diagnósticos |
| Catálogo CUPS | Carga interna (`CupsSeeder`) | Codificación de procedimientos |

### 2.4 Modelo de datos

**Modelo lógico y físico**

El modelo entidad-relación completo está en `docs/er-diagram.html`, generado directamente desde las migraciones Flyway (V1–V4), lo que garantiza correspondencia exacta con el esquema en producción.

El esquema tiene **23 tablas de dominio** más la tabla técnica `shedlock` (bloqueo de tareas programadas, sin valor de negocio). Se organiza en cuatro bloques:

| Bloque | Tablas |
|---|---|
| Identidad y acceso | `rol`, `usuario`, `docente`, `estudiante`, `paciente` |
| Catálogos y recursos físicos | `especialidad`, `docente_especialidad`, `espacio`, `salon`, `cie10`, `cups`, `meta_pacientes_semestre` |
| Agenda | `sesion_practica`, `sesion_estudiante`, `cita` |
| Historia clínica y trazabilidad | `historia_clinica`, `atencion`, `diagnostico`, `procedimiento`, `adenda`, `solicitud_historia`, `auditoria` |

**Decisión de diseño — tabla `atencion`**

La tabla `atencion` concentra ~190 columnas correspondientes al formulario clínico optométrico completo. Es una decisión deliberada:

- El patrón de acceso es *todo o nada*: una atención siempre se lee y se escribe completa, nunca por secciones aisladas. Normalizarla en 13 tablas hijas 1:1 añadiría 13 `JOIN` a cada lectura sin ningún beneficio de consulta.
- A nivel de código **no** es una clase plana: la entidad `Atencion` se compone de ~13 objetos `@Embedded`, de modo que la modularidad relevante para mantenibilidad se conserva en el modelo de dominio. Las columnas anchas son el aplanamiento que hace Hibernate.
- Las columnas llevan prefijo por sección, lo que preserva la agrupación semántica en el esquema físico.
- Contrapartida aceptada: menor analizabilidad del DDL (bloque extenso en `V1`) y dificultad para versionar una sección de forma independiente.

**Diccionario de datos**

*rol* — catálogo de roles del sistema

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_rol | INTEGER (PK, identidad) | No | Identificador |
| nombre_rol | VARCHAR(30) UNIQUE | No | `ROLE_ADMIN`, `ROLE_DOCENTE`, `ROLE_ESTUDIANTE`, `ROLE_PACIENTE` |

*usuario* — cuenta de acceso; un usuario tiene exactamente un rol y un perfil

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_usuario | INTEGER (PK) | No | Identificador |
| id_rol | INTEGER (FK → rol) | No | Rol asignado |
| correo | VARCHAR(100) UNIQUE | No | Identificador de inicio de sesión, siempre en minúsculas |
| contrasena | VARCHAR(255) | Sí | Hash BCrypt. `NULL` en cuentas que solo entran por Microsoft |
| estado | BOOLEAN | No | `true` activo, `false` inactivo |
| expo_push_token | VARCHAR(255) | Sí | Token de notificaciones push del dispositivo |
| firma | TEXT | Sí | Firma digitalizada en base64 |

*docente*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_docente | INTEGER (PK) | No | Identificador |
| id_usuario | INTEGER (FK → usuario, UNIQUE) | No | Cuenta asociada |
| nombre / apellido | VARCHAR(100) | No | Nombres y apellidos |
| cedula | VARCHAR(20) UNIQUE | Sí | Documento de identidad |
| correo | VARCHAR(100) UNIQUE | No | Correo institucional |
| telefono | VARCHAR(20) | Sí | Celular de contacto |

*estudiante*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_estudiante | INTEGER (PK) | No | Identificador |
| id_usuario | INTEGER (FK → usuario, UNIQUE) | No | Cuenta asociada |
| nombre / apellido | VARCHAR(100) | No | Nombres y apellidos |
| cedula | VARCHAR(20) UNIQUE | No | Documento de identidad |
| correo | VARCHAR(100) UNIQUE | No | Correo institucional |
| semestre | INTEGER | No | Semestre cursado (1–10) |
| telefono | VARCHAR(20) | Sí | Celular de contacto |

*paciente*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_paciente | INTEGER (PK) | No | Identificador |
| id_usuario | INTEGER (FK → usuario, UNIQUE) | No | Cuenta asociada |
| nombre / apellido | VARCHAR(100) | No | Nombres y apellidos |
| tipo_documento | VARCHAR(20) | No | `CC`, `TI`, `RC`, `CE`, `PA` |
| numero_documento | VARCHAR(30) UNIQUE | No | Número de documento |
| fecha_nacimiento | DATE | No | No puede ser futura |
| telefono | VARCHAR(20) | No | Celular de contacto |
| correo | VARCHAR(100) UNIQUE | No | Correo del paciente |
| autoriza_tratamiento_datos | BOOLEAN | No | Consentimiento (Ley 1581/2012) |
| fecha_autorizacion_datos | TIMESTAMP | Sí | Obligatoria si autorizó |
| representante_nombre | VARCHAR(150) | Sí | Representante legal (menores) |
| representante_documento | VARCHAR(30) | Sí | Documento del representante |
| representante_parentesco | VARCHAR(50) | Sí | Parentesco |

*especialidad* / *docente_especialidad*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_especialidad | INTEGER (PK) | No | Identificador |
| nombre | VARCHAR(60) UNIQUE | No | Nombre de la especialidad |
| activo | BOOLEAN | No | Disponible para asignación |
| es_default | BOOLEAN | No | Se asigna a todo docente automáticamente |
| requiere_subtipo | BOOLEAN | No | Exige `subtipo_control` en la cita |
| observacion_recordatorio | VARCHAR(255) | Sí | Texto añadido al correo de recordatorio |

`docente_especialidad` es la tabla puente (PK compuesta `id_docente` + `id_especialidad`).

*espacio* / *salon*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_espacio | INTEGER (PK) | No | Identificador del espacio (sede/piso/área) |
| nombre / descripcion | VARCHAR | No / Sí | Denominación |
| activo | BOOLEAN | No | Disponible |
| id_salon | INTEGER (PK) | No | Identificador del salón/consultorio |
| id_espacio | INTEGER (FK → espacio) | No | Espacio que lo contiene |

*sesion_practica* / *sesion_estudiante*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_sesion | INTEGER (PK) | No | Identificador |
| id_docente | INTEGER (FK → docente) | No | Docente supervisor |
| id_salon | INTEGER (FK → salon) | Sí | Consultorio asignado |
| fecha | DATE | No | Día de la sesión |
| hora_inicio / hora_fin | TIME | Sí | Franja horaria (`hora_fin > hora_inicio`) |
| cupos_totales | INTEGER | No | Máximo de bloques de cita (≥ 1) |
| activo | BOOLEAN | No | Sesión vigente |

`sesion_estudiante` es la tabla puente entre sesión y estudiantes asignados (PK compuesta, borrado en cascada desde la sesión).

*cita* — bloque de atención

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_cita | INTEGER (PK) | No | Identificador |
| id_sesion | INTEGER (FK → sesion_practica) | No | Sesión a la que pertenece |
| id_estudiante | INTEGER (FK → estudiante) | No | Estudiante principal |
| id_estudiante_2 | INTEGER (FK → estudiante) | Sí | Segundo estudiante de la pareja |
| id_paciente | INTEGER (FK → paciente) | Sí | Paciente asignado |
| id_especialidad | INTEGER (FK → especialidad) | Sí | Especialidad clínica |
| estado | VARCHAR(20) | No | `DISPONIBLE`, `RESERVADO`, `COMPLETADO`, `CANCELADO`, `VENCIDA` |
| subtipo_control | VARCHAR(50) | Sí | Subtipo cuando la especialidad lo exige |
| observaciones | VARCHAR(255) | Sí | Notas del bloque |
| fecha_reserva | TIMESTAMP | Sí | Momento de la reserva |
| recordatorio_24h_enviado | BOOLEAN | No | Control de envío único |
| recordatorio_1h_enviado | BOOLEAN | No | Control de envío único |
| version | BIGINT | Sí | Bloqueo optimista |

*historia_clinica*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_historia | INTEGER (PK) | No | Identificador |
| id_paciente | INTEGER (FK → paciente, UNIQUE) | No | Paciente titular |
| numero_historia | VARCHAR(50) UNIQUE | Sí | Número institucional de historia |
| estado | VARCHAR(20) | No | Estado de la historia |
| fecha_apertura | TIMESTAMP | No | Fecha de apertura |
| abierta_por_correo | VARCHAR(100) | Sí | Correo de quien la abrió (texto, para conservar trazabilidad) |

*atencion* — registro clínico de una visita

| Columna / grupo | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_atencion | INTEGER (PK) | No | Identificador |
| id_historia | INTEGER (FK → historia_clinica) | No | Historia a la que pertenece |
| id_cita | INTEGER (FK → cita, UNIQUE) | Sí | Cita que la originó |
| id_estudiante | INTEGER (FK → estudiante) | No | Estudiante que la diligencia |
| id_docente_supervisor | INTEGER (FK → docente) | Sí | Docente que la revisa y aprueba |
| estado | VARCHAR(30) | No | `BORRADOR`, `PENDIENTE_REVISION`, `DEVUELTA`, `FINALIZADA` |
| motivo_consulta | TEXT | Sí | Motivo de consulta |
| motivo_devolucion | TEXT | Sí | Razón de devolución del docente |
| fecha_atencion | TIMESTAMP | No | Fecha de la atención |
| fecha_envio_revision | TIMESTAMP | Sí | Envío a revisión |
| fecha_finalizacion | TIMESTAMP | Sí | Sellado definitivo |
| hash_integridad | VARCHAR(64) | Sí | Hash SHA-256 del contenido sellado |
| firma_estudiante / firma_docente / firma_paciente | TEXT | Sí | Firmas digitalizadas en base64 |
| created_at / updated_at | TIMESTAMP | No / Sí | Auditoría técnica |
| version | BIGINT | Sí | Bloqueo optimista |
| `dg_*` (25 columnas) | VARCHAR / TEXT | Sí | **Datos generales**: identificación, contacto, EPS, acompañante, responsable |
| `an_*` (21 columnas) | VARCHAR / TEXT / BOOLEAN | Sí | **Anamnesis**: antecedentes y sintomatología |
| `av_*` (20 columnas) | VARCHAR(20–50) | Sí | **Agudeza visual**: con y sin corrección, lejos/cerca/estenopeico |
| `lm_*` (19 columnas) | VARCHAR | Sí | **Lensometría**: corrección actual del paciente |
| `qm_*` (6 columnas) | VARCHAR / TEXT | Sí | **Queratometría** |
| `rf_*` (36 columnas) | VARCHAR / TEXT | Sí | **Refracción**: estática, dinámica, subjetiva, balanceada, afinada, ciclopléjica, adición |
| `ee_*` (31 columnas) | VARCHAR / TEXT | Sí | **Examen externo**: anexos, segmento anterior, tonometría, Schirmer, diagramas base64 |
| `mt_*` (22 columnas) | VARCHAR / TEXT | Sí | **Motilidad**: ducciones, versiones, reflejos pupilares, PPC, diagramas base64 |
| `fo_*` (12 columnas) | VARCHAR / TEXT | Sí | **Fondo de ojo**: papila, mácula, medios, relación arteria/vena, diagramas base64 |
| `pc_*` (15 columnas) | VARCHAR / TEXT | Sí | **Pruebas complementarias**: campo visual, estereopsis, forias, visión cromática, sensibilidad |
| `ff_*` (43 columnas) | VARCHAR / TEXT | Sí | **Fórmula final**: prescripción lejos/media/cerca/adición, material, filtros, uso |
| `cd_*` (3 columnas) | TEXT | Sí | **Conducta**: manejo, control y remisiones |
| `sv_*` (4 columnas) | VARCHAR | Sí | **Signos vitales**: peso, estatura, IMC, tensión arterial |

*diagnostico* / *procedimiento*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_diagnostico / id_procedimiento | INTEGER (PK) | No | Identificador |
| id_atencion | INTEGER (FK → atencion, CASCADE) | No | Atención a la que pertenece |
| codigo_cie10 / codigo_cups | VARCHAR(20) (FK) | Sí | Código del catálogo |
| descripcion | TEXT | Sí | Descripción libre |
| orden | INTEGER | Sí | Orden de presentación (principal, relacionados) |

*adenda* — corrección posterior al sellado

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_adenda | INTEGER (PK) | No | Identificador |
| id_atencion | INTEGER (FK → atencion, RESTRICT) | No | Atención corregida |
| autor_correo | VARCHAR(100) | No | Autor de la adenda |
| autor_rol | VARCHAR(30) | No | Rol del autor |
| fecha | TIMESTAMP | No | Fecha de la adenda |
| texto | TEXT | No | Contenido de la corrección |

*solicitud_historia* — solicitud de copia por parte del paciente

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_solicitud | INTEGER (PK) | No | Identificador |
| id_paciente | INTEGER (FK → paciente) | No | Solicitante |
| estado | VARCHAR(20) | No | Estado de la solicitud |
| motivo | TEXT | Sí | Motivo del paciente |
| motivo_rechazo | TEXT | Sí | Justificación en caso de rechazo |
| fecha_solicitud / fecha_resolucion | TIMESTAMP | No / Sí | Fechas del trámite |
| resuelta_por_correo | VARCHAR(100) | Sí | Quien la resolvió |

*auditoria* — registro de solo-anexado (Ley 1581/2012)

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id_auditoria | BIGINT (PK) | No | Identificador |
| usuario_correo | VARCHAR(100) | No | Actor (texto, sobrevive al borrado de la cuenta) |
| rol | VARCHAR(30) | Sí | Rol del actor |
| accion | VARCHAR(40) | No | `CREAR_HISTORIA`, `REGISTRAR_CONSENTIMIENTO`, `ABRIR_ATENCION`, `GUARDAR_BORRADOR`, `ENVIAR_REVISION`, `DEVOLVER`, `FINALIZAR`, `CREAR_ADENDA`, `CONSULTAR` |
| entidad / entidad_id | VARCHAR(40) / INTEGER | Sí | Recurso afectado |
| id_paciente | INTEGER | Sí | Paciente involucrado |
| detalle | TEXT | Sí | Detalle de la acción |
| fecha_hora | TIMESTAMP | No | Momento del evento |
| ip / user_agent | VARCHAR | Sí | Origen de la petición |
| hash_previo / hash_registro | VARCHAR(64) | Sí | Encadenamiento de integridad |

*cie10* / *cups* / *meta_pacientes_semestre*

| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| codigo | VARCHAR(10) (PK) | No | Código del catálogo |
| descripcion | VARCHAR(250) | No | Descripción oficial |
| categoria | VARCHAR(120) | Sí | Agrupación |
| activo | BOOLEAN | No | Disponible para selección |
| semestre | INTEGER (PK) | No | Semestre (1–10) |
| minimo_pacientes | INTEGER | No | Meta de pacientes para avanzar (≥ 0) |

### 2.5 Funcionalidad y servicios ofrecidos

**Mapa de navegación por rol**

| Rol | Rutas principales de la SPA |
|---|---|
| Público | `/login`, `/registro` |
| Administrador | `/admin` → pestañas Docentes, Estudiantes, Pacientes, Sesiones, Citas, Espacios, Reportes, Solicitudes |
| Docente | `/docente` → agenda, atenciones pendientes de revisión, historias |
| Estudiante | `/estudiante` → agenda propia, atenciones en curso, progreso hacia la meta |
| Paciente | `/dashboard` → mis citas, reservar cita, solicitar copia de historia |

**Proceso: agendamiento de una cita**

1. El administrador crea un **espacio** y sus **salones** (una sola vez).
2. Crea una **sesión de práctica**: docente supervisor, salón, fecha, franja horaria, cupos y lista de estudiantes asignados.
3. El sistema genera automáticamente los **bloques de cita** en estado `DISPONIBLE`, emparejando estudiantes consecutivos (RN-14).
4. El paciente (o el administrador en su nombre) **reserva** un bloque: pasa a `RESERVADO`, se registra `fecha_reserva` y se envía correo de confirmación a paciente, estudiantes y docente.
5. El sistema envía **recordatorios automáticos** 24 h y 1 h antes (RN-23).
6. Tras la cita, el bloque pasa a `COMPLETADO`; si nadie asistió y la fecha ya pasó, el planificador lo marca `VENCIDA` (RN-22).

**Proceso: registro de historia clínica**

1. Se abre la **historia clínica** del paciente (una sola por paciente, RN-29).
2. A partir de la cita, el estudiante **abre la atención**, que nace en estado `BORRADOR`.
3. El estudiante **diligencia** el formulario por secciones y guarda borradores parciales; puede editar hasta 2 días después de la fecha de la cita (RN-27).
4. Al terminar, **envía a revisión** → `PENDIENTE_REVISION`.
5. El docente supervisor **revisa**: la **devuelve** con observaciones (`DEVUELTA`) o la **finaliza**.
6. Al finalizar, el sistema calcula el **hash de integridad**, registra `fecha_finalizacion` y la atención queda **sellada** e inmutable (RN-26).
7. Cualquier corrección posterior se registra como **adenda**, preservando el original (RN-30).
8. Toda acción sobre la historia queda registrada en `auditoria` de forma inmutable (RN-31).

**Servicios REST expuestos**

Documentación interactiva autogenerada: `/swagger-ui.html` (requiere `SWAGGER_ENABLED=true`). Especificación OpenAPI: `/v3/api-docs`.

| Base path | Controlador | Rol requerido | Servicios |
|---|---|---|---|
| `/api/auth` | AuthController | Público | Inicio de sesión, registro de paciente, inicio de sesión con Microsoft, cierre de sesión |
| `/api/usuario` | UsuarioController | Autenticado | Perfil propio, firma, registro de token push |
| `/api/admin` | AdminController | `ROLE_ADMIN` | CRUD de docentes, estudiantes y pacientes; carga masiva por Excel; reactivación |
| `/api/admin/citas` | CitaAdminController | `ROLE_ADMIN` | Crear/editar/eliminar bloques, cambiar estado, reprogramar, historial por paciente, indicadores, distribución, ocupación, inasistencias, metas por semestre, avance de semestre, exportación CSV/Excel/PDF y envío por correo |
| `/api/admin/sesiones` | SesionController | `ROLE_ADMIN` | Crear sesión, crear sesiones recurrentes, listar, editar, desactivar, eliminar |
| `/api/admin/espacios` | EspacioController | `ROLE_ADMIN` | CRUD de espacios y salones |
| `/api/admin/solicitudes` | SolicitudAdminController | `ROLE_ADMIN` | Listar y resolver solicitudes de copia de historia |
| `/api/docente` | DocenteController | `ROLE_DOCENTE` | Agenda propia, sesiones, citas supervisadas |
| `/api/estudiante` | CitaEstudianteController | `ROLE_ESTUDIANTE` | Citas asignadas, progreso hacia la meta |
| `/api/paciente` | CitaPacienteController | `ROLE_PACIENTE` | Mis citas, reservar, cancelar, reprogramar |
| `/api/paciente/solicitudes` | SolicitudPacienteController | `ROLE_PACIENTE` | Crear y consultar solicitudes de copia |
| `/api/historias` | HistoriaClinicaController | `ROLE_ESTUDIANTE`, `ROLE_DOCENTE`, `ROLE_ADMIN` | Abrir y consultar historia, verificación de integridad, exportación PDF |
| `/api/historias/atenciones` | AtencionController | `ROLE_ESTUDIANTE`, `ROLE_DOCENTE`, `ROLE_ADMIN` | Abrir, guardar borrador, enviar a revisión, devolver, finalizar, crear adenda |
| `/api/historias/cie10` · `/cups` | Cie10Controller · CupsController | Autenticado clínico | Búsqueda en catálogos |
| `/api` | EspecialidadController | Autenticado | Listado de especialidades |

Todos los endpoints responden con el envoltorio uniforme `ApiResponse<T>`:

```json
{ "mensaje": "Cita reservada", "data": { "idCita": 42, "estado": "RESERVADO" } }
```

**Atributos de calidad atendidos**

| Atributo (ISO/IEC 25010) | Mecanismo |
|---|---|
| Seguridad | RBAC, JWT, BCrypt, HSTS, CSP, bloqueo por fuerza bruta, lista negra de tokens |
| Fiabilidad | Bloqueo optimista, transacciones, restricciones de integridad en la base de datos, ShedLock para evitar ejecución duplicada de tareas |
| Mantenibilidad | Arquitectura en capas, paquetes por dominio, migraciones versionadas, pruebas unitarias por servicio |
| Rendimiento | Conteos mediante `COUNT` en lugar de cargar entidades, `@NamedEntityGraph` para evitar N+1, caché Caffeine, endpoints paginados |
| Usabilidad | Formulario clínico seccionado, calendario visual, mensajes de error de dominio en español |
| Cumplimiento normativo | Res. 1995/1999, Ley 1581/2012, Dec. 1377/2013 |

---

## 3. DESPLIEGUE Y CONFIGURACIÓN DE LOS COMPONENTES

### 3.1 Organización de componentes

Arquitectura física y lógica documentada en:

| Diagrama | Archivo |
|---|---|
| Arquitectura general | `docs/architecture-diagram.html` |
| Componentes | `docs/component-diagram.html` |
| Despliegue | `docs/deployment-diagram.html` |
| Clases relevantes | `docs/class-diagram.html` |
| Servicios expuestos | Tabla de la sección 2.5 y Swagger UI (`/swagger-ui.html`) |

No aplican coreografías ni orquestación de servicios (el sistema no integra un bus de servicios ni motor BPM), ni modelo dimensional (no existe *data warehouse*; los reportes se calculan sobre el modelo transaccional).

**Estructura del repositorio**

```
neovision/                         Backend (Maven, Java 21)
  src/main/java/com/neovision/
    admin/                         Gestión de docentes, estudiantes y pacientes
      controller/ dto/ service/
    auth/                          Autenticación, JWT, login Microsoft
      controller/ dto/ service/
    clinico/                       Agenda clínica
      controller/ dto/ entity/ repository/ service/ scheduler/ config/
      historia/                    Historia clínica electrónica
        controller/ dto/ entity/ embeddable/ repository/ service/
    common/                        Transversal
      config/ dto/ exception/ service/ validation/
    usuario/                       Entidad Usuario y perfil propio
  src/main/resources/
    application.properties         Configuración y variables de entorno
    db/migration/                  Migraciones Flyway V1–V4
    static/                        Build del frontend (generado, no editar)
  src/test/java/                   Pruebas unitarias por servicio
  pom.xml

neovision-frontend/                Frontend web (Vite + React + TypeScript)
  src/api/                         Clientes HTTP por dominio
  src/auth/                        Configuración MSAL
  src/components/                  Componentes reutilizables
  src/pages/                       Pantallas por rol
  package.json · vite.config.ts

neovision-mobile/                  App móvil (Expo + React Native)
  src/ · app.config.js · eas.json

docs/                              Manual técnico y diagramas
deploy-azure.sh                    Script de despliegue
```

### 3.2 Instalación

**Prerrequisitos**

| Software | Versión | Uso |
|---|---|---|
| JDK | 21 | Compilación y ejecución del backend |
| Node.js | 20 LTS o superior | Compilación del frontend |
| PostgreSQL | 16 (o 14+) | Base de datos |
| Git | Cualquiera reciente | Clonado del repositorio |
| Azure CLI | Reciente | Solo para desplegar a Azure |

Maven no requiere instalación: el repositorio incluye el *wrapper* (`mvnw` / `mvnw.cmd`).

**Instalación de la base de datos**

El esquema **no se instala manualmente**. Flyway ejecuta las migraciones automáticamente al arrancar la aplicación:

| Script | Contenido |
|---|---|
| `V1__estructura_base.sql` | Tablas, columnas y claves primarias (23 tablas de dominio + `shedlock`) |
| `V2__reconciliar_columnas.sql` | Reconciliación de columnas para bases creadas antes de adoptar Flyway |
| `V3__integridad_relacional.sql` | Claves foráneas, restricciones únicas, CHECK, índices y triggers |
| `V4__correos_a_minusculas.sql` | Normalización de correos existentes a minúsculas |

Pasos:

```bash
# 1. Crear la base de datos vacía
createdb -U postgres neovision_db

# 2. Configurar la conexión en application.properties o por variables de entorno

# 3. Arrancar la aplicación: Flyway crea el esquema
cd neovision && ./mvnw spring-boot:run
```

En el log debe aparecer `Successfully applied N migrations to schema "public"` seguido de `Started NeovisionApplication`.

> **Bases preexistentes:** `spring.flyway.baseline-on-migrate=true` con `baseline-version=0` permite que una base creada antes de adoptar Flyway (mediante `ddl-auto=update`) converja al mismo esquema sin recrearse.

**Carga inicial de datos**

Se ejecuta automáticamente al arrancar, de forma idempotente:

| Componente | Datos cargados |
|---|---|
| `Cie10Seeder` | Catálogo de diagnósticos CIE-10 |
| `CupsSeeder` | Catálogo de procedimientos CUPS |
| `EspecialidadSeeder` | Especialidades clínicas, incluida la marcada `es_default` |
| `MetaPacientesSemestreSeeder` | Metas de pacientes por semestre |
| Bootstrap de administrador | Cuenta administradora inicial a partir de `ADMIN_EMAIL` / `ADMIN_PASSWORD` |

El sistema **no carga datos de prueba** en producción.

**Instalación de los componentes de software**

```bash
git clone <url-del-repositorio>
cd ThesisClaude

# Backend
cd neovision && ./mvnw clean package

# Frontend web
cd ../neovision-frontend && npm ci && npm run build

# App móvil
cd ../neovision-mobile && npm ci
```

### 3.3 Configuración

**Variables de entorno**

En desarrollo se toman los valores por defecto de `application.properties`; en producción se definen como *App Settings* de Azure App Service y **nunca se versionan en el repositorio**.

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Sí (prod) | `jdbc:postgresql://localhost:5432/neovision_db` | Cadena JDBC |
| `SPRING_DATASOURCE_USERNAME` | Sí (prod) | `postgres` | Usuario de base de datos |
| `SPRING_DATASOURCE_PASSWORD` | Sí (prod) | vacío | Contraseña |
| `JWT_SECRET` | **Sí** | *sin valor* | Secreto de firma; mínimo 32 caracteres o la aplicación no arranca |
| `SPRING_MAIL_USERNAME` | No | vacío | Cuenta SMTP remitente |
| `SPRING_MAIL_PASSWORD` | No | vacío | Contraseña de aplicación SMTP |
| `ADMIN_EMAIL` | No | `admin@neovision.com` | Correo del administrador inicial |
| `ADMIN_PASSWORD` | Sí (prod) | *sin valor* | Contraseña del administrador inicial |
| `APP_CORS_ALLOWED_ORIGINS` | Sí (prod) | — | Orígenes permitidos, separados por coma |
| `SWAGGER_ENABLED` | No | `false` en producción | Expone Swagger UI |
| `PORT` | No | `8080` | Puerto de escucha (lo inyecta Azure) |

**Parámetros de la aplicación**

| Parámetro | Valor | Efecto |
|---|---|---|
| `jwt.expiration` | `28800000` (8 h) | Vigencia del token |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate solo valida; el esquema lo gobierna Flyway |
| `spring.flyway.enabled` | `true` | Migración automática al arrancar |
| `spring.servlet.multipart.max-file-size` | `5MB` | Límite de carga de archivos |
| `spring.mail.host` / `port` | `smtp.gmail.com` / `587` | Servidor SMTP con STARTTLS |

**Perfiles y roles de autenticación/autorización**

| Rol | Cómo se obtiene la cuenta | Método de acceso | Alcance |
|---|---|---|---|
| `ROLE_ADMIN` | Bootstrap inicial o creación por otro administrador | Correo + contraseña | Acceso total a `/api/admin/**`, historias en modo secretaría |
| `ROLE_DOCENTE` | Creado por el administrador (sin contraseña) | Microsoft (institucional) | `/api/docente/**` y revisión/finalización de historias |
| `ROLE_ESTUDIANTE` | Creado por el administrador (sin contraseña) | Microsoft (institucional) | `/api/estudiante/**` y diligenciamiento de historias |
| `ROLE_PACIENTE` | Autorregistro o creación por el administrador | Correo + contraseña | `/api/paciente/**` |

> Docentes y estudiantes se crean con `contrasena = NULL`: **solo** pueden ingresar mediante Microsoft. Si se requiere acceso local, debe asignarse un hash BCrypt explícitamente.

### 3.4 Despliegue

**Infraestructura**

| Nodo | Recurso | Configuración |
|---|---|---|
| Servidor de aplicaciones | Azure App Service `neovision-demo-api` | Linux, Java 21, plan `neovision-demo-plan`, grupo `neovision-demo-rg`, región South Central US |
| Base de datos | Neon PostgreSQL 16 | Endpoint *pooler*, TLS obligatorio, región AWS `us-east-2` |
| Distribución de frontend | El mismo App Service | La SPA se empaqueta dentro del jar (`src/main/resources/static`) |
| Distribución móvil | EAS (Expo Application Services) | Compilación y publicación de binarios Android |

La configuración de puertos y protocolos está detallada en la sección 2.3 y en `docs/deployment-diagram.html`.

**Procedimiento de despliegue**

```bash
./deploy-azure.sh                 # build completo con pruebas + despliegue + logs
SKIP_TESTS=1 ./deploy-azure.sh    # omite las pruebas unitarias
TAIL_LOGS=0  ./deploy-azure.sh    # no queda siguiendo los logs
```

Pasos que ejecuta el script:

1. Compila el frontend (`npm ci && npm run build`).
2. Copia el resultado a `neovision/src/main/resources/static`.
3. Empaqueta el jar (`./mvnw package`).
4. Publica el artefacto (`az webapp deploy --type jar --clean true --restart true`).
5. Sigue los logs hasta confirmar el arranque.

**Verificación posterior al despliegue**

| Verificación | Resultado esperado |
|---|---|
| Log de Flyway | `Successfully applied N migrations` o `Schema "public" is up to date` |
| Log de arranque | `Started NeovisionApplication in N seconds` |
| Endpoint de autenticación | `POST /api/auth/login` responde 401 con credenciales inválidas (no 404) |
| Frontend | La raíz `/` sirve la SPA |

**Reversión**

El despliegue anterior puede restaurarse desde el historial de despliegues de Azure App Service. Las migraciones Flyway **no son reversibles automáticamente**: antes de desplegar un cambio de esquema debe crearse una rama de respaldo en Neon.

**Consideraciones de escalamiento**

| Escenario | Configuración recomendada |
|---|---|
| Desarrollo / demostración | App Service F1 (Free) + Neon Free |
| Producción, uso concurrente real | App Service B1 o superior con *Always On* activado + Neon plan pago |

---

## 4. RESOLUCIÓN DE PROBLEMAS

### 4.1 Errores técnicos más comunes y su solución

**E-01 · Un usuario creado con correo en mayúsculas no puede iniciar sesión**

| | |
|---|---|
| Síntomas | «Correo o contraseña inválidos» o «No estás registrado en el sistema» pese a existir el registro |
| Causa | PostgreSQL compara texto distinguiendo mayúsculas; el correo llega en minúsculas desde el formulario o el token de Microsoft y no coincide con el almacenado |
| Diagnóstico | `SELECT correo FROM usuario WHERE lower(correo) = lower('<correo>');` devuelve fila, pero la búsqueda exacta no |
| Solución | Corregido: la migración `V4` normalizó los existentes y `ValidacionesUsuario.normalizarCorreo()` normaliza los nuevos. Si reaparece, verificar que el punto de creación invoque dicha normalización |

**E-02 · Un endpoint responde 404 pese a existir en el código**

| | |
|---|---|
| Síntomas | `POST /api/auth/microsoft` (u otro) devuelve 404 en el servidor desplegado |
| Causa | El artefacto desplegado es anterior al código que introdujo el endpoint |
| Diagnóstico | Comparar el endpoint contra el controlador en el repositorio; revisar la fecha del último despliegue en Azure |
| Solución | Redesplegar: `./deploy-azure.sh` |

**E-03 · La aplicación se degrada o se reinicia con varios usuarios simultáneos**

| | |
|---|---|
| Síntomas | Respuestas de varios segundos, errores intermitentes, reinicios del contenedor o HTTP 429 |
| Causa | Plan App Service F1: tope de 60 minutos de CPU/día, sin *Always On*, heap de 1 GB al límite de la instancia |
| Diagnóstico | `az webapp log tail`; revisar métricas de CPU y cuota en el portal de Azure |
| Solución | Escalar el plan: `az appservice plan update -g neovision-demo-rg -n neovision-demo-plan --sku B1` y activar *Always On*. Si debe permanecer en F1, reducir el heap con `JAVA_OPTS=-Xmx512m -XX:MaxMetaspaceSize=192m` |

**E-04 · Arranque lento tras un período de inactividad**

| | |
|---|---|
| Síntomas | La primera petición tarda 25–70 s o expira |
| Causa | Sin *Always On*, App Service descarga la aplicación tras 20 minutos de inactividad; adicionalmente Neon suspende el cómputo tras 5 minutos |
| Diagnóstico | El log muestra un arranque completo de Spring Boot en el momento de la petición |
| Solución | Activar *Always On* (requiere plan B1+). Como paliativo, realizar una petición de calentamiento antes de un uso previsto |

**E-05 · La aplicación no arranca por falta de secreto JWT**

| | |
|---|---|
| Síntomas | Fallo al iniciar; el log indica que el secreto es inválido o demasiado corto |
| Causa | `JWT_SECRET` ausente o con menos de 32 caracteres |
| Diagnóstico | Verificar la *App Setting* `JWT_SECRET` |
| Solución | Definir un secreto de al menos 32 caracteres y reiniciar |

**E-06 · Error de validación de Flyway al arrancar**

| | |
|---|---|
| Síntomas | `Validate failed: migration checksum mismatch` o `applied migration not resolved locally` |
| Causa | Se editó una migración ya aplicada, o se desplegó una versión que no contiene una migración presente en la base |
| Diagnóstico | `SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;` |
| Solución | No editar migraciones aplicadas: crear una nueva `V{n+1}__*.sql`. Si el cambio ya se hizo, revertir el archivo a su contenido original y aplicar la corrección en una migración nueva |

**E-07 · `psql` responde «current transaction is aborted»**

| | |
|---|---|
| Síntomas | Toda sentencia posterior falla con el mismo mensaje |
| Causa | Una sentencia anterior falló dentro de una transacción abierta y no se ejecutó `ROLLBACK` |
| Diagnóstico | La sesión permanece en estado de error tras un fallo previo |
| Solución | Ejecutar `ROLLBACK;` y reintentar. Preferir `psql -f archivo.sql -v ON_ERROR_STOP=1`, que revierte automáticamente al fallar |

**E-08 · No se puede eliminar una cita o una sesión**

| | |
|---|---|
| Síntomas | Error de integridad referencial al eliminar |
| Causa | Restricciones `RESTRICT`: una cita con atención asociada no puede borrarse, y una sesión con citas tampoco |
| Diagnóstico | `SELECT count(*) FROM atencion WHERE id_cita = <id>;` |
| Solución | Desactivar en lugar de eliminar. Si el borrado es legítimo, eliminar primero las entidades dependientes respetando el orden: `adenda` → `atencion` → `cita` → `sesion_practica` |

**E-09 · No llegan los correos de confirmación o recordatorio**

| | |
|---|---|
| Síntomas | La operación se completa pero el destinatario no recibe el mensaje |
| Causa | Credenciales SMTP ausentes o inválidas, o límite de envío de la cuenta alcanzado |
| Diagnóstico | Revisar el log; `GlobalExceptionHandler` registra los fallos de correo sin interrumpir la operación |
| Solución | Verificar `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` (contraseña de aplicación, no la personal). El envío es asíncrono: su fallo nunca bloquea la transacción principal |

**E-10 · Un usuario queda bloqueado tras varios intentos fallidos**

| | |
|---|---|
| Síntomas | «Demasiados intentos» pese a introducir la contraseña correcta |
| Causa | Protección contra fuerza bruta: 5 intentos fallidos bloquean 15 minutos |
| Diagnóstico | Comportamiento esperado, no es un defecto |
| Solución | Esperar 15 minutos. El contador se reinicia con un inicio de sesión exitoso |

**E-11 · El inicio de sesión con Microsoft no finaliza**

| | |
|---|---|
| Síntomas | La ventana de MSAL se abre pero el inicio de sesión nunca se completa |
| Causa | La Content Security Policy bloquea la conexión a `login.microsoftonline.com`, o el usuario no existe en el sistema |
| Diagnóstico | Consola del navegador: errores de CSP o respuesta 401/404 de `/api/auth/microsoft` |
| Solución | Verificar que la CSP incluya `connect-src ... https://login.microsoftonline.com`. Si el usuario no existe, el administrador debe crearlo: el sistema no crea cuentas automáticamente desde Microsoft |

**E-12 · Conflicto de escritura simultánea sobre una cita o atención**

| | |
|---|---|
| Síntomas | `ObjectOptimisticLockingFailureException` |
| Causa | Dos usuarios modificaron el mismo registro a la vez; el bloqueo optimista (`@Version`) lo detectó |
| Diagnóstico | Comportamiento esperado del control de concurrencia |
| Solución | Recargar el registro y reintentar. Es la protección que impide, por ejemplo, que dos pacientes reserven el mismo bloque |

---

## 5. ANEXOS — ÍNDICE DE DIAGRAMAS

| Diagrama | Archivo | Exigido por |
|---|---|---|
| Casos de uso | `docs/use-case-diagram.html` | Tabla 1 — Diagrama de casos de uso |
| Entidad-relación | `docs/er-diagram.html` | Tabla 1 — Diagrama ER |
| Componentes | `docs/component-diagram.html` | Tabla 1 — Diagrama de componentes |
| Despliegue | `docs/deployment-diagram.html` | Tabla 1 — Diagrama de despliegue |
| Clases relevantes | `docs/class-diagram.html` | Tabla 1 — Diagrama de clases |
| Arquitectura general | `docs/architecture-diagram.html` | Complementario |
| Servicios expuestos | Sección 2.5 y `/swagger-ui.html` | Tabla 1 — Diagrama de servicios |
| Diccionario de datos | Sección 2.4 | Tabla 1 — Diccionario de datos |
| Scripts de instalación | `neovision/src/main/resources/db/migration/V1`–`V4` | Tabla 1 — Scripts de instalación |
| Procesos de negocio (BPMN TO-BE) | `docs/diagramas.pdf`, diagramas 6–9 | Complementario |

Los diagramas en formato `.html` se abren en cualquier navegador e incluyen botones para exportar a SVG o PNG (para insertar en este documento) o imprimir a PDF.

---

*Fin del documento.*
