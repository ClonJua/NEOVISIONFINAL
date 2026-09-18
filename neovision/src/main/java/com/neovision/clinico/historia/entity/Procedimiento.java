package com.neovision.clinico.historia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Procedimiento (folio 4) con código CUPS. Documenta qué se le hizo al paciente
 * en la atención, complementando al diagnóstico (CIE-10). Una atención puede
 * tener varios. Se crea y bloquea junto con la atención al finalizar.
 */
@Entity
@Table(name = "procedimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Procedimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procedimiento")
    private Integer idProcedimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_atencion", nullable = false)
    private Atencion atencion;

    @Column(name = "codigo_cups", length = 20)
    private String codigoCups;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "orden")
    private Integer orden;
}
