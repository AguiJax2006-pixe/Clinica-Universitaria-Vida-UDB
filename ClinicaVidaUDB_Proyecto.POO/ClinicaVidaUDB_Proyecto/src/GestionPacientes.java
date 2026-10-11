
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel; 

/**
 * Vista y lógica básica de pacientes.
 * Esta versión guarda datos en memoria; aún debe conectarse al DAO/JDBC del equipo.
 */
public class GestionPacientes extends JPanel {
    private final List<Paciente> pacientes;
    private final DefaultTableModel modelo = UtilVentanas.modelo(
            "Código", "Nombre", "Edad", "Teléfono", "Correo", "Tipo", "Estado");
    private final JTable tabla = new JTable(modelo);
    private final PacienteDAO pacienteDAO = new PacienteDAO();

    public GestionPacientes(List<Paciente> pacientes) {
        this.pacientes = pacientes;
        setLayout(new BorderLayout(8, 8));
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton registrar = new JButton("Registrar");
        JButton editar = new JButton("Editar");
        JButton estado = new JButton("Activar / Inactivar");
        JButton consultar = new JButton("Consultar");
        acciones.add(registrar); acciones.add(editar); acciones.add(estado); acciones.add(consultar);
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
                    UtilVentanas.pedir(this, "Código de paciente:", ""), "código");
            if (buscar(codigo) != null) throw new IllegalArgumentException("Ya existe un paciente con ese código.");
            String nombre = Validaciones.textoObligatorio(
                    UtilVentanas.pedir(this, "Nombre completo:", ""), "nombre");
            int edad = Validaciones.edad(UtilVentanas.pedir(this, "Edad (0 a 120):", ""));
            String telefono = Validaciones.telefono(UtilVentanas.pedir(this, "Teléfono (8 dígitos, sin guion):", ""));
            String correo = Validaciones.correo(UtilVentanas.pedir(this, "Correo electrónico:", ""));
            TipoPaciente tipo = (TipoPaciente) JOptionPane.showInputDialog(this, "Tipo de paciente:",
                    "Tipo", JOptionPane.QUESTION_MESSAGE, null, TipoPaciente.values(), TipoPaciente.ESTUDIANTE);
            if (tipo == null) return;
            Paciente nuevo = new Paciente(
                    codigo, nombre, edad, telefono, correo, tipo
            );

            pacienteDAO.insertar(nuevo);
            refrescar();

            UtilVentanas.estado(
                    this, "Paciente registrado correctamente en la base de datos."
         );
        } catch (Exception ex) { UtilVentanas.error(this, ex); }
    }

    private void editar() {
        Paciente p = seleccionado();
        if (p == null) return;
        try {
            String nombre = Validaciones.textoObligatorio(
                    UtilVentanas.pedir(this, "Nombre completo:", p.getNombreCompleto()), "nombre");
            int edad = Validaciones.edad(UtilVentanas.pedir(this, "Edad:", String.valueOf(p.getEdad())));
            String telefono = Validaciones.telefono(UtilVentanas.pedir(this, "Teléfono (8 dígitos):", p.getTelefono()));
            String correo = Validaciones.correo(UtilVentanas.pedir(this, "Correo:", p.getCorreo()));
            TipoPaciente tipo = (TipoPaciente) JOptionPane.showInputDialog(this, "Tipo de paciente:",
                    "Tipo", JOptionPane.QUESTION_MESSAGE, null, TipoPaciente.values(), p.getTipoPaciente());
            if (tipo == null) return;
            p.setNombreCompleto(nombre);
            p.setEdad(edad);
            p.setTelefono(telefono);
            p.setCorreo(correo);
            p.setTipoPaciente(tipo);

            pacienteDAO.actualizar(p);
            refrescar();

            UtilVentanas.estado(
                    this, "Paciente actualizado correctamente en la base de datos."
        );
        } catch (Exception ex) { UtilVentanas.error(this, ex); }
    }

    
    private void cambiarEstado() {
    Paciente p = seleccionado();
    if (p == null) return;

    EstadoRegistro nuevo = p.estaActivo()
            ? EstadoRegistro.INACTIVO
            : EstadoRegistro.ACTIVO;

    if (UtilVentanas.confirmar(this,
            "¿Cambiar el estado de " + p.getNombreCompleto()
            + " a " + nuevo + "?")) {
        try {
            pacienteDAO.cambiarEstado(p.getCodigoPaciente(), nuevo);
            refrescar();
            UtilVentanas.estado(this,
                    "Estado actualizado correctamente en la base de datos.");
        } catch (Exception ex) {
            UtilVentanas.error(this, ex);
        }
    }
}


    
private void consultar() {
    String codigo = JOptionPane.showInputDialog(
            this,
            "Código del paciente que deseas consultar:");

    if (codigo == null || codigo.trim().isEmpty()) {
        return;
    }

    try {
        Paciente p = pacienteDAO.buscarPorCodigo(codigo.trim());

        if (p == null) {
            UtilVentanas.estado(
                    this,
                    "No se encontró un paciente con ese código.");
        } else {
            UtilVentanas.estado(this, p.mostrarDatos());
        }

    } catch (Exception ex) {
        UtilVentanas.error(this, ex);
    }
}


    private Paciente seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { UtilVentanas.estado(this, "Selecciona un paciente de la tabla primero."); return null; }
        return buscar(modelo.getValueAt(fila, 0).toString());
    }

    private Paciente buscar(String codigo) {
        for (Paciente p : pacientes) if (p.getCodigoPaciente().equalsIgnoreCase(codigo)) return p;
        return null;
    }

    
private void refrescar() {
    modelo.setRowCount(0);

    try {
        List<Paciente> lista = pacienteDAO.listar();

        pacientes.clear();
        pacientes.addAll(lista);

        for (Paciente p : pacientes) {
            modelo.addRow(new Object[]{
                p.getCodigoPaciente(),
                p.getNombreCompleto(),
                p.getEdad(),
                p.getTelefono(),
                p.getCorreo(),
                p.getTipoPaciente(),
                p.getEstadoPaciente()
            });
        }
    } catch (Exception ex) {
        UtilVentanas.error(this, ex);
    }
}
}
