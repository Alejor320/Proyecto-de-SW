package com.concesionario.mantenimiento.view;

import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.mantenimiento.controller.MantenimientoController;
import com.concesionario.mantenimiento.model.RegistroHistorial;

import java.time.LocalDate;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Pantalla del CU-MF-04 Gestionar historial de mantenimiento del vehículo.
 * Los registros de órdenes cerradas los crea el sistema; aquí también se registran servicios manuales.
 */
public class HistorialPanel extends FormularioCrud<RegistroHistorial> {

    private final MantenimientoController controller;
    private final JComboBox<Vehiculo> cmbVehiculo = new JComboBox<>();
    private final JTextField txtFecha = new JTextField(20);
    private final JTextField txtKilometraje = new JTextField(20);
    private final JTextField txtDescripcion = new JTextField(20);
    private final JTextField txtCosto = new JTextField(20);

    public HistorialPanel(MantenimientoController controller) {
        super("CU-MF-04 · Gestionar historial de mantenimiento del vehículo",
                "ID", "Placa", "Fecha", "Km", "Servicio realizado", "Costo", "Origen");
        this.controller = controller;
        agregarCampo("Vehículo:", cmbVehiculo);
        agregarCampo("Fecha del servicio (aaaa-mm-dd):", txtFecha);
        agregarCampo("Kilometraje:", txtKilometraje);
        agregarCampo("Servicio realizado:", txtDescripcion);
        agregarCampo("Costo:", txtCosto);
        refrescar();
    }

    @Override
    protected void actualizarListas() throws Exception {
        cmbVehiculo.setModel(new DefaultComboBoxModel<>(controller.listarVehiculos().toArray(new Vehiculo[0])));
    }

    @Override
    protected List<RegistroHistorial> cargarDatos() throws Exception {
        return controller.listarHistorial();
    }

    @Override
    protected Object[] fila(RegistroHistorial h) {
        return new Object[]{h.getId(), h.getPlaca(), h.getFecha(), h.getKilometraje(), h.getDescripcion(),
                h.getCosto(), h.esAutomatico() ? "Orden N.º " + h.getIdOrden() : "Manual"};
    }

    @Override
    protected RegistroHistorial leerFormulario() throws ValidacionException {
        RegistroHistorial h = new RegistroHistorial();
        h.setId(seleccionado == null ? 0 : seleccionado.getId());
        h.setIdVehiculo(Campos.seleccion(cmbVehiculo, "Vehículo").getId());
        h.setFecha(Campos.fecha(txtFecha, "fecha del servicio", true));
        h.setKilometraje(Campos.entero(txtKilometraje, "Kilometraje"));
        h.setDescripcion(Campos.texto(txtDescripcion));
        h.setCosto(Campos.decimal(txtCosto, "Costo"));
        return h;
    }

    @Override
    protected void mostrarEnFormulario(RegistroHistorial h) {
        Campos.seleccionarPorId(cmbVehiculo, Vehiculo::getId, h.getIdVehiculo());
        txtFecha.setText(Campos.texto(h.getFecha()));
        txtKilometraje.setText(String.valueOf(h.getKilometraje()));
        txtDescripcion.setText(h.getDescripcion());
        txtCosto.setText(h.getCosto().toPlainString());
    }

    @Override
    protected void limpiarFormulario() {
        cmbVehiculo.setSelectedIndex(-1);
        txtFecha.setText(LocalDate.now().toString());
        txtKilometraje.setText("");
        txtDescripcion.setText("");
        txtCosto.setText("0");
    }

    @Override
    protected void guardar(RegistroHistorial h) throws Exception {
        controller.guardarRegistroHistorial(h);
    }

    @Override
    protected void eliminar(RegistroHistorial h) throws Exception {
        controller.eliminarRegistroHistorial(h.getId());
    }
}
