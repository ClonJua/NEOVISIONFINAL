package com.neovision.clinico.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EspecialidadResponse {
    private Integer idEspecialidad;
    private String nombre;
    private String observacionRecordatorio;
    private Boolean requiereSubtipo;
    private Boolean esDefault;
    private Boolean activo;
}
