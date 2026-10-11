/**
 * La operación rompería una relación entre tablas (llave foránea): eliminar un
 * paciente o un doctor que tiene citas, o registrar una cita para un paciente o
 * un doctor que no existe.
 */
public class IntegridadReferencialException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public IntegridadReferencialException(String mensaje) {
        super(mensaje);
    }

    public IntegridadReferencialException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
