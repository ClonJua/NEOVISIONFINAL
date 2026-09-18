-- =============================================================================
-- V3: integridad relacional, reglas de negocio e inmutabilidad
-- -----------------------------------------------------------------------------
-- Idempotente: deja el mismo resultado sobre una BD recien creada por V1 o
-- sobre una BD existente creada por Hibernate (restricciones con nombre hash,
-- que aqui se renombran o se recrean con nombre legible).
--
-- Toda restriccion se agrega como NOT VALID y luego se intenta validar. Si hay
-- filas historicas que la incumplen, queda NOT VALID (se aplica solo a filas
-- nuevas) y se registra un WARNING en vez de abortar el despliegue.
-- =============================================================================

-- ---------- Helpers (se eliminan al final del script) ------------------------

CREATE OR REPLACE FUNCTION nv_validar(p_tabla text, p_nombre text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format('ALTER TABLE %I VALIDATE CONSTRAINT %I', p_tabla, p_nombre);
EXCEPTION WHEN check_violation OR foreign_key_violation THEN
    RAISE WARNING 'Restriccion %.% queda NOT VALID: hay filas existentes que la incumplen',
        p_tabla, p_nombre;
END $$;

-- Deja exactamente una FK sobre la columna, con el nombre y la politica ON
-- DELETE indicados (recrea la existente si difiere en cualquiera de los dos).
CREATE OR REPLACE FUNCTION nv_fk(p_tabla text, p_col text, p_ref text, p_ref_col text,
                                 p_nombre text, p_on_delete text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    v_actual  text;
    v_regla   text;
    v_deseada text := CASE p_on_delete WHEN 'CASCADE'  THEN 'c'
                                        WHEN 'RESTRICT' THEN 'r'
                                        WHEN 'SET NULL' THEN 'n'
                                        ELSE 'a' END;
BEGIN
    SELECT c.conname, c.confdeltype::text INTO v_actual, v_regla
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
    WHERE c.contype = 'f'
      AND c.conrelid = p_tabla::regclass
      AND array_length(c.conkey, 1) = 1
      AND a.attname = p_col
    LIMIT 1;

    IF v_actual = p_nombre AND v_regla = v_deseada THEN
        RETURN;
    END IF;
    IF v_actual IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', p_tabla, v_actual);
    END IF;
    EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I (%I) ON DELETE %s NOT VALID',
                   p_tabla, p_nombre, p_col, p_ref, p_ref_col, p_on_delete);
    PERFORM nv_validar(p_tabla, p_nombre);
END $$;

-- Garantiza un UNIQUE con nombre legible sobre las columnas dadas: renombra el
-- indice/constraint existente si ya hay uno equivalente, o lo crea si falta.
CREATE OR REPLACE FUNCTION nv_unique(p_tabla text, p_cols text[], p_nombre text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    v_indice     text;
    v_indice_oid oid;
    v_constraint text;
BEGIN
    SELECT ic.relname, i.indexrelid INTO v_indice, v_indice_oid
    FROM pg_index i
    JOIN pg_class ic ON ic.oid = i.indexrelid
    WHERE i.indrelid = p_tabla::regclass
      AND i.indisunique AND NOT i.indisprimary AND i.indpred IS NULL
      AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
           FROM unnest(i.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)
           JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = k.attnum) = p_cols
    LIMIT 1;

    IF v_indice IS NULL THEN
        BEGIN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I UNIQUE (%s)', p_tabla, p_nombre,
                           (SELECT string_agg(format('%I', c), ', ') FROM unnest(p_cols) AS c));
        EXCEPTION WHEN unique_violation THEN
            RAISE WARNING 'UNIQUE %.% omitido: hay valores duplicados en (%)',
                p_tabla, p_nombre, array_to_string(p_cols, ', ');
        END;
        RETURN;
    END IF;
    IF v_indice = p_nombre THEN
        RETURN;
    END IF;
    SELECT conname INTO v_constraint
    FROM pg_constraint WHERE conindid = v_indice_oid AND contype = 'u';
    IF v_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %I RENAME CONSTRAINT %I TO %I', p_tabla, v_constraint, p_nombre);
    ELSE
        EXECUTE format('ALTER INDEX %I RENAME TO %I', v_indice, p_nombre);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION nv_check(p_tabla text, p_nombre text, p_expr text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', p_tabla, p_nombre);
    EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK (%s) NOT VALID', p_tabla, p_nombre, p_expr);
    PERFORM nv_validar(p_tabla, p_nombre);
END $$;


-- ---------- 1. Claves foraneas -----------------------------------------------
-- RESTRICT: nada clinico o academico se borra en cascada; el servicio ofrece
-- "desactivar" en su lugar. CASCADE solo en filas que no tienen sentido sin su
-- padre: tablas de union y las partes de una atencion (diagnosticos y
-- procedimientos, que JPA ya gestiona con orphanRemoval).

SELECT nv_fk('usuario',              'id_rol',                'rol',              'id_rol',          'fk_usuario_rol',                        'RESTRICT');
SELECT nv_fk('docente',              'id_usuario',            'usuario',          'id_usuario',      'fk_docente_usuario',                    'RESTRICT');
SELECT nv_fk('estudiante',           'id_usuario',            'usuario',          'id_usuario',      'fk_estudiante_usuario',                 'RESTRICT');
SELECT nv_fk('paciente',             'id_usuario',            'usuario',          'id_usuario',      'fk_paciente_usuario',                   'RESTRICT');
SELECT nv_fk('docente_especialidad', 'id_docente',            'docente',          'id_docente',      'fk_docente_especialidad_docente',       'CASCADE');
SELECT nv_fk('docente_especialidad', 'id_especialidad',       'especialidad',     'id_especialidad', 'fk_docente_especialidad_especialidad',  'RESTRICT');
SELECT nv_fk('salon',                'id_espacio',            'espacio',          'id_espacio',      'fk_salon_espacio',                      'RESTRICT');
SELECT nv_fk('sesion_practica',      'id_docente',            'docente',          'id_docente',      'fk_sesion_docente',                     'RESTRICT');
SELECT nv_fk('sesion_practica',      'id_salon',              'salon',            'id_salon',        'fk_sesion_salon',                       'RESTRICT');
SELECT nv_fk('sesion_estudiante',    'id_sesion',             'sesion_practica',  'id_sesion',       'fk_sesion_estudiante_sesion',           'CASCADE');
SELECT nv_fk('sesion_estudiante',    'id_estudiante',         'estudiante',       'id_estudiante',   'fk_sesion_estudiante_estudiante',       'RESTRICT');
SELECT nv_fk('cita',                 'id_sesion',             'sesion_practica',  'id_sesion',       'fk_cita_sesion',                        'RESTRICT');
SELECT nv_fk('cita',                 'id_estudiante',         'estudiante',       'id_estudiante',   'fk_cita_estudiante',                    'RESTRICT');
SELECT nv_fk('cita',                 'id_estudiante_2',       'estudiante',       'id_estudiante',   'fk_cita_estudiante2',                   'RESTRICT');
SELECT nv_fk('cita',                 'id_paciente',           'paciente',         'id_paciente',     'fk_cita_paciente',                      'RESTRICT');
SELECT nv_fk('cita',                 'id_especialidad',       'especialidad',     'id_especialidad', 'fk_cita_especialidad',                  'RESTRICT');
SELECT nv_fk('historia_clinica',     'id_paciente',           'paciente',         'id_paciente',     'fk_historia_paciente',                  'RESTRICT');
SELECT nv_fk('atencion',             'id_historia',           'historia_clinica', 'id_historia',     'fk_atencion_historia',                  'RESTRICT');
SELECT nv_fk('atencion',             'id_cita',               'cita',             'id_cita',         'fk_atencion_cita',                      'RESTRICT');
SELECT nv_fk('atencion',             'id_estudiante',         'estudiante',       'id_estudiante',   'fk_atencion_estudiante',                'RESTRICT');
SELECT nv_fk('atencion',             'id_docente_supervisor', 'docente',          'id_docente',      'fk_atencion_docente_supervisor',        'RESTRICT');
SELECT nv_fk('diagnostico',          'id_atencion',           'atencion',         'id_atencion',     'fk_diagnostico_atencion',               'CASCADE');
SELECT nv_fk('procedimiento',        'id_atencion',           'atencion',         'id_atencion',     'fk_procedimiento_atencion',             'CASCADE');
SELECT nv_fk('adenda',               'id_atencion',           'atencion',         'id_atencion',     'fk_adenda_atencion',                    'RESTRICT');
SELECT nv_fk('solicitud_historia',   'id_paciente',           'paciente',         'id_paciente',     'fk_solicitud_paciente',                 'RESTRICT');

-- Nuevas: los codigos clinicos dejan de ser texto libre y pasan a referenciar
-- los catalogos controlados (un codigo se desactiva, nunca se borra).
SELECT nv_fk('diagnostico',          'codigo_cie10',          'cie10',            'codigo',          'fk_diagnostico_cie10',                  'RESTRICT');
SELECT nv_fk('procedimiento',        'codigo_cups',           'cups',             'codigo',          'fk_procedimiento_cups',                 'RESTRICT');


-- ---------- 2. Unicidad --------------------------------------------------------
-- Las entidades declaraban unique en correo de docente/estudiante/paciente,
-- pero ddl-auto=update nunca lo agrega sobre columnas ya existentes.

SELECT nv_unique('rol',              ARRAY['nombre_rol'],       'uk_rol_nombre');
SELECT nv_unique('usuario',          ARRAY['correo'],           'uk_usuario_correo');
SELECT nv_unique('docente',          ARRAY['id_usuario'],       'uk_docente_usuario');
SELECT nv_unique('docente',          ARRAY['cedula'],           'uk_docente_cedula');
SELECT nv_unique('docente',          ARRAY['correo'],           'uk_docente_correo');
SELECT nv_unique('estudiante',       ARRAY['id_usuario'],       'uk_estudiante_usuario');
SELECT nv_unique('estudiante',       ARRAY['cedula'],           'uk_estudiante_cedula');
SELECT nv_unique('estudiante',       ARRAY['correo'],           'uk_estudiante_correo');
SELECT nv_unique('paciente',         ARRAY['id_usuario'],       'uk_paciente_usuario');
SELECT nv_unique('paciente',         ARRAY['numero_documento'], 'uk_paciente_numero_documento');
SELECT nv_unique('paciente',         ARRAY['correo'],           'uk_paciente_correo');
SELECT nv_unique('especialidad',     ARRAY['nombre'],           'uk_especialidad_nombre');
SELECT nv_unique('historia_clinica', ARRAY['id_paciente'],      'uk_historia_paciente');
SELECT nv_unique('historia_clinica', ARRAY['numero_historia'],  'uk_historia_numero');
SELECT nv_unique('atencion',         ARRAY['id_cita'],          'uk_atencion_cita');

-- Solo puede existir una especialidad por defecto (la que cualquier docente supervisa).
CREATE UNIQUE INDEX IF NOT EXISTS uq_especialidad_default ON especialidad (es_default) WHERE es_default;


-- ---------- 3. Reglas de negocio (CHECK) ------------------------------------
-- Se reemplazan los checks con nombre generado (Hibernate / manuales) por
-- nombres legibles y se agregan reglas que hasta ahora solo vivian en Java.

ALTER TABLE cita               DROP CONSTRAINT IF EXISTS chk_estado;
ALTER TABLE atencion           DROP CONSTRAINT IF EXISTS atencion_estado_check;
ALTER TABLE auditoria          DROP CONSTRAINT IF EXISTS auditoria_accion_check;
ALTER TABLE solicitud_historia DROP CONSTRAINT IF EXISTS solicitud_historia_estado_check;

SELECT nv_check('rol', 'chk_rol_nombre',
    $$nombre_rol IN ('ROLE_ADMIN', 'ROLE_DOCENTE', 'ROLE_ESTUDIANTE', 'ROLE_PACIENTE')$$);

SELECT nv_check('estudiante', 'chk_estudiante_semestre',
    $$semestre BETWEEN 1 AND 10$$);

SELECT nv_check('meta_pacientes_semestre', 'chk_meta_semestre',
    $$semestre BETWEEN 1 AND 10$$);
SELECT nv_check('meta_pacientes_semestre', 'chk_meta_minimo',
    $$minimo_pacientes >= 0$$);

SELECT nv_check('paciente', 'chk_paciente_tipo_documento',
    $$tipo_documento IN ('CC', 'TI', 'RC', 'CE', 'PA')$$);
SELECT nv_check('paciente', 'chk_paciente_fecha_nacimiento',
    $$fecha_nacimiento <= CURRENT_DATE$$);
SELECT nv_check('paciente', 'chk_paciente_numero_documento',
    $$numero_documento <> ''$$);
-- Si autorizo el tratamiento de datos, debe quedar constancia de cuando.
SELECT nv_check('paciente', 'chk_paciente_autorizacion_datos',
    $$autoriza_tratamiento_datos = FALSE OR fecha_autorizacion_datos IS NOT NULL$$);

SELECT nv_check('sesion_practica', 'chk_sesion_horario',
    $$hora_inicio IS NULL OR hora_fin IS NULL OR hora_fin > hora_inicio$$);
SELECT nv_check('sesion_practica', 'chk_sesion_cupos',
    $$cupos_totales >= 1$$);

SELECT nv_check('cita', 'chk_cita_estado',
    $$estado IN ('DISPONIBLE', 'RESERVADO', 'COMPLETADO', 'CANCELADO', 'VENCIDA')$$);
-- Un cupo DISPONIBLE no tiene paciente; uno RESERVADO o COMPLETADO siempre lo
-- tiene. CANCELADO y VENCIDA pueden venir de cualquiera de los dos.
SELECT nv_check('cita', 'chk_cita_paciente_segun_estado',
    $$(estado = 'DISPONIBLE' AND id_paciente IS NULL)
      OR (estado IN ('RESERVADO', 'COMPLETADO') AND id_paciente IS NOT NULL)
      OR estado IN ('CANCELADO', 'VENCIDA')$$);
SELECT nv_check('cita', 'chk_cita_estudiantes_distintos',
    $$id_estudiante_2 IS NULL OR id_estudiante_2 <> id_estudiante$$);

SELECT nv_check('atencion', 'chk_atencion_estado',
    $$estado IN ('BORRADOR', 'PENDIENTE_REVISION', 'DEVUELTA', 'FINALIZADA')$$);
-- Una atencion FINALIZADA siempre esta sellada (Res. 1995/1999 art. 18).
SELECT nv_check('atencion', 'chk_atencion_finalizada_sellada',
    $$estado <> 'FINALIZADA' OR (hash_integridad IS NOT NULL AND fecha_finalizacion IS NOT NULL)$$);

SELECT nv_check('auditoria', 'chk_auditoria_accion',
    $$accion IN ('CREAR_HISTORIA', 'REGISTRAR_CONSENTIMIENTO', 'ABRIR_ATENCION', 'GUARDAR_BORRADOR',
                 'ENVIAR_REVISION', 'DEVOLVER', 'FINALIZAR', 'CREAR_ADENDA', 'CONSULTAR')$$);

SELECT nv_check('solicitud_historia', 'chk_solicitud_estado',
    $$estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')$$);
SELECT nv_check('solicitud_historia', 'chk_solicitud_resolucion',
    $$(estado = 'PENDIENTE' AND fecha_resolucion IS NULL)
      OR (estado <> 'PENDIENTE' AND fecha_resolucion IS NOT NULL)$$);

-- Formato de los codigos de catalogo: CIE-10 "H52.1"; CUPS 6 digitos.
SELECT nv_check('cie10', 'chk_cie10_codigo_formato', $$codigo ~ '^[A-Z][0-9]{2}(\.[0-9A-Z]{1,2})?$'$$);
SELECT nv_check('cups',  'chk_cups_codigo_formato',  $$codigo ~ '^[0-9]{6}$'$$);


-- ---------- 4. Ajustes de nulabilidad heredados --------------------------------
-- ddl-auto=update agrego estas columnas sin NOT NULL aunque la entidad lo declara.
UPDATE cita SET recordatorio_24h_enviado = FALSE WHERE recordatorio_24h_enviado IS NULL;
UPDATE cita SET recordatorio_1h_enviado  = FALSE WHERE recordatorio_1h_enviado  IS NULL;
ALTER TABLE cita
    ALTER COLUMN recordatorio_24h_enviado SET DEFAULT FALSE,
    ALTER COLUMN recordatorio_24h_enviado SET NOT NULL,
    ALTER COLUMN recordatorio_1h_enviado  SET DEFAULT FALSE,
    ALTER COLUMN recordatorio_1h_enviado  SET NOT NULL;


-- ---------- 5. Indices ------------------------------------------------------
-- Los ya declarados en las entidades (por si la BD es nueva) mas los que
-- faltaban para las consultas reales de los repositorios y para las columnas
-- FK que Postgres no indexa solo.

CREATE INDEX IF NOT EXISTS idx_usuario_rol                       ON usuario (id_rol);
CREATE INDEX IF NOT EXISTS idx_salon_espacio                     ON salon (id_espacio);
CREATE INDEX IF NOT EXISTS idx_docente_especialidad_especialidad ON docente_especialidad (id_especialidad);
CREATE INDEX IF NOT EXISTS idx_sesion_estudiante_estudiante      ON sesion_estudiante (id_estudiante);

CREATE INDEX IF NOT EXISTS idx_sesion_docente                    ON sesion_practica (id_docente);
CREATE INDEX IF NOT EXISTS idx_sesion_fecha                      ON sesion_practica (fecha);
CREATE INDEX IF NOT EXISTS idx_sesion_salon_fecha                ON sesion_practica (id_salon, fecha);

CREATE INDEX IF NOT EXISTS idx_cita_estado                       ON cita (estado);
CREATE INDEX IF NOT EXISTS idx_cita_sesion                       ON cita (id_sesion);
CREATE INDEX IF NOT EXISTS idx_cita_paciente                     ON cita (id_paciente);
CREATE INDEX IF NOT EXISTS idx_cita_estudiante                   ON cita (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_cita_estudiante2                  ON cita (id_estudiante_2);
CREATE INDEX IF NOT EXISTS idx_cita_estado_sesion                ON cita (estado, id_sesion);
CREATE INDEX IF NOT EXISTS idx_cita_paciente_estado              ON cita (id_paciente, estado);
CREATE INDEX IF NOT EXISTS idx_cita_especialidad                 ON cita (id_especialidad);

CREATE INDEX IF NOT EXISTS idx_atencion_historia                 ON atencion (id_historia);
CREATE INDEX IF NOT EXISTS idx_atencion_estudiante               ON atencion (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_atencion_docente                  ON atencion (id_docente_supervisor);
CREATE INDEX IF NOT EXISTS idx_atencion_estado                   ON atencion (estado);
CREATE INDEX IF NOT EXISTS idx_atencion_docente_estado           ON atencion (id_docente_supervisor, estado);

CREATE INDEX IF NOT EXISTS idx_diagnostico_atencion              ON diagnostico (id_atencion);
CREATE INDEX IF NOT EXISTS idx_diagnostico_cie10                 ON diagnostico (codigo_cie10);
CREATE INDEX IF NOT EXISTS idx_procedimiento_atencion            ON procedimiento (id_atencion);
CREATE INDEX IF NOT EXISTS idx_procedimiento_cups                ON procedimiento (codigo_cups);
CREATE INDEX IF NOT EXISTS idx_adenda_atencion                   ON adenda (id_atencion);

CREATE INDEX IF NOT EXISTS idx_auditoria_paciente                ON auditoria (id_paciente);
CREATE INDEX IF NOT EXISTS idx_auditoria_entidad                 ON auditoria (entidad, entidad_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_timestamp               ON auditoria (fecha_hora);

CREATE INDEX IF NOT EXISTS idx_solicitud_paciente                ON solicitud_historia (id_paciente);
CREATE INDEX IF NOT EXISTS idx_solicitud_estado                  ON solicitud_historia (estado);

CREATE INDEX IF NOT EXISTS idx_cie10_descripcion                 ON cie10 (descripcion);
CREATE INDEX IF NOT EXISTS idx_cups_descripcion                  ON cups (descripcion);


-- ---------- 6. Inmutabilidad: tablas de solo anexar ------------------------------
-- La bitacora de auditoria y las adendas no se editan ni se borran nunca, ni
-- siquiera con acceso directo a la BD (Res. 1995/1999 art. 18; Ley 1581/2012).
-- Esta funcion es permanente: los triggers dependen de ella.

CREATE OR REPLACE FUNCTION neovision_solo_anexar() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La tabla % es de solo anexar: no se permite %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END $$;

DROP TRIGGER IF EXISTS trg_auditoria_solo_anexar ON auditoria;
CREATE TRIGGER trg_auditoria_solo_anexar
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION neovision_solo_anexar();

DROP TRIGGER IF EXISTS trg_adenda_solo_anexar ON adenda;
CREATE TRIGGER trg_adenda_solo_anexar
    BEFORE UPDATE OR DELETE ON adenda
    FOR EACH ROW EXECUTE FUNCTION neovision_solo_anexar();


-- ---------- 7. Limpieza -------------------------------------------------------
-- Tabla huerfana de una version anterior del modulo de historia (ya no existe
-- la entidad). Solo se elimina si esta vacia.
DO $$
BEGIN
    IF to_regclass('public.consentimiento_informado') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM consentimiento_informado) THEN
            DROP TABLE consentimiento_informado;
        END IF;
    END IF;
END $$;

DROP FUNCTION nv_fk(text, text, text, text, text, text);
DROP FUNCTION nv_unique(text, text[], text);
DROP FUNCTION nv_check(text, text, text);
DROP FUNCTION nv_validar(text, text);
