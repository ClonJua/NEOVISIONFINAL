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
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle completo de una atención (todos los folios) + adendas e integridad. */
@Getter
@Setter
public class AtencionResponse {

    private Integer idAtencion;
    private Integer idHistoria;
    private Integer idPaciente;
    private String nombrePaciente;
    private Integer idCita;
    private String fechaCita;

    private String estado;
    private LocalDateTime fechaAtencion;
    private LocalDateTime fechaEnvioRevision;
    private LocalDateTime fechaFinalizacion;
    private String motivoDevolucion;

    /**
     * Último día en que el estudiante puede editar los campos clínicos. Es el mayor
     * entre la ventana de la cita y la que abre la última devolución del docente, de
     * modo que el tiempo en revisión no consuma su plazo. {@code null} si la atención
     * no cuelga de una cita y nunca fue devuelta (no aplica la ventana).
     */
    private LocalDate limiteEdicion;

    /** {@code true} si hoy el estudiante todavía puede editar. Evita que el cliente reimplemente la regla. */
    private Boolean editablePorEstudiante;

    private Integer idEstudiante;
    private String nombreEstudiante;
    private Integer idDocenteSupervisor;
    private String nombreDocente;

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

    private String firmaEstudiante;
    private String firmaDocente;
    private String firmaPaciente;
    private String hashIntegridad;

    private List<AdendaResponse> adendas;
}
