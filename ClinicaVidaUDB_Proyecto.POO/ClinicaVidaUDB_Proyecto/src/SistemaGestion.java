
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class SistemaGestion {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            List<Paciente> pacientes = new ArrayList<>();
            List<Doctor> doctores = new ArrayList<>();

            JFrame ventana = new JFrame(
                    "Sistema de Gestión - Clínica Universitaria Vida UDB"
            );

            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setSize(1100, 600);
            ventana.setLocationRelativeTo(null);

            JTabbedPane pestanas = new JTabbedPane();

            pestanas.addTab(
                    "Gestión de pacientes",
                    new GestionPacientes(pacientes)
            );

            pestanas.addTab(
                    "Gestión de doctores",
                    new GestionDoctores(doctores)
            );

            pestanas.addTab(
                    "Gestión de citas",
                    new GestionCitas()
            );

            pestanas.addTab(
                    "Reportes",
                    new GestionReportes()
            );

            ventana.add(pestanas, BorderLayout.CENTER);
            ventana.setVisible(true);
        });
    }
}
