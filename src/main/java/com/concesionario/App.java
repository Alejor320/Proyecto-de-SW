package com.concesionario;

import com.concesionario.comun.view.VentanaPrincipal;
import com.concesionario.config.InicializadorBD;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada de la aplicación de escritorio. */
public class App {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // Si no se puede usar el aspecto del sistema, Swing usa el suyo por defecto.
        }

        try {
            InicializadorBD.inicializar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "No fue posible preparar la base de datos.\nDetalle: " + ex.getMessage(),
                    "Error al iniciar", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
