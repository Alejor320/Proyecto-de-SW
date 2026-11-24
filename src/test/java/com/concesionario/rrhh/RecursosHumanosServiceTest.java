package com.concesionario.rrhh;

import static org.junit.jupiter.api.Assertions.assertAll;
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

    // ---------- Reglas y validaciones que faltaban para cubrir todo el servicio ----------

    private Empleado empleado(String documento, String telefono, String correo) {
        Empleado e = new Empleado();
        e.setDocumento(documento);
        e.setNombres("Ana");
        e.setApellidos("Pérez");
        e.setCargo("Asesora");
        e.setTelefono(telefono);
        e.setCorreo(correo);
        e.setFechaIngreso(LocalDate.of(2024, 1, 15));
        return e;
    }

    @Test
    void rechazaDocumentoTelefonoYCorreoInvalidos() {
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarEmpleado(empleado("", null, null))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarEmpleado(empleado("12AB45", null, null))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarEmpleado(empleado("1012345678", "12", null))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarEmpleado(empleado("1012345678", "", "sin-arroba"))));
    }

    @Test
    void actualizaLosDatosDeUnEmpleado() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        e.setTelefono("3001234567");
        e.setCorreo("ana@concesionario.com");
        e.setCargo("Jefe de taller");
        service.guardarEmpleado(e);
        assertEquals(1, service.listarEmpleados().size());
        assertEquals("Jefe de taller", service.listarEmpleados().get(0).getCargo());
    }

    @Test
    void rechazaContratosConDatosIncompletos() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        ContratoLaboral sinSalario = contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2026, 1, 1), null);
        sinSalario.setSalario(null);
        ContratoLaboral salarioCero = contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2026, 1, 1), null);
        salarioCero.setSalario(BigDecimal.ZERO);
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarContrato(
                        contrato(0, TipoContrato.INDEFINIDO, LocalDate.of(2026, 1, 1), null))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarContrato(
                        contrato(999, TipoContrato.INDEFINIDO, LocalDate.of(2026, 1, 1), null))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarContrato(sinSalario)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarContrato(salarioCero)));
    }

    @Test
    void actualizaYEliminaContratos() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        ContratoLaboral anterior = contrato(e.getId(), TipoContrato.TERMINO_FIJO, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        service.guardarContrato(anterior);
        ContratoLaboral actual = contrato(e.getId(), TipoContrato.INDEFINIDO, LocalDate.of(2025, 1, 1), null);
        service.guardarContrato(actual);   // los dos quedan vigentes porque no se cruzan

        actual.setFechaFin(LocalDate.of(2027, 12, 31));   // un indefinido también puede tener fecha de fin
        actual.setSalario(new BigDecimal("3000000"));
        service.guardarContrato(actual);
        service.guardarContrato(contrato(e.getId(), TipoContrato.TERMINO_FIJO, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)));
        assertEquals(3, service.listarContratos().size());

        service.eliminarContrato(anterior.getId());
        assertEquals(2, service.listarContratos().size());
    }

    @Test
    void rechazaPermisosConDatosInvalidos() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        LocalDate dia = LocalDate.of(2026, 10, 20);
        PermisoLaboral motivoLargo = permiso(e.getId(), dia, dia);
        motivoLargo.setMotivo("x".repeat(201));
        PermisoLaboral soporteLargo = permiso(e.getId(), dia, dia);
        soporteLargo.setSoporte("x".repeat(256));
        PermisoLaboral inexistente = permiso(e.getId(), dia, dia);
        inexistente.setId(999);
        inexistente.setEstado(EstadoPermiso.PENDIENTE);
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarPermiso(permiso(0, dia, dia))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarPermiso(permiso(999, dia, dia))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarPermiso(motivoLargo)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarPermiso(soporteLargo)),
                () -> assertThrows(ValidacionException.class, () -> service.guardarPermiso(inexistente)));

        PermisoLaboral sinSoporte = permiso(e.getId(), dia, dia);
        sinSoporte.setSoporte(null);   // el soporte es opcional
        service.guardarPermiso(sinSoporte);
        assertEquals(1, service.listarPermisos().size());
    }
}
