
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class GestionDoctores extends JPanel {

    private final List<Doctor> doctores;

    private final DefaultTableModel modelo = UtilVentanas.modelo(
            "Código", "Nombre", "Especialidad",
            "Teléfono", "Correo", "Estado");

    private final JTable tabla = new JTable(modelo);
    private final DoctorDAO doctorDAO = new DoctorDAO();

    public GestionDoctores(List<Doctor> doctores) {
        this.doctores = doctores;

        setLayout(new BorderLayout(8, 8));
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton registrar = new JButton("Registrar");
        JButton editar = new JButton("Editar");
        JButton estado = new JButton("Activar / Inactivar");
        JButton consultar = new JButton("Consultar");

        acciones.add(registrar);
        acciones.add(editar);
        acciones.add(estado);
        acciones.add(consultar);

        add(acciones, BorderLayout.SOUTH);

        registrar.addActionListener(e -> registrar());
        editar.addActionListener(e -> editar());
        estado.addActionListener(e -> cambiarEstado());
        consultar.addActionListener(e -> consultar());

        refrescar();
    }

    private void registrar() {
        try {
            String codigo = Validaciones.textoObligatorio(
                    UtilVentanas.pedir(
                            this, "Código de doctor:", ""),
                    "código");

            if (doctorDAO.existe(codigo)) {
                throw new IllegalArgumentException(
                        "Ya existe un doctor con ese código.");
            }

            String nombre = Validaciones.textoObligatorio(
                    UtilVentanas.pedir(
                            this, "Nombre completo:", ""),
                    "nombre");

            Especialidad especialidad =
                    (Especialidad) JOptionPane.showInputDialog(
                            this,
                            "Selecciona la especialidad:",
                            "Especialidad",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            Especialidad.values(),
                            Especialidad.MEDICINA_GENERAL);

            if (especialidad == null) {
                return;
            }

            String telefono = Validaciones.telefono(
                    UtilVentanas.pedir(
                            this,
                            "Teléfono (8 dígitos, sin guion):",
                            ""));

            String correo = Validaciones.correo(
                    UtilVentanas.pedir(
                            this, "Correo electrónico:", ""));

            Doctor nuevo = new Doctor(
                    codigo, nombre, especialidad, telefono, correo);

            doctorDAO.insertar(nuevo);
            refrescar();

            UtilVentanas.estado(
                    this,
                    "Doctor registrado correctamente en la base de datos.");

        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
        }
    }

    private void editar() {
        Doctor seleccionado = seleccionado();

        if (seleccionado == null) {
            return;
        }

        try {
            Doctor d = doctorDAO.buscarPorCodigo(
                    seleccionado.getCodigoDoctor());

            String nombre = Validaciones.textoObligatorio(
                    UtilVentanas.pedir(
                            this,
                            "Nombre completo:",
                            d.getNombreCompleto()),
                    "nombre");

            Especialidad especialidad =
                    (Especialidad) JOptionPane.showInputDialog(
                            this,
                            "Selecciona la especialidad:",
                            "Especialidad",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            Especialidad.values(),
                            d.getEspecialidad());

            if (especialidad == null) {
                return;
            }

            String telefono = Validaciones.telefono(
                    UtilVentanas.pedir(
                            this,
                            "Teléfono (8 dígitos):",
                            d.getTelefono()));

            String correo = Validaciones.correo(
                    UtilVentanas.pedir(
                            this,
                            "Correo electrónico:",
                            d.getCorreo()));

            d.setNombreCompleto(nombre);
            d.setEspecialidad(especialidad);
            d.setTelefono(telefono);
            d.setCorreo(correo);

            doctorDAO.actualizar(d);
            refrescar();

            UtilVentanas.estado(
                    this,
                    "Doctor actualizado correctamente en la base de datos.");

        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
        }
    }

    private void cambiarEstado() {
        Doctor seleccionado = seleccionado();

        if (seleccionado == null) {
            return;
        }

        try {
            Doctor d = doctorDAO.buscarPorCodigo(
                    seleccionado.getCodigoDoctor());

            EstadoRegistro nuevo = d.estaActivo()
                    ? EstadoRegistro.INACTIVO
                    : EstadoRegistro.ACTIVO;

            if (UtilVentanas.confirmar(
                    this,
                    "¿Cambiar el estado de "
                            + d.getNombreCompleto()
                            + " a " + nuevo + "?")) {

                doctorDAO.cambiarEstado(
                        d.getCodigoDoctor(), nuevo);

                refrescar();

                UtilVentanas.estado(
                        this,
                        "Estado actualizado correctamente en la base de datos.");
            }

        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
        }
    }

    private void consultar() {
        String codigo = JOptionPane.showInputDialog(
                this,
                "Código del doctor que deseas consultar:");

        if (codigo == null || codigo.trim().isEmpty()) {
            return;
        }

        try {
            Doctor d = doctorDAO.buscarPorCodigo(codigo.trim());

            UtilVentanas.estado(this, d.mostrarDatos());

        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
        }
    }

    private Doctor seleccionado() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            UtilVentanas.estado(
                    this,
                    "Selecciona un doctor de la tabla primero.");
            return null;
        }

        String codigo = modelo.getValueAt(
                tabla.convertRowIndexToModel(fila), 0).toString();

        for (Doctor d : doctores) {
            if (d.getCodigoDoctor().equalsIgnoreCase(codigo)) {
                return d;
            }
        }

        try {
            return doctorDAO.buscarPorCodigo(codigo);
        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
            return null;
        }
    }

    private void refrescar() {
        modelo.setRowCount(0);

        try {
            List<Doctor> lista = doctorDAO.listar();

            doctores.clear();
            doctores.addAll(lista);

            for (Doctor d : lista) {
                modelo.addRow(new Object[]{
                    d.getCodigoDoctor(),
                    d.getNombreCompleto(),
                    d.getEspecialidad(),
                    d.getTelefono(),
                    d.getCorreo(),
                    d.getEstadoDoctor()
                });
            }

        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
        }
    }
}
