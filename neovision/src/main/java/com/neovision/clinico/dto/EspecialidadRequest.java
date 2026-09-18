package com.neovision.clinico.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EspecialidadRequest {
    @NotBlank
    private String nombre;
    private String observacionRecordatorio;
    private Boolean requiereSubtipo;
    private Boolean esDefault;
}
