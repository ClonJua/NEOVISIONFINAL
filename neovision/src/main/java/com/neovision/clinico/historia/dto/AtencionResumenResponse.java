package com.neovision.clinico.historia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** Vista compacta de una atención para listas y timelines. */
@Getter
@AllArgsConstructor
public class AtencionResumenResponse {
    private Integer idAtencion;
    private LocalDateTime fechaAtencion;
    private String estado;
    private Integer idPaciente;
    private String nombrePaciente;
    private String nombreEstudiante;
    private String nombreDocente;
    private String motivoConsulta;
    private Integer numDiagnosticos;
}
