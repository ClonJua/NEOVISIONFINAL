package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lensometría del folio 1 (tabla OD/OI × esfera, cilindro, eje, prisma, base,
 * adición, dist. mecánica) + diseño del lente. Columnas con prefijo {@code lm_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Lensometria {

    // Ojo derecho
    @Column(name = "lm_od_esfera", length = 20)   private String odEsfera;
    @Column(name = "lm_od_cilindro", length = 20) private String odCilindro;
    @Column(name = "lm_od_eje", length = 20)      private String odEje;
    @Column(name = "lm_od_prisma", length = 20)   private String odPrisma;
    @Column(name = "lm_od_base", length = 20)     private String odBase;
    @Column(name = "lm_od_adicion", length = 20)  private String odAdicion;
    @Column(name = "lm_od_dist_mecanica", length = 20) private String odDistMecanica;

    // Ojo izquierdo
    @Column(name = "lm_oi_esfera", length = 20)   private String oiEsfera;
    @Column(name = "lm_oi_cilindro", length = 20) private String oiCilindro;
    @Column(name = "lm_oi_eje", length = 20)      private String oiEje;
    @Column(name = "lm_oi_prisma", length = 20)   private String oiPrisma;
    @Column(name = "lm_oi_base", length = 20)     private String oiBase;
    @Column(name = "lm_oi_adicion", length = 20)  private String oiAdicion;
    @Column(name = "lm_oi_dist_mecanica", length = 20) private String oiDistMecanica;

    // Diseño del lente
    @Column(name = "lm_diseno", length = 100)    private String diseno;
    @Column(name = "lm_material", length = 100)  private String material;
    @Column(name = "lm_uso", length = 100)       private String uso;
    @Column(name = "lm_otros", length = 200)     private String otros;
    @Column(name = "lm_dist_naso_pupilar", length = 30) private String distanciaNasoPupilar;
}
