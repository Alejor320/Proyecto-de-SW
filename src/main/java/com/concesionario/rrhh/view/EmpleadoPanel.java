package com.concesionario.rrhh.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.controller.RecursosHumanosController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoEmpleado;

import java.time.LocalDate;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Pantalla del CU-RH-01 Gestionar ciclo laboral de empleados. */
public class EmpleadoPanel extends FormularioCrud<Empleado> {

    private final RecursosHumanosController controller;
    private final JTextField txtDocumento = new JTextField(20);
    private final JTextField txtNombres = new JTextField(20);
    private final JTextField txtApellidos = new JTextField(20);
    private final JTextField txtCargo = new JTextField(20);
    private final JTextField txtTelefono = new JTextField(20);
    private final JTextField txtCorreo = new JTextField(20);
    private final JTextField txtFechaIngreso = new JTextField(20);
    private final JComboBox<EstadoEmpleado> cmbEstado = new JComboBox<>(EstadoEmpleado.values());

    public EmpleadoPanel(RecursosHumanosController controller) {
        super("CU-RH-01 · Gestionar ciclo laboral de empleados",
                "ID", "Documento", "Nombre", "Cargo", "Teléfono", "Correo", "Ingreso", "Estado");
        this.controller = controller;
        agregarCampo("Documento:", txtDocumento);
        agregarCampo("Nombres:", txtNombres);
        agregarCampo("Apellidos:", txtApellidos);
        agregarCampo("Cargo:", txtCargo);
        agregarCampo("Teléfono (opcional):", txtTelefono);
        agregarCampo("Correo (opcional):", txtCorreo);
        agregarCampo("Fecha de ingreso (aaaa-mm-dd):", txtFechaIngreso);
        agregarCampo("Estado:", cmbEstado);
        refrescar();
    }

    @Override
    protected List<Empleado> cargarDatos() throws Exception {
        return controller.listarEmpleados();
    }

    @Override
    protected Object[] fila(Empleado e) {
        return new Object[]{e.getId(), e.getDocumento(), e.getNombreCompleto(), e.getCargo(),
                Campos.texto(e.getTelefono()), Campos.texto(e.getCorreo()), e.getFechaIngreso(), e.getEstado()};
    }

    @Override
    protected Empleado leerFormulario() throws ValidacionException {
        Empleado e = new Empleado();
        e.setId(seleccionado == null ? 0 : seleccionado.getId());
        e.setDocumento(Campos.texto(txtDocumento));
        e.setNombres(Campos.texto(txtNombres));
        e.setApellidos(Campos.texto(txtApellidos));
        e.setCargo(Campos.texto(txtCargo));
        e.setTelefono(Campos.textoOpcional(txtTelefono));
        e.setCorreo(Campos.textoOpcional(txtCorreo));
        e.setFechaIngreso(Campos.fecha(txtFechaIngreso, "fecha de ingreso", true));
        e.setEstado(Campos.seleccion(cmbEstado, "Estado"));
        return e;
    }

    @Override
    protected void mostrarEnFormulario(Empleado e) {
        txtDocumento.setText(e.getDocumento());
        txtNombres.setText(e.getNombres());
        txtApellidos.setText(e.getApellidos());
        txtCargo.setText(e.getCargo());
        txtTelefono.setText(Campos.texto(e.getTelefono()));
        txtCorreo.setText(Campos.texto(e.getCorreo()));
        txtFechaIngreso.setText(Campos.texto(e.getFechaIngreso()));
        cmbEstado.setSelectedItem(e.getEstado());
    }

    @Override
    protected void limpiarFormulario() {
        txtDocumento.setText("");
        txtNombres.setText("");
        txtApellidos.setText("");
        txtCargo.setText("");
        txtTelefono.setText("");
        txtCorreo.setText("");
        txtFechaIngreso.setText(LocalDate.now().toString());
        cmbEstado.setSelectedItem(EstadoEmpleado.ACTIVO);
    }

    @Override
    protected void guardar(Empleado e) throws Exception {
        controller.guardarEmpleado(e);
    }

    @Override
    protected void eliminar(Empleado e) throws Exception {
        controller.eliminarEmpleado(e.getId());
    }
}
