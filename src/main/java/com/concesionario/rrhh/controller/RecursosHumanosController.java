package com.concesionario.rrhh.controller;

import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.PermisoLaboral;
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

    // CU-RH-04 Permisos laborales
    public List<PermisoLaboral> listarPermisos() throws SQLException {
        return service.listarPermisos();
    }

    public void guardarPermiso(PermisoLaboral permiso) throws SQLException, ValidacionException {
        service.guardarPermiso(permiso);
    }

    public void eliminarPermiso(int id) throws SQLException {
        service.eliminarPermiso(id);
    }
}
