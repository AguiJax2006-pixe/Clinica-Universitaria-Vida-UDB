/** Especialidades médicas permitidas. */
public enum Especialidad {
    MEDICINA_GENERAL("Medicina General"),
    PSICOLOGIA("Psicología"),
    NUTRICION("Nutrición"),
    FISIOTERAPIA("Fisioterapia");

    private final String etiqueta;

    Especialidad(String etiqueta) { this.etiqueta = etiqueta; }

    @Override
    public String toString() { return etiqueta; }
}
