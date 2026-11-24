package com.concesionario.rrhh;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.concesionario.PruebaBase;
import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.DiaSemana;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.EstadoPermiso;
import com.concesionario.rrhh.model.EstadoVacaciones;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.model.PermisoLaboral;
import com.concesionario.rrhh.model.SolicitudVacaciones;
import com.concesionario.rrhh.model.TipoContrato;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;

/** Pruebas de RecursosHumanosService para el CU-RH-05 Gestionar solicitud y aprobación de vacaciones. */
class SolicitudVacacionesTest extends PruebaBase {

    private final RecursosHumanosService service = new RecursosHumanosService();

    /** Lunes dentro de dos semanas, para que las fechas siempre sean futuras. */
    private final LocalDate lunes = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    private ContratoLaboral contrato;

    /** Empleado activo con contrato indefinido y horario de lunes a viernes. */
    private Empleado empleadoListo(String documento, LocalDate ingreso, DiaSemana... dias) throws Exception {
        Empleado e = new Empleado();
        e.setDocumento(documento);
        e.setNombres("Nombre" + documento);
        e.setApellidos("Apellido");
        e.setCargo("Asesor");
        e.setFechaIngreso(ingreso);
        service.guardarEmpleado(e);

        contrato = new ContratoLaboral();
        contrato.setIdEmpleado(e.getId());
        contrato.setTipo(TipoContrato.INDEFINIDO);
        contrato.setFechaInicio(ingreso);
        contrato.setSalario(new BigDecimal("2500000"));
        service.guardarContrato(contrato);

        DiaSemana[] laborales = dias.length > 0 ? dias
                : new DiaSemana[]{DiaSemana.LUNES, DiaSemana.MARTES, DiaSemana.MIERCOLES, DiaSemana.JUEVES, DiaSemana.VIERNES};
        for (DiaSemana d : laborales) {
            HorarioLaboral h = new HorarioLaboral();
            h.setIdEmpleado(e.getId());
            h.setDia(d);
            h.setHoraInicio(LocalTime.of(8, 0));
            h.setHoraFin(LocalTime.of(17, 0));
            service.guardarHorario(h);
        }
        return e;
    }

    private SolicitudVacaciones solicitud(int idEmpleado, LocalDate inicio, LocalDate fin) {
        SolicitudVacaciones v = new SolicitudVacaciones();
        v.setIdEmpleado(idEmpleado);
        v.setFechaInicio(inicio);
        v.setFechaFin(fin);
        return v;
    }

    private SolicitudVacaciones buscar(int id) throws Exception {
        return service.listarSolicitudesVacaciones().stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
    }

    @Test
    void solicitudValidaQuedaPendienteConLosDiasHabiles() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(7));
        service.solicitarVacaciones(v);

        SolicitudVacaciones guardada = buscar(v.getId());
        assertEquals(EstadoVacaciones.PENDIENTE, guardada.getEstado());
        assertEquals(6, guardada.getDiasHabiles(), "Lunes a lunes con horario L-V son 6 días hábiles");
    }

    @Test
    void cuentaSoloLosDiasQueElEmpleadoTieneEnSuHorario() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15),
                DiaSemana.LUNES, DiaSemana.MIERCOLES, DiaSemana.VIERNES);
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(13));
        service.solicitarVacaciones(v);
        assertEquals(6, buscar(v.getId()).getDiasHabiles(), "Dos semanas con horario L-M-V son 6 días hábiles");
    }

    @Test
    void noPermiteSolicitarSinSaldoSuficiente() throws Exception {
        Empleado nuevo = empleadoListo("1012345678", LocalDate.now().minusDays(60));
        assertThrows(ValidacionException.class, () ->
                service.solicitarVacaciones(solicitud(nuevo.getId(), lunes, lunes.plusDays(4))));
    }

    @Test
    void noPermiteSolicitarSinHorarioAsignado() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        ContratoLaboral k = new ContratoLaboral();
        k.setIdEmpleado(e.getId());
        k.setTipo(TipoContrato.INDEFINIDO);
        k.setFechaInicio(LocalDate.of(2024, 1, 15));
        k.setSalario(new BigDecimal("2500000"));
        service.guardarContrato(k);
        assertThrows(ValidacionException.class, () ->
                service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4))));
    }

    @Test
    void noPermiteSolicitarSinContratoVigente() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        contrato.setEstado(EstadoContrato.TERMINADO);
        service.guardarContrato(contrato);
        assertThrows(ValidacionException.class, () ->
                service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4))));
    }

    @Test
    void noPermiteVacacionesQueSeCruzanConUnPermiso() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        PermisoLaboral p = new PermisoLaboral();
        p.setIdEmpleado(e.getId());
        p.setFechaInicio(lunes.plusDays(2));
        p.setFechaFin(lunes.plusDays(2));
        p.setMotivo("Cita médica");
        p.setEstado(EstadoPermiso.APROBADO);
        service.guardarPermiso(p);
        assertThrows(ValidacionException.class, () ->
                service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4))));
    }

    @Test
    void noPermiteDosSolicitudesEnElMismoPeriodo() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4)));
        assertThrows(ValidacionException.class, () ->
                service.solicitarVacaciones(solicitud(e.getId(), lunes.plusDays(3), lunes.plusDays(8))));
    }

    @Test
    void aprobarDejaLaSolicitudAprobadaYDescuentaElSaldo() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        int saldoInicial = service.consultarSaldoVacaciones(e.getId());

        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);
        service.aprobarVacaciones(v.getId(), jefe.getId());

        SolicitudVacaciones aprobada = buscar(v.getId());
        assertEquals(EstadoVacaciones.APROBADA, aprobada.getEstado());
        assertEquals(Integer.valueOf(jefe.getId()), aprobada.getIdAprobador());
        assertEquals(saldoInicial - 5, service.consultarSaldoVacaciones(e.getId()));
    }

    @Test
    void unEmpleadoNoPuedeAprobarSusPropiasVacaciones() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);
        assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(v.getId(), e.getId()));
        assertEquals(EstadoVacaciones.PENDIENTE, buscar(v.getId()).getEstado());
    }

    @Test
    void rechazarExigeMotivoYDevuelveElSaldo() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        int saldoInicial = service.consultarSaldoVacaciones(e.getId());
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);

        assertThrows(ValidacionException.class, () -> service.rechazarVacaciones(v.getId(), jefe.getId(), " "));
        service.rechazarVacaciones(v.getId(), jefe.getId(), "Temporada alta de ventas");

        assertEquals(EstadoVacaciones.RECHAZADA, buscar(v.getId()).getEstado());
        assertEquals(saldoInicial, service.consultarSaldoVacaciones(e.getId()));
    }

    @Test
    void noApruebaSiElContratoDejoDeEstarVigenteYNoCambiaNada() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);

        contrato.setEstado(EstadoContrato.TERMINADO);
        service.guardarContrato(contrato);

        assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(v.getId(), jefe.getId()));
        SolicitudVacaciones igual = buscar(v.getId());
        assertEquals(EstadoVacaciones.PENDIENTE, igual.getEstado());
        assertNull(igual.getIdAprobador());
    }

    @Test
    void soloSeCancelanSolicitudesPendientes() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);
        service.aprobarVacaciones(v.getId(), jefe.getId());
        assertThrows(ValidacionException.class, () -> service.cancelarSolicitudVacaciones(v.getId()));
    }

    // ---------- Reglas y validaciones que faltaban para cubrir todo el servicio ----------

    private PermisoLaboral permiso(int idEmpleado, LocalDate inicio, LocalDate fin) throws Exception {
        PermisoLaboral p = new PermisoLaboral();
        p.setIdEmpleado(idEmpleado);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setMotivo("Diligencia personal");
        service.guardarPermiso(p);
        return p;
    }

    @Test
    void rechazaSolicitudesConDatosInvalidos() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado inactivo = crearEmpleado("1098765432", EstadoEmpleado.INACTIVO);
        SolicitudVacaciones observacionLarga = solicitud(e.getId(), lunes, lunes.plusDays(4));
        observacionLarga.setObservacion("x".repeat(201));
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(solicitud(0, lunes, lunes.plusDays(4)))),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(solicitud(e.getId(), lunes.plusDays(4), lunes))),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(
                        solicitud(e.getId(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(3)))),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(observacionLarga)),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(solicitud(999, lunes, lunes.plusDays(4)))),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(solicitud(inactivo.getId(), lunes, lunes.plusDays(4)))),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(
                        solicitud(e.getId(), lunes.plusDays(5), lunes.plusDays(6)))),   // solo sábado y domingo
                () -> assertThrows(ValidacionException.class, () -> service.consultarSaldoVacaciones(999)));
        assertEquals(0, service.listarSolicitudesVacaciones().size());
    }

    @Test
    void modificaYCancelaUnaSolicitudPendiente() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado otro = empleadoListo("1098765432", LocalDate.of(2024, 1, 15));
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);

        v.setFechaFin(lunes.plusDays(2));
        v.setObservacion("Solo tres días");
        service.solicitarVacaciones(v);   // al revisar cruces se salta a sí misma
        assertEquals(3, buscar(v.getId()).getDiasHabiles());

        SolicitudVacaciones inexistente = solicitud(e.getId(), lunes, lunes);
        inexistente.setId(999);
        SolicitudVacaciones deOtroEmpleado = solicitud(otro.getId(), lunes, lunes.plusDays(2));
        deOtroEmpleado.setId(v.getId());
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(inexistente)),
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(deOtroEmpleado)));

        service.cancelarSolicitudVacaciones(v.getId());
        assertEquals(0, service.listarSolicitudesVacaciones().size());
    }

    @Test
    void validaAlJefeYNoCambiaUnaSolicitudYaDecidida() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        Empleado jefeInactivo = crearEmpleado("1076543210", EstadoEmpleado.INACTIVO);
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(v.getId(), 0)),
                () -> assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(v.getId(), 999)),
                () -> assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(v.getId(), jefeInactivo.getId())),
                () -> assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(999, jefe.getId())),
                () -> assertThrows(ValidacionException.class, () -> service.rechazarVacaciones(v.getId(), jefe.getId(), "x".repeat(201))));

        service.aprobarVacaciones(v.getId(), jefe.getId());
        v.setFechaFin(lunes.plusDays(2));
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(v)),
                () -> assertThrows(ValidacionException.class, () -> service.rechazarVacaciones(v.getId(), jefe.getId(), "Cierre de mes")));
    }

    @Test
    void noApruebaSiElSaldoYaNoAlcanza() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        SolicitudVacaciones v = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(v);

        e.setFechaIngreso(LocalDate.now().minusDays(30));   // se corrigió la fecha de ingreso: ahora tiene 1 día ganado
        service.guardarEmpleado(e);

        assertThrows(ValidacionException.class, () -> service.aprobarVacaciones(v.getId(), jefe.getId()));
        assertEquals(EstadoVacaciones.PENDIENTE, buscar(v.getId()).getEstado());
    }

    @Test
    void elContratoDebeCubrirTodoElPeriodo() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        contrato.setTipo(TipoContrato.TERMINO_FIJO);
        contrato.setFechaFin(lunes.plusDays(2));   // termina en mitad de las vacaciones
        service.guardarContrato(contrato);
        assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4))));

        contrato.setFechaInicio(lunes.plusDays(1));   // empieza después del primer día de vacaciones
        contrato.setFechaFin(lunes.plusDays(60));
        service.guardarContrato(contrato);
        assertThrows(ValidacionException.class, () -> service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4))));

        contrato.setFechaInicio(LocalDate.of(2024, 1, 15));   // ahora sí cubre todo el período
        service.guardarContrato(contrato);
        service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4)));
        assertEquals(1, service.listarSolicitudesVacaciones().size());
    }

    @Test
    void lasAusenciasRechazadasODeOtrasFechasNoImpidenSolicitar() throws Exception {
        Empleado e = empleadoListo("1012345678", LocalDate.of(2024, 1, 15));
        Empleado jefe = crearEmpleado("1098765432", EstadoEmpleado.ACTIVO);
        PermisoLaboral rechazado = permiso(e.getId(), lunes, lunes);
        rechazado.setEstado(EstadoPermiso.RECHAZADO);
        service.guardarPermiso(rechazado);
        permiso(e.getId(), LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 3));    // permiso anterior
        permiso(e.getId(), lunes.plusWeeks(4), lunes.plusWeeks(4));               // permiso posterior

        SolicitudVacaciones primera = solicitud(e.getId(), lunes, lunes.plusDays(4));
        service.solicitarVacaciones(primera);
        service.rechazarVacaciones(primera.getId(), jefe.getId(), "Cierre de mes");

        service.solicitarVacaciones(solicitud(e.getId(), lunes, lunes.plusDays(4)));                                   // misma fecha que la rechazada
        service.solicitarVacaciones(solicitud(e.getId(), lunes.plusWeeks(1), lunes.plusWeeks(1).plusDays(4)));         // después
        service.solicitarVacaciones(solicitud(e.getId(), lunes.minusWeeks(1), lunes.minusWeeks(1).plusDays(4)));       // antes
        assertEquals(4, service.listarSolicitudesVacaciones().size());
    }
}
