package com.neovision.clinico.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Citas COMPLETADO por estudiante, para el reporte de distribución vs. meta semestral. */
@Getter
@AllArgsConstructor
public class DistribucionEstudianteResponse {
    private Integer idEstudiante;
    private String nombre;
    private Integer semestre;
    private long completadas;
}
