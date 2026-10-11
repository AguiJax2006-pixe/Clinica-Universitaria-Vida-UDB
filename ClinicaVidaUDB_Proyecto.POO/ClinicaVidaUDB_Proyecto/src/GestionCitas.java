import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class GestionCitas extends JPanel {

    private final CitaDAO citaDAO = new CitaDAO();
    private final PacienteDAO pacienteDAO = new PacienteDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    private final JComboBox<Paciente> comboPacientes = new JComboBox<>();
    private final JComboBox<Doctor> comboDoctores = new JComboBox<>();
    private final JTextField txtFecha = new JTextField();
    private final JTextField txtHora = new JTextField();
    private final JTextField txtMotivo = new JTextField();

    private final DefaultTableModel modelo = new DefaultTableModel(
        new String[]{"Código", "Paciente", "Doctor", "Fecha", "Hora", "Motivo", "Estado"}, 0
    ) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modelo);

    public GestionCitas() {
        setLayout(new BorderLayout(8, 8));

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.add(new JLabel("Paciente:"));
        formulario.add(comboPacientes);
        formulario.add(new JLabel("Doctor:"));
        formulario.add(comboDoctores);
        formulario.add(new JLabel("Fecha (AAAA-MM-DD):"));
        formulario.add(txtFecha);
        formulario.add(new JLabel("Hora (HH:MM):"));
        formulario.add(txtHora);
        formulario.add(new JLabel("Motivo:"));
        formulario.add(txtMotivo);

        JButton btnRegistrar = new JButton("Registrar cita");
        JButton btnActualizar = new JButton("Actualizar tabla");

        JPanel botones = new JPanel();
        botones.add(btnRegistrar);
        botones.add(btnActualizar);

        JPanel superior = new JPanel(new BorderLayout(5, 5));
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        add(superior, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrarCita());
        btnActualizar.addActionListener(e -> cargarDatos());

        cargarCombos();
        cargarDatos();
    }

    private void cargarCombos() {
        try {
            comboPacientes.removeAllItems();
            List<Paciente> pacientes =
                pacienteDAO.listarPorEstado(EstadoRegistro.ACTIVO);

            for (Paciente paciente : pacientes) {
                comboPacientes.addItem(paciente);
            }

            comboDoctores.removeAllItems();
            List<Doctor> doctores =
                doctorDAO.listarPorEstado(EstadoRegistro.ACTIVO);

            for (Doctor doctor : doctores) {
                comboDoctores.addItem(doctor);
            }
        } catch (Exception ex) {
            mostrarError("No se pudieron cargar pacientes y doctores: "
                + ex.getMessage());
        }
    }

    private void registrarCita() {
        try {
            Paciente paciente =
                (Paciente) comboPacientes.getSelectedItem();
            Doctor doctor =
                (Doctor) comboDoctores.getSelectedItem();

            if (paciente == null || doctor == null) {
                throw new IllegalArgumentException(
                    "Debe existir un paciente y un doctor activos.");
            }

            LocalDate fecha = LocalDate.parse(txtFecha.getText().trim());
            LocalTime hora = LocalTime.parse(txtHora.getText().trim());
            String motivo = txtMotivo.getText().trim();

            Cita cita = new Cita(
                citaDAO.siguienteCodigo(),
                paciente,
                doctor,
                fecha,
                hora,
                motivo
            );

            citaDAO.insertar(cita);

            JOptionPane.showMessageDialog(
                this, "Cita registrada correctamente."
            );

            txtFecha.setText("");
            txtHora.setText("");
            txtMotivo.setText("");
            cargarDatos();

        } catch (DateTimeParseException ex) {
            mostrarError(
                "Revisa la fecha y la hora.\n"
                + "Fecha: AAAA-MM-DD (ejemplo: 2026-10-15)\n"
                + "Hora: HH:MM (ejemplo: 09:30)"
            );
        } catch (Exception ex) {
            mostrarError("No se pudo registrar la cita: "
                + ex.getMessage());
        }
    }

    private void cargarDatos() {
        try {
            modelo.setRowCount(0);

            for (Cita cita : citaDAO.listar()) {
                modelo.addRow(new Object[]{
                    cita.getCodigoCita(),
                    cita.getPaciente().getNombreCompleto(),
                    cita.getDoctor().getNombreCompleto(),
                    cita.getFecha(),
                    cita.getHora(),
                    cita.getMotivo(),
                    cita.getEstadoCita()
                });
            }
        } catch (Exception ex) {
            mostrarError("No se pudieron consultar las citas: "
                + ex.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
            this, mensaje, "Sistema de citas",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
