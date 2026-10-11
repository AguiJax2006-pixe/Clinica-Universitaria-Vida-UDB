import java.sql.SQLException;

public class PersistenciaException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /**
     * Detalle técnico para depurar o dejar en bitácora: código de error de MySQL
     * y SQLState cuando la causa fue una SQLException. No está pensado para el
     * usuario final; para él se usa getMessage().
     *
     * @return el detalle en una sola línea, o cadena vacía si no hay causa
     */
    public String getDetalleTecnico() {
        Throwable causa = getCause();
        if (causa == null) {
            return "";
        }
        if (causa instanceof SQLException) {
            SQLException sql = (SQLException) causa;
            return "MySQL " + sql.getErrorCode() + " / SQLState " + sql.getSQLState()
                    + ": " + enUnaLinea(sql.getMessage());
        }
        return causa.getClass().getSimpleName() + ": " + enUnaLinea(causa.getMessage());
    }

    private static String enUnaLinea(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }
}
