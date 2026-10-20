package com.concesionario.rrhh.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.rrhh.controller.RecursosHumanosController;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoPermiso;
import com.concesionario.rrhh.model.PermisoLaboral;

import java.time.LocalDate;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Pantalla del CU-RH-04 Gestionar permisos laborales. */
public class PermisoLaboralPanel extends FormularioCrud<PermisoLaboral> {

    private final RecursosHumanosController controller;
    private final JComboBox<Empleado> cmbEmpleado = new JComboBox<>();
    private final JTextField txtFechaInicio = new JTextField(20);
    private final JTextField txtFechaFin = new JTextField(20);
    private final JTextField txtMotivo = new JTextField(20);
    private final JTextField txtSoporte = new JTextField(20);
    private final JComboBox<EstadoPermiso> cmbEstado = new JComboBox<>(EstadoPermiso.values());

    public PermisoLaboralPanel(RecursosHumanosController controller) {
        super("CU-RH-04 · Gestionar permisos laborales",
                "ID", "Empleado", "Inicio", "Fin", "Motivo", "Soporte", "Estado");
        this.controller = controller;
        agregarCampo("Empleado:", cmbEmpleado);
        agregarCampo("Fecha de inicio (aaaa-mm-dd):", txtFechaInicio);
        agregarCampo("Fecha de fin (aaaa-mm-dd):", txtFechaFin);
        agregarCampo("Motivo:", txtMotivo);
        agregarCampo("Soporte (ruta, enlace o referencia):", txtSoporte);
        agregarCampo("Estado:", cmbEstado);
        refrescar();
    }

    @Override
    protected void actualizarListas() throws Exception {
        cmbEmpleado.setModel(new DefaultComboBoxModel<>(controller.listarEmpleados().toArray(new Empleado[0])));
    }

    @Override
    protected List<PermisoLaboral> cargarDatos() throws Exception {
        return controller.listarPermisos();
    }

    @Override
    protected Object[] fila(PermisoLaboral p) {
        return new Object[]{p.getId(), p.getNombreEmpleado(), p.getFechaInicio(), p.getFechaFin(),
                p.getMotivo(), Campos.texto(p.getSoporte()), p.getEstado()};
    }

    @Override
    protected PermisoLaboral leerFormulario() throws ValidacionException {
        PermisoLaboral p = new PermisoLaboral();
        p.setId(seleccionado == null ? 0 : seleccionado.getId());
        p.setIdEmpleado(Campos.seleccion(cmbEmpleado, "Empleado").getId());
        p.setFechaInicio(Campos.fecha(txtFechaInicio, "fecha de inicio", true));
        p.setFechaFin(Campos.fecha(txtFechaFin, "fecha de fin", true));
        p.setMotivo(Campos.texto(txtMotivo));
        p.setSoporte(Campos.textoOpcional(txtSoporte));
        p.setEstado(Campos.seleccion(cmbEstado, "Estado"));
        return p;
    }

    @Override
    protected void mostrarEnFormulario(PermisoLaboral p) {
        Campos.seleccionarPorId(cmbEmpleado, Empleado::getId, p.getIdEmpleado());
        cmbEmpleado.setEnabled(false);
        txtFechaInicio.setText(Campos.texto(p.getFechaInicio()));
        txtFechaFin.setText(Campos.texto(p.getFechaFin()));
        txtMotivo.setText(p.getMotivo());
        txtSoporte.setText(Campos.texto(p.getSoporte()));
        cmbEstado.setSelectedItem(p.getEstado());
        cmbEstado.setEnabled(true);
    }

    @Override
    protected void limpiarFormulario() {
        cmbEmpleado.setEnabled(true);
        cmbEmpleado.setSelectedIndex(-1);
        txtFechaInicio.setText(LocalDate.now().toString());
        txtFechaFin.setText(LocalDate.now().toString());
        txtMotivo.setText("");
        txtSoporte.setText("");
        cmbEstado.setSelectedItem(EstadoPermiso.PENDIENTE);
        cmbEstado.setEnabled(false);
    }

    @Override
    protected void guardar(PermisoLaboral p) throws Exception {
        controller.guardarPermiso(p);
    }

    @Override
    protected void eliminar(PermisoLaboral p) throws Exception {
        controller.eliminarPermiso(p.getId());
    }
}
