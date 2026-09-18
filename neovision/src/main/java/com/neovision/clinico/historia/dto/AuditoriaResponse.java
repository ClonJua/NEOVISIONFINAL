package com.neovision.clinico.historia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditoriaResponse {
    private Long idAuditoria;
    private String usuarioCorreo;
    private String rol;
    private String accion;
    private String entidad;
    private Integer entidadId;
    private LocalDateTime fechaHora;
    private String detalle;
}
