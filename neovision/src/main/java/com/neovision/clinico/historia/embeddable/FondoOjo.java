package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fondo de ojo / oftalmoscopía del folio 3 (OD/OI) + diagramas (PNG base64).
 * Columnas con prefijo {@code fo_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class FondoOjo {

    @Column(name = "fo_medios_od", length = 200)        private String mediosRefringentesOd;
    @Column(name = "fo_medios_oi", length = 200)        private String mediosRefringentesOi;
    @Column(name = "fo_coloracion_od", length = 200)    private String coloracionFondoOd;
    @Column(name = "fo_coloracion_oi", length = 200)    private String coloracionFondoOi;
    @Column(name = "fo_papila_od", length = 200)        private String papilaExcavacionOd;
    @Column(name = "fo_papila_oi", length = 200)        private String papilaExcavacionOi;
    @Column(name = "fo_art_vena_od", length = 200)      private String relacionArteriaVenaOd;
    @Column(name = "fo_art_vena_oi", length = 200)      private String relacionArteriaVenaOi;
    @Column(name = "fo_macula_od", length = 200)        private String maculaFijacionOd;
    @Column(name = "fo_macula_oi", length = 200)        private String maculaFijacionOi;

    // Diagramas del fondo (PNG en base64)
    @Column(name = "fo_diagrama_od", columnDefinition = "TEXT") private String diagramaOd;
    @Column(name = "fo_diagrama_oi", columnDefinition = "TEXT") private String diagramaOi;
}
