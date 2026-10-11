
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Utilidades visuales compartidas por las ventanas de gestión. */
final class UtilVentanas {
    private UtilVentanas() {}

    static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    static void error(Component padre, Exception ex) {
        JOptionPane.showMessageDialog(padre, ex.getMessage(), "Revisa los datos", JOptionPane.WARNING_MESSAGE);
    }

    static String pedir(Component padre, String etiqueta, String actual) {
        return JOptionPane.showInputDialog(padre, etiqueta, actual);
    }

    static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    static void estado(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Clínica Vida UDB", JOptionPane.INFORMATION_MESSAGE);
    }
}
