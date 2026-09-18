package com.neovision.clinico.historia.entity;

import com.neovision.clinico.entity.Paciente;
import com.neovision.clinico.historia.enums.CanalSolicitud;
import com.neovision.clinico.historia.enums.EstadoSolicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud del paciente para recibir una copia de su historia clínica
 * (derecho de acceso — Ley 1266/2008, Resolución 1995/1999). El admin la
 * aprueba (se envía el PDF al correo registrado) o la rechaza con motivo.
 */
@Entity
@Table(name = "solicitud_historia", indexes = {
        @Index(name = "idx_solicitud_paciente", columnList = "id_paciente"),
        @Index(name = "idx_solicitud_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudHistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "resuelta_por_correo", length = 100)
    private String resueltaPorCorreo;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    /**
     * Canal por el que llegó. En {@link CanalSolicitud#PRESENCIAL} la identidad
     * del paciente la verificó quien aparece en {@link #creadaPorCorreo}, no la
     * autenticación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalSolicitud canal = CanalSolicitud.DIGITAL;

    /**
     * Quién registró la solicitud. En el canal digital es el propio paciente
     * (queda {@code null}); en el presencial, la persona de secretaría que
     * verificó el documento — es el responsable de esa verificación.
     */
    @Column(name = "creada_por_correo", length = 100)
    private String creadaPorCorreo;
}
