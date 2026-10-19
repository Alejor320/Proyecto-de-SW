package com.concesionario.comun.view;

import com.concesionario.comun.ValidacionException;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla base para los casos de uso CRUD.
 * Arriba va el formulario, luego los botones y abajo la tabla con los registros.
 * Cada caso de uso solo define sus campos y qué hacer al guardar o eliminar,
 * así todas las pantallas se ven y se comportan igual.
 */
public abstract class FormularioCrud<T> extends JPanel {

    private final JPanel panelCampos = new JPanel(new GridBagLayout());
    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JButton btnEliminar = new JButton("Eliminar");
    private final List<T> datos = new ArrayList<>();
    private int filaActual = 0;

    /** Registro elegido en la tabla, o null si se está creando uno nuevo. */
    protected T seleccionado;

    protected FormularioCrud(String titulo, String... columnas) {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int fila = tabla.getSelectedRow();
            if (fila >= 0 && fila < datos.size()) {
                seleccionado = datos.get(fila);
                mostrarEnFormulario(seleccionado);
            }
        });

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 16f));

        JButton btnNuevo = new JButton("Nuevo");
        JButton btnGuardar = new JButton("Guardar");
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> accionGuardar());
        btnEliminar.addActionListener(e -> accionEliminar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnEliminar);

        JPanel norte = new JPanel(new BorderLayout(6, 6));
        norte.add(lblTitulo, BorderLayout.NORTH);
        norte.add(panelCampos, BorderLayout.CENTER);
        norte.add(botones, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    // ---------- Lo que cada caso de uso debe definir ----------

    protected abstract List<T> cargarDatos() throws Exception;

    protected abstract Object[] fila(T registro);

    protected abstract T leerFormulario() throws ValidacionException;

    protected abstract void mostrarEnFormulario(T registro);

    protected abstract void limpiarFormulario();

    protected abstract void guardar(T registro) throws Exception;

    protected abstract void eliminar(T registro) throws Exception;

    /** Recarga las listas desplegables (empleados, vehículos, etc.). Opcional. */
    protected void actualizarListas() throws Exception {
    }

    // ---------- Ayudas para las subclases ----------

    protected void agregarCampo(String etiqueta, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 10);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = filaActual;
        panelCampos.add(new JLabel(etiqueta), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        panelCampos.add(campo, g);
        filaActual++;
    }

    protected void ocultarEliminar() {
        btnEliminar.setVisible(false);
    }

    /** Vuelve a leer los datos de la base y actualiza la tabla. */
    public void refrescar() {
        try {
            actualizarListas();
            List<T> nuevos = cargarDatos();
            datos.clear();
            datos.addAll(nuevos);
            modeloTabla.setRowCount(0);
            for (T registro : datos) {
                modeloTabla.addRow(fila(registro));
            }
            nuevo();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void nuevo() {
        seleccionado = null;
        tabla.clearSelection();
        limpiarFormulario();
    }

    private void accionGuardar() {
        try {
            T registro = leerFormulario();
            guardar(registro);
            refrescar();
            JOptionPane.showMessageDialog(this, "El registro se guardó correctamente.",
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (ValidacionException ex) {
            mostrarAdvertencia(ex.getMessage());
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void accionEliminar() {
        if (seleccionado == null) {
            mostrarAdvertencia("Seleccione un registro de la tabla para eliminarlo.");
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this, "¿Seguro que desea eliminar el registro seleccionado?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            eliminar(seleccionado);
            refrescar();
        } catch (ValidacionException ex) {
            mostrarAdvertencia(ex.getMessage());
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Revise los datos", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this,
                "No fue posible completar la operación con la base de datos.\nDetalle: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
