package com.neovision.clinico.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgresoResponse {
    private int semestre;
    private int atendidos;
    private int meta;
}
