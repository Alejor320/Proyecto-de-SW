package com.concesionario.mantenimiento.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.controller.MantenimientoController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.apoyo.model.Vehiculo;

import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Pantalla del CU-MF-01 Gestionar órdenes de mantenimiento. */
public class OrdenMantenimientoPanel extends FormularioCrud<OrdenMantenimiento> {

    private final MantenimientoController controller;
    private final JComboBox<Vehiculo> cmbVehiculo = new JComboBox<>();
    private final JComboBox<Tecnico> cmbTecnico = new JComboBox<>();
    private final JTextField txtMotivo = new JTextField(20);
    private final JTextField txtKilometraje = new JTextField(20);
    private final JComboBox<Prioridad> cmbPrioridad = new JComboBox<>(Prioridad.values());
    private final JComboBox<EstadoOrden> cmbEstado = new JComboBox<>(EstadoOrden.values());

    public OrdenMantenimientoPanel(MantenimientoController controller) {
        super("CU-MF-01 · Gestionar órdenes de mantenimiento",
                "N.º", "Placa", "Técnico", "Apertura", "Cierre", "Motivo", "Km", "Prioridad", "Estado");
        this.controller = controller;
        agregarCampo("Vehículo:", cmbVehiculo);
        agregarCampo("Técnico:", cmbTecnico);
        agregarCampo("Motivo:", txtMotivo);
        agregarCampo("Kilometraje actual:", txtKilometraje);
        agregarCampo("Prioridad:", cmbPrioridad);
        agregarCampo("Estado:", cmbEstado);
        refrescar();
    }

    @Override
    protected void actualizarListas() throws Exception {
        cmbVehiculo.setModel(new DefaultComboBoxModel<>(controller.listarVehiculos().toArray(new Vehiculo[0])));
        cmbTecnico.setModel(new DefaultComboBoxModel<>(controller.listarTecnicos().toArray(new Tecnico[0])));
    }

    @Override
    protected List<OrdenMantenimiento> cargarDatos() throws Exception {
        return controller.listarOrdenes();
    }

    @Override
    protected Object[] fila(OrdenMantenimiento o) {
        return new Object[]{o.getId(), o.getPlaca(), o.getNombreTecnico(), o.getFechaApertura(),
                Campos.texto(o.getFechaCierre()), o.getMotivo(), o.getKilometraje(), o.getPrioridad(), o.getEstado()};
    }

    @Override
    protected OrdenMantenimiento leerFormulario() throws ValidacionException {
        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setId(seleccionado == null ? 0 : seleccionado.getId());
        if (seleccionado == null) {
            o.setIdVehiculo(Campos.seleccion(cmbVehiculo, "Vehículo").getId());
        }
        o.setIdTecnico(Campos.seleccion(cmbTecnico, "Técnico").getId());
        o.setMotivo(Campos.texto(txtMotivo));
        o.setKilometraje(Campos.entero(txtKilometraje, "Kilometraje actual"));
        o.setPrioridad(Campos.seleccion(cmbPrioridad, "Prioridad"));
        o.setEstado(Campos.seleccion(cmbEstado, "Estado"));
        return o;
    }

    @Override
    protected void mostrarEnFormulario(OrdenMantenimiento o) {
        Campos.seleccionarPorId(cmbVehiculo, Vehiculo::getId, o.getIdVehiculo());
        cmbVehiculo.setEnabled(false); // el vehículo no cambia después de abrir la orden
        Campos.seleccionarPorId(cmbTecnico, Tecnico::getId, o.getIdTecnico());
        txtMotivo.setText(o.getMotivo());
        txtKilometraje.setText(String.valueOf(o.getKilometraje()));
        cmbPrioridad.setSelectedItem(o.getPrioridad());
        cmbEstado.setSelectedItem(o.getEstado());
        cmbEstado.setEnabled(true);
    }

    @Override
    protected void limpiarFormulario() {
        cmbVehiculo.setEnabled(true);
        cmbVehiculo.setSelectedIndex(-1);
        cmbTecnico.setSelectedIndex(-1);
        txtMotivo.setText("");
        txtKilometraje.setText("");
        cmbPrioridad.setSelectedItem(Prioridad.MEDIA);
        cmbEstado.setSelectedItem(EstadoOrden.ABIERTA);
        cmbEstado.setEnabled(false); // toda orden nueva empieza ABIERTA
    }

    @Override
    protected void guardar(OrdenMantenimiento o) throws Exception {
        controller.guardarOrden(o);
    }

    @Override
    protected void eliminar(OrdenMantenimiento o) throws Exception {
        controller.eliminarOrden(o.getId());
    }
}
