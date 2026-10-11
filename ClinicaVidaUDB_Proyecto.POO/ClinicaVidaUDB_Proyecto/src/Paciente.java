
public class Paciente extends Persona {

    private static final long serialVersionUID = 1L;

    private String codigoPaciente;
    private int edad;
    private TipoPaciente tipoPaciente;
    private EstadoRegistro estadoPaciente;

    public Paciente(String codigoPaciente, String nombreCompleto, int edad,
                    String telefono, String correo, TipoPaciente tipoPaciente) {
        super(nombreCompleto, telefono, correo);
        setCodigoPaciente(codigoPaciente);
        setEdad(edad);
        setTipoPaciente(tipoPaciente);
        this.estadoPaciente = EstadoRegistro.ACTIVO;
    }

    public String getCodigoPaciente() {
        return codigoPaciente;
    }

    public int getEdad() {
        return edad;
    }

    public TipoPaciente getTipoPaciente() {
        return tipoPaciente;
    }

    public EstadoRegistro getEstadoPaciente() {
        return estadoPaciente;
    }

    public void setCodigoPaciente(String codigoPaciente) {
        if (codigoPaciente == null || codigoPaciente.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El código de paciente no puede estar vacío.");
        }
        this.codigoPaciente = codigoPaciente.trim();
    }

    public void setEdad(int edad) {
        if (edad < 0 || edad > 120) {
            throw new IllegalArgumentException(
                    "La edad debe estar entre 0 y 120 años.");
        }
        this.edad = edad;
    }

    public void setTipoPaciente(TipoPaciente tipoPaciente) {
        if (tipoPaciente == null) {
            throw new IllegalArgumentException(
                    "El tipo de paciente es obligatorio.");
        }
        this.tipoPaciente = tipoPaciente;
    }

    public void setEstadoPaciente(EstadoRegistro estadoPaciente) {
        if (estadoPaciente == null) {
            throw new IllegalArgumentException(
                    "El estado del paciente es obligatorio.");
        }
        this.estadoPaciente = estadoPaciente;
    }

    public boolean estaActivo() {
        return estadoPaciente == EstadoRegistro.ACTIVO;
    }

    @Override
    public String mostrarDatos() {
        return "Código: " + codigoPaciente + " | "
                + super.mostrarDatos()
                + " | Edad: " + edad
                + " | Tipo: " + tipoPaciente
                + " | Estado: " + estadoPaciente
                + " (Paciente)";
    }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
