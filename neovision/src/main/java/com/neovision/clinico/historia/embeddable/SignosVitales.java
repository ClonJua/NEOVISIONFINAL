package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Signos vitales del folio 1. Columnas con prefijo {@code sv_}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SignosVitales {

    @Column(name = "sv_peso", length = 20)
    private String peso;

    @Column(name = "sv_estatura", length = 20)
    private String estatura;

    @Column(name = "sv_imc", length = 20)
    private String imc;

    @Column(name = "sv_tension_arterial", length = 30)
    private String tensionArterial;
}
