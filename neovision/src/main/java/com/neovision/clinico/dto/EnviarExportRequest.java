package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Solicitud para enviar un reporte de citas exportado a un correo indicado.")
@Getter
@Setter
public class EnviarExportRequest {

    @Schema(description = "Formato del archivo a generar.", example = "excel")
    @NotBlank(message = "El formato es obligatorio")
    private String formato;

    @Schema(description = "Filtro de estado aplicado al reporte (opcional).", example = "RESERVADO")
    private String estado;

    @Schema(description = "Correo al que se enviará el reporte.", example = "coordinacion@unbosque.edu.co")
    @NotBlank(message = "El correo de destino es obligatorio")
    @Email(message = "El correo de destino debe tener un formato válido")
    private String correoDestino;
}
