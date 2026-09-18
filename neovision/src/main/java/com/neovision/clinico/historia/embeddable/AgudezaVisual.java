package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agudeza visual del folio 1: sin/con corrección × (lejos, cerca, estenopeico)
 * × (OD, OI, AO). Columnas con prefijo {@code av_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AgudezaVisual {

    // Sin corrección
    @Column(name = "av_sc_od_lejos", length = 20)  private String scOdLejos;
    @Column(name = "av_sc_od_cerca", length = 20)  private String scOdCerca;
    @Column(name = "av_sc_od_esten", length = 20)  private String scOdEstenopeico;
    @Column(name = "av_sc_oi_lejos", length = 20)  private String scOiLejos;
    @Column(name = "av_sc_oi_cerca", length = 20)  private String scOiCerca;
    @Column(name = "av_sc_oi_esten", length = 20)  private String scOiEstenopeico;
    @Column(name = "av_sc_ao_lejos", length = 20)  private String scAoLejos;
    @Column(name = "av_sc_ao_cerca", length = 20)  private String scAoCerca;
    @Column(name = "av_sc_ao_esten", length = 20)  private String scAoEstenopeico;

    // Con corrección
    @Column(name = "av_cc_od_lejos", length = 20)  private String ccOdLejos;
    @Column(name = "av_cc_od_cerca", length = 20)  private String ccOdCerca;
    @Column(name = "av_cc_od_esten", length = 20)  private String ccOdEstenopeico;
    @Column(name = "av_cc_oi_lejos", length = 20)  private String ccOiLejos;
    @Column(name = "av_cc_oi_cerca", length = 20)  private String ccOiCerca;
    @Column(name = "av_cc_oi_esten", length = 20)  private String ccOiEstenopeico;
    @Column(name = "av_cc_ao_lejos", length = 20)  private String ccAoLejos;
    @Column(name = "av_cc_ao_cerca", length = 20)  private String ccAoCerca;
    @Column(name = "av_cc_ao_esten", length = 20)  private String ccAoEstenopeico;

    @Column(name = "av_sistema_notacion", length = 50) private String sistemaNotacion;
    @Column(name = "av_optotipo", length = 50)         private String optotipo;
}
