package com.neovision.clinico.historia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Corrección/anotación posterior sobre una atención ya FINALIZADA. Append-only:
 * una vez creada no se modifica ni elimina. Es el único mecanismo de corrección
 * permitido tras finalizar (Resolución 1995/1999, Art. 18), preservando el
 * registro original intacto.
 */
@Entity
@Table(name = "adenda", indexes = {
        @Index(name = "idx_adenda_atencion", columnList = "id_atencion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Adenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adenda")
    private Integer idAdenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_atencion", nullable = false)
    private Atencion atencion;

    @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "autor_correo", nullable = false, length = 100)
    private String autorCorreo;

    @Column(name = "autor_rol", nullable = false, length = 30)
    private String autorRol;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
