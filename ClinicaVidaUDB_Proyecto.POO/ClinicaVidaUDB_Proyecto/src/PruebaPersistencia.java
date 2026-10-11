import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * demostración que trata: conexión JDBC activa, sentencias preparadas, excepciones personalizadas y transacciones.
 * Se provoca a propósito cada tipo de error y muestra que el sistema lo captura y sigue funcionando. 
 */
public class PruebaPersistencia {

    /** Marca de los registros de prueba, para poder borrarlos sin tocar datos reales. */
    private static final String MARCA = "DEMO PARTE 3";

    private static final PacienteDAO pacientes = new PacienteDAO();
    private static final DoctorDAO doctores = new DoctorDAO();
    private static final CitaDAO citas = new CitaDAO();

    private static Paciente paciente;
    private static Doctor doctor;
    private static Cita cita;

    public static void main(String[] args) {
        titulo("1. CONEXIÓN A MYSQL MEDIANTE JDBC");
        try {
            System.out.println("Conectado a " + ConexionBD.describirConexion());
        } catch (ConexionException e) {
            mostrar(e);
            System.out.println("Sin conexión no hay demostración, pero el programa termina de forma ordenada.");
            return;
        }

        try {
            limpiar();                  // por si una ejecución anterior se interrumpió
            lectura();
            escritura();
            erroresControlados();
            transacciones();
        } catch (PersistenciaException e) {
            // Red de seguridad: cualquier error de base de datos no previsto arriba
            mostrar(e);
        } finally {
            titulo("6. LIMPIEZA");
            try {
                limpiar();
                System.out.println("Registros de prueba eliminados. La base de datos queda como estaba.");
            } catch (PersistenciaException e) {
                mostrar(e);
            }
        }
        System.out.println();
        System.out.println("Fin de la demostración: cada error fue capturado y el programa terminó con normalidad.");
    }

    // ------------------------------------------------------------------
    // 2. Lectura
    // ------------------------------------------------------------------

    private static void lectura() throws PersistenciaException {
        titulo("2. LECTURA CON PreparedStatement (SELECT)");

        List<Paciente> listaPacientes = pacientes.listar();
        System.out.println("Pacientes registrados: " + listaPacientes.size());
        for (Paciente p : listaPacientes) {
            System.out.println("  " + p.mostrarDatos());
        }

        List<Doctor> listaDoctores = doctores.listar();
        System.out.println("Doctores registrados: " + listaDoctores.size());
        for (Doctor d : listaDoctores) {
            System.out.println("  " + d.mostrarDatos());
        }

        List<Cita> listaCitas = citas.listar();
        System.out.println("Citas registradas (JOIN de las tres tablas): " + listaCitas.size());
        for (Cita c : listaCitas) {
            System.out.println("  " + c.mostrarDatos());
        }
    }

    // ------------------------------------------------------------------
    // 3. Escritura
    // ------------------------------------------------------------------

    private static void escritura() throws PersistenciaException {
        titulo("3. ESCRITURA CON PreparedStatement (INSERT y UPDATE)");

        paciente = new Paciente(pacientes.siguienteCodigo(), MARCA + " Paciente", 25, "71234567",
                "demo.paciente@parte3.test", TipoPaciente.ESTUDIANTE);
        pacientes.insertar(paciente);
        System.out.println("Paciente registrado:  "
                + pacientes.buscarPorCodigo(paciente.getCodigoPaciente()).mostrarDatos());

        doctor = new Doctor(doctores.siguienteCodigo(), MARCA + " Doctora", Especialidad.PSICOLOGIA,
                "72345678", "demo.doctora@parte3.test");
        doctores.insertar(doctor);
        System.out.println("Doctor registrado:    "
                + doctores.buscarPorCodigo(doctor.getCodigoDoctor()).mostrarDatos());

        cita = new Cita(citas.siguienteCodigo(), paciente, doctor, LocalDate.now().plusDays(30),
                LocalTime.of(9, 0), "Consulta de demostración");
        citas.insertar(cita);
        System.out.println("Cita registrada:      "
                + citas.buscarPorCodigo(cita.getCodigoCita()).mostrarDatos());

        paciente.setEdad(26);
        paciente.setTelefono("79876543");
        pacientes.actualizar(paciente);
        System.out.println("Paciente actualizado: "
                + pacientes.buscarPorCodigo(paciente.getCodigoPaciente()).mostrarDatos());
    }

    // ------------------------------------------------------------------
    // 4. Errores provocados a propósito
    // ------------------------------------------------------------------

    private static void erroresControlados() throws PersistenciaException {
        titulo("4. ERRORES PROVOCADOS A PROPÓSITO (el programa no se cae)");

        paso("4.1 Registrar otro paciente con un código que ya existe");
        try {
            pacientes.insertar(new Paciente(paciente.getCodigoPaciente(), MARCA + " Repetido", 30, "70000001",
                    "demo.repetido@parte3.test", TipoPaciente.VISITANTE));
            noOcurrio();
        } catch (RegistroDuplicadoException e) {
            mostrar(e);
        }

        paso("4.2 Registrar a la misma persona con otro código (mismo correo)");
        try {
            pacientes.insertar(new Paciente(pacientes.siguienteCodigo(), MARCA + " Repetido", 30, "70000001",
                    paciente.getCorreo(), TipoPaciente.VISITANTE));
            noOcurrio();
        } catch (RegistroDuplicadoException e) {
            mostrar(e);
        }

        paso("4.3 Buscar con un código que no es numérico");
        try {
            pacientes.buscarPorCodigo("PAC-001");
            noOcurrio();
        } catch (DatoInvalidoException e) {
            mostrar(e);
        }

        paso("4.4 Teléfono inválido: lo detiene la validación del modelo (Parte 2) antes de llegar a MySQL");
        try {
            pacientes.insertar(new Paciente(pacientes.siguienteCodigo(), MARCA + " Inválido", 30, "123",
                    "demo.invalido@parte3.test", TipoPaciente.VISITANTE));
            noOcurrio();
        } catch (IllegalArgumentException e) {
            System.out.println("  [ERROR CONTROLADO] IllegalArgumentException");
            System.out.println("      Mensaje para el usuario: " + e.getMessage());
        }

        paso("4.5 Consultar un doctor que no existe");
        try {
            doctores.buscarPorCodigo("999999");
            noOcurrio();
        } catch (RegistroNoEncontradoException e) {
            mostrar(e);
        }

        paso("4.6 Eliminar un paciente que tiene citas (llave foránea)");
        try {
            pacientes.eliminar(paciente.getCodigoPaciente());
            noOcurrio();
        } catch (IntegridadReferencialException e) {
            mostrar(e);
        }

        paso("4.7 Dar otra cita al mismo doctor en la misma fecha y hora");
        try {
            citas.insertar(new Cita(citas.siguienteCodigo(), paciente, doctor, cita.getFecha(), cita.getHora(),
                    "Cita que choca con la anterior"));
            noOcurrio();
        } catch (HorarioNoDisponibleException e) {
            mostrar(e);
        }

        paso("4.8 Intento de inyección de SQL en la búsqueda por nombre");
        List<Paciente> resultado = pacientes.buscarPorNombre("' OR '1'='1");
        System.out.println("  Resultados: " + resultado.size()
                + " (con PreparedStatement el texto se trata como un dato, no como SQL)");

        paso("4.9 Conectarse con una contraseña incorrecta");
        try {
            ConexionBD.obtenerConexion(ConexionBD.getUrl(), ConexionBD.getUsuario(),
                    "clave-incorrecta-de-prueba").close();
            noOcurrio();
        } catch (ConexionException e) {
            mostrar(e);
        } catch (SQLException e) {
            System.out.println("  No se pudo cerrar la conexión de prueba: " + e.getMessage());
        }

        paso("4.10 Conectarse a un puerto donde no hay ningún servidor MySQL");
        try {
            ConexionBD.obtenerConexion("jdbc:mysql://localhost:3399/clinica_universitaria?connectTimeout=3000",
                    ConexionBD.getUsuario(), "").close();
            noOcurrio();
        } catch (ConexionException e) {
            mostrar(e);
        } catch (SQLException e) {
            System.out.println("  No se pudo cerrar la conexión de prueba: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 5. Transacciones
    // ------------------------------------------------------------------

    private static void transacciones() throws PersistenciaException {
        titulo("5. TRANSACCIONES (commit y rollback)");

        paso("5.1 ROLLBACK: dos sentencias en una transacción y la segunda falla");
        String codigo = paciente.getCodigoPaciente();
        int codigoEnBd = MapeoBD.codigoABd(codigo, "código de paciente");
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);                                   
            try (PreparedStatement cambio = con.prepareStatement(
                         "UPDATE pacientes SET nombre_completo = ? WHERE codigo_paciente = ?");
                 PreparedStatement repetido = con.prepareStatement(
                         "INSERT INTO pacientes (codigo_paciente, nombre_completo, edad, telefono, "
                         + "correo_electronico, tipo_paciente, estado_paciente) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                cambio.setString(1, MARCA + " Nombre provisional");
                cambio.setInt(2, codigoEnBd);
                cambio.executeUpdate();
                System.out.println("  Paso 1 (UPDATE) ejecutado: el nombre cambió dentro de la transacción.");

                repetido.setInt(1, codigoEnBd);                         
                repetido.setString(2, MARCA + " Repetido");
                repetido.setInt(3, 30);
                repetido.setString(4, "7000-0001");
                repetido.setString(5, "demo.repetido@parte3.test");
                repetido.setString(6, TipoPaciente.VISITANTE.toString());
                repetido.setString(7, MapeoBD.estadoABd(EstadoRegistro.ACTIVO));
                repetido.executeUpdate();

                con.commit();
                noOcurrio();
            } catch (SQLException e) {
                con.rollback();                                         // deshace también el paso 1
                System.out.println("  Paso 2 (INSERT) falló: " + e.getMessage());
                System.out.println("  ROLLBACK ejecutado.");
            }
        } catch (SQLException e) {
            System.out.println("  Error de base de datos en la demostración del rollback: " + e.getMessage());
        }
        System.out.println("  Nombre del paciente después del rollback: "
                + pacientes.buscarPorCodigo(codigo).getNombreCompleto() + " (el paso 1 no quedó guardado)");

        paso("5.2 COMMIT: dar de baja al doctor y cancelar sus citas programadas en una sola transacción");
        int canceladas = doctores.darDeBaja(doctor.getCodigoDoctor());
        System.out.println("  Citas canceladas: " + canceladas);
        System.out.println("  Estado del doctor:  " + doctores.buscarPorCodigo(doctor.getCodigoDoctor()).getEstadoDoctor());
        System.out.println("  Estado de la cita:  " + citas.buscarPorCodigo(cita.getCodigoCita()).getEstadoCita());
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /** Elimina los registros de prueba: primero sus citas y luego los pacientes y doctores. */
    private static void limpiar() throws PersistenciaException {
        for (Paciente p : pacientes.buscarPorNombre(MARCA)) {
            for (Cita c : citas.listarPorPaciente(p.getCodigoPaciente())) {
                citas.eliminar(c.getCodigoCita());
            }
        }
        for (Doctor d : doctores.buscarPorNombre(MARCA)) {
            for (Cita c : citas.listarPorDoctor(d.getCodigoDoctor())) {
                citas.eliminar(c.getCodigoCita());
            }
        }
        for (Paciente p : pacientes.buscarPorNombre(MARCA)) {
            pacientes.eliminar(p.getCodigoPaciente());
        }
        for (Doctor d : doctores.buscarPorNombre(MARCA)) {
            doctores.eliminar(d.getCodigoDoctor());
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println(texto);
        System.out.println("==================================================================");
    }

    private static void paso(String texto) {
        System.out.println();
        System.out.println(texto);
    }

    /** Muestra el error capturado: el mensaje para el usuario y el detalle técnico. */
    private static void mostrar(PersistenciaException e) {
        System.out.println("  [ERROR CONTROLADO] " + e.getClass().getSimpleName());
        System.out.println("      Mensaje para el usuario: " + e.getMessage());
        if (!e.getDetalleTecnico().isEmpty()) {
            System.out.println("      Detalle técnico:         " + e.getDetalleTecnico());
        }
    }

    private static void noOcurrio() {
        System.out.println("  [REVISAR] No se produjo el error que se esperaba.");
    }
}
