package com.concesionario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.concesionario.apoyo.model.TipoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Prueba del atributo de calidad RNF-05 (rendimiento): las operaciones habituales de consulta
 * y registro deben responder en menos de 1,5 segundos con un volumen de datos realista.
 */
class RendimientoTest extends PruebaBase {

    private static final Duration LIMITE = Duration.ofMillis(1500);

    private final RecursosHumanosService rrhh = new RecursosHumanosService();
    private final MantenimientoService mantenimiento = new MantenimientoService();

    /** 300 empleados, 100 vehículos y 200 órdenes cerradas con su historial. */
    private void cargarVolumen() throws Exception {
        for (int i = 0; i < 300; i++) {
            Empleado e = new Empleado();
            e.setDocumento(String.valueOf(1_000_000_000L + i));
            e.setNombres("Empleado" + i);
            e.setApellidos("Prueba");
            e.setCargo("Asesor");
            e.setFechaIngreso(LocalDate.of(2023, 1, 1));
            rrhh.guardarEmpleado(e);
        }
        Tecnico t = new Tecnico();
        t.setIdEmpleado(1);
        t.setEspecialidad("General");
        mantenimiento.guardarTecnico(t);
        for (int i = 0; i < 100; i++) {
            Vehiculo v = new Vehiculo();
            v.setPlaca(String.format("AAA%03d", i));
            v.setMarca("Renault");
            v.setModelo("Logan");
            v.setTipo(TipoVehiculo.AUTOMOVIL);
            v.setAnio(2022);
            mantenimiento.registrarVehiculo(v);
        }
        for (int i = 0; i < 200; i++) {
            OrdenMantenimiento o = nuevaOrden(1 + (i % 100), 10000 + i);
            mantenimiento.guardarOrden(o);
            OrdenMantenimiento abierta = mantenimiento.listarOrdenes().get(0);
            abierta.setEstado(com.concesionario.mantenimiento.model.EstadoOrden.CERRADA);
            mantenimiento.guardarOrden(abierta);
        }
    }

    /** Ejecuta la operación con el límite de RNF-05 y deja en la consola cuánto tardó. */
    private void medir(String operacion, Executable accion) {
        long inicio = System.nanoTime();
        assertTimeout(LIMITE, accion);
        System.out.printf("RNF-05  %-38s %7.1f ms%n", operacion, (System.nanoTime() - inicio) / 1_000_000.0);
    }

    private OrdenMantenimiento nuevaOrden(int idVehiculo, int km) {
        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setIdVehiculo(idVehiculo);
        o.setIdTecnico(1);
        o.setMotivo("Mantenimiento preventivo");
        o.setKilometraje(km);
        o.setPrioridad(Prioridad.BAJA);
        return o;
    }

    @Test
    void lasConsultasHabitualesRespondenEnMenosDeUnSegundoYMedio() throws Exception {
        cargarVolumen();
        medir("Consultar 300 empleados", () -> assertEquals(300, rrhh.listarEmpleados().size()));
        medir("Consultar 200 órdenes", () -> assertEquals(200, mantenimiento.listarOrdenes().size()));
        medir("Consultar historial (200 registros)", () -> assertEquals(200, mantenimiento.listarHistorial().size()));
        medir("Consultar historial de un vehículo", () -> assertEquals(2, mantenimiento.listarHistorialDeVehiculo(1).size()));
    }

    @Test
    void abrirYCerrarUnaOrdenRespondeEnMenosDeUnSegundoYMedio() throws Exception {
        cargarVolumen();
        medir("Abrir una orden nueva", () -> mantenimiento.guardarOrden(nuevaOrden(50, 99999)));
        OrdenMantenimiento abierta = mantenimiento.listarOrdenes().get(0);
        abierta.setEstado(com.concesionario.mantenimiento.model.EstadoOrden.CERRADA);
        medir("Cerrar la orden", () -> mantenimiento.guardarOrden(abierta));
    }
}
