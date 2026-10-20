package com.concesionario.mantenimiento.view;

import com.concesionario.mantenimiento.controller.MantenimientoController;

import javax.swing.JTabbedPane;

/** Vista del área de Mantenimiento y Flota: una pestaña por cada caso de uso de esta entrega. */
public class MantenimientoView extends JTabbedPane {

    public MantenimientoView() {
        MantenimientoController controller = new MantenimientoController();
        addTab("Órdenes de mantenimiento", new OrdenMantenimientoPanel(controller));
        addTab("Técnicos", new TecnicoPanel(controller));
        addTab("Repuestos", new RepuestoPanel(controller));
        addTab("Vehículos (apoyo)", new VehiculoPanel(controller));
    }
}
