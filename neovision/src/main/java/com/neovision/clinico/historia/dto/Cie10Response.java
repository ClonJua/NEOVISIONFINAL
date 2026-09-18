package com.neovision.clinico.historia.dto;

/** Item del catálogo CIE-10 para autocompletar diagnósticos. */
public record Cie10Response(String codigo, String descripcion, String categoria) {
}
