package co.edu.unicolombo.usuariosrender.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class InputValidatorTest {
    @Test
    public void rechazaIdentificadoresInvalidos() {
        assertTrue(InputValidator.identificadorValido("f24a7c31-9b68-4e15-a032-6d87c451be20"));
        assertFalse(InputValidator.identificadorValido("f24a7c31"));
        assertFalse(InputValidator.identificadorValido("../users"));
        assertFalse(InputValidator.identificadorValido(null));
    }

    @Test
    public void contrasenaOpcionalSoloAlActualizar() {
        assertFalse(InputValidator.contrasenaValida("", false));
        assertTrue(InputValidator.contrasenaValida("", true));
        assertFalse(InputValidator.contrasenaValida("1234567", true));
        assertTrue(InputValidator.contrasenaValida("Prueba2026!", false));
        assertFalse(InputValidator.contrasenaValida("        ", true));
        assertFalse(InputValidator.contrasenaValida("  corto  ", false));
    }

    @Test
    public void aceptaCorreosConAlias() {
        assertTrue(InputValidator.correoValido("luzdaricrespo+android@gmail.com"));
        assertFalse(InputValidator.correoValido("sin-arroba.gmail.com"));
        assertFalse(InputValidator.correoValido("luz dari@gmail.com"));
        assertFalse(InputValidator.correoValido("a@b"));
    }

    @Test
    public void respetaLimitesDelNombre() {
        assertTrue(InputValidator.nombreValido("Luz Dari Crespo"));
        assertFalse(InputValidator.nombreValido(" ab "));
        assertFalse(InputValidator.nombreValido("x".repeat(101)));
    }
}
