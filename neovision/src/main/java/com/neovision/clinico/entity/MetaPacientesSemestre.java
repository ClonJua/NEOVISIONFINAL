package com.neovision.clinico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Numero minimo de pacientes que un estudiante debe atender en un semestre
 * clinico (7, 8 o 9) para aprobar. Antes vivia hardcodeado y duplicado entre
 * {@code CitaServiceImpl} y el frontend de reportes; ahora es editable por el
 * admin sin necesidad de un nuevo despliegue.
 */
@Entity
@Table(name = "meta_pacientes_semestre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetaPacientesSemestre {

    @Id
    @Column(name = "semestre")
    private Integer semestre;

    @Column(name = "minimo_pacientes", nullable = false)
    private Integer minimoPacientes;
}
