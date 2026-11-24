package com.concesionario.mantenimiento;

import static org.junit.jupiter.api.Assertions.assertAll;
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
import com.concesionario.apoyo.model.TipoVehiculo;
import java.time.Year;

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

    // ---------- Reglas y validaciones que faltaban para cubrir todo el servicio ----------

    private Vehiculo vehiculo(String placa, TipoVehiculo tipo, int anio) {
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setMarca("Mazda");
        v.setModelo("3");
        v.setTipo(tipo);
        v.setAnio(anio);
        return v;
    }

    @Test
    void validaLosDatosDelVehiculo() throws Exception {
        crearVehiculo("ABC123");
        Vehiculo yaRegistrado = vehiculo("XYZ987", TipoVehiculo.AUTOMOVIL, 2022);
        yaRegistrado.setId(5);
        int anioSiguiente = Year.now().getValue() + 2;
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.registrarVehiculo(yaRegistrado)),
                () -> assertThrows(ValidacionException.class, () -> service.registrarVehiculo(vehiculo("AB12", TipoVehiculo.AUTOMOVIL, 2022))),
                () -> assertThrows(ValidacionException.class, () -> service.registrarVehiculo(vehiculo("ABC123", TipoVehiculo.MOTOCICLETA, 2022))),
                () -> assertThrows(ValidacionException.class, () -> service.registrarVehiculo(vehiculo("XYZ987", TipoVehiculo.AUTOMOVIL, 1949))),
                () -> assertThrows(ValidacionException.class, () -> service.registrarVehiculo(vehiculo("XYZ987", TipoVehiculo.AUTOMOVIL, anioSiguiente))),
                () -> assertThrows(ValidacionException.class, () -> service.registrarVehiculo(vehiculo("abc-123", TipoVehiculo.AUTOMOVIL, 2022))));
        service.registrarVehiculo(vehiculo("xyz 45b", TipoVehiculo.MOTOCICLETA, 2024));
        assertEquals(2, service.listarVehiculos().size());
    }

    @Test
    void actualizaYEliminaTecnicos() throws Exception {
        Tecnico t = crearTecnico("1012345678", true);
        t.setEspecialidad("Frenos");
        service.guardarTecnico(t);   // es el mismo técnico, no un duplicado
        assertEquals("Frenos", service.listarTecnicos().get(0).getEspecialidad());

        Tecnico sinEmpleado = new Tecnico();
        sinEmpleado.setEspecialidad("Motor");
        Tecnico empleadoInexistente = new Tecnico();
        empleadoInexistente.setIdEmpleado(999);
        empleadoInexistente.setEspecialidad("Motor");
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarTecnico(sinEmpleado)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarTecnico(empleadoInexistente)));

        Vehiculo v = crearVehiculo("ABC123");
        service.guardarOrden(orden(v.getId(), t.getId()));
        Tecnico libre = crearTecnico("1098765432", false);
        assertEquals(1, service.listarTecnicosDisponibles().size());
        assertThrows(ValidacionException.class, () -> service.eliminarTecnico(t.getId()));
        service.eliminarTecnico(libre.getId());
        assertEquals(1, service.listarTecnicos().size());
    }

    @Test
    void validaActualizaYEliminaRepuestos() throws Exception {
        Repuesto r = repuesto("FIL-001", 10, 3, "35000");
        service.guardarRepuesto(r);
        r.setCantidad(20);
        service.guardarRepuesto(r);   // mismo código, mismo repuesto
        assertEquals(20, service.listarRepuestos().get(0).getCantidad());

        Repuesto minimoNegativo = repuesto("ACE-020", 5, -1, "25000");
        Repuesto sinPrecio = repuesto("ACE-021", 5, 1, "25000");
        sinPrecio.setPrecio(null);
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarRepuesto(minimoNegativo)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarRepuesto(sinPrecio)));

        service.eliminarRepuesto(r.getId());
        assertEquals(0, service.listarRepuestos().size());
    }

    @Test
    void validaLosDatosDeLaOrden() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        OrdenMantenimiento motivoLargo = orden(v.getId(), t.getId());
        motivoLargo.setMotivo("x".repeat(201));
        OrdenMantenimiento kmNegativo = orden(v.getId(), t.getId());
        kmNegativo.setKilometraje(-1);
        OrdenMantenimiento inexistente = orden(v.getId(), t.getId());
        inexistente.setId(999);
        inexistente.setEstado(EstadoOrden.ABIERTA);
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(orden(0, t.getId()))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(orden(v.getId(), 0))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(motivoLargo)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(kmNegativo)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(orden(999, t.getId()))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(orden(v.getId(), 999))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarOrden(inexistente)),
                () -> assertThrows(ValidacionException.class, () -> service.eliminarOrden(999)));
        assertEquals(EstadoVehiculo.DISPONIBLE, estadoDe(v.getId()));
    }

    @Test
    void modificaUnaOrdenSinSaltarseElDiagnostico() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t1 = crearTecnico("1012345678", true);
        Tecnico t2 = crearTecnico("1098765432", true);
        service.guardarOrden(orden(v.getId(), t1.getId()));
        OrdenMantenimiento o = service.listarOrdenes().get(0);

        o.setIdTecnico(t2.getId());
        o.setMotivo("Ruido en la suspensión");
        service.guardarOrden(o);
        assertEquals(t2.getId(), service.listarOrdenes().get(0).getIdTecnico());

        o.setEstado(EstadoOrden.EN_PROCESO);
        assertThrows(ValidacionException.class, () -> service.guardarOrden(o));
        o.setEstado(EstadoOrden.PENDIENTE_REPUESTO);
        assertThrows(ValidacionException.class, () -> service.guardarOrden(o));
        assertEquals(EstadoOrden.ABIERTA, service.listarOrdenes().get(0).getEstado());
    }

    @Test
    void listaSoloLasOrdenesQueNoEstanCerradas() throws Exception {
        Vehiculo v1 = crearVehiculo("ABC123");
        Vehiculo v2 = crearVehiculo("DEF456");
        Tecnico t = crearTecnico("1012345678", true);
        service.guardarOrden(orden(v1.getId(), t.getId()));
        service.guardarOrden(orden(v2.getId(), t.getId()));
        OrdenMantenimiento primera = service.listarOrdenes().stream()
                .filter(o -> o.getIdVehiculo() == v1.getId()).findFirst().orElseThrow();
        primera.setEstado(EstadoOrden.CERRADA);
        service.guardarOrden(primera);

        assertEquals(1, service.listarOrdenesActivas().size());
        assertThrows(ValidacionException.class, () -> service.eliminarOrden(primera.getId()));
    }
}
