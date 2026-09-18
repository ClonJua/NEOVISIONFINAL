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
 * Código diagnóstico CIE-10 (catálogo controlado). Sustituye el texto libre en
 * los diagnósticos de una atención: el estudiante solo puede registrar códigos
 * existentes y activos, garantizando un vocabulario clínico estandarizado.
 */
@Entity
@Table(name = "cie10", indexes = {
        @Index(name = "idx_cie10_descripcion", columnList = "descripcion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cie10 {

    /** Código CIE-10 (p. ej. "H52.1"). Es la clave natural. */
    @Id
    @Column(name = "codigo", length = 10)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    /** Agrupación legible (capítulo/categoría) para filtrar en la UI. */
    @Column(name = "categoria", length = 120)
    private String categoria;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
