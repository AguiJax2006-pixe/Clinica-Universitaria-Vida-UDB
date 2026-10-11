/**
 * Se intentó guardar un registro que ya existe: el código (llave primaria) ya
 * está en uso, o ya hay un paciente o un doctor con el mismo correo electrónico.
 * Resuelve el problema de "duplicidad de registros" del planteamiento.
 */
public class RegistroDuplicadoException extends PersistenciaException {

    private static final long serialVersionUID = 1L;

    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public RegistroDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
