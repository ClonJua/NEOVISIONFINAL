package com.neovision.clinico.historia.entity;

import com.neovision.clinico.entity.Cita;
import com.neovision.clinico.entity.Docente;
import com.neovision.clinico.entity.Estudiante;
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
import com.neovision.clinico.historia.enums.EstadoAtencion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una atención clínica (encuentro) sobre una historia. Núcleo del módulo:
 * concentra todos los campos del "Examen Funcional Optométrico" (folios 1–4)
 * organizados por sección. Conceptualmente alineada con FHIR {@code Encounter}.
 *
 * <p>Inmutabilidad (Resolución 1995/1999, Art. 18): al pasar a
 * {@link EstadoAtencion#FINALIZADA} se calcula y guarda {@code hashIntegridad}
 * y la atención no puede volver a editarse; las correcciones se hacen por adenda.
 */
@Entity
@Table(name = "atencion", indexes = {
        @Index(name = "idx_atencion_historia", columnList = "id_historia"),
        @Index(name = "idx_atencion_estudiante", columnList = "id_estudiante"),
        @Index(name = "idx_atencion_docente", columnList = "id_docente_supervisor"),
        @Index(name = "idx_atencion_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
public class Atencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atencion")
    private Integer idAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_historia", nullable = false)
    private HistoriaClinica historiaClinica;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente_supervisor")
    private Docente docenteSupervisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoAtencion estado = EstadoAtencion.BORRADOR;

    @Column(name = "fecha_atencion", nullable = false)
    private LocalDateTime fechaAtencion;

    @Column(name = "fecha_envio_revision")
    private LocalDateTime fechaEnvioRevision;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    /**
     * Momento de la última devolución del docente. Reabre la ventana de edición
     * del estudiante: el tiempo que la atención pasó esperando revisión no debe
     * consumir su plazo, porque la demora no es suya.
     */
    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;

    @Column(name = "motivo_devolucion", columnDefinition = "TEXT")
    private String motivoDevolucion;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    // Firmas (PNG base64). La del estudiante se captura al enviar a revisión,
    // la del docente al finalizar, la del paciente es opcional.
    @Column(name = "firma_estudiante", columnDefinition = "TEXT")
    private String firmaEstudiante;

    @Column(name = "firma_docente", columnDefinition = "TEXT")
    private String firmaDocente;

    @Column(name = "firma_paciente", columnDefinition = "TEXT")
    private String firmaPaciente;

    // Sello de integridad SHA-256 calculado al finalizar.
    @Column(name = "hash_integridad", length = 64)
    private String hashIntegridad;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Bloqueo optimista: evita que dos ediciones concurrentes (p. ej. estudiante y
    // docente, o dos pestañas) se sobreescriban silenciosamente (lost update).
    @Version
    @Column(name = "version")
    private Long version;

    // ===================== Secciones clínicas (folios 1–4) =====================
    @Embedded private DatosGenerales datosGenerales = new DatosGenerales();
    @Embedded private Anamnesis anamnesis = new Anamnesis();
    @Embedded private SignosVitales signosVitales = new SignosVitales();
    @Embedded private Lensometria lensometria = new Lensometria();
    @Embedded private AgudezaVisual agudezaVisual = new AgudezaVisual();
    @Embedded private Queratometria queratometria = new Queratometria();
    @Embedded private Refraccion refraccion = new Refraccion();
    @Embedded private PruebasComplementarias pruebasComplementarias = new PruebasComplementarias();
    @Embedded private FormulaFinal formulaFinal = new FormulaFinal();
    @Embedded private ExamenExterno examenExterno = new ExamenExterno();
    @Embedded private Motilidad motilidad = new Motilidad();
    @Embedded private FondoOjo fondoOjo = new FondoOjo();
    @Embedded private Conducta conducta = new Conducta();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Procedimiento> procedimientos = new ArrayList<>();
}
