package com.neovision.clinico.historia.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** Historia clínica de un paciente con el resumen de sus atenciones. */
@Getter
@Setter
public class HistoriaClinicaResponse {
    private Integer idHistoria;
    private Integer idPaciente;
    private String nombrePaciente;
    private String numeroDocumento;
    private String numeroHistoria;
    private LocalDateTime fechaApertura;
    private String estado;
    private boolean tieneConsentimiento;
    private List<AtencionResumenResponse> atenciones;
}
