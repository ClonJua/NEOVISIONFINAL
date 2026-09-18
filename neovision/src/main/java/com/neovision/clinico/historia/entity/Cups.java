package com.neovision.clinico.historia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Código de procedimiento CUPS (Clasificación Única de Procedimientos en Salud,
 * catálogo controlado). Documenta QUÉ procedimiento se realizó en la atención,
 * complementando al CIE-10 (que documenta el diagnóstico).
 */
@Entity
@Table(name = "cups", indexes = {
        @Index(name = "idx_cups_descripcion", columnList = "descripcion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cups {

    /** Código CUPS (p. ej. "890201"). Es la clave natural. */
    @Id
    @Column(name = "codigo", length = 10)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    /** Agrupación legible (p. ej. "Consulta", "Procedimientos diagnósticos") para filtrar en la UI. */
    @Column(name = "categoria", length = 120)
    private String categoria;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
