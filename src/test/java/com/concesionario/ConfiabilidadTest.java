package com.concesionario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.concesionario.apoyo.model.EstadoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.config.ConexionBD;
import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.RepuestoOrden;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba del atributo de calidad RNF-04 (integridad transaccional): si el cierre de una orden
 * falla a mitad de camino, la base de datos debe quedar exactamente como estaba antes.
 * Para provocar la falla renombramos la tabla del historial, que es el último paso del cierre.
 */
class ConfiabilidadTest extends PruebaBase {

    private final MantenimientoService service = new MantenimientoService();
    private Vehiculo vehiculo;
    private OrdenMantenimiento orden;
    private Repuesto filtro;

    @BeforeEach
    void prepararOrdenEnProceso() throws Exception {
        vehiculo = crearVehiculo("ABC123");
        Tecnico tecnico = crearTecnico("1012345678", true);

        filtro = new Repuesto();
        filtro.setCodigo("FIL-001");
        filtro.setNombre("Filtro de aceite");
        filtro.setCantidad(10);
        filtro.setStockMinimo(1);
        filtro.setPrecio(new BigDecimal("35000"));
        service.guardarRepuesto(filtro);

        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setIdVehiculo(vehiculo.getId());
        o.setIdTecnico(tecnico.getId());
        o.setMotivo("Cambio de aceite");
        o.setKilometraje(40000);
        o.setPrioridad(Prioridad.MEDIA);
        service.guardarOrden(o);
        orden = service.listarOrdenes().get(0);

        service.registrarDiagnostico(orden.getId(), "Filtro saturado", false, new BigDecimal("1"),
                List.of(new RepuestoOrden(filtro.getId(), 1)));
    }

    private void bloquearHistorial() throws SQLException {
        try (Connection c = ConexionBD.obtener(); Statement s = c.createStatement()) {
            s.execute("ALTER TABLE historial_mantenimiento RENAME TO historial_bloqueado");
        }
    }

    private OrdenMantenimiento ordenActual() throws Exception {
        return service.listarOrdenes().stream().filter(o -> o.getId() == orden.getId()).findFirst().orElseThrow();
    }

    private EstadoVehiculo estadoVehiculo() throws Exception {
        return service.listarVehiculos().stream().filter(v -> v.getId() == vehiculo.getId())
                .findFirst().orElseThrow().getEstado();
    }

    private int stockFiltro() throws Exception {
        return service.listarRepuestos().stream().filter(r -> r.getId() == filtro.getId())
                .findFirst().orElseThrow().getCantidad();
    }

    @Test
    void siFallaElHistorialAlAprobarCalidadLaOrdenNoSeCierra() throws Exception {
        bloquearHistorial();

        assertThrows(SQLException.class, () -> service.aprobarControlCalidad(orden.getId()));

        OrdenMantenimiento despues = ordenActual();
        assertEquals(EstadoOrden.EN_PROCESO, despues.getEstado());
        assertNull(despues.getFechaCierre());
        assertEquals(EstadoVehiculo.EN_MANTENIMIENTO, estadoVehiculo());
        assertEquals(9, stockFiltro());
    }

    @Test
    void siFallaElHistorialAlDeclararIrreparableElVehiculoNoQuedaNoDisponible() throws Exception {
        bloquearHistorial();

        assertThrows(SQLException.class, () -> service.declararIrreparable(orden.getId(), "Chasis doblado"));

        OrdenMantenimiento despues = ordenActual();
        assertEquals(EstadoOrden.EN_PROCESO, despues.getEstado());
        assertNull(despues.getObservacionCalidad());
        assertEquals(EstadoVehiculo.EN_MANTENIMIENTO, estadoVehiculo());
    }
}
