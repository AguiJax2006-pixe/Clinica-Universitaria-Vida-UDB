
public class Doctor extends Persona {

    private static final long serialVersionUID = 1L;

    private String codigoDoctor;
    private Especialidad especialidad;
    private EstadoRegistro estadoDoctor;

    public Doctor(String codigoDoctor, String nombreCompleto, Especialidad especialidad,
                  String telefono, String correo) {
        super(nombreCompleto, telefono, correo);
        setCodigoDoctor(codigoDoctor);
        setEspecialidad(especialidad);
        this.estadoDoctor = EstadoRegistro.ACTIVO;
    }

    public String getCodigoDoctor() {
        return codigoDoctor;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public EstadoRegistro getEstadoDoctor() {
        return estadoDoctor;
    }

    public void setCodigoDoctor(String codigoDoctor) {
        if (codigoDoctor == null || codigoDoctor.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El código de doctor no puede estar vacío.");
        }
        this.codigoDoctor = codigoDoctor.trim();
    }

    public void setEspecialidad(Especialidad especialidad) {
        if (especialidad == null) {
            throw new IllegalArgumentException(
                    "La especialidad es obligatoria.");
        }
        this.especialidad = especialidad;
    }

    public void setEstadoDoctor(EstadoRegistro estadoDoctor) {
        if (estadoDoctor == null) {
            throw new IllegalArgumentException(
                    "El estado del doctor es obligatorio.");
        }
        this.estadoDoctor = estadoDoctor;
    }

    public boolean estaActivo() {
        return estadoDoctor == EstadoRegistro.ACTIVO;
    }

    @Override
    public String mostrarDatos() {
        return "Código: " + codigoDoctor + " | "
                + super.mostrarDatos()
                + " | Especialidad: " + especialidad
                + " | Estado: " + estadoDoctor
                + " (Doctor)";
    }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
