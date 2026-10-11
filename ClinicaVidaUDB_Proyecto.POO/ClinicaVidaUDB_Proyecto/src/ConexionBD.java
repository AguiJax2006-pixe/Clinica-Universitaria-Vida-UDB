import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public final class ConexionBD {

    
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static final String ARCHIVO = "db.properties";

    
    private static final String URL_POR_DEFECTO =
            "jdbc:mysql://localhost:3306/clinica_universitaria"
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=America/El_Salvador"
            + "&characterEncoding=UTF-8&connectTimeout=5000";
    private static final String USUARIO_POR_DEFECTO = "root";
    private static final String CLAVE_POR_DEFECTO = "";

    private static String url;
    private static String usuario;
    private static String clave;
    private static boolean configurada = false;

    /** Clase de utilidad: no se crean objetos de ella. */
    private ConexionBD() {
    }

    /**
     * Abre una conexión con los datos de db.properties (o los valores por defecto).
     * Quien la pide es responsable de cerrarla; lo normal es hacerlo con
     * try (Connection con = ConexionBD.obtenerConexion()) { ... }
     *
     * @throws ConexionException si no se puede conectar; el mensaje dice por qué
     */
    public static Connection obtenerConexion() throws ConexionException {
        cargarConfiguracion();
        return obtenerConexion(url, usuario, clave);
    }

    public static Connection obtenerConexion(String url, String usuario, String clave)
            throws ConexionException {
        try {
            // Carga el driver. Si falta el .jar en el proyecto, se avisa con claridad.
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ConexionException("No se encontró el driver JDBC de MySQL (" + DRIVER
                    + "). Agregue el archivo mysql-connector-j-x.x.x.jar a las bibliotecas del proyecto.", e);
        }
        if (url == null || url.trim().isEmpty()) {
            throw new ConexionException("Falta la URL de conexión (db.url).");
        }
        try {
            return DriverManager.getConnection(url, usuario, clave);
        } catch (SQLException e) {
            throw traducir(e, url, usuario);
        }
    }

    /**
     * Comprueba la conexión y describe a qué servidor se conectó.
     *
     * @return por ejemplo "MySQL 8.0.36 | base de datos: clinica_universitaria | usuario: root@localhost | driver: ..."
     * @throws ConexionException si no hay conexión
     */
    public static String describirConexion() throws ConexionException {
        try (Connection con = obtenerConexion()) {
            DatabaseMetaData datos = con.getMetaData();
            String driver = datos.getDriverName() + " " + datos.getDriverVersion();
            int corte = driver.indexOf(" (");            // quita "(Revision: ...)"
            if (corte > 0) {
                driver = driver.substring(0, corte);
            }
            return datos.getDatabaseProductName() + " " + datos.getDatabaseProductVersion()
                    + " | base de datos: " + con.getCatalog()
                    + " | usuario: " + datos.getUserName()
                    + " | driver: " + driver;
        } catch (SQLException e) {
            throw new ConexionException(
                    "La conexión se abrió, pero el servidor no respondió a la consulta de prueba.", e);
        }
    }

    /** URL de conexión en uso (sin la contraseña, que nunca se expone). */
    public static String getUrl() throws ConexionException {
        cargarConfiguracion();
        return url;
    }

    /** Usuario de MySQL en uso. */
    public static String getUsuario() throws ConexionException {
        cargarConfiguracion();
        return usuario;
    }

    // ------------------------------------------------------------------
    // Configuración
    // ------------------------------------------------------------------

    private static synchronized void cargarConfiguracion() throws ConexionException {
        if (configurada) {
            return;
        }
        Properties propiedades = new Properties();
        try (InputStream entrada = abrirArchivo()) {
            if (entrada != null) {
                propiedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new ConexionException("No se pudo leer el archivo " + ARCHIVO + ": " + e.getMessage(), e);
        }
        url = valor(propiedades, "db.url", URL_POR_DEFECTO);
        usuario = valor(propiedades, "db.usuario", USUARIO_POR_DEFECTO);
        // La contraseña se toma tal cual (puede ser vacía y no se le quitan espacios).
        clave = propiedades.getProperty("db.clave", CLAVE_POR_DEFECTO);
        configurada = true;
    }

    /** Busca db.properties en la carpeta de trabajo y, si no está, en el classpath (src). */
    private static InputStream abrirArchivo() throws IOException {
        File archivo = new File(ARCHIVO);
        if (archivo.isFile()) {
            return new FileInputStream(archivo);
        }
        return ConexionBD.class.getResourceAsStream("/" + ARCHIVO);   // null si no existe
    }

    private static String valor(Properties propiedades, String nombre, String porDefecto) {
        String leido = propiedades.getProperty(nombre);
        return (leido == null || leido.trim().isEmpty()) ? porDefecto : leido.trim();
    }

    // ------------------------------------------------------------------
    // Traducción de los errores de conexión
    // ------------------------------------------------------------------

    /**
     * Convierte la SQLException del intento de conexión en un mensaje que dice
     * qué revisar. Se apoya en el código de error de MySQL y, si no hay uno
     * conocido, en el SQLState estándar (los que empiezan con 08 son de conexión).
     */
    private static ConexionException traducir(SQLException e, String url, String usuario) {
        String destino = sinParametros(url);
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String detalle = e.getMessage() == null ? "" : e.getMessage();
        String mensaje;

        if (e.getErrorCode() == 1045) {                         // ER_ACCESS_DENIED_ERROR
            mensaje = "MySQL rechazó el usuario '" + usuario + "' o su contraseña. Revise db.usuario y db.clave en "
                    + ARCHIVO + " (cuidado con espacios al final).";
        } else if (e.getErrorCode() == 1049) {                  // ER_BAD_DB_ERROR
            mensaje = "La base de datos indicada en " + destino
                    + " no existe. Ejecute primero el script SQL de la Parte 1.";
        } else if (e.getErrorCode() == 1044) {                  // ER_DBACCESS_DENIED_ERROR
            mensaje = "El usuario '" + usuario + "' no tiene permisos sobre la base de datos de " + destino + ".";
        } else if (detalle.contains("Public Key Retrieval")) {
            mensaje = "MySQL exige recuperar la llave pública para validar la contraseña. "
                    + "Agregue allowPublicKeyRetrieval=true a db.url.";
        } else if (detalle.startsWith("No suitable driver")) {
            mensaje = "La URL de conexión no es válida: " + destino
                    + ". Debe tener la forma jdbc:mysql://servidor:puerto/base_de_datos";
        } else if (detalle.contains("time zone")) {
            mensaje = "El conector no reconoce la zona horaria del servidor. "
                    + "Agregue serverTimezone=America/El_Salvador a db.url.";
        } else if (estado.startsWith("08")) {                   // clase 08: error de conexión
            mensaje = "No se pudo establecer comunicación con MySQL en " + destino
                    + ". Verifique que MySQL esté iniciado y que el servidor y el puerto de db.url sean correctos.";
        } else {
            mensaje = "No se pudo abrir la conexión con " + destino + ": " + detalle;
        }
        return new ConexionException(mensaje, e);
    }

    /** Quita los parámetros de la URL para mostrarla en los mensajes. */
    private static String sinParametros(String url) {
        int corte = url.indexOf('?');
        return corte < 0 ? url : url.substring(0, corte);
    }
}
