package com.neovision.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "Resultado de una carga masiva de docentes o estudiantes desde un archivo Excel.")
@Getter
@AllArgsConstructor
public class CargaMasivaResponse {

    @Schema(description = "Filas de datos leídas del archivo (sin contar la cabecera ni filas vacías).")
    private int totalFilas;

    @Schema(description = "Registros creados exitosamente.")
    private int creados;

    @Schema(description = "Filas que no se pudieron crear, con el motivo.")
    private List<Incidencia> errores;

    @Schema(description = "Registros creados pero con algo que revisar (ej. una especialidad del Excel que no existe en el catálogo).")
    private List<Incidencia> advertencias;

    @Schema(description = "Una fila del Excel (la cabecera es la fila 1) y el mensaje asociado.")
    public record Incidencia(int fila, String mensaje) {}
}
