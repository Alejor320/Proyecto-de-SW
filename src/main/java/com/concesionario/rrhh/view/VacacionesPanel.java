package com.concesionario.rrhh.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.rrhh.controller.RecursosHumanosController;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.SolicitudVacaciones;

import java.time.LocalDate;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Pantalla del CU-RH-05 Gestionar solicitud y aprobación de vacaciones.
 * Guardar registra la solicitud (queda PENDIENTE); Aprobar y Rechazar los usa el jefe inmediato.
 */
public class VacacionesPanel extends FormularioCrud<SolicitudVacaciones> {

    private final RecursosHumanosController controller;
    private final JComboBox<Empleado> cmbEmpleado = new JComboBox<>();
    private final JLabel lblSaldo = new JLabel("Seleccione un empleado para ver su saldo.");
    private final JTextField txtFechaInicio = new JTextField(20);
    private final JTextField txtFechaFin = new JTextField(20);
    private final JTextField txtObservacion = new JTextField(20);
    private final JComboBox<Empleado> cmbJefe = new JComboBox<>();
    private final JTextField txtMotivoRechazo = new JTextField(20);

    public VacacionesPanel(RecursosHumanosController controller) {
        super("CU-RH-05 · Gestionar solicitud y aprobación de vacaciones",
                "ID", "Empleado", "Inicio", "Fin", "Días hábiles", "Estado", "Decidió", "Fecha decisión", "Motivo rechazo");
        this.controller = controller;
        agregarCampo("Empleado:", cmbEmpleado);
        agregarCampo("Saldo disponible:", lblSaldo);
        agregarCampo("Fecha de inicio (aaaa-mm-dd):", txtFechaInicio);
        agregarCampo("Fecha de fin (aaaa-mm-dd):", txtFechaFin);
        agregarCampo("Observación (opcional):", txtObservacion);
        agregarCampo("Jefe inmediato que decide:", cmbJefe);
        agregarCampo("Motivo del rechazo:", txtMotivoRechazo);
        cambiarTextoEliminar("Cancelar solicitud");
        agregarAccion("Aprobar", "La solicitud quedó APROBADA.",
                "No fue posible completar la aprobación de vacaciones. No se realizaron cambios.",
                v -> controller.aprobarVacaciones(v.getId(), idJefe()));
        agregarAccion("Rechazar", "La solicitud quedó RECHAZADA.",
                v -> controller.rechazarVacaciones(v.getId(), idJefe(), Campos.texto(txtMotivoRechazo)));
        cmbEmpleado.addActionListener(e -> mostrarSaldo());
        refrescar();
    }

    private int idJefe() throws ValidacionException {
        return Campos.seleccion(cmbJefe, "Jefe inmediato que decide").getId();
    }

    private void mostrarSaldo() {
        Empleado e = cmbEmpleado.getItemAt(cmbEmpleado.getSelectedIndex());
        if (e == null) {
            lblSaldo.setText("Seleccione un empleado para ver su saldo.");
            return;
        }
        try {
            lblSaldo.setText(controller.consultarSaldoVacaciones(e.getId()) + " días hábiles");
        } catch (Exception ex) {
            lblSaldo.setText("No se pudo consultar el saldo.");
        }
    }

    @Override
    protected void actualizarListas() throws Exception {
        Empleado[] empleados = controller.listarEmpleados().toArray(new Empleado[0]);
        cmbEmpleado.setModel(new DefaultComboBoxModel<>(empleados));
        cmbJefe.setModel(new DefaultComboBoxModel<>(empleados));
    }

    @Override
    protected List<SolicitudVacaciones> cargarDatos() throws Exception {
        return controller.listarSolicitudesVacaciones();
    }

    @Override
    protected Object[] fila(SolicitudVacaciones v) {
        return new Object[]{v.getId(), v.getNombreEmpleado(), v.getFechaInicio(), v.getFechaFin(), v.getDiasHabiles(),
                v.getEstado(), Campos.texto(v.getNombreAprobador()), Campos.texto(v.getFechaDecision()),
                Campos.texto(v.getMotivoRechazo())};
    }

    @Override
    protected SolicitudVacaciones leerFormulario() throws ValidacionException {
        SolicitudVacaciones v = new SolicitudVacaciones();
        v.setId(seleccionado == null ? 0 : seleccionado.getId());
        v.setIdEmpleado(Campos.seleccion(cmbEmpleado, "Empleado").getId());
        v.setFechaInicio(Campos.fecha(txtFechaInicio, "fecha de inicio", true));
        v.setFechaFin(Campos.fecha(txtFechaFin, "fecha de fin", true));
        v.setObservacion(Campos.textoOpcional(txtObservacion));
        return v;
    }

    @Override
    protected void mostrarEnFormulario(SolicitudVacaciones v) {
        Campos.seleccionarPorId(cmbEmpleado, Empleado::getId, v.getIdEmpleado());
        cmbEmpleado.setEnabled(false);
        txtFechaInicio.setText(Campos.texto(v.getFechaInicio()));
        txtFechaFin.setText(Campos.texto(v.getFechaFin()));
        txtObservacion.setText(Campos.texto(v.getObservacion()));
        txtMotivoRechazo.setText(Campos.texto(v.getMotivoRechazo()));
        mostrarSaldo();
    }

    @Override
    protected void limpiarFormulario() {
        cmbEmpleado.setEnabled(true);
        cmbEmpleado.setSelectedIndex(-1);
        cmbJefe.setSelectedIndex(-1);
        txtFechaInicio.setText(LocalDate.now().plusDays(7).toString());
        txtFechaFin.setText(LocalDate.now().plusDays(11).toString());
        txtObservacion.setText("");
        txtMotivoRechazo.setText("");
        mostrarSaldo();
    }

    @Override
    protected void guardar(SolicitudVacaciones v) throws Exception {
        controller.solicitarVacaciones(v);
    }

    @Override
    protected void eliminar(SolicitudVacaciones v) throws Exception {
        controller.cancelarSolicitudVacaciones(v.getId());
    }
}
