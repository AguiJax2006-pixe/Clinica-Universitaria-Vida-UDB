import java.io.Serializable;

/**
 * Superclase del dominio. Reúne los datos que comparten pacientes y doctores
 * (nombre completo, teléfono y correo electrónico) para reutilizarlos.
 * Es abstracta porque en la clínica nunca existe una "persona genérica".
 */
public abstract class Persona implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombreCompleto;
    private String telefono;
    private String correo;

    public Persona(String nombreCompleto, String telefono, String correo) {
        setNombreCompleto(nombreCompleto);
        setTelefono(telefono);
        setCorreo(correo);
    }

    // Getters
    public String getNombreCompleto() { return nombreCompleto; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }

    // Setters con validación: protegen la integridad del objeto
    public void setNombreCompleto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre completo no puede estar vacío.");
        }
        this.nombreCompleto = nombreCompleto.trim();
    }

    public void setTelefono(String telefono) {
        if (telefono == null || !telefono.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("El teléfono debe tener 8 dígitos.");
        }
        this.telefono = telefono;
    }

    public void setCorreo(String correo) {
        if (correo == null || !correo.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }
        this.correo = correo.trim();
    }

    // Método que será sobreescrito por las subclases
    public String mostrarDatos() {
        return "Nombre: " + nombreCompleto + " | Teléfono: " + telefono + " | Correo: " + correo;
    }
}
