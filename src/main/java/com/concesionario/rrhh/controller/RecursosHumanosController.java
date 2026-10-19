package com.concesionario.rrhh.controller;

import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de Recursos Humanos: recibe las acciones de las pantallas (view)
 * y se las pasa al servicio, que es donde están las reglas de negocio.
 */
public class RecursosHumanosController {

    private final RecursosHumanosService service = new RecursosHumanosService();

    // CU-RH-01 Empleados
    public List<Empleado> listarEmpleados() throws SQLException {
        return service.listarEmpleados();
    }

    public void guardarEmpleado(Empleado empleado) throws SQLException, ValidacionException {
        service.guardarEmpleado(empleado);
    }

    public void eliminarEmpleado(int id) throws SQLException, ValidacionException {
        service.eliminarEmpleado(id);
    }

    // CU-RH-03 Contratos laborales
    public List<ContratoLaboral> listarContratos() throws SQLException {
        return service.listarContratos();
    }

    public void guardarContrato(ContratoLaboral contrato) throws SQLException, ValidacionException {
        service.guardarContrato(contrato);
    }

    public void eliminarContrato(int id) throws SQLException {
        service.eliminarContrato(id);
    }

    // CU-RH-02 Horarios laborales
    public List<HorarioLaboral> listarHorarios() throws SQLException {
        return service.listarHorarios();
    }

    public void guardarHorario(HorarioLaboral horario) throws SQLException, ValidacionException {
        service.guardarHorario(horario);
    }

    public void eliminarHorario(int id) throws SQLException {
        service.eliminarHorario(id);
    }
}
