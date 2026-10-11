import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla pacientes (patrón DAO).
 *
 * Todas las sentencias son PreparedStatement: los valores viajan como
 * parámetros (?) y nunca se concatenan dentro del SQL, lo que evita la
 * inyección de SQL. Cada método abre su conexión y la cierra con
 * try-with-resources, y todo error sale como una PersistenciaException.
 */
public class PacienteDAO extends DAOBase {

    private static final String COLUMNAS =
            "codigo_paciente, nombre_completo, edad, telefono, correo_electronico, tipo_paciente, estado_paciente";

    private static final String SQL_INSERTAR =
            "INSERT INTO pacientes (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE pacientes SET nombre_completo = ?, edad = ?, telefono = ?, correo_electronico = ?, "
            + "tipo_paciente = ?, estado_paciente = ? WHERE codigo_paciente = ?";

    private static final String SQL_CAMBIAR_ESTADO =
            "UPDATE pacientes SET estado_paciente = ? WHERE codigo_paciente = ?";

    private static final String SQL_ELIMINAR =
            "DELETE FROM pacientes WHERE codigo_paciente = ?";

    private static final String SQL_LISTAR =
            "SELECT " + COLUMNAS + " FROM pacientes ORDER BY codigo_paciente";

    private static final String SQL_POR_CODIGO =
            "SELECT " + COLUMNAS + " FROM pacientes WHERE codigo_paciente = ?";

    private static final String SQL_POR_ESTADO =
            "SELECT " + COLUMNAS + " FROM pacientes WHERE estado_paciente = ? ORDER BY nombre_completo";

    private static final String SQL_POR_NOMBRE =
            "SELECT " + COLUMNAS + " FROM pacientes WHERE nombre_completo LIKE ? ORDER BY nombre_completo";

    private static final String SQL_EXISTE =
            "SELECT 1 FROM pacientes WHERE codigo_paciente = ?";

    private static final String SQL_CORREO_EN_USO =
            "SELECT codigo_paciente FROM pacientes WHERE correo_electronico = ? AND codigo_paciente <> ?";

    private static final String SQL_SIGUIENTE_CODIGO =
            "SELECT COALESCE(MAX(codigo_paciente), 0) + 1 FROM pacientes";

    private static final String SQL_CANCELAR_CITAS =
            "UPDATE citas SET estado_cita = ? WHERE codigo_paciente = ? AND estado_cita = ?";

    // ------------------------------------------------------------------
    // Altas, cambios y bajas
    // ------------------------------------------------------------------

    /**
     * Registra un paciente nuevo.
     *
     * @throws RegistroDuplicadoException si el código o el correo ya están registrados
     * @throws DatoInvalidoException      si algún dato no puede guardarse
     * @throws PersistenciaException      ante cualquier otro error de base de datos
     */
    public void insertar(Paciente paciente) throws PersistenciaException {
        exigirNoNulo(paciente, "el paciente");
        int codigo = MapeoBD.codigoABd(paciente.getCodigoPaciente(), "código de paciente");
        validarTamanos(paciente);

        try (Connection con = conectar()) {
            verificarCorreoLibre(con, paciente.getCorreo(), codigo);
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERTAR)) {
                ps.setInt(1, codigo);
                ps.setString(2, paciente.getNombreCompleto());
                ps.setInt(3, paciente.getEdad());
                ps.setString(4, MapeoBD.telefonoABd(paciente.getTelefono()));
                ps.setString(5, paciente.getCorreo());
                ps.setString(6, paciente.getTipoPaciente().toString());
                ps.setString(7, MapeoBD.estadoABd(paciente.getEstadoPaciente()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw traducir(e, "registrar el paciente " + codigo);
        }
    }

    /**
     * Guarda los cambios de un paciente existente (todos sus datos menos el código).
     *
     * @throws RegistroNoEncontradoException si no existe un paciente con ese código
     * @throws RegistroDuplicadoException    si el correo nuevo ya lo usa otro paciente
     */
    public void actualizar(Paciente paciente) throws PersistenciaException {
        exigirNoNulo(paciente, "el paciente");
        int codigo = MapeoBD.codigoABd(paciente.getCodigoPaciente(), "código de paciente");
        validarTamanos(paciente);

        try (Connection con = conectar()) {
            verificarCorreoLibre(con, paciente.getCorreo(), codigo);
            int filas;
            try (PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR)) {
                ps.setString(1, paciente.getNombreCompleto());
                ps.setInt(2, paciente.getEdad());
                ps.setString(3, MapeoBD.telefonoABd(paciente.getTelefono()));
                ps.setString(4, paciente.getCorreo());
                ps.setString(5, paciente.getTipoPaciente().toString());
                ps.setString(6, MapeoBD.estadoABd(paciente.getEstadoPaciente()));
                ps.setInt(7, codigo);
                filas = ps.executeUpdate();
            }
            if (filas == 0 && !existe(con, codigo)) {
                throw noEncontrado(codigo);
            }
        } catch (SQLException e) {
            throw traducir(e, "actualizar el paciente " + codigo);
        }
    }

    /**
     * Cambia solo el estado (Activo o Inactivo). No toca las citas del paciente;
     * para inactivarlo y cancelar sus citas pendientes use darDeBaja.
     */
    public void cambiarEstado(String codigoPaciente, EstadoRegistro nuevoEstado) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoPaciente, "código de paciente");
        exigirNoNulo(nuevoEstado, "el nuevo estado");

        try (Connection con = conectar()) {
            int filas;
            try (PreparedStatement ps = con.prepareStatement(SQL_CAMBIAR_ESTADO)) {
                ps.setString(1, MapeoBD.estadoABd(nuevoEstado));
                ps.setInt(2, codigo);
                filas = ps.executeUpdate();
            }
            if (filas == 0 && !existe(con, codigo)) {
                throw noEncontrado(codigo);
            }
        } catch (SQLException e) {
            throw traducir(e, "cambiar el estado del paciente " + codigo);
        }
    }

    /**
     * Da de baja al paciente en una sola TRANSACCIÓN: lo pasa a Inactivo y
     * cancela sus citas en estado Programada. O se guardan los dos cambios
     * (commit) o no se guarda ninguno (rollback).
     *
     * @return cuántas citas programadas se cancelaron
     */
    public int darDeBaja(String codigoPaciente) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoPaciente, "código de paciente");

        try (Connection con = conectar()) {
            boolean confirmada = false;
            con.setAutoCommit(false);                       // inicia la transacción
            try {
                int filas;
                try (PreparedStatement ps = con.prepareStatement(SQL_CAMBIAR_ESTADO)) {
                    ps.setString(1, MapeoBD.estadoABd(EstadoRegistro.INACTIVO));
                    ps.setInt(2, codigo);
                    filas = ps.executeUpdate();
                }
                if (filas == 0 && !existe(con, codigo)) {
                    throw noEncontrado(codigo);
                }
                int canceladas;
                try (PreparedStatement ps = con.prepareStatement(SQL_CANCELAR_CITAS)) {
                    ps.setString(1, EstadoCita.CANCELADA.toString());
                    ps.setInt(2, codigo);
                    ps.setString(3, EstadoCita.PROGRAMADA.toString());
                    canceladas = ps.executeUpdate();
                }
                con.commit();                               // confirma los dos cambios juntos
                confirmada = true;
                return canceladas;
            } finally {
                if (!confirmada) {
                    deshacer(con);                          // algo falló: no queda nada a medias
                }
            }
        } catch (SQLException e) {
            throw traducir(e, "dar de baja al paciente " + codigo);
        }
    }

    /**
     * Elimina definitivamente al paciente. MySQL no lo permite si tiene citas
     * (llave foránea); en ese caso lo correcto es inactivarlo.
     *
     * @throws IntegridadReferencialException si el paciente tiene citas registradas
     * @throws RegistroNoEncontradoException  si no existe
     */
    public void eliminar(String codigoPaciente) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoPaciente, "código de paciente");

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(SQL_ELIMINAR)) {
            ps.setInt(1, codigo);
            if (ps.executeUpdate() == 0) {
                throw noEncontrado(codigo);
            }
        } catch (SQLException e) {
            throw traducir(e, "eliminar el paciente " + codigo);
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * @throws RegistroNoEncontradoException si no existe un paciente con ese código
     */
    public Paciente buscarPorCodigo(String codigoPaciente) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoPaciente, "código de paciente");
        List<Paciente> encontrados = consultar(SQL_POR_CODIGO, "consultar el paciente " + codigo, codigo);
        if (encontrados.isEmpty()) {
            throw noEncontrado(codigo);
        }
        return encontrados.get(0);
    }

    public boolean existe(String codigoPaciente) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoPaciente, "código de paciente");
        try (Connection con = conectar()) {
            return existe(con, codigo);
        } catch (SQLException e) {
            throw traducir(e, "consultar el paciente " + codigo);
        }
    }

    /** Todos los pacientes, ordenados por código. */
    public List<Paciente> listar() throws PersistenciaException {
        return consultar(SQL_LISTAR, "listar los pacientes");
    }

    /** Por ejemplo, solo los activos: listarPorEstado(EstadoRegistro.ACTIVO). */
    public List<Paciente> listarPorEstado(EstadoRegistro estado) throws PersistenciaException {
        exigirNoNulo(estado, "el estado");
        return consultar(SQL_POR_ESTADO, "listar los pacientes por estado", MapeoBD.estadoABd(estado));
    }

    /** Pacientes cuyo nombre contiene el texto indicado (sin distinguir mayúsculas). */
    public List<Paciente> buscarPorNombre(String texto) throws PersistenciaException {
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        return consultar(SQL_POR_NOMBRE, "buscar pacientes por nombre", patron);
    }

    /** Siguiente código libre (el mayor registrado más uno), útil al registrar. */
    public String siguienteCodigo() throws PersistenciaException {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(SQL_SIGUIENTE_CODIGO);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return MapeoBD.codigoDesdeBd(rs.getInt(1));
        } catch (SQLException e) {
            throw traducir(e, "calcular el siguiente código de paciente");
        }
    }

    // ------------------------------------------------------------------
    // Apoyo interno
    // ------------------------------------------------------------------

    /** Ejecuta un SELECT con sus parámetros y convierte cada fila en un Paciente. */
    private List<Paciente> consultar(String sql, String operacion, Object... parametros)
            throws PersistenciaException {
        List<Paciente> pacientes = new ArrayList<>();
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(mapear(rs, ""));
                }
            }
        } catch (SQLException e) {
            throw traducir(e, operacion);
        }
        return pacientes;
    }

    /**
     * Construye un Paciente con la fila actual del ResultSet.
     *
     * @param prefijo prefijo de los nombres de columna: "" en las consultas de
     *                esta clase y "p_" en el JOIN que hace CitaDAO
     * @throws DatoInvalidoException si la fila tiene un dato que la clase Paciente no acepta
     */
    static Paciente mapear(ResultSet rs, String prefijo) throws SQLException, DatoInvalidoException {
        String codigo = MapeoBD.codigoDesdeBd(rs.getInt(prefijo + "codigo_paciente"));
        try {
            // El constructor y los setters de la Parte 2 validan cada dato
            Paciente paciente = new Paciente(
                    codigo,
                    rs.getString(prefijo + "nombre_completo"),
                    rs.getInt(prefijo + "edad"),
                    MapeoBD.telefonoDesdeBd(rs.getString(prefijo + "telefono")),
                    rs.getString(prefijo + "correo_electronico"),
                    MapeoBD.tipoPaciente(rs.getString(prefijo + "tipo_paciente")));
            paciente.setEstadoPaciente(MapeoBD.estadoRegistro(rs.getString(prefijo + "estado_paciente")));
            return paciente;
        } catch (IllegalArgumentException | DatoInvalidoException e) {
            throw new DatoInvalidoException("El paciente " + codigo
                    + " guardado en la base de datos tiene un dato que el sistema no acepta. " + e.getMessage(), e);
        }
    }

    private static boolean existe(Connection con, int codigo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_EXISTE)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Evita registrar dos veces a la misma persona con códigos distintos. */
    private static void verificarCorreoLibre(Connection con, String correo, int codigoPropio)
            throws SQLException, RegistroDuplicadoException {
        try (PreparedStatement ps = con.prepareStatement(SQL_CORREO_EN_USO)) {
            ps.setString(1, correo);
            ps.setInt(2, codigoPropio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new RegistroDuplicadoException("El correo " + correo
                            + " ya pertenece al paciente " + rs.getInt(1) + ".");
                }
            }
        }
    }

    private static void validarTamanos(Paciente paciente) throws DatoInvalidoException {
        exigirLongitud(paciente.getNombreCompleto(), 100, "nombre completo");
        exigirLongitud(paciente.getCorreo(), 100, "correo electrónico");
    }

    private static RegistroNoEncontradoException noEncontrado(int codigo) {
        return new RegistroNoEncontradoException("No existe un paciente con el código " + codigo + ".");
    }
}
