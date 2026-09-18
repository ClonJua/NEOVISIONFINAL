package com.neovision.clinico.historia.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de identificación del folio 1 (sección PERSONALES). Se captura como
 * snapshot en la atención; se pre-llena desde {@code Paciente} donde exista,
 * sin modificar el módulo de citas. Columnas con prefijo {@code dg_}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DatosGenerales {

    // Orden de los campos alineado con el formato fisico oficial
    // (Examen Funcional Optometrico, Universidad El Bosque), de arriba a abajo.

    @Column(name = "dg_tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "dg_numero_documento", length = 30)
    private String numeroDocumento;

    @Column(name = "dg_apellidos", length = 150)
    private String apellidos;

    @Column(name = "dg_nombres", length = 150)
    private String nombres;

    @Column(name = "dg_numero_consultorio", length = 50)
    private String numeroConsultorio;

    @Column(name = "dg_fecha_nacimiento", length = 20)
    private String fechaNacimiento;

    @Column(name = "dg_edad", length = 10)
    private String edad;

    @Column(name = "dg_estado_civil", length = 30)
    private String estadoCivil;

    @Column(name = "dg_sexo", length = 10)
    private String sexo;

    @Column(name = "dg_direccion", length = 200)
    private String direccion;

    @Column(name = "dg_localidad", length = 100)
    private String localidad;

    @Column(name = "dg_ciudad", length = 100)
    private String ciudad;

    @Column(name = "dg_celular", length = 30)
    private String celular;

    @Column(name = "dg_telefono", length = 30)
    private String telefono;

    @Column(name = "dg_ocupacion", length = 100)
    private String ocupacion;

    @Column(name = "dg_acompanante", length = 150)
    private String acompanante;

    @Column(name = "dg_acompanante_telefono", length = 30)
    private String acompananteTelefono;

    @Column(name = "dg_responsable", length = 150)
    private String responsable;

    @Column(name = "dg_responsable_telefono", length = 30)
    private String responsableTelefono;

    // El papel oficial trae "Parentesco" junto al RESPONSABLE, no al
    // acompañante — estaba mal ubicado (dg_acompanante_parentesco).
    @Column(name = "dg_responsable_parentesco", length = 50)
    private String responsableParentesco;

    @Column(name = "dg_eps", length = 150)
    private String eps;

    @Column(name = "dg_tipo_vinculacion", length = 100)
    private String tipoVinculacion;

    @Column(name = "dg_remitido_por", length = 150)
    private String remitidoPor;

    @Column(name = "dg_ultimo_control", length = 150)
    private String ultimoControlVisual;
}
