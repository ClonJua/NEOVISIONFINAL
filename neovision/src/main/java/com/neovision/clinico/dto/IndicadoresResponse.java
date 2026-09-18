package com.neovision.clinico.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Indicadores agregados para el panel de reportes del admin. Se calcula con
 * consultas de agregación en base de datos (GROUP BY / COUNT DISTINCT), no
 * cargando todas las citas a memoria — escala independientemente del volumen
 * histórico de citas.
 */
@Getter
@AllArgsConstructor
public class IndicadoresResponse {
    private Map<String, Long> porEstado;
    private long consultoriosUsados;
    private long estudiantesActivos;
    private long total;
}
