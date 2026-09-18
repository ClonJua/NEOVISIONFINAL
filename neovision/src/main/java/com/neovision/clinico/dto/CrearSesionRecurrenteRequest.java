package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Datos para crear sesiones recurrentes en un rango de fechas para un día de la semana.")
public class CrearSesionRecurrenteRequest {

    @NotNull(message = "Día de semana requerido (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)")
    @Pattern(regexp = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY")
    private String diaSemana;

    @NotNull(message = "Fecha inicio requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "Fecha fin requerida")
    private LocalDate fechaFin;

    @NotNull(message = "Hora inicio requerida")
    private LocalTime horaInicio;

    @NotNull(message = "Hora fin requerida")
    private LocalTime horaFin;

    @NotNull(message = "Cupos totales requerido")
    private Integer cuposTotales;

    private Integer idDocente;
    private Integer idSalon;
    private List<Integer> idsEstudiantes;

    public CrearSesionRecurrenteRequest() {}

    public CrearSesionRecurrenteRequest(String diaSemana, LocalDate fechaInicio, LocalDate fechaFin,
                                         LocalTime horaInicio, LocalTime horaFin, Integer cuposTotales,
                                         Integer idDocente, Integer idSalon) {
        this.diaSemana = diaSemana;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.cuposTotales = cuposTotales;
        this.idDocente = idDocente;
        this.idSalon = idSalon;
    }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public Integer getCuposTotales() { return cuposTotales; }
    public void setCuposTotales(Integer cuposTotales) { this.cuposTotales = cuposTotales; }

    public Integer getIdDocente() { return idDocente; }
    public void setIdDocente(Integer idDocente) { this.idDocente = idDocente; }

    public Integer getIdSalon() { return idSalon; }
    public void setIdSalon(Integer idSalon) { this.idSalon = idSalon; }

    public List<Integer> getIdsEstudiantes() { return idsEstudiantes; }
    public void setIdsEstudiantes(List<Integer> idsEstudiantes) { this.idsEstudiantes = idsEstudiantes; }
}
