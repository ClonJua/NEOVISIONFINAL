package com.neovision.clinico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "especialidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @Column(name = "nombre", nullable = false, length = 60, unique = true)
    private String nombre;

    @Column(name = "observacion_recordatorio", length = 255)
    private String observacionRecordatorio;

    @Column(name = "requiere_subtipo", nullable = false)
    private Boolean requiereSubtipo = false;

    @Column(name = "es_default", nullable = false)
    private Boolean esDefault = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
