package com.neovision.clinico.historia.enums;

/**
 * Canal por el que llegó la solicitud de copia de historia clínica.
 *
 * <p>El derecho de acceso (Res. 1995/1999, Ley 1581/2012) no depende de que el
 * paciente tenga correo o cuenta. La distinción importa para la trazabilidad:
 * en {@link #DIGITAL} la identidad del solicitante la verifica la autenticación;
 * en {@link #PRESENCIAL} la verifica una persona de la secretaría, que queda
 * registrada en {@code creada_por_correo}.
 */
public enum CanalSolicitud {
    /** El propio paciente la creó desde su sesión. */
    DIGITAL,
    /** La secretaría la registró en nombre del paciente, tras verificar su documento. */
    PRESENCIAL
}
