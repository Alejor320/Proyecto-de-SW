package com.concesionario.rrhh.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.controller.RecursosHumanosController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.TipoContrato;

import java.time.LocalDate;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Pantalla del CU-RH-03 Gestionar contratos laborales. */
public class ContratoLaboralPanel extends FormularioCrud<ContratoLaboral> {

    private final RecursosHumanosController controller;
    private final JComboBox<Empleado> cmbEmpleado = new JComboBox<>();
    private final JComboBox<TipoContrato> cmbTipo = new JComboBox<>(TipoContrato.values());
    private final JTextField txtFechaInicio = new JTextField(20);
    private final JTextField txtFechaFin = new JTextField(20);
    private final JTextField txtSalario = new JTextField(20);
    private final JComboBox<EstadoContrato> cmbEstado = new JComboBox<>(EstadoContrato.values());

    public ContratoLaboralPanel(RecursosHumanosController controller) {
        super("CU-RH-03 · Gestionar contratos laborales",
                "ID", "Empleado", "Tipo", "Inicio", "Fin", "Salario", "Estado");
        this.controller = controller;
        agregarCampo("Empleado:", cmbEmpleado);
        agregarCampo("Tipo de contrato:", cmbTipo);
        agregarCampo("Fecha de inicio (aaaa-mm-dd):", txtFechaInicio);
        agregarCampo("Fecha de fin (vacía si es indefinido):", txtFechaFin);
        agregarCampo("Salario mensual:", txtSalario);
        agregarCampo("Estado:", cmbEstado);
        refrescar();
    }

    @Override
    protected void actualizarListas() throws Exception {
        cmbEmpleado.setModel(new DefaultComboBoxModel<>(controller.listarEmpleados().toArray(new Empleado[0])));
    }

    @Override
    protected List<ContratoLaboral> cargarDatos() throws Exception {
        return controller.listarContratos();
    }

    @Override
    protected Object[] fila(ContratoLaboral k) {
        return new Object[]{k.getId(), k.getNombreEmpleado(), k.getTipo(), k.getFechaInicio(),
                k.getFechaFin() == null ? "Indefinido" : k.getFechaFin(), k.getSalario(), k.getEstado()};
    }

    @Override
    protected ContratoLaboral leerFormulario() throws ValidacionException {
        ContratoLaboral k = new ContratoLaboral();
        k.setId(seleccionado == null ? 0 : seleccionado.getId());
        k.setIdEmpleado(Campos.seleccion(cmbEmpleado, "Empleado").getId());
        k.setTipo(Campos.seleccion(cmbTipo, "Tipo de contrato"));
        k.setFechaInicio(Campos.fecha(txtFechaInicio, "fecha de inicio", true));
        k.setFechaFin(Campos.fecha(txtFechaFin, "fecha de fin", false));
        k.setSalario(Campos.decimal(txtSalario, "Salario"));
        k.setEstado(Campos.seleccion(cmbEstado, "Estado"));
        return k;
    }

    @Override
    protected void mostrarEnFormulario(ContratoLaboral k) {
        Campos.seleccionarPorId(cmbEmpleado, Empleado::getId, k.getIdEmpleado());
        cmbTipo.setSelectedItem(k.getTipo());
        txtFechaInicio.setText(Campos.texto(k.getFechaInicio()));
        txtFechaFin.setText(Campos.texto(k.getFechaFin()));
        txtSalario.setText(k.getSalario().toPlainString());
        cmbEstado.setSelectedItem(k.getEstado());
    }

    @Override
    protected void limpiarFormulario() {
        cmbEmpleado.setSelectedIndex(-1);
        cmbTipo.setSelectedItem(TipoContrato.INDEFINIDO);
        txtFechaInicio.setText(LocalDate.now().toString());
        txtFechaFin.setText("");
        txtSalario.setText("");
        cmbEstado.setSelectedItem(EstadoContrato.VIGENTE);
    }

    @Override
    protected void guardar(ContratoLaboral k) throws Exception {
        controller.guardarContrato(k);
    }

    @Override
    protected void eliminar(ContratoLaboral k) throws Exception {
        controller.eliminarContrato(k.getId());
    }
}
