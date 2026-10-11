
import java.util.regex.Pattern;

/** Validaciones reutilizables para los formularios de pacientes y doctores. */
public final class Validaciones {
    private static final Pattern CORREO =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validaciones() {}

    public static String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return valor.trim();
    }

    public static String telefono(String valor) {
        if (valor == null || !valor.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("El teléfono debe contener exactamente 8 dígitos, sin guiones.");
        }
        return valor;
    }

    public static String correo(String valor) {
        if (valor == null || !CORREO.matcher(valor.trim()).matches()) {
            throw new IllegalArgumentException("Escribe un correo válido. Ejemplo: nombre@correo.com");
        }
        return valor.trim();
    }

    public static int edad(String valor) {
        try {
            int edad = Integer.parseInt(valor.trim());
            if (edad < 0 || edad > 120) {
                throw new IllegalArgumentException("La edad debe estar entre 0 y 120 años.");
            }
            return edad;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("La edad debe ser un número entero.");
        }
    }
}
