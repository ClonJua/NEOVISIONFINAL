package com.neovision.clinico.historia.dto;

import com.neovision.clinico.historia.embeddable.AgudezaVisual;
import com.neovision.clinico.historia.embeddable.Anamnesis;
import com.neovision.clinico.historia.embeddable.Conducta;
import com.neovision.clinico.historia.embeddable.DatosGenerales;
import com.neovision.clinico.historia.embeddable.ExamenExterno;
import com.neovision.clinico.historia.embeddable.FondoOjo;
import com.neovision.clinico.historia.embeddable.FormulaFinal;
import com.neovision.clinico.historia.embeddable.Lensometria;
import com.neovision.clinico.historia.embeddable.Motilidad;
import com.neovision.clinico.historia.embeddable.PruebasComplementarias;
import com.neovision.clinico.historia.embeddable.Queratometria;
import com.neovision.clinico.historia.embeddable.Refraccion;
import com.neovision.clinico.historia.embeddable.SignosVitales;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Payload completo del "Examen Funcional Optométrico" para guardar/actualizar el
 * borrador de una atención. Reutiliza los mismos value objects de la entidad para
 * evitar duplicar ~150 campos. Cualquier sección puede venir nula (parcial).
 */
@Getter
@Setter
@NoArgsConstructor
public class GuardarAtencionRequest {

    private String motivoConsulta;

    private DatosGenerales datosGenerales;
    private Anamnesis anamnesis;
    private SignosVitales signosVitales;
    private Lensometria lensometria;
    private AgudezaVisual agudezaVisual;
    private Queratometria queratometria;
    private Refraccion refraccion;
    private PruebasComplementarias pruebasComplementarias;
    private FormulaFinal formulaFinal;
    private ExamenExterno examenExterno;
    private Motilidad motilidad;
    private FondoOjo fondoOjo;
    private Conducta conducta;

    private List<DiagnosticoDto> diagnosticos;
    private List<ProcedimientoDto> procedimientos;
}
