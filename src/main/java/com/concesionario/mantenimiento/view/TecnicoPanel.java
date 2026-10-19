package com.concesionario.mantenimiento.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.controller.MantenimientoController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.rrhh.model.Empleado;

import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Pantalla del CU-MF-03 Gestionar técnicos de mantenimiento. */
public class TecnicoPanel extends FormularioCrud<Tecnico> {

    private final MantenimientoController controller;
    private final JComboBox<Empleado> cmbEmpleado = new JComboBox<>();
    private final JTextField txtEspecialidad = new JTextField(20);
    private final JCheckBox chkDisponible = new JCheckBox("Disponible para recibir órdenes");

    public TecnicoPanel(MantenimientoController controller) {
        super("CU-MF-03 · Gestionar técnicos de mantenimiento",
                "ID", "Técnico", "Especialidad", "Disponible");
        this.controller = controller;
        agregarCampo("Empleado (de Recursos Humanos):", cmbEmpleado);
        agregarCampo("Especialidad:", txtEspecialidad);
        agregarCampo("", chkDisponible);
        refrescar();
    }

    @Override
    protected void actualizarListas() throws Exception {
        cmbEmpleado.setModel(new DefaultComboBoxModel<>(controller.listarEmpleados().toArray(new Empleado[0])));
    }

    @Override
    protected List<Tecnico> cargarDatos() throws Exception {
        return controller.listarTecnicos();
    }

    @Override
    protected Object[] fila(Tecnico t) {
        return new Object[]{t.getId(), t.getNombreEmpleado(), t.getEspecialidad(), t.isDisponible() ? "Sí" : "No"};
    }

    @Override
    protected Tecnico leerFormulario() throws ValidacionException {
        Tecnico t = new Tecnico();
        t.setId(seleccionado == null ? 0 : seleccionado.getId());
        t.setIdEmpleado(Campos.seleccion(cmbEmpleado, "Empleado").getId());
        t.setEspecialidad(Campos.texto(txtEspecialidad));
        t.setDisponible(chkDisponible.isSelected());
        return t;
    }

    @Override
    protected void mostrarEnFormulario(Tecnico t) {
        Campos.seleccionarPorId(cmbEmpleado, Empleado::getId, t.getIdEmpleado());
        txtEspecialidad.setText(t.getEspecialidad());
        chkDisponible.setSelected(t.isDisponible());
    }

    @Override
    protected void limpiarFormulario() {
        cmbEmpleado.setSelectedIndex(-1);
        txtEspecialidad.setText("");
        chkDisponible.setSelected(true);
    }

    @Override
    protected void guardar(Tecnico t) throws Exception {
        controller.guardarTecnico(t);
    }

    @Override
    protected void eliminar(Tecnico t) throws Exception {
        controller.eliminarTecnico(t.getId());
    }
}
