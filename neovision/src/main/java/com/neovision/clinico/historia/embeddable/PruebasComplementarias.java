package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pruebas complementarias del folio 2. Columnas con prefijo {@code pc_}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PruebasComplementarias {

    @Column(name = "pc_vision_cromatica_test", length = 100) private String visionCromaticaTest;
    @Column(name = "pc_vision_cromatica_od", length = 100)   private String visionCromaticaOd;
    @Column(name = "pc_vision_cromatica_oi", length = 100)   private String visionCromaticaOi;

    @Column(name = "pc_estereopsis", length = 100)            private String estereopsis;
    @Column(name = "pc_estereopsis_test", length = 100)       private String estereopsisTest;

    @Column(name = "pc_campo_visual_test", length = 100)     private String campoVisualTest;
    @Column(name = "pc_campo_visual_od", length = 100)       private String campoVisualOd;
    @Column(name = "pc_campo_visual_oi", length = 100)       private String campoVisualOi;

    @Column(name = "pc_sensibilidad_test", length = 100)     private String sensibilidadContrasteTest;
    @Column(name = "pc_sensibilidad_od", length = 100)       private String sensibilidadContrasteOd;
    @Column(name = "pc_sensibilidad_oi", length = 100)       private String sensibilidadContrasteOi;

    @Column(name = "pc_foria_distancia", length = 50)        private String foriaDistancia;
    @Column(name = "pc_foria_40cm", length = 50)             private String foria40cm;
    @Column(name = "pc_foria_test", length = 150)            private String foriaTest;

    @Column(name = "pc_otras", columnDefinition = "TEXT")    private String otrasPruebas;
}
