package com.concesionario.rrhh.view;

import com.concesionario.rrhh.controller.RecursosHumanosController;

import javax.swing.JTabbedPane;

/** Vista del área de Recursos Humanos: una pestaña por cada caso de uso de esta entrega. */
public class RecursosHumanosView extends JTabbedPane {

    public RecursosHumanosView() {
        RecursosHumanosController controller = new RecursosHumanosController();
        addTab("Empleados", new EmpleadoPanel(controller));
        addTab("Contratos laborales", new ContratoLaboralPanel(controller));
        addTab("Horarios laborales", new HorarioLaboralPanel(controller));
    }
}
