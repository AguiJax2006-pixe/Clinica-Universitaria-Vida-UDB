import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Clase de prueba: demuestra herencia, polimorfismo y encapsulamiento. */
public class PruebaDominio {
    public static void main(String[] args) {
        Paciente p = new Paciente("PAC-001", "Ana Martínez", 21, "71234567",
                                  "ana@correo.com", TipoPaciente.ESTUDIANTE);
        Doctor d = new Doctor("DOC-001", "Luis Ramírez", Especialidad.MEDICINA_GENERAL,
                              "72345678", "luis@vidaudb.edu.sv");

        // Polimorfismo: una lista de Persona con dos tipos distintos
        List<Persona> personas = new ArrayList<>();
        personas.add(p);
        personas.add(d);
        for (Persona persona : personas) {
            System.out.println(persona.mostrarDatos());
        }

        Cita c = new Cita("CIT-001", p, d, LocalDate.now().plusDays(2), LocalTime.of(9, 30), "Control general");
        System.out.println(c.mostrarDatos());
        c.marcarAtendida();
        System.out.println("Estado de la cita: " + c.getEstadoCita());

        // Encapsulamiento: los setters rechazan datos inválidos
        try {
            p.setTelefono("abc");
        } catch (IllegalArgumentException e) {
            System.out.println("Validación: " + e.getMessage());
        }
        try {
            c.cancelar();
        } catch (IllegalStateException e) {
            System.out.println("Regla: " + e.getMessage());
        }
    }
}
