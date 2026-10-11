/**
 * El doctor ya tiene otra cita en estado Programada en la misma fecha y hora.
 * Resuelve el problema de "errores al asignar horarios" del planteamiento.
 */
public class HorarioNoDisponibleException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public HorarioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public HorarioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
