-- =============================================================================
-- V5: paciente sin cuenta de acceso + canal de la solicitud de historia clinica
-- -----------------------------------------------------------------------------
-- Un paciente es un sujeto de atencion clinica, no necesariamente un usuario del
-- sistema. La secretaria registra adultos mayores que no tienen correo: tienen
-- historia clinica y citas, pero nunca inician sesion. Hasta ahora el modelo lo
-- impedia porque paciente.correo y paciente.id_usuario eran NOT NULL.
--
-- Como efecto secundario esto tambien desbloquea el caso del estudiante que se
-- atiende como paciente: su registro de paciente se identifica por cedula y no
-- necesita una cuenta que choque con su correo institucional.
--
-- Nota: en PostgreSQL una restriccion UNIQUE trata los NULL como distintos, asi
-- que uk_paciente_correo y uk_paciente_usuario siguen siendo correctas: unicidad
-- entre los valores presentes, y tantos NULL como haga falta.
-- =============================================================================

ALTER TABLE paciente ALTER COLUMN correo     DROP NOT NULL;
ALTER TABLE paciente ALTER COLUMN id_usuario DROP NOT NULL;

-- -----------------------------------------------------------------------------
-- Canal de la solicitud de copia de historia clinica.
--
-- El derecho de acceso a la historia (Res. 1995/1999, Ley 1581/2012) no depende
-- de tener correo. Un paciente sin cuenta la pide presencialmente y la secretaria
-- la registra en su nombre. La diferencia importa para la trazabilidad: en el
-- canal DIGITAL la identidad la verifica la autenticacion; en el PRESENCIAL la
-- verifica una persona, y debe quedar registrado quien.
-- -----------------------------------------------------------------------------

ALTER TABLE solicitud_historia ADD COLUMN IF NOT EXISTS canal             VARCHAR(20);
ALTER TABLE solicitud_historia ADD COLUMN IF NOT EXISTS creada_por_correo VARCHAR(100);

-- Las solicitudes existentes son todas del flujo de autoservicio.
UPDATE solicitud_historia SET canal = 'DIGITAL' WHERE canal IS NULL;

ALTER TABLE solicitud_historia ALTER COLUMN canal SET NOT NULL;

ALTER TABLE solicitud_historia DROP CONSTRAINT IF EXISTS chk_solicitud_canal;
ALTER TABLE solicitud_historia ADD  CONSTRAINT chk_solicitud_canal
    CHECK (canal IN ('DIGITAL', 'PRESENCIAL'));

-- En el canal presencial siempre debe constar quien la registro (responsabilidad
-- de la verificacion de identidad); en el digital la registra el propio paciente.
ALTER TABLE solicitud_historia DROP CONSTRAINT IF EXISTS chk_solicitud_presencial_responsable;
ALTER TABLE solicitud_historia ADD  CONSTRAINT chk_solicitud_presencial_responsable
    CHECK (canal <> 'PRESENCIAL' OR creada_por_correo IS NOT NULL);

COMMENT ON COLUMN paciente.correo     IS 'Correo del paciente. NULL si no tiene (registro presencial); la notificacion se hace por telefono';
COMMENT ON COLUMN paciente.id_usuario IS 'Cuenta de acceso. NULL si el paciente no usa el sistema en linea';
COMMENT ON COLUMN solicitud_historia.canal             IS 'DIGITAL (el paciente la creo autenticado) o PRESENCIAL (la secretaria la registro en su nombre)';
COMMENT ON COLUMN solicitud_historia.creada_por_correo IS 'Quien registro la solicitud. En PRESENCIAL es quien verifico la identidad del paciente';
