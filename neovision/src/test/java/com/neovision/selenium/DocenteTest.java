package com.neovision.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC10: Flujo del docente — confirmar la asistencia de una cita de sus sesiones.
 * Movido aqui desde EstudianteTest: ahora es el docente responsable de la
 * sesion quien confirma, no el estudiante.
 */
@DisplayName("Docente: Confirmar asistencia")
class DocenteTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC10 - Docente confirma la asistencia de una cita de su sesion")
    void confirmarAsistencia() {
        loginInstitucional(DOCENTE_CORREO, DOCENTE_PASS);

        clickTab("Citas");

        WebElement botonConfirmar = waitForClickable(
                By.xpath("//button[contains(.,'Confirmar asistencia') or contains(.,'Confirmar')]")
        );
        botonConfirmar.click();

        // Puede aparecer un diálogo de confirmación
        try {
            WebElement botonAceptar = driver.findElement(
                    By.xpath("//button[contains(.,'Confirmar') or contains(.,'Aceptar') or contains(.,'Sí')]")
            );
            botonAceptar.click();
        } catch (Exception ignored) {
            // Confirmación directa sin diálogo
        }

        // Verificar que el estado cambió a COMPLETADO
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("completado") || body.contains("asistencia confirmada");
        });

        String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
        assertTrue(body.contains("completado") || body.contains("asistencia"),
                "La cita debe mostrar estado COMPLETADO tras confirmar asistencia");
    }
}
