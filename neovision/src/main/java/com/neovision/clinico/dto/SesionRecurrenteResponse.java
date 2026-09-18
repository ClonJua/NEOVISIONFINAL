package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de la creación masiva de sesiones recurrentes.")
public class SesionRecurrenteResponse {
    @Schema(description = "Número de sesiones creadas.")
    private Integer sesionesCreadas;
    @Schema(description = "Mensaje descriptivo del resultado.")
    private String mensaje;

    public SesionRecurrenteResponse(Integer sesionesCreadas, String mensaje) {
        this.sesionesCreadas = sesionesCreadas;
        this.mensaje = mensaje;
    }

    public Integer getSesionesCreadas() { return sesionesCreadas; }
    public void setSesionesCreadas(Integer sesionesCreadas) { this.sesionesCreadas = sesionesCreadas; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
