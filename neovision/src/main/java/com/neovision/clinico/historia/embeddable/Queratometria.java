package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Queratometría del folio 2. Columnas con prefijo {@code qm_}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Queratometria {

    @Column(name = "qm_od", length = 100)       private String ojoDerecho;
    @Column(name = "qm_oi", length = 100)       private String ojoIzquierdo;
    @Column(name = "qm_miras_od", length = 100) private String mirasOd;
    @Column(name = "qm_miras_oi", length = 100) private String mirasOi;
    @Column(name = "qm_av", length = 20)        private String av;

    @Column(name = "qm_observaciones_instrumento", columnDefinition = "TEXT")
    private String observacionesEInstrumento;
}
