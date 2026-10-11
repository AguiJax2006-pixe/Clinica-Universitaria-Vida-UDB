/**
 * Un dato no puede guardarse o leerse: no pasó la validación de entrada del DAO
 * (código no numérico, texto demasiado largo, objeto nulo), la base de datos lo
 * rechazó (restricción CHECK, valor obligatorio, formato incorrecto) o una regla
 * del negocio lo impide (programar una cita con un doctor o paciente inactivo).
 */
public class DatoInvalidoException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public DatoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
