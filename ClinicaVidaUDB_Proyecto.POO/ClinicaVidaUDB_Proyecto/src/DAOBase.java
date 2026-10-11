import java.sql.Connection;
import java.sql.SQLException;


public abstract class DAOBase {

    /** Pide una conexión nueva; cada método la cierra con try-with-resources. */
    protected Connection conectar() throws ConexionException {
        return ConexionBD.obtenerConexion();
    }

    /**
     * Deshace lo hecho en una transacción que no llegó al commit. Si el rollback
     * también falla no hay nada más que hacer: la conexión se cierra enseguida y
     * MySQL descarta por su cuenta todo lo que no se confirmó.
     */
    protected static void deshacer(Connection con) {
        try {
            con.rollback();
        } catch (SQLException e) {
            System.err.println("Aviso: no se pudo ejecutar el rollback (" + e.getMessage() + ").");
        }
    }

    // ------------------------------------------------------------------
    // Validaciones de entrada (antes de enviar nada a MySQL)
    // ------------------------------------------------------------------

    protected static void exigirNoNulo(Object valor, String nombre) throws DatoInvalidoException {
        if (valor == null) {
            throw new DatoInvalidoException("Falta indicar " + nombre + ".");
        }
    }

    /** El texto no puede superar el tamaño de su columna VARCHAR. */
    protected static void exigirLongitud(String valor, int maximo, String campo) throws DatoInvalidoException {
        if (valor != null && valor.length() > maximo) {
            throw new DatoInvalidoException("El dato '" + campo + "' admite como máximo " + maximo
                    + " caracteres y tiene " + valor.length() + ".");
        }
    }

    // ------------------------------------------------------------------
    // Traducción de errores de MySQL
    // ------------------------------------------------------------------

    /**
     * Convierte una SQLException en la excepción personalizada que corresponde.
     *
     * Primero mira el código de error propio de MySQL (getErrorCode), que dice
     * exactamente qué pasó; si no es uno de los previstos, usa los dos primeros
     * caracteres del SQLState, que es el estándar común a todas las bases de datos.
     *
     * @param e         el error original, que se conserva como causa
     * @param operacion lo que se intentaba hacer, por ejemplo "registrar el paciente 6"
     * @return la excepción lista para lanzarse con throw
     */
    protected static PersistenciaException traducir(SQLException e, String operacion) {
        String inicio = "No se pudo " + operacion + ": ";
        String detalle = e.getMessage() == null ? "" : e.getMessage();
        String estado = e.getSQLState() == null ? "" : e.getSQLState();

        switch (e.getErrorCode()) {
            case 1062:      // llave primaria o índice único repetido
                return new RegistroDuplicadoException(inicio + motivoDuplicado(detalle), e);

            case 1451:      // se intenta borrar un registro del que dependen otros
                return new IntegridadReferencialException(inicio
                        + "tiene citas registradas. Cambie su estado a Inactivo en lugar de eliminarlo.", e);

            case 1452:      // se hace referencia a un registro que no existe
                return new IntegridadReferencialException(inicio + motivoReferencia(detalle), e);

            case 3819:      // restricción CHECK violada (MySQL 8.0.16 o superior)
            case 4025:      // restricción CHECK violada (MariaDB)
                return new DatoInvalidoException(inicio
                        + "un valor no está entre los que permite la base de datos" + nombreRestriccion(detalle) + ".", e);

            case 1048:      // columna NOT NULL sin valor
                return new DatoInvalidoException(inicio + "falta un dato obligatorio.", e);

            case 1406:      // texto más largo que la columna
                return new DatoInvalidoException(inicio
                        + "un texto supera la longitud que admite la base de datos.", e);

            case 1264:      // número fuera de rango
            case 1265:      // dato truncado
            case 1292:      // fecha u hora incorrecta
            case 1366:      // valor con formato incorrecto para la columna
            case 1525:      // valor incorrecto en una comparación
                return new DatoInvalidoException(inicio
                        + "un dato tiene un valor o un formato que la base de datos no admite.", e);

            case 1146:      // la tabla no existe
            case 1054:      // la columna no existe
                return new PersistenciaException(inicio + "la base de datos no tiene la estructura esperada. "
                        + "Ejecute de nuevo el script SQL de la Parte 1.", e);

            case 1205:      // se agotó la espera de un bloqueo
            case 1213:      // interbloqueo entre dos transacciones
                return new PersistenciaException(inicio
                        + "otro usuario estaba modificando los mismos datos. Intente de nuevo.", e);

            default:
                break;
        }

        // Sin código conocido: se decide por la clase del SQLState (estándar SQL)
        if (estado.startsWith("08")) {
            return new ConexionException(inicio + "se perdió la comunicación con MySQL.", e);
        }
        if (estado.startsWith("23")) {
            return new DatoInvalidoException(inicio
                    + "los datos incumplen una regla de integridad de la base de datos.", e);
        }
        if (estado.startsWith("22")) {
            return new DatoInvalidoException(inicio
                    + "un dato tiene un valor o un formato que la base de datos no admite.", e);
        }
        return new PersistenciaException(inicio + "ocurrió un error en la base de datos"
                + (detalle.isEmpty() ? "." : " (" + detalle + ")."), e);
    }

    /** MySQL informa: Duplicate entry '1' for key 'PRIMARY' (o el nombre del índice único). */
    private static String motivoDuplicado(String detalle) {
        int posicion = detalle.lastIndexOf("for key ");
        String llave = posicion < 0 ? null : textoEntre(detalle.substring(posicion), '\'');
        if (llave == null || llave.toUpperCase().endsWith("PRIMARY")) {
            return "ya existe un registro con ese código.";
        }
        return "ya existe un registro con el mismo valor en " + llave + ".";
    }

    /** MySQL informa qué llave foránea falló y a qué tabla apunta. */
    private static String motivoReferencia(String detalle) {
        if (detalle.contains("REFERENCES `pacientes`")) {
            return "el paciente indicado no existe.";
        }
        if (detalle.contains("REFERENCES `doctores`")) {
            return "el doctor indicado no existe.";
        }
        return "hace referencia a un registro que no existe.";
    }

    /** Devuelve " (restricción NOMBRE)" si el mensaje de MySQL o MariaDB trae el nombre. */
    private static String nombreRestriccion(String detalle) {
        String nombre = textoEntre(detalle, '\'');
        if (nombre == null) {
            nombre = textoEntre(detalle, '`');
        }
        return nombre == null ? "" : " (restricción " + nombre + ")";
    }

    private static String textoEntre(String texto, char comilla) {
        int inicio = texto.indexOf(comilla);
        int fin = inicio < 0 ? -1 : texto.indexOf(comilla, inicio + 1);
        return fin < 0 ? null : texto.substring(inicio + 1, fin);
    }
}
