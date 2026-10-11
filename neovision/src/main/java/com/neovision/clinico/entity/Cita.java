package com.neovision.clinico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cita", indexes = {
        @Index(name = "idx_cita_estado", columnList = "estado"),
        @Index(name = "idx_cita_sesion", columnList = "id_sesion"),
        @Index(name = "idx_cita_paciente", columnList = "id_paciente"),
        @Index(name = "idx_cita_estudiante", columnList = "id_estudiante"),
        @Index(name = "idx_cita_estudiante2", columnList = "id_estudiante_2"),
        @Index(name = "idx_cita_estado_sesion", columnList = "estado, id_sesion")
})
@NamedEntityGraph(
        name = "cita-full",
        attributeNodes = {
                @NamedAttributeNode(value = "sesion", subgraph = "sesion-subgraph"),
                @NamedAttributeNode("estudiante"),
                @NamedAttributeNode("estudiante2"),
                @NamedAttributeNode("paciente"),
                @NamedAttributeNode("especialidad")
        },
        subgraphs = {
                @NamedSubgraph(name = "sesion-subgraph", attributeNodes = {
                        @NamedAttributeNode(value = "salon", subgraph = "salon-subgraph"),
                        @NamedAttributeNode("docente")
                }),
                @NamedSubgraph(name = "salon-subgraph", attributeNodes = {
                        @NamedAttributeNode("espacio")
                })
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer idCita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sesion", nullable = false)
    private SesionPractica sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    // En la practica clinica un paciente es atendido normalmente por DOS
    // estudiantes a la vez. Eso es UNA sola cita (un paciente, un horario, una
    // historia clinica) con hasta dos estudiantes — no dos citas distintas que
    // el paciente vea duplicadas. Opcional: un bloque puede tener un solo estudiante.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante_2")
    private Estudiante estudiante2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "DISPONIBLE";

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "fecha_reserva")
    private LocalDateTime fechaReserva;

    @Column(name = "recordatorio_24h_enviado", nullable = false)
    private Boolean recordatorio24hEnviado = false;

    @Column(name = "recordatorio_1h_enviado", nullable = false)
    private Boolean recordatorio1hEnviado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    @Column(name = "subtipo_control", length = 50)
    private String subtipoControl;

    // Registro de la cancelacion. La fila cancelada conserva al paciente para su
    // historial y para el limite de cancelaciones; el cupo se libera con una
    // cita DISPONIBLE nueva en el mismo bloque.
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    /** PACIENTE | ESTUDIANTE | ADMIN. Solo PACIENTE cuenta para el limite. */
    @Column(name = "cancelada_por", length = 20)
    private String canceladaPor;

    /** Veces que el paciente ha movido la reserva actual. Viaja con la reserva al reprogramar. */
    @Column(name = "reprogramaciones_paciente", nullable = false)
    private Integer reprogramacionesPaciente = 0;

    // Bloqueo optimista: evita que dos usuarios reserven el mismo bloque
    // "DISPONIBLE" simultáneamente sin que el sistema lo detecte.
    @Version
    @Column(name = "version")
    private Long version;
}
