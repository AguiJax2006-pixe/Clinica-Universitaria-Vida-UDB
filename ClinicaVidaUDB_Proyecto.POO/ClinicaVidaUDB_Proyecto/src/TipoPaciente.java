/** Tipos de paciente permitidos por la Clínica Universitaria Vida UDB. */
public enum TipoPaciente {
    ESTUDIANTE("Estudiante"),
    DOCENTE("Docente"),
    ADMINISTRATIVO("Administrativo"),
    VISITANTE("Visitante");

    private final String etiqueta;

    TipoPaciente(String etiqueta) { this.etiqueta = etiqueta; }

    @Override
    public String toString() { return etiqueta; }
}
