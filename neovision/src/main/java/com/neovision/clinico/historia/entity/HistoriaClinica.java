package com.neovision.clinico.historia.entity;

import com.neovision.clinico.entity.Paciente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Contenedor de la historia clínica de un paciente (1:1). Agrupa sus atenciones
 * (encuentros) y el consentimiento informado. Conceptualmente alineada con el
 * recurso FHIR {@code Patient} + su registro clínico.
 */
@Entity
@Table(name = "historia_clinica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historia")
    private Integer idHistoria;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false, unique = true)
    private Paciente paciente;

    @Column(name = "numero_historia", length = 50, unique = true)
    private String numeroHistoria;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "abierta_por_correo", length = 100)
    private String abiertaPorCorreo;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVA";
}
