package com.concesionario.rrhh.controller;

import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.model.PermisoLaboral;
import com.concesionario.rrhh.model.SolicitudVacaciones;
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

    // CU-RH-05 Solicitud y aprobación de vacaciones
    public List<SolicitudVacaciones> listarSolicitudesVacaciones() throws SQLException {
        return service.listarSolicitudesVacaciones();
    }

    public int consultarSaldoVacaciones(int idEmpleado) throws SQLException, ValidacionException {
        return service.consultarSaldoVacaciones(idEmpleado);
    }

    public void solicitarVacaciones(SolicitudVacaciones solicitud) throws SQLException, ValidacionException {
        service.solicitarVacaciones(solicitud);
    }

    public void aprobarVacaciones(int idSolicitud, int idJefe) throws SQLException, ValidacionException {
        service.aprobarVacaciones(idSolicitud, idJefe);
    }

    public void rechazarVacaciones(int idSolicitud, int idJefe, String motivo) throws SQLException, ValidacionException {
        service.rechazarVacaciones(idSolicitud, idJefe, motivo);
    }

    public void cancelarSolicitudVacaciones(int idSolicitud) throws SQLException, ValidacionException {
        service.cancelarSolicitudVacaciones(idSolicitud);
    }
}
