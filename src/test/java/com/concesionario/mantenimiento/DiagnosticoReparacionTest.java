package com.concesionario.mantenimiento;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.concesionario.PruebaBase;
import com.concesionario.apoyo.model.EstadoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.RepuestoOrden;
import com.concesionario.mantenimiento.model.ResultadoDiagnostico;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de MantenimientoService para el CU-MF-05 Diagnosticar y reparar vehículo. */
class DiagnosticoReparacionTest extends PruebaBase {

    private final MantenimientoService service = new MantenimientoService();
    private Vehiculo vehiculo;
    private OrdenMantenimiento orden;
    private Repuesto filtro;
    private Repuesto pastillas;

    @BeforeEach
    void prepararOrdenAbierta() throws Exception {
        vehiculo = crearVehiculo("ABC123");
        Tecnico tecnico = crearTecnico("1012345678", true);
        filtro = repuesto("FIL-001", "Filtro de aceite", 10, "35000");
        pastillas = repuesto("FRE-010", "Pastillas de freno", 1, "120000");

        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setIdVehiculo(vehiculo.getId());
        o.setIdTecnico(tecnico.getId());
        o.setMotivo("Ruido al frenar");
        o.setKilometraje(52000);
        o.setPrioridad(Prioridad.MEDIA);
        service.guardarOrden(o);
        orden = service.listarOrdenes().get(0);
    }

    private Repuesto repuesto(String codigo, String nombre, int cantidad, String precio) throws Exception {
        Repuesto r = new Repuesto();
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setCantidad(cantidad);
        r.setStockMinimo(1);
        r.setPrecio(new BigDecimal(precio));
        service.guardarRepuesto(r);
        return r;
    }

    private int stock(Repuesto r) throws Exception {
        return service.listarRepuestos().stream().filter(x -> x.getId() == r.getId()).findFirst().orElseThrow().getCantidad();
    }

    private OrdenMantenimiento ordenActual() throws Exception {
        return service.listarOrdenes().stream().filter(o -> o.getId() == orden.getId()).findFirst().orElseThrow();
    }

    private EstadoVehiculo estadoVehiculo() throws Exception {
        return service.listarVehiculos().stream().filter(v -> v.getId() == vehiculo.getId()).findFirst().orElseThrow().getEstado();
    }

    private ResultadoDiagnostico diagnosticar(List<RepuestoOrden> repuestos) throws Exception {
        return service.registrarDiagnostico(orden.getId(), "Pastillas gastadas y filtro saturado", false,
                new BigDecimal("2"), repuestos);
    }

    @Test
    void conInventarioSuficienteDescuentaRepuestosYPasaAEnProceso() throws Exception {
        ResultadoDiagnostico r = diagnosticar(List.of(new RepuestoOrden(filtro.getId(), 1), new RepuestoOrden(pastillas.getId(), 1)));

        assertEquals(EstadoOrden.EN_PROCESO, r.getEstado());
        assertFalse(r.hayFaltantes());
        assertEquals(9, stock(filtro));
        assertEquals(0, stock(pastillas));
        assertEquals("Pastillas gastadas y filtro saturado", ordenActual().getDiagnostico());
    }

    @Test
    void sinInventarioSuficienteQuedaPendienteYNoDescuentaNada() throws Exception {
        ResultadoDiagnostico r = diagnosticar(List.of(new RepuestoOrden(filtro.getId(), 1), new RepuestoOrden(pastillas.getId(), 2)));

        assertEquals(EstadoOrden.PENDIENTE_REPUESTO, r.getEstado());
        assertEquals(1, r.getFaltantes().size());
        assertTrue(r.getFaltantes().get(0).startsWith("FRE-010"));
        assertEquals(10, stock(filtro), "No se descuenta ningún repuesto si falta alguno");
        assertEquals(1, stock(pastillas));
    }

    @Test
    void alLlegarInventarioSeReintentanLosRepuestos() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(pastillas.getId(), 2)));
        pastillas.setCantidad(5);
        service.guardarRepuesto(pastillas);

        ResultadoDiagnostico r = service.reintentarRepuestos(orden.getId());
        assertEquals(EstadoOrden.EN_PROCESO, r.getEstado());
        assertEquals(3, stock(pastillas));
    }

    @Test
    void unDanoSeveroMarcaLaOrdenComoCritica() throws Exception {
        service.registrarDiagnostico(orden.getId(), "Fisura en el bloque del motor", true, new BigDecimal("8"), List.of());
        assertEquals(Prioridad.CRITICA, ordenActual().getPrioridad());
    }

    @Test
    void aprobarCalidadCierraLaOrdenLiberaElVehiculoYRegistraElCosto() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(filtro.getId(), 1), new RepuestoOrden(pastillas.getId(), 1)));
        service.aprobarControlCalidad(orden.getId());

        assertEquals(EstadoOrden.CERRADA, ordenActual().getEstado());
        assertEquals(EstadoVehiculo.DISPONIBLE, estadoVehiculo());
        RegistroHistorial h = service.listarHistorialDeVehiculo(vehiculo.getId()).get(0);
        // 35.000 + 120.000 de repuestos + 2 h x 45.000 de mano de obra
        assertEquals(0, new BigDecimal("245000").compareTo(h.getCosto()), "Costo en el historial: " + h.getCosto());
    }

    @Test
    void rechazarCalidadDevuelveADiagnosticoYElVehiculoSigueEnElTaller() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(pastillas.getId(), 1)));
        assertThrows(ValidacionException.class, () -> service.rechazarControlCalidad(orden.getId(), ""));
        service.rechazarControlCalidad(orden.getId(), "Persiste el ruido en la rueda delantera izquierda");

        assertEquals(EstadoOrden.ABIERTA, ordenActual().getEstado());
        assertEquals(EstadoVehiculo.EN_MANTENIMIENTO, estadoVehiculo());
        assertEquals(0, service.listarHistorialDeVehiculo(vehiculo.getId()).size());
    }

    @Test
    void noSeApruebaCalidadSinDiagnosticoNiConRepuestosPendientes() throws Exception {
        assertThrows(ValidacionException.class, () -> service.aprobarControlCalidad(orden.getId()));
        diagnosticar(List.of(new RepuestoOrden(pastillas.getId(), 3)));
        assertThrows(ValidacionException.class, () -> service.aprobarControlCalidad(orden.getId()));
    }

    @Test
    void declararIrreparableDejaElVehiculoNoDisponible() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(pastillas.getId(), 3)));
        service.declararIrreparable(orden.getId(), "Chasis con daño estructural");

        assertEquals(EstadoOrden.CERRADA, ordenActual().getEstado());
        assertEquals(EstadoVehiculo.NO_DISPONIBLE, estadoVehiculo());
        assertTrue(service.listarHistorialDeVehiculo(vehiculo.getId()).get(0).getDescripcion().contains("dar de baja"));
    }

    @Test
    void siFallaElDiagnosticoNoQuedaNadaAMedias() throws Exception {
        List<RepuestoOrden> conUnoInexistente = List.of(new RepuestoOrden(filtro.getId(), 1), new RepuestoOrden(9999, 1));
        assertThrows(ValidacionException.class, () -> diagnosticar(conUnoInexistente));

        assertEquals(EstadoOrden.ABIERTA, ordenActual().getEstado());
        assertEquals(10, stock(filtro));
        assertEquals(0, service.listarRepuestosDeOrden(orden.getId()).size());
    }

    @Test
    void noSeEliminaUnaOrdenConRepuestosUsadosNiUnRepuestoUsado() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(filtro.getId(), 1)));
        service.rechazarControlCalidad(orden.getId(), "Fuga de aceite");
        assertThrows(ValidacionException.class, () -> service.eliminarOrden(orden.getId()));
        assertThrows(ValidacionException.class, () -> service.eliminarRepuesto(filtro.getId()));
    }

    // ---------- Reglas y validaciones que faltaban para cubrir todo el servicio ----------

    @Test
    void rechazaDiagnosticosYDecisionesConDatosInvalidos() throws Exception {
        List<RepuestoOrden> uno = List.of(new RepuestoOrden(filtro.getId(), 1));
        BigDecimal unaHora = new BigDecimal("1");
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(orden.getId(), "x".repeat(501), false, unaHora, uno)),
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(orden.getId(), "Falla", false, null, uno)),
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(orden.getId(), "Falla", false, new BigDecimal("-1"), uno)),
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(orden.getId(), "Falla", false, new BigDecimal("201"), uno)),
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(orden.getId(), "Falla", false, unaHora,
                        List.of(new RepuestoOrden(filtro.getId(), 0)))),
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(orden.getId(), "Falla", false, unaHora,
                        List.of(new RepuestoOrden(filtro.getId(), 1), new RepuestoOrden(filtro.getId(), 2)))),
                () -> assertThrows(ValidacionException.class, () -> service.registrarDiagnostico(999, "Falla", false, unaHora, uno)),
                () -> assertThrows(ValidacionException.class, () -> service.reintentarRepuestos(orden.getId())),
                () -> assertThrows(ValidacionException.class, () -> service.rechazarControlCalidad(orden.getId(), "x".repeat(301))),
                () -> assertThrows(ValidacionException.class, () -> service.rechazarControlCalidad(999, "Vibración")),
                () -> assertThrows(ValidacionException.class, () -> service.declararIrreparable(orden.getId(), "x".repeat(301))),
                () -> assertThrows(ValidacionException.class, () -> service.declararIrreparable(orden.getId(), "Chasis doblado")));
        assertEquals(EstadoOrden.ABIERTA, ordenActual().getEstado());
        assertEquals(10, stock(filtro));
    }

    @Test
    void siSiguenFaltandoRepuestosLaOrdenSigueEsperando() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(pastillas.getId(), 3)));   // solo hay 1 en inventario
        ResultadoDiagnostico r = service.reintentarRepuestos(orden.getId());
        assertEquals(EstadoOrden.PENDIENTE_REPUESTO, r.getEstado());
        assertEquals(1, stock(pastillas));

        // El técnico cambia el diagnóstico mientras espera: con una sola pastilla ya alcanza.
        ResultadoDiagnostico nuevo = diagnosticar(List.of(new RepuestoOrden(pastillas.getId(), 1)));
        assertEquals(EstadoOrden.EN_PROCESO, nuevo.getEstado());
        assertEquals(0, stock(pastillas));
    }

    @Test
    void trasUnRechazoSeVuelveADiagnosticarYSeCierraConLaDescripcionResumida() throws Exception {
        diagnosticar(List.of(new RepuestoOrden(filtro.getId(), 1)));
        assertThrows(ValidacionException.class, () -> diagnosticar(List.of()));   // ya está EN_PROCESO
        service.rechazarControlCalidad(orden.getId(), "Sigue el ruido al frenar");

        service.registrarDiagnostico(orden.getId(), "D".repeat(450), false, new BigDecimal("1"),
                List.of(new RepuestoOrden(pastillas.getId(), 1)));   // el filtro ya descontado no se vuelve a descontar
        assertEquals(9, stock(filtro));
        assertEquals(0, stock(pastillas));

        service.aprobarControlCalidad(orden.getId());
        RegistroHistorial h = service.listarHistorialDeVehiculo(vehiculo.getId()).get(0);
        assertEquals(300, h.getDescripcion().length());
        assertTrue(h.getDescripcion().endsWith("..."));
        assertEquals(0, new BigDecimal("200000").compareTo(h.getCosto()));   // 1 h × 45.000 + 35.000 + 120.000

        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.reintentarRepuestos(orden.getId())),
                () -> assertThrows(ValidacionException.class, () -> service.declararIrreparable(orden.getId(), "Tarde")),
                () -> assertThrows(ValidacionException.class, () -> service.eliminarOrden(orden.getId())));
    }
}
