package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Motilidad / reflejos pupilares del folio 3. Columnas con prefijo {@code mt_}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Motilidad {

    // Reflejos pupilares
    @Column(name = "mt_fotomotor_od", length = 80)   private String fotomotorDirectoOd;
    @Column(name = "mt_consensual_od", length = 80)  private String consensualOd;
    @Column(name = "mt_acomodativo_od", length = 80) private String acomodativoOd;
    @Column(name = "mt_fotomotor_oi", length = 80)   private String fotomotorDirectoOi;
    @Column(name = "mt_consensual_oi", length = 80)  private String consensualOi;
    @Column(name = "mt_acomodativo_oi", length = 80) private String acomodativoOi;

    @Column(name = "mt_defecto_pupilar", columnDefinition = "TEXT") private String defectoPupilar;
    @Column(name = "mt_ojo_dominante", length = 50)  private String ojoDominante;

    // Kappa / Hirschberg
    @Column(name = "mt_kappa_od", length = 50)       private String kappaOd;
    @Column(name = "mt_kappa_oi", length = 50)       private String kappaOi;
    @Column(name = "mt_hirschberg", length = 80)     private String hirschberg;

    // Estado motor
    @Column(name = "mt_estado_motor_test", length = 150) private String estadoMotorTest;
    @Column(name = "mt_estado_motor_con_correccion", length = 30) private String estadoMotorCorreccion; // SIN / CON
    @Column(name = "mt_estado_motor_6m", length = 40)  private String estadoMotor6m;
    @Column(name = "mt_estado_motor_4m", length = 40)  private String estadoMotor4m;
    @Column(name = "mt_estado_motor_3m", length = 40)  private String estadoMotor3m;
    @Column(name = "mt_estado_motor_40cm", length = 40) private String estadoMotor40cm;
    @Column(name = "mt_estado_motor_20cm", length = 40) private String estadoMotor20cm;

    // Punto próximo de convergencia (PPC)
    @Column(name = "mt_ppc_defecto_real", length = 80)   private String ppcDefectoReal;
    @Column(name = "mt_ppc_luz", length = 80)            private String ppcLuz;
    @Column(name = "mt_ppc_luz_filtro_rojo", length = 80) private String ppcLuzFiltroRojo;

    // Ducciones / versiones
    @Column(name = "mt_ducciones_od", length = 150)  private String duccionesOd;
    @Column(name = "mt_ducciones_oi", length = 150)  private String duccionesOi;

    // Versiones: diagrama de las 9 posiciones de mirada (PNG en base64), uno por ojo.
    @Column(name = "mt_diagrama_versiones_od", columnDefinition = "TEXT") private String diagramaVersionesOd;
    @Column(name = "mt_diagrama_versiones_oi", columnDefinition = "TEXT") private String diagramaVersionesOi;

    @Column(name = "mt_observaciones", columnDefinition = "TEXT") private String observaciones;
}
