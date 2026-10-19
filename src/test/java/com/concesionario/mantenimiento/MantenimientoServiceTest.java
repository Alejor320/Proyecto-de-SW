package com.concesionario.mantenimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.concesionario.PruebaBase;
import com.concesionario.apoyo.model.EstadoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoEmpleado;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pruebas de MantenimientoService: CU-MF-01, CU-MF-02 y CU-MF-03. */
class MantenimientoServiceTest extends PruebaBase {

    private final MantenimientoService service = new MantenimientoService();

    private Repuesto repuesto(String codigo, int cantidad, int minimo, String precio) {
        Repuesto r = new Repuesto();
        r.setCodigo(codigo);
        r.setNombre("Filtro " + codigo);
        r.setCantidad(cantidad);
        r.setStockMinimo(minimo);
        r.setPrecio(new BigDecimal(precio));
        return r;
    }

    private OrdenMantenimiento orden(int idVehiculo, int idTecnico) {
        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setIdVehiculo(idVehiculo);
        o.setIdTecnico(idTecnico);
        o.setMotivo("Cambio de aceite y revisión de frenos");
        o.setKilometraje(45000);
        o.setPrioridad(Prioridad.MEDIA);
        return o;
    }

    private EstadoVehiculo estadoDe(int idVehiculo) throws Exception {
        return service.listarVehiculos().stream()
                .filter(v -> v.getId() == idVehiculo).findFirst().orElseThrow().getEstado();
    }

    // ---------- CU-MF-03 Técnicos ----------

    @Test
    void registraTecnicoDesdeUnEmpleadoActivo() throws Exception {
        crearTecnico("1012345678", true);
        crearTecnico("1098765432", false);
        assertEquals(2, service.listarTecnicos().size());
        assertEquals(1, service.listarTecnicosDisponibles().size());
    }

    @Test
    void unEmpleadoNoPuedeSerTecnicoDosVeces() throws Exception {
        Tecnico existente = crearTecnico("1012345678", true);
        Tecnico repetido = new Tecnico();
        repetido.setIdEmpleado(existente.getIdEmpleado());
        repetido.setEspecialidad("Frenos");
        assertThrows(ValidacionException.class, () -> service.guardarTecnico(repetido));
    }

    @Test
    void unEmpleadoInactivoNoPuedeSerTecnico() throws Exception {
        Empleado inactivo = crearEmpleado("1012345678", EstadoEmpleado.INACTIVO);
        Tecnico t = new Tecnico();
        t.setIdEmpleado(inactivo.getId());
        t.setEspecialidad("Motor");
        assertThrows(ValidacionException.class, () -> service.guardarTecnico(t));
    }

    // ---------- CU-MF-02 Repuestos ----------

    @Test
    void detectaRepuestosQueDebenReabastecerse() throws Exception {
        service.guardarRepuesto(repuesto("FIL-001", 3, 5, "45000"));
        service.guardarRepuesto(repuesto("FIL-002", 20, 5, "45000"));
        assertEquals(1, service.listarRepuestosParaReabastecer().size());
        assertEquals("FIL-001", service.listarRepuestosParaReabastecer().get(0).getCodigo());
    }

    @Test
    void noPermiteCodigoDeRepuestoDuplicado() throws Exception {
        service.guardarRepuesto(repuesto("FIL-001", 3, 5, "45000"));
        assertThrows(ValidacionException.class, () -> service.guardarRepuesto(repuesto("fil-001", 1, 1, "1000")));
    }

    @Test
    void noPermitePrecioCeroONegativo() {
        assertThrows(ValidacionException.class, () -> service.guardarRepuesto(repuesto("FIL-003", 3, 5, "0")));
    }

    @Test
    void noPermiteCantidadNegativa() {
        assertThrows(ValidacionException.class, () -> service.guardarRepuesto(repuesto("FIL-004", -1, 5, "1000")));
    }

    // ---------- CU-MF-01 Órdenes de mantenimiento ----------

    @Test
    void abrirOrdenDejaElVehiculoEnMantenimiento() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        service.guardarOrden(orden(v.getId(), t.getId()));

        assertEquals(EstadoVehiculo.EN_MANTENIMIENTO, estadoDe(v.getId()));
        assertEquals(EstadoOrden.ABIERTA, service.listarOrdenes().get(0).getEstado());
    }

    @Test
    void unVehiculoEnMantenimientoNoRecibeOtraOrden() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        service.guardarOrden(orden(v.getId(), t.getId()));
        assertThrows(ValidacionException.class, () -> service.guardarOrden(orden(v.getId(), t.getId())));
    }

    @Test
    void noAsignaTecnicoNoDisponible() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", false);
        assertThrows(ValidacionException.class, () -> service.guardarOrden(orden(v.getId(), t.getId())));
        assertEquals(EstadoVehiculo.DISPONIBLE, estadoDe(v.getId()));
    }

    @Test
    void cerrarOrdenDejaElVehiculoDisponible() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        service.guardarOrden(orden(v.getId(), t.getId()));

        OrdenMantenimiento abierta = service.listarOrdenes().get(0);
        abierta.setEstado(EstadoOrden.CERRADA);
        service.guardarOrden(abierta);

        OrdenMantenimiento cerrada = service.listarOrdenes().get(0);
        assertEquals(EstadoOrden.CERRADA, cerrada.getEstado());
        assertNotNull(cerrada.getFechaCierre());
        assertEquals(EstadoVehiculo.DISPONIBLE, estadoDe(v.getId()));
    }

    @Test
    void noSePuedeModificarUnaOrdenCerrada() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        service.guardarOrden(orden(v.getId(), t.getId()));
        OrdenMantenimiento o = service.listarOrdenes().get(0);
        o.setEstado(EstadoOrden.CERRADA);
        service.guardarOrden(o);

        OrdenMantenimiento cerrada = service.listarOrdenes().get(0);
        cerrada.setMotivo("Otro motivo");
        assertThrows(ValidacionException.class, () -> service.guardarOrden(cerrada));
    }

    @Test
    void eliminarOrdenAbiertaLiberaElVehiculo() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        service.guardarOrden(orden(v.getId(), t.getId()));
        service.eliminarOrden(service.listarOrdenes().get(0).getId());

        assertEquals(0, service.listarOrdenes().size());
        assertEquals(EstadoVehiculo.DISPONIBLE, estadoDe(v.getId()));
    }
}
