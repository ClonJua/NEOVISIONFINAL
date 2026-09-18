package com.neovision.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Envuelve todas las respuestas de la API con un indicador de éxito, mensaje y datos.
 *
 * @param <T> tipo del contenido retornado en el campo data
 */
@Schema(description = "Respuesta estándar de la API.")
@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {

    @Schema(description = "Indica si la operación fue exitosa.")
    private boolean success;
    @Schema(description = "Mensaje descriptivo del resultado.")
    private String message;
    @Schema(description = "Datos retornados por la operación. Null en caso de error.")
    private T data;

    /**
     * Construye una respuesta exitosa con datos.
     *
     * @param message mensaje descriptivo
     * @param data    datos a retornar
     * @return ApiResponse con success=true
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Construye una respuesta de error sin datos.
     *
     * @param message mensaje de error
     * @return ApiResponse con success=false y data=null
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
