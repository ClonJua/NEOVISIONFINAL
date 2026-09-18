package com.neovision.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Clase base para todos los tests Selenium de NeoVision.
 * Gestiona el ciclo de vida del WebDriver y provee helpers reutilizables.
 *
 * Prerequisitos antes de correr:
 *   1. Backend corriendo en http://localhost:8080 (mvn spring-boot:run)
 *   2. Frontend corriendo en http://localhost:5173 (npm run dev)
 *   3. PostgreSQL activo con usuarios de prueba para cada rol
 */
public abstract class BaseSeleniumTest {

    protected static final String BASE_URL = "http://localhost:5173";

    // Credenciales de prueba: deben coincidir con las que siembra TestDataSeeder
    // (todos los usuarios de demo comparten la contraseña "neovision2026").
    protected static final String ADMIN_CORREO      = "admin@neovision.com";
    protected static final String ADMIN_PASS        = "admin123";
    protected static final String PACIENTE_CORREO   = "ana.rios@gmail.com";
    protected static final String PACIENTE_PASS     = "neovision2026";
    protected static final String ESTUDIANTE_CORREO = "laura.martinez@estudiante.unbosque.edu.co";
    protected static final String ESTUDIANTE_PASS   = "neovision2026";
    protected static final String DOCENTE_CORREO    = "carlos.restrepo@unbosque.edu.co";
    protected static final String DOCENTE_PASS      = "neovision2026";

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // Descomentar para correr sin ventana visible (CI):
        // options.addArguments("--headless=new");
        options.addArguments("--window-size=1280,900");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        driver.get(BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Espera a que el elemento sea visible y lo retorna. */
    protected WebElement waitFor(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Espera a que el elemento sea clickeable y lo retorna. */
    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Ingresa al sistema como paciente (tab "Soy Paciente"). */
    protected void loginPaciente(String correo, String password) {
        driver.get(BASE_URL + "/login");
        waitForClickable(By.xpath("//button[@role='tab'][contains(.,'Soy Paciente')]")).click();
        waitFor(By.name("correo")).sendKeys(correo);
        driver.findElement(By.name("contrasena")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    /** Ingresa al sistema con cuenta institucional (campo correo visible en ambos tabs). */
    protected void loginInstitucional(String correo, String password) {
        driver.get(BASE_URL + "/login");
        // El tab institucional puede requerir correo con dominio @unbosque.edu.co
        waitFor(By.name("correo")).sendKeys(correo);
        driver.findElement(By.name("contrasena")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    /** Hace click en un tab por su texto visible. */
    protected void clickTab(String textoTab) {
        waitForClickable(By.xpath("//button[contains(.,'" + textoTab + "')]")).click();
    }

    /** Selecciona una opción en un <select> por su valor. */
    protected void selectByValue(By locator, String value) {
        new Select(waitFor(locator)).selectByValue(value);
    }

    /** Verifica que hay al menos un elemento visible que coincide con el locator. */
    protected boolean isVisible(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
