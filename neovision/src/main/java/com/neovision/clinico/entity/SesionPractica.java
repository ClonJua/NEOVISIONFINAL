package com.neovision.clinico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sesion_practica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesionPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Integer idSesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_salon")
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "cupos_totales", nullable = false)
    private Integer cuposTotales = 1;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToMany
    @JoinTable(
            name = "sesion_estudiante",
            joinColumns = @JoinColumn(name = "id_sesion"),
            inverseJoinColumns = @JoinColumn(name = "id_estudiante")
    )
    private List<Estudiante> estudiantes = new ArrayList<>();
}
