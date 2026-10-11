import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;


public final class MapeoBD {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MapeoBD() {
    }

    // ------------------------------------------------------------------
    // Códigos: INT en la base de datos, String en los objetos
    // ------------------------------------------------------------------

    /**
     * Convierte el código de un objeto al entero que usa la llave primaria.
     *
     * @param codigo texto del código, por ejemplo "6"
     * @param campo  nombre del dato para el mensaje, por ejemplo "código de paciente"
     * @throws DatoInvalidoException si no es un entero positivo
     */
    public static int codigoABd(String codigo, String campo) throws DatoInvalidoException {
        if (codigo == null || !codigo.trim().matches("[0-9]{1,9}") || Integer.parseInt(codigo.trim()) == 0) {
            throw new DatoInvalidoException("El " + campo + " debe ser un número entero positivo, por ejemplo 6. "
                    + "Se recibió: " + entreComillas(codigo) + ".");
        }
        return Integer.parseInt(codigo.trim());
    }

    public static String codigoDesdeBd(int codigo) {
        return String.valueOf(codigo);
    }

    // ------------------------------------------------------------------
    // Teléfono: 'NNNN-NNNN' en la base de datos, 8 dígitos en los objetos
    // ------------------------------------------------------------------

    /** "70123456" se guarda como "7012-3456", el formato de los datos de la Parte 1. */
    public static String telefonoABd(String telefono) {
        if (telefono != null && telefono.matches("[0-9]{8}")) {
            return telefono.substring(0, 4) + "-" + telefono.substring(4);
        }
        return telefono;
    }

    /** Deja solo los dígitos: "7012-3456" se entrega a Persona como "70123456". */
    public static String telefonoDesdeBd(String telefono) {
        return telefono == null ? null : telefono.replaceAll("[^0-9]", "");
    }

    // ------------------------------------------------------------------
    // Estado de pacientes y doctores
    // ------------------------------------------------------------------

    /** ACTIVO se guarda como "Activo" e INACTIVO como "Inactivo". */
    public static String estadoABd(EstadoRegistro estado) {
        String nombre = estado.name();
        return nombre.substring(0, 1) + nombre.substring(1).toLowerCase(Locale.ROOT);
    }

    public static EstadoRegistro estadoRegistro(String texto) throws DatoInvalidoException {
        for (EstadoRegistro estado : EstadoRegistro.values()) {
            if (mismoTexto(estado.name(), texto)) {
                return estado;
            }
        }
        throw new DatoInvalidoException("Estado no permitido: " + entreComillas(texto)
                + ". Valores válidos: Activo, Inactivo.");
    }

    // ------------------------------------------------------------------
    // Enumeraciones con etiqueta (su toString() es el texto que se guarda)
    // ------------------------------------------------------------------

    public static TipoPaciente tipoPaciente(String texto) throws DatoInvalidoException {
        for (TipoPaciente tipo : TipoPaciente.values()) {
            if (mismoTexto(tipo.toString(), texto) || mismoTexto(tipo.name(), texto)) {
                return tipo;
            }
        }
        throw new DatoInvalidoException("Tipo de paciente no permitido: " + entreComillas(texto)
                + ". Valores válidos: Estudiante, Docente, Administrativo, Visitante.");
    }

    public static Especialidad especialidad(String texto) throws DatoInvalidoException {
        for (Especialidad especialidad : Especialidad.values()) {
            if (mismoTexto(especialidad.toString(), texto) || mismoTexto(especialidad.name(), texto)) {
                return especialidad;
            }
        }
        throw new DatoInvalidoException("Especialidad no permitida: " + entreComillas(texto)
                + ". Valores válidos: Medicina General, Psicología, Nutrición, Fisioterapia.");
    }

    public static EstadoCita estadoCita(String texto) throws DatoInvalidoException {
        for (EstadoCita estado : EstadoCita.values()) {
            if (mismoTexto(estado.toString(), texto) || mismoTexto(estado.name(), texto)) {
                return estado;
            }
        }
        throw new DatoInvalidoException("Estado de cita no permitido: " + entreComillas(texto)
                + ". Valores válidos: Programada, Atendida, Cancelada.");
    }

    // ------------------------------------------------------------------
    // Fecha y hora
    // ------------------------------------------------------------------

    /*
     * Las fechas y horas viajan como texto ISO ('2026-10-12', '08:00:00') y MySQL
     * las convierte a DATE y TIME. Así el valor guardado no depende de la zona
     * horaria de la computadora ni de la versión del conector.
     */

    /**
     * @throws DatoInvalidoException si el año está fuera del rango que admite una columna DATE
     */
    public static String fechaABd(LocalDate fecha) throws DatoInvalidoException {
        if (fecha.getYear() < 1000 || fecha.getYear() > 9999) {
            throw new DatoInvalidoException("La fecha " + fecha + " no es válida: el año debe estar entre 1000 y 9999.");
        }
        return fecha.toString();                      // AAAA-MM-DD
    }

    public static String horaABd(LocalTime hora) {
        return hora.format(FORMATO_HORA);             // HH:MM:SS
    }

    public static LocalDate fechaDesdeBd(String texto) throws DatoInvalidoException {
        try {
            return LocalDate.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("No es una fecha válida: " + entreComillas(texto)
                    + ". Formato esperado: AAAA-MM-DD.", e);
        }
    }

    public static LocalTime horaDesdeBd(String texto) throws DatoInvalidoException {
        try {
            return LocalTime.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("No es una hora válida: " + entreComillas(texto)
                    + ". Formato esperado: HH:MM:SS, entre 00:00:00 y 23:59:59.", e);
        }
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /** Compara sin distinguir mayúsculas, tildes, guiones bajos ni espacios de más. */
    private static boolean mismoTexto(String a, String b) {
        return a != null && b != null && normalizar(a).equals(normalizar(b));
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replace('_', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String entreComillas(String texto) {
        return texto == null ? "(vacío)" : "'" + texto + "'";
    }
}
