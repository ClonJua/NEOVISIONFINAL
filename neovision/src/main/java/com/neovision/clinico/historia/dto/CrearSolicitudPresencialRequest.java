package com.neovision.clinico.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud de copia de historia clínica registrada por la secretaría en nombre
 * del paciente, cuando este la pide de forma presencial.
 *
 * <p>Es el canal para quien no tiene cuenta ni correo: el derecho de acceso
 * (Res. 1995/1999, Ley 1581/2012) no depende de tenerlos. La identidad del
 * paciente la verifica quien registra la solicitud, no la autenticación.
 */
@Schema(description = "Datos para registrar una solicitud presencial de copia de historia clínica.")
@Getter
@Setter
@NoArgsConstructor
public class CrearSolicitudPresencialRequest {

    @Schema(description = "ID del paciente que solicita su historia clínica.", example = "12")
    @NotNull
    private Integer idPaciente;

    @Schema(description = "Motivo que indica el paciente. Opcional.", example = "Consulta con especialista externo")
    private String motivo;
}
