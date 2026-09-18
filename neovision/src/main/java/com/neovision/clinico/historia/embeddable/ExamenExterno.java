package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Examen externo / biomicroscopía del folio 3 (tabla OD/OI), Schirmer, ruptura
 * lagrimal, tonometría, y los diagramas del segmento anterior (PNG base64).
 * Columnas con prefijo {@code ee_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ExamenExterno {

    @Column(name = "ee_general", columnDefinition = "TEXT") private String general;

    // Biomicroscopía por estructura (OD / OI)
    @Column(name = "ee_orbita_od", length = 200)     private String orbitaOd;
    @Column(name = "ee_orbita_oi", length = 200)     private String orbitaOi;
    @Column(name = "ee_cejas_od", length = 200)      private String cejasOd;
    @Column(name = "ee_cejas_oi", length = 200)      private String cejasOi;
    @Column(name = "ee_pestanas_od", length = 200)   private String pestanasOd;
    @Column(name = "ee_pestanas_oi", length = 200)   private String pestanasOi;
    @Column(name = "ee_parpados_od", length = 200)   private String parpadosOd;
    @Column(name = "ee_parpados_oi", length = 200)   private String parpadosOi;
    @Column(name = "ee_conjuntiva_od", length = 200) private String conjuntivaOd;
    @Column(name = "ee_conjuntiva_oi", length = 200) private String conjuntivaOi;
    @Column(name = "ee_esclera_od", length = 200)    private String escleraOd;
    @Column(name = "ee_esclera_oi", length = 200)    private String escleraOi;
    @Column(name = "ee_cornea_od", length = 200)     private String corneaOd;
    @Column(name = "ee_cornea_oi", length = 200)     private String corneaOi;
    @Column(name = "ee_iris_od", length = 200)       private String irisOd;
    @Column(name = "ee_iris_oi", length = 200)       private String irisOi;
    @Column(name = "ee_pupila_od", length = 200)     private String pupilaOd;
    @Column(name = "ee_pupila_oi", length = 200)     private String pupilaOi;
    @Column(name = "ee_camara_ant_od", length = 200) private String camaraAnteriorOd;
    @Column(name = "ee_camara_ant_oi", length = 200) private String camaraAnteriorOi;
    @Column(name = "ee_cristalino_od", length = 200) private String cristalinoOd;
    @Column(name = "ee_cristalino_oi", length = 200) private String cristalinoOi;

    // Pruebas lagrimales / presión
    @Column(name = "ee_schirmer_od", length = 50)    private String schirmerOd;
    @Column(name = "ee_schirmer_oi", length = 50)    private String schirmerOi;
    @Column(name = "ee_brut_od", length = 50)        private String rupturaLagrimalOd;
    @Column(name = "ee_brut_oi", length = 50)        private String rupturaLagrimalOi;
    @Column(name = "ee_tono_instrumento", length = 80) private String tonometriaInstrumento;
    @Column(name = "ee_tono_hora", length = 30)      private String tonometriaHora;
    @Column(name = "ee_tono_od", length = 50)        private String tonometriaOd;
    @Column(name = "ee_tono_oi", length = 50)        private String tonometriaOi;

    // Diagramas del segmento anterior (PNG en base64)
    @Column(name = "ee_diagrama_od", columnDefinition = "TEXT") private String diagramaOd;
    @Column(name = "ee_diagrama_oi", columnDefinition = "TEXT") private String diagramaOi;
}
