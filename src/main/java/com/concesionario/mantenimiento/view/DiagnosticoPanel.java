package com.concesionario.mantenimiento.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.Refrescable;
import com.concesionario.mantenimiento.controller.MantenimientoController;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.RepuestoOrden;
import com.concesionario.mantenimiento.model.ResultadoDiagnostico;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla del CU-MF-05 Diagnosticar y reparar vehículo.
 * El técnico elige una orden abierta, registra el diagnóstico y los repuestos, y al terminar
 * la reparación aprueba o rechaza el control de calidad.
 */
public class DiagnosticoPanel extends JPanel implements Refrescable {

    private final MantenimientoController controller;
    private final JComboBox<OrdenMantenimiento> cmbOrden = new JComboBox<>();
    private final JLabel lblDetalle = new JLabel(" ");
    private final JTextArea txtDiagnostico = new JTextArea(3, 40);
    private final JCheckBox chkSevero = new JCheckBox("Daño de alta severidad (la orden pasa a prioridad CRITICA)");
    private final JTextField txtHoras = new JTextField(8);
    private final JComboBox<Repuesto> cmbRepuesto = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(4);
    private final JTextField txtObservacion = new JTextField(30);
    private final DefaultTableModel modeloRequeridos = tablaSoloLectura("Código", "Repuesto", "Cantidad");
    private final DefaultTableModel modeloOrden = tablaSoloLectura("Código", "Repuesto", "Cantidad", "Precio", "Estado");
    private final DefaultTableModel modeloHistorial = tablaSoloLectura("Fecha", "Km", "Servicio realizado", "Costo");
    private final JTable tablaRequeridos = new JTable(modeloRequeridos);
    private final List<RepuestoOrden> requeridos = new ArrayList<>();
    private int filaActual = 0;

    public DiagnosticoPanel(MantenimientoController controller) {
        super(new BorderLayout(10, 10));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("CU-MF-05 · Diagnosticar y reparar vehículo");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        txtDiagnostico.setLineWrap(true);
        txtDiagnostico.setWrapStyleWord(true);

        JPanel repuesto = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        repuesto.add(cmbRepuesto);
        repuesto.add(new JLabel("Cantidad:"));
        repuesto.add(txtCantidad);
        repuesto.add(boton("Agregar", this::agregarRepuesto));
        repuesto.add(boton("Quitar", this::quitarRepuesto));

        JPanel campos = new JPanel(new GridBagLayout());
        agregarCampo(campos, "Orden de mantenimiento:", cmbOrden);
        agregarCampo(campos, "Detalle:", lblDetalle);
        agregarCampo(campos, "Diagnóstico (componentes y fallas):", new JScrollPane(txtDiagnostico));
        agregarCampo(campos, "", chkSevero);
        agregarCampo(campos, "Horas de mano de obra:", txtHoras);
        agregarCampo(campos, "Repuesto requerido:", repuesto);
        agregarCampo(campos, "Observación (calidad o baja):", txtObservacion);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(boton("Registrar diagnóstico", this::registrarDiagnostico));
        acciones.add(boton("Reintentar repuestos", this::reintentarRepuestos));
        acciones.add(boton("Aprobar control de calidad", this::aprobarCalidad));
        acciones.add(boton("Rechazar control de calidad", this::rechazarCalidad));
        acciones.add(boton("Declarar irreparable", this::declararIrreparable));

        JPanel norte = new JPanel(new BorderLayout(6, 6));
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(campos, BorderLayout.CENTER);
        norte.add(acciones, BorderLayout.SOUTH);

        JPanel tablas = new JPanel(new GridLayout(1, 3, 10, 0));
        tablas.add(conTitulo("Repuestos para el diagnóstico", new JScrollPane(tablaRequeridos)));
        tablas.add(conTitulo("Repuestos de la orden", new JScrollPane(new JTable(modeloOrden))));
        tablas.add(conTitulo("Historial del vehículo", new JScrollPane(new JTable(modeloHistorial))));

        add(norte, BorderLayout.NORTH);
        add(tablas, BorderLayout.CENTER);

        cmbOrden.addActionListener(e -> mostrarOrden());
        refrescar();
    }

    // ---------- Acciones ----------

    private void registrarDiagnostico() {
        ejecutar(() -> {
            OrdenMantenimiento orden = ordenSeleccionada();
            boolean sinRepuestos = requeridos.isEmpty();
            ResultadoDiagnostico r = controller.registrarDiagnostico(orden.getId(), txtDiagnostico.getText().trim(),
                    chkSevero.isSelected(), Campos.decimal(txtHoras, "Horas de mano de obra"), requeridos);
            String aviso = chkSevero.isSelected()
                    ? "\nLa orden quedó con prioridad CRITICA. Avise al coordinador de flota." : "";
            if (r.hayFaltantes()) {
                return "Faltan repuestos en el inventario:\n- " + String.join("\n- ", r.getFaltantes())
                        + "\nLa orden quedó PENDIENTE_REPUESTO y no se descontó nada del inventario." + aviso;
            }
            return (sinRepuestos
                    ? "Diagnóstico registrado sin repuestos. La orden pasó a EN_PROCESO."
                    : "Diagnóstico registrado. Los repuestos salieron del inventario y la orden pasó a EN_PROCESO.") + aviso;
        });
    }

    private void reintentarRepuestos() {
        ejecutar(() -> {
            ResultadoDiagnostico r = controller.reintentarRepuestos(ordenSeleccionada().getId());
            return r.hayFaltantes()
                    ? "Todavía faltan repuestos:\n- " + String.join("\n- ", r.getFaltantes())
                    : "Ya hay inventario. Los repuestos salieron del inventario y la orden pasó a EN_PROCESO.";
        });
    }

    private void aprobarCalidad() {
        ejecutar(() -> {
            controller.aprobarControlCalidad(ordenSeleccionada().getId());
            return "Control de calidad aprobado. La orden quedó CERRADA, el vehículo DISPONIBLE "
                    + "y el servicio se agregó a su historial.";
        }, "No fue posible completar la aprobación del vehículo. No se realizaron cambios.");
    }

    private void rechazarCalidad() {
        ejecutar(() -> {
            controller.rechazarControlCalidad(ordenSeleccionada().getId(), txtObservacion.getText().trim());
            return "Control de calidad rechazado. La orden volvió a diagnóstico y el vehículo sigue en el taller.";
        });
    }

    private void declararIrreparable() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "El vehículo quedará NO_DISPONIBLE y la orden se cerrará. ¿Desea continuar?",
                "Declarar irreparable", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        ejecutar(() -> {
            controller.declararIrreparable(ordenSeleccionada().getId(), txtObservacion.getText().trim());
            return "La orden se cerró y el vehículo quedó NO_DISPONIBLE. Se requiere autorización para darlo de baja.";
        });
    }

    private void agregarRepuesto() {
        try {
            Repuesto r = Campos.seleccion(cmbRepuesto, "Repuesto requerido");
            int cantidad = Campos.entero(txtCantidad, "Cantidad");
            if (cantidad <= 0) {
                throw new ValidacionException("La cantidad debe ser mayor que cero.");
            }
            requeridos.removeIf(x -> x.getIdRepuesto() == r.getId());
            RepuestoOrden fila = new RepuestoOrden(r.getId(), cantidad);
            fila.setCodigo(r.getCodigo());
            fila.setNombreRepuesto(r.getNombre());
            requeridos.add(fila);
            mostrarRequeridos();
            txtCantidad.setText("1");
        } catch (ValidacionException ex) {
            advertencia(ex.getMessage());
        }
    }

    private void quitarRepuesto() {
        int fila = tablaRequeridos.getSelectedRow();
        if (fila < 0) {
            advertencia("Seleccione en la tabla el repuesto que quiere quitar.");
            return;
        }
        requeridos.remove(fila);
        mostrarRequeridos();
    }

    // ---------- Carga de datos ----------

    @Override
    public void refrescar() {
        try {
            Integer actual = cmbOrden.getSelectedIndex() >= 0 ? ordenSeleccionadaONull().getId() : null;
            cmbOrden.setModel(new DefaultComboBoxModel<>(controller.listarOrdenesActivas().toArray(new OrdenMantenimiento[0])));
            cmbRepuesto.setModel(new DefaultComboBoxModel<>(controller.listarRepuestos().toArray(new Repuesto[0])));
            cmbRepuesto.setSelectedIndex(-1);
            if (actual != null) {
                Campos.seleccionarPorId(cmbOrden, OrdenMantenimiento::getId, actual);
            } else {
                cmbOrden.setSelectedIndex(-1);
            }
            mostrarOrden();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void mostrarOrden() {
        OrdenMantenimiento o = ordenSeleccionadaONull();
        requeridos.clear();
        modeloOrden.setRowCount(0);
        modeloHistorial.setRowCount(0);
        txtObservacion.setText("");
        txtCantidad.setText("1");
        chkSevero.setSelected(false);
        if (o == null) {
            lblDetalle.setText("Seleccione una orden que no esté cerrada.");
            txtDiagnostico.setText("");
            txtHoras.setText("0");
            mostrarRequeridos();
            return;
        }
        lblDetalle.setText("Vehículo " + o.getPlaca() + " · Técnico " + o.getNombreTecnico() + " · Estado "
                + o.getEstado() + " · Prioridad " + o.getPrioridad() + " · Motivo: " + o.getMotivo()
                + (o.getObservacionCalidad() == null ? "" : " · Última observación: " + o.getObservacionCalidad()));
        txtDiagnostico.setText(Campos.texto(o.getDiagnostico()));
        txtHoras.setText(o.getHorasManoObra() == null ? "0" : o.getHorasManoObra().stripTrailingZeros().toPlainString());
        try {
            for (RepuestoOrden r : controller.listarRepuestosDeOrden(o.getId())) {
                modeloOrden.addRow(new Object[]{r.getCodigo(), r.getNombreRepuesto(), r.getCantidad(), r.getPrecioUnitario(),
                        r.isDescontado() ? "Tomado del inventario" : "Pendiente"});
                if (!r.isDescontado()) {
                    requeridos.add(r);
                }
            }
            for (RegistroHistorial h : controller.listarHistorialDeVehiculo(o.getIdVehiculo())) {
                modeloHistorial.addRow(new Object[]{h.getFecha(), h.getKilometraje(), h.getDescripcion(), h.getCosto()});
            }
        } catch (Exception ex) {
            error(ex);
        }
        mostrarRequeridos();
    }

    private void mostrarRequeridos() {
        modeloRequeridos.setRowCount(0);
        for (RepuestoOrden r : requeridos) {
            modeloRequeridos.addRow(new Object[]{r.getCodigo(), r.getNombreRepuesto(), r.getCantidad()});
        }
    }

    // ---------- Ayudas ----------

    @FunctionalInterface
    private interface Accion {
        String ejecutar() throws Exception;
    }

    private void ejecutar(Accion accion) {
        ejecutar(accion, null);
    }

    /** Ejecuta la acción; si la base de datos falla y hay {@code mensajeError}, muestra ese mensaje. */
    private void ejecutar(Accion accion, String mensajeError) {
        try {
            String mensaje = accion.ejecutar();
            refrescar();
            JOptionPane.showMessageDialog(this, mensaje, "Listo", JOptionPane.INFORMATION_MESSAGE);
        } catch (ValidacionException ex) {
            advertencia(ex.getMessage());
        } catch (Exception ex) {
            if (mensajeError == null) {
                error(ex);
            } else {
                JOptionPane.showMessageDialog(this, mensajeError + "\nDetalle: " + Campos.detalleError(ex),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private OrdenMantenimiento ordenSeleccionada() throws ValidacionException {
        return Campos.seleccion(cmbOrden, "Orden de mantenimiento");
    }

    private OrdenMantenimiento ordenSeleccionadaONull() {
        return cmbOrden.getItemAt(cmbOrden.getSelectedIndex());
    }

    private void advertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Revise los datos", JOptionPane.WARNING_MESSAGE);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this,
                "No fue posible completar la operación con la base de datos.\nDetalle: " + Campos.detalleError(ex),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void agregarCampo(JPanel panel, String etiqueta, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 10);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = filaActual;
        panel.add(new JLabel(etiqueta), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        panel.add(campo, g);
        filaActual++;
    }

    private static JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    private static JPanel conTitulo(String titulo, JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    private static DefaultTableModel tablaSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }
}
