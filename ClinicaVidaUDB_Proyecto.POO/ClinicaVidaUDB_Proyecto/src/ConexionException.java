/**
 * No se pudo abrir la conexión con MySQL o se perdió durante una operación:
 * servidor apagado, host o puerto incorrectos, usuario o contraseña rechazados,
 * base de datos inexistente o driver JDBC ausente del proyecto.
 */
public class ConexionException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public ConexionException(String mensaje) {
        super(mensaje);
    }

    public ConexionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
