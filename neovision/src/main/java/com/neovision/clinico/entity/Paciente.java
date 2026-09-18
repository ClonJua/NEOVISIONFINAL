package com.neovision.clinico.entity;

import com.neovision.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paciente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Integer idPaciente;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30, unique = true)
    private String numeroDocumento;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    /**
     * Correo del paciente. Opcional: hay pacientes (típicamente adultos mayores)
     * que no tienen. Sin correo no se les envía confirmación ni recordatorio; el
     * contacto es por {@link #telefono}, que sí es obligatorio.
     */
    @Column(name = "correo", length = 100, unique = true)
    private String correo;

    /**
     * Cuenta de acceso al sistema. Opcional: un paciente es un sujeto de atención
     * clínica, no necesariamente un usuario. Si es {@code null}, el paciente tiene
     * historia clínica y citas pero no inicia sesión — la secretaría opera por él.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    /** Autorización de tratamiento de datos personales/clínicos (Ley 1581/2012). */
    // columnDefinition con DEFAULT para que ddl-auto=update pueda agregar la
    // columna NOT NULL sobre tablas con filas existentes sin fallar.
    @Column(name = "autoriza_tratamiento_datos", columnDefinition = "boolean not null default false")
    private Boolean autorizaTratamientoDatos = false;

    @Column(name = "fecha_autorizacion_datos")
    private LocalDateTime fechaAutorizacionDatos;

    /**
     * Representante legal (obligatorio si el paciente es menor de edad — tipo de
     * documento TI o RC). El consentimiento de tratamiento de datos de un menor
     * lo debe dar su representante, no el menor (Ley 1581 de 2012).
     */
    @Column(name = "representante_nombre", length = 150)
    private String representanteNombre;

    @Column(name = "representante_documento", length = 30)
    private String representanteDocumento;

    @Column(name = "representante_parentesco", length = 50)
    private String representanteParentesco;
}
