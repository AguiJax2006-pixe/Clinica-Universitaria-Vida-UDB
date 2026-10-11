import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class CitaDAO extends DAOBase {

    // Select común a todas las consultas; las columnas del JOIN llevan prefijo p_ y d_
    private static final String SQL_SELECT =
            "SELECT c.codigo_cita, c.fecha, c.hora, c.motivo, c.estado_cita, "
            + "p.codigo_paciente AS p_codigo_paciente, p.nombre_completo AS p_nombre_completo, "
            + "p.edad AS p_edad, p.telefono AS p_telefono, p.correo_electronico AS p_correo_electronico, "
            + "p.tipo_paciente AS p_tipo_paciente, p.estado_paciente AS p_estado_paciente, "
            + "d.codigo_doctor AS d_codigo_doctor, d.nombre_completo AS d_nombre_completo, "
            + "d.especialidad AS d_especialidad, d.telefono AS d_telefono, "
            + "d.correo_electronico AS d_correo_electronico, d.estado_doctor AS d_estado_doctor "
            + "FROM citas c "
            + "INNER JOIN pacientes p ON p.codigo_paciente = c.codigo_paciente "
            + "INNER JOIN doctores d ON d.codigo_doctor = c.codigo_doctor";

    private static final String ORDEN = " ORDER BY c.fecha, c.hora, c.codigo_cita";

    private static final String SQL_INSERTAR =
            "INSERT INTO citas (codigo_cita, codigo_paciente, codigo_doctor, fecha, hora, motivo, estado_cita) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE citas SET codigo_paciente = ?, codigo_doctor = ?, fecha = ?, hora = ?, motivo = ?, "
            + "estado_cita = ? WHERE codigo_cita = ?";

    private static final String SQL_CAMBIAR_ESTADO =
            "UPDATE citas SET estado_cita = ? WHERE codigo_cita = ?";

    private static final String SQL_ELIMINAR =
            "DELETE FROM citas WHERE codigo_cita = ?";

    // FOR UPDATE bloquea la fila hasta que la transacción termina
    private static final String SQL_BLOQUEAR_CITA =
            "SELECT codigo_paciente, codigo_doctor, fecha, hora FROM citas WHERE codigo_cita = ? FOR UPDATE";

    private static final String SQL_BLOQUEAR_DOCTOR =
            "SELECT nombre_completo, estado_doctor FROM doctores WHERE codigo_doctor = ? FOR UPDATE";

    // Bloqueo compartido
    private static final String SQL_BLOQUEAR_PACIENTE =
            "SELECT nombre_completo, estado_paciente FROM pacientes WHERE codigo_paciente = ? LOCK IN SHARE MODE";

    private static final String SQL_CONTAR_CHOQUES =
            "SELECT COUNT(*) FROM citas WHERE codigo_doctor = ? AND fecha = ? AND hora = ? AND estado_cita = ?";

    private static final String SQL_SIGUIENTE_CODIGO =
            "SELECT COALESCE(MAX(codigo_cita), 0) + 1 FROM citas";

    private static final DateTimeFormatter FECHA_LEGIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_LEGIBLE = DateTimeFormatter.ofPattern("HH:mm");

    // ------------------------------------------------------------------
    // Altas, cambios y bajas
    // ------------------------------------------------------------------

    /**
     * Registra una cita nueva dentro de una transacción.
     *
     * @throws HorarioNoDisponibleException   si el doctor ya tiene una cita programada en esa fecha y hora
     * @throws RegistroDuplicadoException     si el código de cita ya existe
     * @throws IntegridadReferencialException si el paciente o el doctor no existen en la base de datos
     * @throws DatoInvalidoException          si el paciente o el doctor están inactivos o un dato no puede guardarse
     */
    public void insertar(Cita cita) throws PersistenciaException {
        exigirNoNulo(cita, "la cita");
        int codigo = MapeoBD.codigoABd(cita.getCodigoCita(), "código de cita");
        int codigoPaciente = MapeoBD.codigoABd(cita.getPaciente().getCodigoPaciente(), "código de paciente");
        int codigoDoctor = MapeoBD.codigoABd(cita.getDoctor().getCodigoDoctor(), "código de doctor");
        String fecha = MapeoBD.fechaABd(cita.getFecha());
        String hora = MapeoBD.horaABd(cita.getHora());
        exigirLongitud(cita.getMotivo(), 255, "motivo de consulta");

        try (Connection con = conectar()) {
            boolean confirmada = false;
            con.setAutoCommit(false);                       // inicia la transacción
            try {
                if (cita.getEstadoCita() == EstadoCita.PROGRAMADA) {
                    verificarQueSePuedeProgramar(con, codigo, codigoPaciente, codigoDoctor,
                            cita.getFecha(), cita.getHora());
                }
                try (PreparedStatement ps = con.prepareStatement(SQL_INSERTAR)) {
                    ps.setInt(1, codigo);
                    ps.setInt(2, codigoPaciente);
                    ps.setInt(3, codigoDoctor);
                    ps.setString(4, fecha);
                    ps.setString(5, hora);
                    ps.setString(6, cita.getMotivo());
                    ps.setString(7, cita.getEstadoCita().toString());
                    ps.executeUpdate();
                }
                con.commit();                               // todo salió bien: se confirma
                confirmada = true;
            } finally {
                if (!confirmada) {
                    deshacer(con);                          // algo falló: no queda nada a medias
                }
            }
        } catch (SQLException e) {
            throw traducir(e, "registrar la cita " + codigo);
        }
    }

    /**
     * Guarda los cambios de una cita existente 
     * @throws RegistroNoEncontradoException si no existe una cita con ese código
     * @throws HorarioNoDisponibleException  si el nuevo horario choca con otra cita del doctor
     */
    public void actualizar(Cita cita) throws PersistenciaException {
        exigirNoNulo(cita, "la cita");
        int codigo = MapeoBD.codigoABd(cita.getCodigoCita(), "código de cita");
        int codigoPaciente = MapeoBD.codigoABd(cita.getPaciente().getCodigoPaciente(), "código de paciente");
        int codigoDoctor = MapeoBD.codigoABd(cita.getDoctor().getCodigoDoctor(), "código de doctor");
        String fecha = MapeoBD.fechaABd(cita.getFecha());
        String hora = MapeoBD.horaABd(cita.getHora());
        exigirLongitud(cita.getMotivo(), 255, "motivo de consulta");

        try (Connection con = conectar()) {
            boolean confirmada = false;
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(SQL_BLOQUEAR_CITA)) {
                    ps.setInt(1, codigo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw noEncontrada(codigo);
                        }
                    }
                }
                if (cita.getEstadoCita() == EstadoCita.PROGRAMADA) {
                    verificarQueSePuedeProgramar(con, codigo, codigoPaciente, codigoDoctor,
                            cita.getFecha(), cita.getHora());
                }
                try (PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR)) {
                    ps.setInt(1, codigoPaciente);
                    ps.setInt(2, codigoDoctor);
                    ps.setString(3, fecha);
                    ps.setString(4, hora);
                    ps.setString(5, cita.getMotivo());
                    ps.setString(6, cita.getEstadoCita().toString());
                    ps.setInt(7, codigo);
                    ps.executeUpdate();
                }
                con.commit();
                confirmada = true;
            } finally {
                if (!confirmada) {
                    deshacer(con);
                }
            }
        } catch (SQLException e) {
            throw traducir(e, "actualizar la cita " + codigo);
        }
    }

    /**
     * Cambia solo el estado de la cita 
     * @throws RegistroNoEncontradoException si no existe una cita con ese código
     */
    public void cambiarEstado(String codigoCita, EstadoCita nuevoEstado) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoCita, "código de cita");
        exigirNoNulo(nuevoEstado, "el nuevo estado");

        try (Connection con = conectar()) {
            boolean confirmada = false;
            con.setAutoCommit(false);
            try {
                int codigoPaciente;
                int codigoDoctor;
                LocalDate fecha;
                LocalTime hora;
                try (PreparedStatement ps = con.prepareStatement(SQL_BLOQUEAR_CITA)) {
                    ps.setInt(1, codigo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw noEncontrada(codigo);
                        }
                        codigoPaciente = rs.getInt("codigo_paciente");
                        codigoDoctor = rs.getInt("codigo_doctor");
                        fecha = MapeoBD.fechaDesdeBd(rs.getString("fecha"));
                        hora = MapeoBD.horaDesdeBd(rs.getString("hora"));
                    }
                }
                if (nuevoEstado == EstadoCita.PROGRAMADA) {
                    verificarQueSePuedeProgramar(con, codigo, codigoPaciente, codigoDoctor, fecha, hora);
                }
                try (PreparedStatement ps = con.prepareStatement(SQL_CAMBIAR_ESTADO)) {
                    ps.setString(1, nuevoEstado.toString());
                    ps.setInt(2, codigo);
                    ps.executeUpdate();
                }
                con.commit();
                confirmada = true;
            } finally {
                if (!confirmada) {
                    deshacer(con);
                }
            }
        } catch (SQLException e) {
            throw traducir(e, "cambiar el estado de la cita " + codigo);
        }
    }

    /**
     * Elimina la cita 
     * @throws RegistroNoEncontradoException si no existe
     */
    public void eliminar(String codigoCita) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoCita, "código de cita");

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(SQL_ELIMINAR)) {
            ps.setInt(1, codigo);
            if (ps.executeUpdate() == 0) {
                throw noEncontrada(codigo);
            }
        } catch (SQLException e) {
            throw traducir(e, "eliminar la cita " + codigo);
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * @throws RegistroNoEncontradoException si no existe una cita con ese código
     */
    public Cita buscarPorCodigo(String codigoCita) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoCita, "código de cita");
        List<Cita> encontradas = consultar(SQL_SELECT + " WHERE c.codigo_cita = ?",
                "consultar la cita " + codigo, codigo);
        if (encontradas.isEmpty()) {
            throw noEncontrada(codigo);
        }
        return encontradas.get(0);
    }

    //Todas las citas, ordenadas por fecha y hora
    public List<Cita> listar() throws PersistenciaException {
        return consultar(SQL_SELECT + ORDEN, "listar las citas");
    }

    //Citas de un doctor 
    public List<Cita> listarPorDoctor(String codigoDoctor) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");
        return consultar(SQL_SELECT + " WHERE c.codigo_doctor = ?" + ORDEN,
                "listar las citas del doctor " + codigo, codigo);
    }

    //Historial de citas de un paciente
    public List<Cita> listarPorPaciente(String codigoPaciente) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoPaciente, "código de paciente");
        return consultar(SQL_SELECT + " WHERE c.codigo_paciente = ?" + ORDEN,
                "listar las citas del paciente " + codigo, codigo);
    }

    //Citas de un día (reporte por fecha)
    public List<Cita> listarPorFecha(LocalDate fecha) throws PersistenciaException {
        exigirNoNulo(fecha, "la fecha");
        return consultar(SQL_SELECT + " WHERE c.fecha = ?" + ORDEN,
                "listar las citas por fecha", MapeoBD.fechaABd(fecha));
    }

    
    public List<Cita> listarPorEstado(EstadoCita estado) throws PersistenciaException {
        exigirNoNulo(estado, "el estado");
        return consultar(SQL_SELECT + " WHERE c.estado_cita = ?" + ORDEN,
                "listar las citas por estado", estado.toString());
    }

    //Agenda de un doctor en un día
    public List<Cita> listarPorDoctorYFecha(String codigoDoctor, LocalDate fecha) throws PersistenciaException {
        int codigo = MapeoBD.codigoABd(codigoDoctor, "código de doctor");
        exigirNoNulo(fecha, "la fecha");
        return consultar(SQL_SELECT + " WHERE c.codigo_doctor = ? AND c.fecha = ?" + ORDEN,
                "listar la agenda del doctor " + codigo, codigo, MapeoBD.fechaABd(fecha));
    }

    /**
     * Indica si el doctor tiene libre ese horario
     */
    public boolean horarioDisponible(String codigoDoctor, LocalDate fecha, LocalTime hora)
            throws PersistenciaException {
        return horarioDisponible(codigoDoctor, fecha, hora, null);
    }

    /**
     * Igual que el anterior, pero ignora la cita indicada (para cuando se está modificando una cita y no se quiere que choque consigo misma)
     * @param codigoCitaAIgnorar código de la cita que se está modificando, o null
     */
    public boolean horarioDisponible(String codigoDoctor, LocalDate fecha, LocalTime hora,
            String codigoCitaAIgnorar) throws PersistenciaException {
        int doctor = MapeoBD.codigoABd(codigoDoctor, "código de doctor");
        exigirNoNulo(fecha, "la fecha");
        exigirNoNulo(hora, "la hora");
        Integer ignorar = codigoCitaAIgnorar == null
                ? null : Integer.valueOf(MapeoBD.codigoABd(codigoCitaAIgnorar, "código de cita"));

        try (Connection con = conectar()) {
            return contarChoques(con, doctor, fecha, hora, ignorar) == 0;
        } catch (SQLException e) {
            throw traducir(e, "consultar la disponibilidad del doctor " + doctor);
        }
    }

    // Siguiente código libre (el mayor registrado más uno), es útil al registrar
    public String siguienteCodigo() throws PersistenciaException {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(SQL_SIGUIENTE_CODIGO);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return MapeoBD.codigoDesdeBd(rs.getInt(1));
        } catch (SQLException e) {
            throw traducir(e, "calcular el siguiente código de cita");
        }
    }

    // ------------------------------------------------------------------
    // Apoyo interno
    // ------------------------------------------------------------------

    /**
     * Comprobaciones previas a dejar una cita en estado Programada. Se ejecutan dentro de la transacción de quien llama
     */
    private static void verificarQueSePuedeProgramar(Connection con, int codigoCita, int codigoPaciente,
            int codigoDoctor, LocalDate fecha, LocalTime hora) throws SQLException, PersistenciaException {

        String doctor;
        try (PreparedStatement ps = con.prepareStatement(SQL_BLOQUEAR_DOCTOR)) {
            ps.setInt(1, codigoDoctor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IntegridadReferencialException(
                            "No existe un doctor con el código " + codigoDoctor + ".");
                }
                doctor = "El doctor " + codigoDoctor + " (" + rs.getString("nombre_completo") + ")";
                if (MapeoBD.estadoRegistro(rs.getString("estado_doctor")) != EstadoRegistro.ACTIVO) {
                    throw new DatoInvalidoException(doctor + " está inactivo y no puede recibir citas.");
                }
            }
        }

        try (PreparedStatement ps = con.prepareStatement(SQL_BLOQUEAR_PACIENTE)) {
            ps.setInt(1, codigoPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IntegridadReferencialException(
                            "No existe un paciente con el código " + codigoPaciente + ".");
                }
                if (MapeoBD.estadoRegistro(rs.getString("estado_paciente")) != EstadoRegistro.ACTIVO) {
                    throw new DatoInvalidoException("El paciente " + codigoPaciente + " ("
                            + rs.getString("nombre_completo") + ") está inactivo y no puede recibir citas.");
                }
            }
        }

        if (contarChoques(con, codigoDoctor, fecha, hora, Integer.valueOf(codigoCita)) > 0) {
            throw new HorarioNoDisponibleException(doctor + " ya tiene una cita programada el "
                    + fecha.format(FECHA_LEGIBLE) + " a las " + hora.format(HORA_LEGIBLE) + ".");
        }
    }

    // Cuenta las citas programadas del doctor en esa fecha y hora, sin contar la cita indicada
    private static int contarChoques(Connection con, int codigoDoctor, LocalDate fecha, LocalTime hora,
            Integer citaAIgnorar) throws SQLException, DatoInvalidoException {
        String sql = SQL_CONTAR_CHOQUES + (citaAIgnorar == null ? "" : " AND codigo_cita <> ?");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoDoctor);
            ps.setString(2, MapeoBD.fechaABd(fecha));
            ps.setString(3, MapeoBD.horaABd(hora));
            ps.setString(4, EstadoCita.PROGRAMADA.toString());
            if (citaAIgnorar != null) {
                ps.setInt(5, citaAIgnorar.intValue());
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // Ejecuta un SELECT con sus parámetros y convierte cada fila en una Cita
    private List<Cita> consultar(String sql, String operacion, Object... parametros)
            throws PersistenciaException {
        List<Cita> citas = new ArrayList<>();
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    citas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw traducir(e, operacion);
        }
        return citas;
    }

    /**
     * Construye una Cita completa con su paciente y su doctor) a partir de la
     * fila actual del JOIN.
     */
    private static Cita mapear(ResultSet rs) throws SQLException, DatoInvalidoException {
        String codigo = MapeoBD.codigoDesdeBd(rs.getInt("codigo_cita"));
        Paciente paciente = PacienteDAO.mapear(rs, "p_");
        Doctor doctor = DoctorDAO.mapear(rs, "d_");
        try {
            LocalDate fecha = MapeoBD.fechaDesdeBd(rs.getString("fecha"));
            LocalTime hora = MapeoBD.horaDesdeBd(rs.getString("hora"));
            EstadoCita estado = MapeoBD.estadoCita(rs.getString("estado_cita"));

            EstadoRegistro estadoPaciente = paciente.getEstadoPaciente();
            EstadoRegistro estadoDoctor = doctor.getEstadoDoctor();
            paciente.setEstadoPaciente(EstadoRegistro.ACTIVO);
            doctor.setEstadoDoctor(EstadoRegistro.ACTIVO);

            Cita cita = new Cita(codigo, paciente, doctor, fecha, hora, rs.getString("motivo"));

            paciente.setEstadoPaciente(estadoPaciente);
            doctor.setEstadoDoctor(estadoDoctor);
            cita.setEstadoCita(estado);       // el constructor siempre la deja en Programada
            return cita;
        } catch (IllegalArgumentException | DatoInvalidoException e) {
            throw new DatoInvalidoException("La cita " + codigo
                    + " guardada en la base de datos tiene un dato que el sistema no acepta. " + e.getMessage(), e);
        }
    }

    private static RegistroNoEncontradoException noEncontrada(int codigo) {
        return new RegistroNoEncontradoException("No existe una cita con el código " + codigo + ".");
    }
}
