package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Conducta, remisiones y control del folio 4. Columnas con prefijo {@code cd_}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Conducta {

    @Column(name = "cd_conducta", columnDefinition = "TEXT")   private String conducta;
    @Column(name = "cd_remisiones", columnDefinition = "TEXT") private String remisiones;
    @Column(name = "cd_control", columnDefinition = "TEXT")    private String control;
}
