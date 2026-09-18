package com.neovision.clinico.historia.dto;

/** Item del catálogo CUPS para autocompletar procedimientos. */
public record CupsResponse(String codigo, String descripcion, String categoria) {
}
