package com.concesionario.mantenimiento.controller;

import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de Mantenimiento y Flota: recibe las acciones de las pantallas (view)
 * y se las pasa al servicio, que es donde están las reglas de negocio.
 */
public class MantenimientoController {

    private final MantenimientoService service = new MantenimientoService();
    private final RecursosHumanosService rrhhService = new RecursosHumanosService();

    // CU-MF-01 Órdenes de mantenimiento
    public List<OrdenMantenimiento> listarOrdenes() throws SQLException {
        return service.listarOrdenes();
    }

    public void guardarOrden(OrdenMantenimiento orden) throws SQLException, ValidacionException {
        service.guardarOrden(orden);
    }

    public void eliminarOrden(int id) throws SQLException, ValidacionException {
        service.eliminarOrden(id);
    }

    // CU-MF-02 Repuestos
    public List<Repuesto> listarRepuestos() throws SQLException {
        return service.listarRepuestos();
    }

    public void guardarRepuesto(Repuesto repuesto) throws SQLException, ValidacionException {
        service.guardarRepuesto(repuesto);
    }

    public void eliminarRepuesto(int id) throws SQLException {
        service.eliminarRepuesto(id);
    }

    // CU-MF-03 Técnicos
    public List<Tecnico> listarTecnicos() throws SQLException {
        return service.listarTecnicos();
    }

    public void guardarTecnico(Tecnico tecnico) throws SQLException, ValidacionException {
        service.guardarTecnico(tecnico);
    }

    public void eliminarTecnico(int id) throws SQLException, ValidacionException {
        service.eliminarTecnico(id);
    }

    /** Empleados de Recursos Humanos que se pueden registrar como técnicos. */
    public List<Empleado> listarEmpleados() throws SQLException {
        return rrhhService.listarEmpleados();
    }

    // CU-AP-01 Vehículos (apoyo)
    public List<Vehiculo> listarVehiculos() throws SQLException {
        return service.listarVehiculos();
    }

    public void registrarVehiculo(Vehiculo vehiculo) throws SQLException, ValidacionException {
        service.registrarVehiculo(vehiculo);
    }
}
