package com.neovision.clinico.dto;

public record CandidatoAvanceSemestreResponse(
        Integer idEstudiante,
        String nombre,
        Integer semestreActual,
        Integer atendidos,
        Integer meta
) {
}
