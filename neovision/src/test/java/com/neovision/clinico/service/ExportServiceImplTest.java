package com.neovision.clinico.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de la mitigación contra CSV Injection / Formula Injection (OWASP):
 * un campo de texto libre controlado por el paciente (p. ej. "observaciones"
 * al cancelar una cita) no debe poder inyectar una fórmula que Excel/Sheets
 * ejecute al abrir el CSV exportado por el admin.
 */
class ExportServiceImplTest {

    private String sanitizar(String valor) throws Exception {
        Method m = ExportServiceImpl.class.getDeclaredMethod("sanitizarCsv", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, valor);
    }

    @Test
    void anteponeApostrofeSiEmpiezaConIgual() throws Exception {
        assertThat(sanitizar("=cmd|'/c calc'!A1")).isEqualTo("'=cmd|'/c calc'!A1");
    }

    @Test
    void anteponeApostrofeSiEmpiezaConMas() throws Exception {
        assertThat(sanitizar("+1+1")).startsWith("'+");
    }

    @Test
    void anteponeApostrofeSiEmpiezaConMenos() throws Exception {
        assertThat(sanitizar("-2+3")).startsWith("'-");
    }

    @Test
    void anteponeApostrofeSiEmpiezaConArroba() throws Exception {
        assertThat(sanitizar("@SUM(A1:A9)")).startsWith("'@");
    }

    @Test
    void anteponeApostrofeSiEmpiezaConTabOCarriageReturn() throws Exception {
        assertThat(sanitizar("\tmalicioso")).startsWith("'\t");
        assertThat(sanitizar("\rmalicioso")).startsWith("'\r");
    }

    @Test
    void noAlteraTextoNormal() throws Exception {
        assertThat(sanitizar("Paciente canceló por imprevisto laboral"))
                .isEqualTo("Paciente canceló por imprevisto laboral");
    }

    @Test
    void noFallaConValorNuloOVacio() throws Exception {
        assertThat(sanitizar(null)).isNull();
        assertThat(sanitizar("")).isEmpty();
    }

    @Test
    void noAlteraSiElCaracterPeligrosoNoEstaAlInicio() throws Exception {
        // Solo importa el primer caracter: Excel solo interpreta formula si el campo EMPIEZA con estos.
        assertThat(sanitizar("Costo: -100")).isEqualTo("Costo: -100");
    }
}
