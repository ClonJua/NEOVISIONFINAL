package com.neovision.clinico.historia.dto;

/**
 * Resultado de verificar el sello de integridad de una atención finalizada.
 *
 * @param tieneSello      la atención está finalizada y tiene hash almacenado
 * @param valido          el contenido actual coincide con el sello original
 * @param hashAlmacenado  hash guardado al finalizar
 * @param hashRecalculado hash recalculado sobre el contenido actual
 * @param mensaje         descripción legible del resultado
 */
public record VerificacionIntegridadResponse(
        boolean tieneSello,
        boolean valido,
        String hashAlmacenado,
        String hashRecalculado,
        String mensaje) {
}
