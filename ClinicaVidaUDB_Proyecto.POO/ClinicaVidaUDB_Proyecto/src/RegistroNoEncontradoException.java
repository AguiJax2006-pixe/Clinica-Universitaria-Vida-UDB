/**
 * Se pidió buscar, actualizar o eliminar un registro cuyo código no existe en
 * la base de datos.
 */
public class RegistroNoEncontradoException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public RegistroNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RegistroNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
