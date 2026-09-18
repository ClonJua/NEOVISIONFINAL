package com.neovision.clinico.historia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SolicitudResponse {
    private Integer idSolicitud;
    private Integer idPaciente;
    private String nombrePaciente;
    private String correoPaciente;
    private LocalDateTime fechaSolicitud;
    private String estado;
    private String motivo;
    private String motivoRechazo;
    private String resueltaPorCorreo;
    private LocalDateTime fechaResolucion;
}
