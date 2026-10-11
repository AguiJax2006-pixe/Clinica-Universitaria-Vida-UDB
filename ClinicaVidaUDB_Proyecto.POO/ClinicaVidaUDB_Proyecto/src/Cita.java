import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class Cita implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigoCita;
    private Paciente paciente;     // en la BD se guarda como codigo_paciente (llave foránea)
    private Doctor doctor;         // en la BD se guarda como codigo_doctor (llave foránea)
    private LocalDate fecha;
    private LocalTime hora;
    private String motivo;
    private EstadoCita estadoCita;

    public Cita(String codigoCita, Paciente paciente, Doctor doctor,
                LocalDate fecha, LocalTime hora, String motivo) {
        setCodigoCita(codigoCita);
        setPaciente(paciente);
        setDoctor(doctor);
        setFecha(fecha);
        setHora(hora);
        setMotivo(motivo);
        this.estadoCita = EstadoCita.PROGRAMADA;
    }

    public String getCodigoCita() { return codigoCita; }
    public Paciente getPaciente() { return paciente; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public String getMotivo() { return motivo; }
    public EstadoCita getEstadoCita() { return estadoCita; }

    public void setCodigoCita(String codigoCita) {
        if (codigoCita == null || codigoCita.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de cita no puede estar vacío.");
        }
        this.codigoCita = codigoCita.trim();
    }

    public void setPaciente(Paciente paciente) {
        if (paciente == null) throw new IllegalArgumentException("La cita requiere un paciente.");
        if (!paciente.estaActivo()) throw new IllegalArgumentException("El paciente está inactivo.");
        this.paciente = paciente;
    }

    public void setDoctor(Doctor doctor) {
        if (doctor == null) throw new IllegalArgumentException("La cita requiere un doctor.");
        if (!doctor.estaActivo()) throw new IllegalArgumentException("El doctor está inactivo.");
        this.doctor = doctor;
    }

    public void setFecha(LocalDate fecha) {
        if (fecha == null) throw new IllegalArgumentException("La fecha de la cita es obligatoria.");
        this.fecha = fecha;
    }

    public void setHora(LocalTime hora) {
        if (hora == null) throw new IllegalArgumentException("La hora de la cita es obligatoria.");
        this.hora = hora;
    }

    public void setMotivo(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de consulta es obligatorio.");
        }
        this.motivo = motivo.trim();
    }

    public void setEstadoCita(EstadoCita estadoCita) {
        if (estadoCita == null) throw new IllegalArgumentException("El estado de la cita es obligatorio.");
        this.estadoCita = estadoCita;
    }

    public void cancelar() {
        if (estadoCita != EstadoCita.PROGRAMADA) {
            throw new IllegalStateException("Solo se puede cancelar una cita programada.");
        }
        estadoCita = EstadoCita.CANCELADA;
    }

    public void marcarAtendida() {
        if (estadoCita != EstadoCita.PROGRAMADA) {
            throw new IllegalStateException("Solo se puede atender una cita programada.");
        }
        estadoCita = EstadoCita.ATENDIDA;
    }

    public String mostrarDatos() {
        return "Cita: " + codigoCita + " | Paciente: " + paciente.getNombreCompleto() +
               " | Doctor: " + doctor.getNombreCompleto() + " (" + doctor.getEspecialidad() + ")" +
               " | Fecha: " + fecha + " | Hora: " + hora + " | Motivo: " + motivo +
               " | Estado: " + estadoCita;
    }
}
