package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Anamnesis, síntomas y antecedentes del folio 1. Columnas con prefijo {@code an_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Anamnesis {

    @Column(name = "an_como_empezo", columnDefinition = "TEXT")
    private String comoEmpezo;

    @Column(name = "an_asociado_actividades", columnDefinition = "TEXT")
    private String asociadoActividades;

    @Column(name = "an_antiguedad_molestia", length = 150)
    private String antiguedadMolestia;

    // Síntomas (checkboxes del folio 1)
    @Column(name = "an_sint_ardor")            private Boolean ardor = false;
    @Column(name = "an_sint_cansancio")        private Boolean cansancio = false;
    @Column(name = "an_sint_cefalea")          private Boolean cefalea = false;
    @Column(name = "an_sint_dolor_ocular")     private Boolean dolorOcular = false;
    @Column(name = "an_sint_fotofobia")        private Boolean fotofobia = false;
    @Column(name = "an_sint_lagrimeo")         private Boolean lagrimeo = false;
    @Column(name = "an_sint_vision_doble")     private Boolean visionDoble = false;
    @Column(name = "an_sint_hiperemia")        private Boolean hiperemia = false;
    @Column(name = "an_sint_secreciones")      private Boolean secreciones = false;
    @Column(name = "an_sint_prurito")          private Boolean prurito = false;
    @Column(name = "an_sint_miodesopsias")     private Boolean miodesopsias = false;
    @Column(name = "an_sint_confusion_leer")   private Boolean confusionAlLeer = false;
    @Column(name = "an_sint_mareo")            private Boolean mareo = false;
    @Column(name = "an_sint_otros")            private Boolean otros = false;
    @Column(name = "an_sint_otros_detalle", length = 200)
    private String otrosDetalle;

    // Antecedentes
    @Column(name = "an_antecedentes_personales", columnDefinition = "TEXT")
    private String antecedentesPersonales;

    @Column(name = "an_antecedentes_oculares", columnDefinition = "TEXT")
    private String antecedentesOculares;

    @Column(name = "an_medicamentos", columnDefinition = "TEXT")
    private String medicamentos;

    @Column(name = "an_antecedentes_familiares", columnDefinition = "TEXT")
    private String antecedentesFamiliares;
}
