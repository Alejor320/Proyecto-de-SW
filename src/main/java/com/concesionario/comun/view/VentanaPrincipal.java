package com.concesionario.comun.view;

import com.concesionario.mantenimiento.view.MantenimientoView;
import com.concesionario.rrhh.view.RecursosHumanosView;

import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

/** Ventana principal: una pestaña por área (RecursosHumanosView y MantenimientoView). */
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal() {
        super("Concesionario Integrado - Recursos Humanos y Mantenimiento y Flota");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        RecursosHumanosView rrhh = new RecursosHumanosView();
        MantenimientoView mantenimiento = new MantenimientoView();

        JTabbedPane principal = new JTabbedPane();
        principal.addTab("Recursos Humanos", rrhh);
        principal.addTab("Mantenimiento y Flota", mantenimiento);

        // Al cambiar de pestaña se recargan los datos, para que las listas
        // (por ejemplo, los empleados al registrar un técnico) estén al día.
        rrhh.addChangeListener(this::refrescarPestana);
        mantenimiento.addChangeListener(this::refrescarPestana);
        principal.addChangeListener(this::refrescarPestana);

        setContentPane(principal);
        setSize(1150, 720);
        setLocationRelativeTo(null);
    }

    private void refrescarPestana(ChangeEvent e) {
        Component actual = ((JTabbedPane) e.getSource()).getSelectedComponent();
        if (actual instanceof JTabbedPane interna) {
            actual = interna.getSelectedComponent();
        }
        if (actual instanceof FormularioCrud<?> formulario) {
            formulario.refrescar();
        }
    }
}
