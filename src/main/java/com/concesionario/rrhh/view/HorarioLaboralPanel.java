package com.concesionario.rrhh.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.controller.RecursosHumanosController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.rrhh.model.DiaSemana;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.HorarioLaboral;

import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Pantalla del CU-RH-02 Gestionar asignación de horarios laborales. */
public class HorarioLaboralPanel extends FormularioCrud<HorarioLaboral> {

    private final RecursosHumanosController controller;
    private final JComboBox<Empleado> cmbEmpleado = new JComboBox<>();
    private final JComboBox<DiaSemana> cmbDia = new JComboBox<>(DiaSemana.values());
    private final JTextField txtHoraInicio = new JTextField(20);
    private final JTextField txtHoraFin = new JTextField(20);

    public HorarioLaboralPanel(RecursosHumanosController controller) {
        super("CU-RH-02 · Gestionar asignación de horarios laborales",
                "ID", "Empleado", "Día", "Hora de inicio", "Hora de fin");
        this.controller = controller;
        agregarCampo("Empleado:", cmbEmpleado);
        agregarCampo("Día:", cmbDia);
        agregarCampo("Hora de inicio (HH:mm):", txtHoraInicio);
        agregarCampo("Hora de fin (HH:mm):", txtHoraFin);
        refrescar();
    }

    @Override
    protected void actualizarListas() throws Exception {
        cmbEmpleado.setModel(new DefaultComboBoxModel<>(controller.listarEmpleados().toArray(new Empleado[0])));
    }

    @Override
    protected List<HorarioLaboral> cargarDatos() throws Exception {
        return controller.listarHorarios();
    }

    @Override
    protected Object[] fila(HorarioLaboral h) {
        return new Object[]{h.getId(), h.getNombreEmpleado(), h.getDia(), h.getHoraInicio(), h.getHoraFin()};
    }

    @Override
    protected HorarioLaboral leerFormulario() throws ValidacionException {
        HorarioLaboral h = new HorarioLaboral();
        h.setId(seleccionado == null ? 0 : seleccionado.getId());
        h.setIdEmpleado(Campos.seleccion(cmbEmpleado, "Empleado").getId());
        h.setDia(Campos.seleccion(cmbDia, "Día"));
        h.setHoraInicio(Campos.hora(txtHoraInicio, "hora de inicio"));
        h.setHoraFin(Campos.hora(txtHoraFin, "hora de fin"));
        return h;
    }

    @Override
    protected void mostrarEnFormulario(HorarioLaboral h) {
        Campos.seleccionarPorId(cmbEmpleado, Empleado::getId, h.getIdEmpleado());
        cmbDia.setSelectedItem(h.getDia());
        txtHoraInicio.setText(Campos.texto(h.getHoraInicio()));
        txtHoraFin.setText(Campos.texto(h.getHoraFin()));
    }

    @Override
    protected void limpiarFormulario() {
        cmbEmpleado.setSelectedIndex(-1);
        cmbDia.setSelectedItem(DiaSemana.LUNES);
        txtHoraInicio.setText("08:00");
        txtHoraFin.setText("17:00");
    }

    @Override
    protected void guardar(HorarioLaboral h) throws Exception {
        controller.guardarHorario(h);
    }

    @Override
    protected void eliminar(HorarioLaboral h) throws Exception {
        controller.eliminarHorario(h.getId());
    }
}
