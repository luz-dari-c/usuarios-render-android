package co.edu.unicolombo.usuariosrender.data;

import java.util.regex.Pattern;

public final class InputValidator {
    private InputValidator() {}

    public static boolean identificadorValido(String value) {
        return value != null
                && value.matches(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    public static boolean nombreValido(String value) {
        return value != null && value.trim().length() >= 3 && value.trim().length() <= 100;
    }

    public static boolean correoValido(String value) {
        return value != null
                && value.length() <= 150
                && Pattern.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+", value);
    }

    public static boolean contrasenaValida(String value, boolean optional) {
        return value != null && ((optional && value.isEmpty()) || value.trim().length() >= 8);
    }
}
