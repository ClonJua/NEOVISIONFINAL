package com.neovision.clinico.historia.entity;

import com.neovision.clinico.historia.enums.AccionAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bitácora de auditoría append-only del módulo de historia clínica. Registra
 * cada acción (incluidas las lecturas) para garantizar la trazabilidad exigida
 * (HU05) sobre datos sensibles (Ley 1581). No se modifica ni elimina: el módulo
 * no expone endpoints de update/delete sobre esta tabla.
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_paciente", columnList = "id_paciente"),
        @Index(name = "idx_auditoria_entidad", columnList = "entidad, entidad_id"),
        @Index(name = "idx_auditoria_timestamp", columnList = "fecha_hora")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "usuario_correo", nullable = false, length = 100)
    private String usuarioCorreo;

    @Column(name = "rol", length = 30)
    private String rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 40)
    private AccionAuditoria accion;

    @Column(name = "entidad", length = 40)
    private String entidad;

    @Column(name = "entidad_id")
    private Integer entidadId;

    @Column(name = "id_paciente")
    private Integer idPaciente;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    // Origen de la acción (trazabilidad reforzada).
    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // Cadena de integridad: cada registro encadena el hash del anterior, haciendo
    // la bitácora a prueba de manipulación (alterar un registro rompe la cadena).
    @Column(name = "hash_previo", length = 64)
    private String hashPrevio;

    @Column(name = "hash_registro", length = 64)
    private String hashRegistro;
}
