package com.concesionario.mantenimiento;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.concesionario.PruebaBase;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas de MantenimientoService para el CU-MF-04 Gestionar historial de mantenimiento del vehículo. */
class HistorialMantenimientoTest extends PruebaBase {

    private final MantenimientoService service = new MantenimientoService();

    private RegistroHistorial registro(int idVehiculo, LocalDate fecha, int km, String costo) {
        RegistroHistorial h = new RegistroHistorial();
        h.setIdVehiculo(idVehiculo);
        h.setFecha(fecha);
        h.setKilometraje(km);
        h.setDescripcion("Cambio de aceite y filtro");
        h.setCosto(new BigDecimal(costo));
        return h;
    }

    @Test
    void registraUnServicioManualEnElHistorial() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        service.guardarRegistroHistorial(registro(v.getId(), LocalDate.now().minusDays(30), 40000, "180000"));
        assertEquals(1, service.listarHistorialDeVehiculo(v.getId()).size());
    }

    @Test
    void noPermiteFechaFuturaNiCostoNegativo() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        assertThrows(ValidacionException.class, () ->
                service.guardarRegistroHistorial(registro(v.getId(), LocalDate.now().plusDays(1), 40000, "1000")));
        assertThrows(ValidacionException.class, () ->
                service.guardarRegistroHistorial(registro(v.getId(), LocalDate.now(), 40000, "-1")));
    }

    @Test
    void noPermiteKilometrajeMenorQueUnServicioAnterior() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        service.guardarRegistroHistorial(registro(v.getId(), LocalDate.now().minusDays(60), 40000, "100000"));
        assertThrows(ValidacionException.class, () ->
                service.guardarRegistroHistorial(registro(v.getId(), LocalDate.now().minusDays(10), 35000, "100000")));
    }

    @Test
    void listaSoloElHistorialDelVehiculoPedido() throws Exception {
        Vehiculo a = crearVehiculo("ABC123");
        Vehiculo b = crearVehiculo("DEF456");
        service.guardarRegistroHistorial(registro(a.getId(), LocalDate.now().minusDays(5), 1000, "0"));
        service.guardarRegistroHistorial(registro(b.getId(), LocalDate.now().minusDays(5), 2000, "0"));
        service.guardarRegistroHistorial(registro(b.getId(), LocalDate.now().minusDays(1), 2500, "0"));
        assertEquals(1, service.listarHistorialDeVehiculo(a.getId()).size());
        assertEquals(2, service.listarHistorialDeVehiculo(b.getId()).size());
    }

    @Test
    void cerrarUnaOrdenAgregaElServicioAlHistorial() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        Tecnico t = crearTecnico("1012345678", true);
        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setIdVehiculo(v.getId());
        o.setIdTecnico(t.getId());
        o.setMotivo("Revisión de los 40.000 km");
        o.setKilometraje(40000);
        o.setPrioridad(Prioridad.MEDIA);
        service.guardarOrden(o);
        OrdenMantenimiento abierta = service.listarOrdenes().get(0);
        abierta.setEstado(EstadoOrden.CERRADA);
        service.guardarOrden(abierta);

        RegistroHistorial h = service.listarHistorialDeVehiculo(v.getId()).get(0);
        assertEquals(Integer.valueOf(abierta.getId()), h.getIdOrden());
        assertEquals(40000, h.getKilometraje());
        assertTrue(h.getDescripcion().contains("Revisión de los 40.000 km"));
    }

    @Test
    void losRegistrosDeOrdenesCerradasNoSeEliminanNiModifican() throws Exception {
        cerrarUnaOrdenAgregaElServicioAlHistorial();
        RegistroHistorial automatico = service.listarHistorial().get(0);
        assertThrows(ValidacionException.class, () -> service.eliminarRegistroHistorial(automatico.getId()));
        automatico.setDescripcion("Otro texto");
        assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(automatico));
    }

    @Test
    void rechazaRegistrosConDatosInvalidos() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        LocalDate fecha = LocalDate.of(2026, 5, 10);
        RegistroHistorial kmNegativo = registro(v.getId(), fecha, -1, "1000");
        RegistroHistorial descripcionLarga = registro(v.getId(), fecha, 1000, "1000");
        descripcionLarga.setDescripcion("x".repeat(301));
        RegistroHistorial sinCosto = registro(v.getId(), fecha, 1000, "1000");
        sinCosto.setCosto(null);
        RegistroHistorial inexistente = registro(v.getId(), fecha, 1000, "1000");
        inexistente.setId(999);
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(registro(0, fecha, 1000, "1000"))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(registro(999, fecha, 1000, "1000"))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(kmNegativo)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(descripcionLarga)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(sinCosto)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(inexistente)),
                () -> assertThrows(ValidacionException.class, () -> service.eliminarRegistroHistorial(999)));
        assertEquals(0, service.listarHistorial().size());
    }

    @Test
    void modificaYEliminaUnRegistroManual() throws Exception {
        Vehiculo v = crearVehiculo("ABC123");
        RegistroHistorial enero = registro(v.getId(), LocalDate.of(2026, 1, 10), 40000, "150000");
        RegistroHistorial junio = registro(v.getId(), LocalDate.of(2026, 6, 10), 50000, "200000");
        service.guardarRegistroHistorial(enero);
        service.guardarRegistroHistorial(junio);

        RegistroHistorial marzo = registro(v.getId(), LocalDate.of(2026, 3, 10), 55000, "90000");
        assertThrows(ValidacionException.class, () -> service.guardarRegistroHistorial(marzo));   // más km que un servicio posterior

        enero.setKilometraje(41000);
        service.guardarRegistroHistorial(enero);   // se compara consigo mismo (se salta) y con junio
        assertEquals(41000, service.listarHistorialDeVehiculo(v.getId()).stream()
                .filter(h -> h.getId() == enero.getId()).findFirst().orElseThrow().getKilometraje());

        service.eliminarRegistroHistorial(junio.getId());
        assertEquals(1, service.listarHistorial().size());
    }
}
