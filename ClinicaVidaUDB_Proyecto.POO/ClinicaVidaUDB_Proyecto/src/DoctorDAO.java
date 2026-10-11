import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla doctores (patrón DAO). Sigue las mismas reglas
 * que PacienteDAO: sentencias preparadas, una conexión por operación cerrada
 * con try-with-resources y errores convertidos en PersistenciaException.
 */
public class DoctorDAO extends DAOBase {

    private static final String COLUMNAS =
            "codigo_doctor, nombre_completo, especialidad, telefono, correo_electronico, estado_doctor";

    private static final String SQL_INSERTAR =
            "INSERT INTO doctores (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE doctores SET nombre_completo = ?, especialidad = ?, telefono = ?, correo_electronico = ?, "
            + "estado_doctor = ? WHERE codigo_doctor = ?";

    private static final String SQL_CAMBIAR_ESTADO =
            "UPDATE doctores SET estado_doctor = ? WHERE codigo_doctor = ?";

    private static final String SQL_ELIMINAR =
            "DELETE FROM doctores WHERE codigo_doctor = ?";

    private static final String SQL_LISTAR =
            "SELECT " + COLUMNAS + " FROM doctores ORDER BY codigo_doctor";

    private static final String SQL_POR_CODIGO =
            "SELECT " + COLUMNAS + " FROM doctores WHERE codigo_doctor = ?";

    private static final String SQL_POR_ESTADO =
            "SELECT " + COLUMNAS + " FROM doctores WHERE estado_doctor = ? ORDER BY nombre_completo";

    private static final String SQL_POR_ESPECIALIDAD =
            "SELECT " + COLUMNAS + " FROM doctores WHERE especialidad = ? ORDER BY nombre_completo";

    private static final String SQL_POR_NOMBRE =
            "SELECT " + COLUMNAS + " FROM doctores WHERE nombre_completo LIKE ? ORDER BY nombre_completo";

    private static final String SQL_EXISTE =
            "SELECT 1 FROM doctores WHERE codigo_doctor = ?";

    private static final String SQL_CORREO_EN_USO =
            "SELECT codigo_doctor FROM doctores WHERE correo_electronico = ? AND codigo_doctor <> ?";

    private static final String SQL_SIGUIENTE_CODIGO =
            "SELECT COALESCE(MAX(codigo_doctor), 0) + 1 FROM doctores";

    private static final String SQL_CANCELAR_CITAS =
            "UPDATE citas SET estado_cita = ? WHERE codigo_doctor = ? AND estado_cita = ?";

    // ------------------------------------------------------------------
    // Altas, cambios y bajas
    // ------------------------------------------------------------------

    /**
     * Registra un doctor nuevo.
     *
     * @throws RegistroDuplicadoException si el código o el correo ya están registrados
     * @throws DatoInvalidoException      si algún dato no puede guardarse
     * @throws PersistenciaException      ante cualquier otro error de base de datos
     */
    public void insertar(Doctor doctor) throws PersistenciaException {
        exigirNoNulo(doctor, "el doctor");
        int codigo = MapeoBD.codigoABd(doctor.getCodigoDoctor(), "código de doctor");
        validarTamanos(doctor);

        try (Connection con = conectar()) {
            verificarCorreoLibre(con, doctor.getCorreo(), codigo);
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERTAR)) {
                ps.setInt(1, codigo);
                ps.setString(2, doctor.getNombreCompleto());
                ps.setString(3, doctor.getEspecialidad().toString());
                ps.setString(4, MapeoBD.telefonoABd(doctor.getTelefono()));
                ps.setString(5, doctor.getCorreo());
                ps.setString(6, MapeoBD.estadoABd(doctor.getEstadoDoctor()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw traducir(e, "registrar el doctor " + codigo);
        }
    }

    /**
     * Guarda los cambios de un doctor existente (todos sus datos menos el código).
     *
     * @throws RegistroNoEncontradoException si no existe un doctor con ese código
     * @throws RegistroDuplicadoException    si el correo nuevo ya lo usa otro doctor
     */
    public void actualizar(Doctor doctor) throws PersistenciaException {
        exigirNoNulo(doctor, "el doctor");
        int codigo = MapeoBD.codigoABd(doctor.getCodigoDoctor(), "código de doctor");
        validarTamanos(doctor);

        try (Connection con = conectar()) {
            verificarCorreoLibre(con, doctor.getCorreo(), codigo);
            int filas;
            try (PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR)) {
                ps.setString(1, doctor.getNombreCompleto());
                ps.setString(2, doctor.getEspecialidad().toString());
                ps.setString(3, MapeoBD.telefonoABd(doctor.getTelefono()));
                ps.setString(4, doctor.getCorreo());
                ps.setString(5, MapeoBD.estadoABd(doctor.getEstadoDoctor()));
                ps.setInt(6, codigo);
                filas = ps.executeUpdate();
            }
            if (filas == 0 && !existe(con, codigo)) {
                throw noEncontrado(codigo);
            }
        } catch (SQLException e) {
            throw traducir(e, "actualizar el doctor " + codigo);
        }
    }

    /**
     * Cambia solo el estado (Activo o Inactivo). No toca las citas del doctor;
     * para inactivarlo y cancelar sus citas pendientes use darDeBaja.
     */
    public void cambiarEstado(String codigoDoctor, EstadoRegistro nuevoEstado) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");
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
            throw traducir(e, "cambiar el estado del doctor " + codigo);
        }
    }

    /**
     * Da de baja al doctor en una sola TRANSACCIÓN: lo pasa a Inactivo y
     * cancela sus citas en estado Programada. O se guardan los dos cambios
     * (commit) o no se guarda ninguno (rollback).
     *
     * @return cuántas citas programadas se cancelaron
     */
    public int darDeBaja(String codigoDoctor) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");

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
            throw traducir(e, "dar de baja al doctor " + codigo);
        }
    }

    /**
     * Elimina definitivamente al doctor. MySQL no lo permite si tiene citas
     * (llave foránea); en ese caso lo correcto es inactivarlo.
     *
     * @throws IntegridadReferencialException si el doctor tiene citas registradas
     * @throws RegistroNoEncontradoException  si no existe
     */
    public void eliminar(String codigoDoctor) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(SQL_ELIMINAR)) {
            ps.setInt(1, codigo);
            if (ps.executeUpdate() == 0) {
                throw noEncontrado(codigo);
            }
        } catch (SQLException e) {
            throw traducir(e, "eliminar el doctor " + codigo);
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * @throws RegistroNoEncontradoException si no existe un doctor con ese código
     */
    public Doctor buscarPorCodigo(String codigoDoctor) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");
        List<Doctor> encontrados = consultar(SQL_POR_CODIGO, "consultar el doctor " + codigo, codigo);
        if (encontrados.isEmpty()) {
            throw noEncontrado(codigo);
        }
        return encontrados.get(0);
    }

    public boolean existe(String codigoDoctor) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");
        try (Connection con = conectar()) {
            return existe(con, codigo);
        } catch (SQLException e) {
            throw traducir(e, "consultar el doctor " + codigo);
        }
    }

    /** Todos los doctores, ordenados por código. */
    public List<Doctor> listar() throws PersistenciaException {
        return consultar(SQL_LISTAR, "listar los doctores");
    }

    /** Por ejemplo, solo los activos: listarPorEstado(EstadoRegistro.ACTIVO). */
    public List<Doctor> listarPorEstado(EstadoRegistro estado) throws PersistenciaException {
        exigirNoNulo(estado, "el estado");
        return consultar(SQL_POR_ESTADO, "listar los doctores por estado", MapeoBD.estadoABd(estado));
    }

    /** Doctores de una especialidad, activos e inactivos. */
    public List<Doctor> listarPorEspecialidad(Especialidad especialidad) throws PersistenciaException {
        exigirNoNulo(especialidad, "la especialidad");
        return consultar(SQL_POR_ESPECIALIDAD, "listar los doctores por especialidad", especialidad.toString());
    }

    /** Doctores cuyo nombre contiene el texto indicado (sin distinguir mayúsculas). */
    public List<Doctor> buscarPorNombre(String texto) throws PersistenciaException {
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        return consultar(SQL_POR_NOMBRE, "buscar doctores por nombre", patron);
    }

    /** Siguiente código libre (el mayor registrado más uno), útil al registrar. */
    public String siguienteCodigo() throws PersistenciaException {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(SQL_SIGUIENTE_CODIGO);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return MapeoBD.codigoDesdeBd(rs.getInt(1));
        } catch (SQLException e) {
            throw traducir(e, "calcular el siguiente código de doctor");
        }
    }

    // ------------------------------------------------------------------
    // Apoyo interno
    // ------------------------------------------------------------------

    /** Ejecuta un SELECT con sus parámetros y convierte cada fila en un Doctor. */
    private List<Doctor> consultar(String sql, String operacion, Object... parametros)
            throws PersistenciaException {
        List<Doctor> doctores = new ArrayList<>();
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    doctores.add(mapear(rs, ""));
                }
            }
        } catch (SQLException e) {
            throw traducir(e, operacion);
        }
        return doctores;
    }

    /**
     * Construye un Doctor con la fila actual del ResultSet.
     *
     * @param prefijo prefijo de los nombres de columna: "" en las consultas de
     *                esta clase y "d_" en el JOIN que hace CitaDAO
     * @throws DatoInvalidoException si la fila tiene un dato que la clase Doctor no acepta
     */
    static Doctor mapear(ResultSet rs, String prefijo) throws SQLException, DatoInvalidoException {
        String codigo = MapeoBD.codigoDesdeBd(rs.getInt(prefijo + "codigo_doctor"));
        try {
            // El constructor y los setters de la Parte 2 validan cada dato
            Doctor doctor = new Doctor(
                    codigo,
                    rs.getString(prefijo + "nombre_completo"),
                    MapeoBD.especialidad(rs.getString(prefijo + "especialidad")),
                    MapeoBD.telefonoDesdeBd(rs.getString(prefijo + "telefono")),
                    rs.getString(prefijo + "correo_electronico"));
            doctor.setEstadoDoctor(MapeoBD.estadoRegistro(rs.getString(prefijo + "estado_doctor")));
            return doctor;
        } catch (IllegalArgumentException | DatoInvalidoException e) {
            throw new DatoInvalidoException("El doctor " + codigo
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

    /** Evita registrar dos veces al mismo doctor con códigos distintos. */
    private static void verificarCorreoLibre(Connection con, String correo, int codigoPropio)
            throws SQLException, RegistroDuplicadoException {
        try (PreparedStatement ps = con.prepareStatement(SQL_CORREO_EN_USO)) {
            ps.setString(1, correo);
            ps.setInt(2, codigoPropio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new RegistroDuplicadoException("El correo " + correo
                            + " ya pertenece al doctor " + rs.getInt(1) + ".");
                }
            }
        }
    }

    private static void validarTamanos(Doctor doctor) throws DatoInvalidoException {
        exigirLongitud(doctor.getNombreCompleto(), 100, "nombre completo");
        exigirLongitud(doctor.getCorreo(), 100, "correo electrónico");
    }

    private static RegistroNoEncontradoException noEncontrado(int codigo) {
        return new RegistroNoEncontradoException("No existe un doctor con el código " + codigo + ".");
    }
}
