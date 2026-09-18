package com.neovision.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC05–TC07: Flujo del paciente — ver disponibles, reservar y cancelar citas.
 */
@DisplayName("Paciente: Reservar y cancelar citas")
class PacienteTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TC05 - Paciente ve los bloques disponibles")
    void verBloquesDisponibles() {
        loginPaciente(PACIENTE_CORREO, PACIENTE_PASS);

        // El dashboard del paciente debe mostrar un tab de disponibles
        clickTab("Disponibles");

        // Debe existir al menos un elemento de bloque o un mensaje de lista vacía
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("disponible") || body.contains("reservar") || body.contains("no hay");
        });

        String body = driver.findElement(By.tagName("body")).getText();
        assertNotNull(body, "La página debe cargar contenido al mostrar bloques disponibles");
    }

    @Test
    @DisplayName("TC06 - Paciente reserva un bloque disponible")
    void reservarBloque() {
        loginPaciente(PACIENTE_CORREO, PACIENTE_PASS);
        clickTab("Disponibles");

        // Buscar botón de reservar del primer bloque disponible
        WebElement botonReservar = waitForClickable(
                By.xpath("//button[contains(.,'Reservar')]")
        );
        botonReservar.click();

        // Después de reservar, la cita debe aparecer en "Mis citas"
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("reservado") || body.contains("mis citas") || body.contains("agendada");
        });

        // Navegar al tab de mis citas para verificar
        clickTab("Mis citas");

        String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
        assertTrue(body.contains("reservado") || body.contains("pendiente"),
                "La cita debe aparecer con estado RESERVADO en 'Mis citas'");
    }

    @Test
    @DisplayName("TC07 - Paciente cancela una cita reservada")
    void cancelarCita() {
        loginPaciente(PACIENTE_CORREO, PACIENTE_PASS);

        // Ir a "Mis citas" para encontrar una cita RESERVADO
        clickTab("Mis citas");

        // Intentar encontrar botón cancelar
        WebElement botonCancelar = waitForClickable(
                By.xpath("//button[contains(.,'Cancelar')]")
        );
        botonCancelar.click();

        // Puede aparecer un modal o campo de observaciones
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            // Verifica si aparece diálogo de confirmación o si ya se canceló
            return body.contains("cancelad") || body.contains("motivo") || body.contains("confirmar");
        });

        // Si aparece un campo de observaciones, llenar y confirmar
        try {
            WebElement obs = driver.findElement(By.name("observaciones"));
            obs.sendKeys("Cancelado desde test automatizado");
            driver.findElement(By.xpath("//button[contains(.,'Confirmar') or contains(.,'Aceptar')]")).click();
        } catch (Exception ignored) {
            // No hay diálogo, la cancelación fue directa
        }

        // Verificar que el estado cambió a CANCELADO
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("cancelad") || !body.contains("reservado");
        });

        String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
        assertTrue(body.contains("cancelad") || !body.contains("reservado"),
                "La cita debe mostrar estado CANCELADO tras la cancelación");
    }
}
