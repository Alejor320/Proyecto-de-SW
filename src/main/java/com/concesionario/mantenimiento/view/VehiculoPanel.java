package com.concesionario.mantenimiento.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.controller.MantenimientoController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.apoyo.model.TipoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;

import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Pantalla del CU-AP-01 Registrar y consultar vehículos (versión mínima, de apoyo).
 * Solo permite registrar y consultar; el estado lo cambian las órdenes de mantenimiento.
 */
public class VehiculoPanel extends FormularioCrud<Vehiculo> {

    private final MantenimientoController controller;
    private final JTextField txtPlaca = new JTextField(20);
    private final JTextField txtMarca = new JTextField(20);
    private final JTextField txtModelo = new JTextField(20);
    private final JComboBox<TipoVehiculo> cmbTipo = new JComboBox<>(TipoVehiculo.values());
    private final JTextField txtAnio = new JTextField(20);

    public VehiculoPanel(MantenimientoController controller) {
        super("CU-AP-01 · Registrar y consultar vehículos (apoyo para Mantenimiento)",
                "ID", "Placa", "Marca", "Modelo", "Tipo", "Año", "Estado");
        this.controller = controller;
        agregarCampo("Placa:", txtPlaca);
        agregarCampo("Marca:", txtMarca);
        agregarCampo("Modelo:", txtModelo);
        agregarCampo("Tipo:", cmbTipo);
        agregarCampo("Año:", txtAnio);
        ocultarEliminar();
        refrescar();
    }

    @Override
    protected List<Vehiculo> cargarDatos() throws Exception {
        return controller.listarVehiculos();
    }

    @Override
    protected Object[] fila(Vehiculo v) {
        return new Object[]{v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getTipo(), v.getAnio(), v.getEstado()};
    }

    @Override
    protected Vehiculo leerFormulario() throws ValidacionException {
        Vehiculo v = new Vehiculo();
        v.setId(seleccionado == null ? 0 : seleccionado.getId());
        v.setPlaca(Campos.texto(txtPlaca));
        v.setMarca(Campos.texto(txtMarca));
        v.setModelo(Campos.texto(txtModelo));
        v.setTipo(Campos.seleccion(cmbTipo, "Tipo"));
        v.setAnio(Campos.entero(txtAnio, "Año"));
        return v;
    }

    @Override
    protected void mostrarEnFormulario(Vehiculo v) {
        txtPlaca.setText(v.getPlaca());
        txtMarca.setText(v.getMarca());
        txtModelo.setText(v.getModelo());
        cmbTipo.setSelectedItem(v.getTipo());
        txtAnio.setText(String.valueOf(v.getAnio()));
    }

    @Override
    protected void limpiarFormulario() {
        txtPlaca.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        cmbTipo.setSelectedItem(TipoVehiculo.AUTOMOVIL);
        txtAnio.setText("");
    }

    @Override
    protected void guardar(Vehiculo v) throws Exception {
        controller.registrarVehiculo(v);
    }

    @Override
    protected void eliminar(Vehiculo v) throws Exception {
        throw new ValidacionException("El registro mínimo de vehículos no permite eliminar.");
    }
}
