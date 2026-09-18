package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Refracción del folio 2: siete sub-pruebas, cada una con OD/OI, observaciones y AV por ojo.
 * Columnas con prefijo {@code rf_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Refraccion {

    // Retinoscopía estática
    @Column(name = "rf_est_od", length = 80)    private String estaticaOd;
    @Column(name = "rf_est_oi", length = 80)    private String estaticaOi;
    @Column(name = "rf_est_od_av", length = 20) private String estaticaOdAv;
    @Column(name = "rf_est_oi_av", length = 20) private String estaticaOiAv;
    @Column(name = "rf_est_obs", columnDefinition = "TEXT") private String estaticaObservaciones;

    // Retinoscopía dinámica
    @Column(name = "rf_din_od", length = 80)    private String dinamicaOd;
    @Column(name = "rf_din_oi", length = 80)    private String dinamicaOi;
    @Column(name = "rf_din_od_av", length = 20) private String dinamicaOdAv;
    @Column(name = "rf_din_oi_av", length = 20) private String dinamicaOiAv;
    @Column(name = "rf_din_obs", columnDefinition = "TEXT") private String dinamicaObservaciones;

    // Retinoscopía bajo cicloplegia
    @Column(name = "rf_ciclo_od", length = 80)  private String cicloplegiaOd;
    @Column(name = "rf_ciclo_oi", length = 80)  private String cicloplegiaOi;
    @Column(name = "rf_ciclo_od_av", length = 20) private String cicloplegiaOdAv;
    @Column(name = "rf_ciclo_oi_av", length = 20) private String cicloplegiaOiAv;
    @Column(name = "rf_ciclo_obs", columnDefinition = "TEXT") private String cicloplegiaObservaciones;

    // Subjetivo
    @Column(name = "rf_subj_od", length = 80)   private String subjetivoOd;
    @Column(name = "rf_subj_oi", length = 80)   private String subjetivoOi;
    @Column(name = "rf_subj_od_av", length = 20) private String subjetivoOdAv;
    @Column(name = "rf_subj_oi_av", length = 20) private String subjetivoOiAv;
    @Column(name = "rf_subj_obs", columnDefinition = "TEXT") private String subjetivoObservaciones;

    // Afinación
    @Column(name = "rf_afin_od", length = 80)   private String afinacionOd;
    @Column(name = "rf_afin_oi", length = 80)   private String afinacionOi;
    @Column(name = "rf_afin_od_av", length = 20) private String afinacionOdAv;
    @Column(name = "rf_afin_oi_av", length = 20) private String afinacionOiAv;
    @Column(name = "rf_afin_obs", columnDefinition = "TEXT") private String afinacionObservaciones;

    // Balance y/o ecualización
    @Column(name = "rf_bal_od", length = 80)    private String balanceOd;
    @Column(name = "rf_bal_oi", length = 80)    private String balanceOi;
    @Column(name = "rf_bal_od_av", length = 20) private String balanceOdAv;
    @Column(name = "rf_bal_oi_av", length = 20) private String balanceOiAv;
    @Column(name = "rf_bal_obs", columnDefinition = "TEXT") private String balanceObservaciones;

    // Adición
    @Column(name = "rf_add_od", length = 80)    private String adicionOd;
    @Column(name = "rf_add_oi", length = 80)    private String adicionOi;
    @Column(name = "rf_add_od_av", length = 20) private String adicionOdAv;
    @Column(name = "rf_add_oi_av", length = 20) private String adicionOiAv;
    @Column(name = "rf_add_distancia", length = 30) private String adicionDistancia;
    @Column(name = "rf_add_obs", columnDefinition = "TEXT") private String adicionObservaciones;
}
