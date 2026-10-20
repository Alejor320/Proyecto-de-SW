package com.concesionario.rrhh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.concesionario.PruebaBase;
import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.EstadoPermiso;
import com.concesionario.rrhh.model.PermisoLaboral;
import com.concesionario.rrhh.model.TipoContrato;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas de RecursosHumanosService: CU-RH-01, CU-RH-03 y CU-RH-04. */
class RecursosHumanosServiceTest extends PruebaBase {

    private final RecursosHumanosService service = new RecursosHumanosService();

    private ContratoLaboral contrato(int idEmpleado, TipoContrato tipo, LocalDate inicio, LocalDate fin) {
        ContratoLaboral k = new ContratoLaboral();
        k.setIdEmpleado(idEmpleado);
        k.setTipo(tipo);
        k.setFechaInicio(inicio);
        k.setFechaFin(fin);
        k.setSalario(new BigDecimal("2000000"));
        return k;
    }

    private PermisoLaboral permiso(int idEmpleado, LocalDate inicio, LocalDate fin) {
        PermisoLaboral p = new PermisoLaboral();
        p.setIdEmpleado(idEmpleado);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setMotivo("Cita médica");
        p.setSoporte("soportes/cita-medica.pdf");
        return p;
    }

    // ---------- CU-RH-01 Empleados ----------

    @Test
    void guardaYListaEmpleados() throws Exception {
        crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        crearEmpleado("1098765432", EstadoEmpleado.INACTIVO);
        assertEquals(2, service.listarEmpleados().size());
        assertEquals(1, service.listarEmpleadosActivos().size());
    }

    @Test
    void noPermiteDocumentoDuplicado() throws Exception {
        crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        assertThrows(ValidacionException.class, () -> crearEmpleado("1012345678", EstadoEmpleado.ACTIVO));
    }

    @Test
    void noPermiteFechaDeIngresoFutura() {
        Empleado e = new Empleado();
        e.setDocumento("1012345678");
        e.setNombres("Ana");
        e.setApellidos("Pérez");
        e.setCargo("Asesora");
        e.setFechaIngreso(LocalDate.now().plusDays(5));
        assertThrows(ValidacionException.class, () -> service.guardarEmpleado(e));
    }

    @Test
    void noEliminaEmpleadoConContrato() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarContrato(contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2024, 1, 15), null));
        assertThrows(ValidacionException.class, () -> service.eliminarEmpleado(e.getId()));
    }

    @Test
    void eliminaEmpleadoSinRegistrosAsociados() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.eliminarEmpleado(e.getId());
        assertEquals(0, service.listarEmpleados().size());
    }

    // ---------- CU-RH-03 Contratos laborales ----------

    @Test
    void registraContratoValido() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarContrato(contrato(e.getId(), TipoContrato.TERMINO_FIJO, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        assertEquals(1, service.listarContratos().size());
    }

    @Test
    void noPermiteDosContratosVigentesQueSeCruzan() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarContrato(contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2025, 1, 1), null));
        assertThrows(ValidacionException.class, () -> service.guardarContrato(
                contrato(e.getId(), TipoContrato.TERMINO_FIJO, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1))));
    }

    @Test
    void permiteContratoNuevoSiElAnteriorEstaTerminado() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        ContratoLaboral anterior = contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2025, 1, 1), null);
        anterior.setEstado(EstadoContrato.TERMINADO);
        service.guardarContrato(anterior);
        service.guardarContrato(contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2026, 1, 1), null));
        assertEquals(2, service.listarContratos().size());
    }

    @Test
    void noPermiteFechaFinAnteriorAlInicio() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        assertThrows(ValidacionException.class, () -> service.guardarContrato(
                contrato(e.getId(), TipoContrato.TERMINO_FIJO, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1))));
    }

    @Test
    void contratoATerminoFijoExigeFechaFin() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        assertThrows(ValidacionException.class, () -> service.guardarContrato(
                contrato(e.getId(), TipoContrato.TERMINO_FIJO, LocalDate.of(2026, 1, 1), null)));
    }

    @Test
    void noPermiteContratoVigenteParaEmpleadoInactivo() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.INACTIVO);
        assertThrows(ValidacionException.class, () -> service.guardarContrato(
                contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2026, 1, 1), null)));
    }

    // ---------- CU-RH-04 Permisos laborales ----------

    @Test
    void guardaActualizaYEliminaPermiso() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        PermisoLaboral p = permiso(e.getId(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 20));
        p.setEstado(EstadoPermiso.APROBADO); // todo permiso nuevo debe iniciar pendiente

        service.guardarPermiso(p);
        assertEquals(1, service.listarPermisos().size());
        assertEquals(EstadoPermiso.PENDIENTE, service.listarPermisos().get(0).getEstado());
        assertEquals("soportes/cita-medica.pdf", service.listarPermisos().get(0).getSoporte());

        e.setEstado(EstadoEmpleado.INACTIVO);
        service.guardarEmpleado(e);
        p.setMotivo("Cita médica aprobada");
        p.setEstado(EstadoPermiso.APROBADO);
        service.guardarPermiso(p);
        assertEquals("Cita médica aprobada", service.listarPermisos().get(0).getMotivo());
        assertEquals(EstadoPermiso.APROBADO, service.listarPermisos().get(0).getEstado());

        Empleado otro = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        p.setIdEmpleado(otro.getId());
        assertThrows(ValidacionException.class, () -> service.guardarPermiso(p));
        p.setIdEmpleado(e.getId());

        service.eliminarPermiso(p.getId());
        assertEquals(0, service.listarPermisos().size());
    }

    @Test
    void noPermiteFechaFinAnteriorAlInicioDelPermiso() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        assertThrows(ValidacionException.class, () -> service.guardarPermiso(
                permiso(e.getId(), LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 20))));
    }

    @Test
    void noPermitePermisoParaEmpleadoInactivo() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.INACTIVO);
        assertThrows(ValidacionException.class, () -> service.guardarPermiso(
                permiso(e.getId(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 20))));
    }

    @Test
    void noEliminaEmpleadoConPermiso() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarPermiso(permiso(e.getId(), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)));
        assertThrows(ValidacionException.class, () -> service.eliminarEmpleado(e.getId()));
    }
}
