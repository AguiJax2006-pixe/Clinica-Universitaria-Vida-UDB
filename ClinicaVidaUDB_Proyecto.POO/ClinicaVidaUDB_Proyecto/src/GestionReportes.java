
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class GestionReportes extends JPanel {

    private final CitaDAO citaDAO = new CitaDAO();

    private final JComboBox<String> comboTipo = new JComboBox<>(
            new String[]{
                "Citas por fecha",
                "Citas por doctor",
                "Historial del paciente"
            }
    );

    private final JTextField txtBusqueda = new JTextField(12);

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{
                "Código", "Paciente", "Doctor",
                "Fecha", "Hora", "Motivo", "Estado"
            }, 0
    ) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modelo);

    public GestionReportes() {
        setLayout(new BorderLayout(8, 8));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnBuscar = new JButton("Generar reporte");

        controles.add(new JLabel("Tipo de reporte:"));
        controles.add(comboTipo);
        controles.add(new JLabel("Fecha o código:"));
        controles.add(txtBusqueda);
        controles.add(btnBuscar);

        add(controles, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        comboTipo.addActionListener(e -> actualizarEtiqueta());
        btnBuscar.addActionListener(e -> generarReporte());

        actualizarEtiqueta();
    }

    private void actualizarEtiqueta() {
        String tipo = (String) comboTipo.getSelectedItem();

        if ("Citas por fecha".equals(tipo)) {
            txtBusqueda.setToolTipText("AAAA-MM-DD, ejemplo: 2026-10-15");
        } else if ("Citas por doctor".equals(tipo)) {
            txtBusqueda.setToolTipText("Escribe el código del doctor");
        } else {
            txtBusqueda.setToolTipText("Escribe el código del paciente");
        }

        txtBusqueda.setText("");
        modelo.setRowCount(0);
    }

    private void generarReporte() {
        String tipo = (String) comboTipo.getSelectedItem();
        String dato = txtBusqueda.getText().trim();

        if (dato.isEmpty()) {
            mostrarError("Ingresa la fecha o el código solicitado.");
            return;
        }

        try {
            List<Cita> citas;

            if ("Citas por fecha".equals(tipo)) {
                LocalDate fecha = LocalDate.parse(dato);
                citas = citaDAO.listarPorFecha(fecha);

            } else if ("Citas por doctor".equals(tipo)) {
                citas = citaDAO.listarPorDoctor(dato);

            } else {
                citas = citaDAO.listarPorPaciente(dato);
            }

            modelo.setRowCount(0);

            for (Cita cita : citas) {
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

            if (citas.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se encontraron citas para el criterio indicado.",
                        "Reporte",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Reporte generado correctamente. Registros: "
                                + citas.size(),
                        "Reporte",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (Exception ex) {
            mostrarError(
                    "No se pudo generar el reporte.\n"
                    + "Verifica la fecha o el código ingresado.\n\n"
                    + ex.getMessage()
            );
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Reportes",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
