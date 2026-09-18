-- =============================================================================
-- V4: normaliza los correos a minusculas (identidad de inicio de sesion)
-- -----------------------------------------------------------------------------
-- El correo es el identificador de acceso. Postgres compara texto distinguiendo
-- mayusculas, y el login con Microsoft busca el correo tal cual lo devuelve el
-- token (en minusculas). Un docente creado como "LSHMORALES@unbosque.edu.co"
-- nunca casa con "lshmorales@unbosque.edu.co" y queda sin poder entrar.
--
-- Aqui se pasan a minusculas (y se recorta el espacio) los correos ya
-- guardados. El codigo de creacion/edicion y el de login los normalizan de
-- ahora en adelante.
--
-- Idempotente: reejecutar no toca filas ya normalizadas (WHERE ... IS DISTINCT
-- FROM ...). Si dos filas solo se diferencian por mayusculas, la restriccion
-- unica uk_*_correo abortaria el UPDATE: eso senala un duplicado real que hay
-- que resolver a mano antes de reintentar.
-- =============================================================================

UPDATE usuario    SET correo = lower(btrim(correo)) WHERE correo IS DISTINCT FROM lower(btrim(correo));
UPDATE docente    SET correo = lower(btrim(correo)) WHERE correo IS DISTINCT FROM lower(btrim(correo));
UPDATE estudiante SET correo = lower(btrim(correo)) WHERE correo IS DISTINCT FROM lower(btrim(correo));
UPDATE paciente   SET correo = lower(btrim(correo)) WHERE correo IS DISTINCT FROM lower(btrim(correo));

-- Columnas de trazabilidad (texto suelto, sin FK): se alinean para que las
-- consultas por correo sigan casando despues de la normalizacion.
UPDATE adenda             SET autor_correo        = lower(btrim(autor_correo))        WHERE autor_correo        IS DISTINCT FROM lower(btrim(autor_correo));
UPDATE auditoria          SET usuario_correo      = lower(btrim(usuario_correo))      WHERE usuario_correo      IS DISTINCT FROM lower(btrim(usuario_correo));
UPDATE historia_clinica   SET abierta_por_correo  = lower(btrim(abierta_por_correo))  WHERE abierta_por_correo  IS DISTINCT FROM lower(btrim(abierta_por_correo));
UPDATE solicitud_historia SET resuelta_por_correo = lower(btrim(resuelta_por_correo)) WHERE resuelta_por_correo IS DISTINCT FROM lower(btrim(resuelta_por_correo));
