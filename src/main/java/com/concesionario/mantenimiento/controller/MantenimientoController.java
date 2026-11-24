package com.concesionario.mantenimiento.controller;

import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.RepuestoOrden;
import com.concesionario.mantenimiento.model.ResultadoDiagnostico;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.math.BigDecimal;
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

    public void eliminarRepuesto(int id) throws SQLException, ValidacionException {
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

    // CU-MF-04 Historial de mantenimiento
    public List<RegistroHistorial> listarHistorial() throws SQLException {
        return service.listarHistorial();
    }

    public List<RegistroHistorial> listarHistorialDeVehiculo(int idVehiculo) throws SQLException {
        return service.listarHistorialDeVehiculo(idVehiculo);
    }

    public void guardarRegistroHistorial(RegistroHistorial registro) throws SQLException, ValidacionException {
        service.guardarRegistroHistorial(registro);
    }

    public void eliminarRegistroHistorial(int id) throws SQLException, ValidacionException {
        service.eliminarRegistroHistorial(id);
    }

    // CU-MF-05 Diagnosticar y reparar vehículo
    public List<OrdenMantenimiento> listarOrdenesActivas() throws SQLException {
        return service.listarOrdenesActivas();
    }

    public List<RepuestoOrden> listarRepuestosDeOrden(int idOrden) throws SQLException {
        return service.listarRepuestosDeOrden(idOrden);
    }

    public ResultadoDiagnostico registrarDiagnostico(int idOrden, String diagnostico, boolean danoSevero,
                                                     BigDecimal horasManoObra, List<RepuestoOrden> repuestos)
            throws SQLException, ValidacionException {
        return service.registrarDiagnostico(idOrden, diagnostico, danoSevero, horasManoObra, repuestos);
    }

    public ResultadoDiagnostico reintentarRepuestos(int idOrden) throws SQLException, ValidacionException {
        return service.reintentarRepuestos(idOrden);
    }

    public void aprobarControlCalidad(int idOrden) throws SQLException, ValidacionException {
        service.aprobarControlCalidad(idOrden);
    }

    public void rechazarControlCalidad(int idOrden, String observacion) throws SQLException, ValidacionException {
        service.rechazarControlCalidad(idOrden, observacion);
    }

    public void declararIrreparable(int idOrden, String observacion) throws SQLException, ValidacionException {
        service.declararIrreparable(idOrden, observacion);
    }
}
