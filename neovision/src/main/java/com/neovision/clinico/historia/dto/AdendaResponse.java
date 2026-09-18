package com.neovision.clinico.historia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdendaResponse {
    private Integer idAdenda;
    private String texto;
    private String autorCorreo;
    private String autorRol;
    private LocalDateTime fecha;
}
