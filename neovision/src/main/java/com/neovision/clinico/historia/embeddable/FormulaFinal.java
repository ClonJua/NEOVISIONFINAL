package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fórmula final / prescripción del folio 2: lejos/media/cerca/adición × OD/OI ×
 * (esfera, cilindro, eje, prisma, base, AV) + datos del lente.
 * Columnas con prefijo {@code ff_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class FormulaFinal {

    // LEJOS
    @Column(name = "ff_lejos_od_esf", length = 20) private String lejosOdEsfera;
    @Column(name = "ff_lejos_od_cil", length = 20) private String lejosOdCilindro;
    @Column(name = "ff_lejos_od_eje", length = 20) private String lejosOdEje;
    @Column(name = "ff_lejos_od_pri", length = 20) private String lejosOdPrisma;
    @Column(name = "ff_lejos_od_base", length = 20) private String lejosOdBase;
    @Column(name = "ff_lejos_od_av", length = 20)  private String lejosOdAv;
    @Column(name = "ff_lejos_oi_esf", length = 20) private String lejosOiEsfera;
    @Column(name = "ff_lejos_oi_cil", length = 20) private String lejosOiCilindro;
    @Column(name = "ff_lejos_oi_eje", length = 20) private String lejosOiEje;
    @Column(name = "ff_lejos_oi_pri", length = 20) private String lejosOiPrisma;
    @Column(name = "ff_lejos_oi_base", length = 20) private String lejosOiBase;
    @Column(name = "ff_lejos_oi_av", length = 20)  private String lejosOiAv;

    // MEDIA
    @Column(name = "ff_media_od_esf", length = 20) private String mediaOdEsfera;
    @Column(name = "ff_media_od_cil", length = 20) private String mediaOdCilindro;
    @Column(name = "ff_media_od_eje", length = 20) private String mediaOdEje;
    @Column(name = "ff_media_od_pri", length = 20) private String mediaOdPrisma;
    @Column(name = "ff_media_od_base", length = 20) private String mediaOdBase;
    @Column(name = "ff_media_od_av", length = 20)  private String mediaOdAv;
    @Column(name = "ff_media_oi_esf", length = 20) private String mediaOiEsfera;
    @Column(name = "ff_media_oi_cil", length = 20) private String mediaOiCilindro;
    @Column(name = "ff_media_oi_eje", length = 20) private String mediaOiEje;
    @Column(name = "ff_media_oi_pri", length = 20) private String mediaOiPrisma;
    @Column(name = "ff_media_oi_base", length = 20) private String mediaOiBase;
    @Column(name = "ff_media_oi_av", length = 20)  private String mediaOiAv;

    // CERCA
    @Column(name = "ff_cerca_od_esf", length = 20) private String cercaOdEsfera;
    @Column(name = "ff_cerca_od_cil", length = 20) private String cercaOdCilindro;
    @Column(name = "ff_cerca_od_eje", length = 20) private String cercaOdEje;
    @Column(name = "ff_cerca_od_pri", length = 20) private String cercaOdPrisma;
    @Column(name = "ff_cerca_od_base", length = 20) private String cercaOdBase;
    @Column(name = "ff_cerca_od_av", length = 20)  private String cercaOdAv;
    @Column(name = "ff_cerca_oi_esf", length = 20) private String cercaOiEsfera;
    @Column(name = "ff_cerca_oi_cil", length = 20) private String cercaOiCilindro;
    @Column(name = "ff_cerca_oi_eje", length = 20) private String cercaOiEje;
    @Column(name = "ff_cerca_oi_pri", length = 20) private String cercaOiPrisma;
    @Column(name = "ff_cerca_oi_base", length = 20) private String cercaOiBase;
    @Column(name = "ff_cerca_oi_av", length = 20)  private String cercaOiAv;

    // ADICIÓN
    @Column(name = "ff_add_od_esf", length = 20) private String adicionOdEsfera;
    @Column(name = "ff_add_od_cil", length = 20) private String adicionOdCilindro;
    @Column(name = "ff_add_od_eje", length = 20) private String adicionOdEje;
    @Column(name = "ff_add_od_pri", length = 20) private String adicionOdPrisma;
    @Column(name = "ff_add_od_base", length = 20) private String adicionOdBase;
    @Column(name = "ff_add_od_av", length = 20)  private String adicionOdAv;
    @Column(name = "ff_add_oi_esf", length = 20) private String adicionOiEsfera;
    @Column(name = "ff_add_oi_cil", length = 20) private String adicionOiCilindro;
    @Column(name = "ff_add_oi_eje", length = 20) private String adicionOiEje;
    @Column(name = "ff_add_oi_pri", length = 20) private String adicionOiPrisma;
    @Column(name = "ff_add_oi_base", length = 20) private String adicionOiBase;
    @Column(name = "ff_add_oi_av", length = 20)  private String adicionOiAv;

    // Datos del lente
    @Column(name = "ff_dist_naso_pupilar", length = 30) private String distanciaNasoPupilar;
    @Column(name = "ff_tipo_lente_marca", length = 150) private String tipoLenteYMarca;
    @Column(name = "ff_material", length = 100)         private String material;
    @Column(name = "ff_filtros", length = 100)          private String filtros;
    @Column(name = "ff_color", length = 100)            private String color;
    @Column(name = "ff_uso", length = 100)              private String uso;
    @Column(name = "ff_observaciones", columnDefinition = "TEXT") private String observaciones;
}
