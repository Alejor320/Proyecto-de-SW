package com.concesionario.rrhh;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.concesionario.PruebaBase;
import com.concesionario.comun.ValidacionException;
import com.concesionario.rrhh.model.DiaSemana;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** Pruebas de RecursosHumanosService para el CU-RH-02 Gestionar asignación de horarios laborales. */
class HorarioLaboralTest extends PruebaBase {

    private final RecursosHumanosService service = new RecursosHumanosService();

    private HorarioLaboral horario(int idEmpleado, DiaSemana dia, String inicio, String fin) {
        HorarioLaboral h = new HorarioLaboral();
        h.setIdEmpleado(idEmpleado);
        h.setDia(dia);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFin(LocalTime.parse(fin));
        return h;
    }

    @Test
    void permiteTurnosSeguidosElMismoDia() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarHorario(horario(e.getId(), DiaSemana.LUNES, "08:00", "12:00"));
        service.guardarHorario(horario(e.getId(), DiaSemana.LUNES, "12:00", "17:00"));
        assertEquals(2, service.listarHorarios().size());
    }

    @Test
    void noPermiteTurnosQueSeCruzan() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarHorario(horario(e.getId(), DiaSemana.MARTES, "08:00", "12:00"));
        assertThrows(ValidacionException.class, () ->
                service.guardarHorario(horario(e.getId(), DiaSemana.MARTES, "11:00", "15:00")));
    }

    @Test
    void permiteMismaHoraEnOtroDia() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarHorario(horario(e.getId(), DiaSemana.MIERCOLES, "08:00", "17:00"));
        service.guardarHorario(horario(e.getId(), DiaSemana.JUEVES, "08:00", "17:00"));
        assertEquals(2, service.listarHorarios().size());
    }

    @Test
    void noPermiteHoraFinAntesDeInicio() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        assertThrows(ValidacionException.class, () ->
                service.guardarHorario(horario(e.getId(), DiaSemana.VIERNES, "17:00", "08:00")));
    }

    @Test
    void noAsignaHorarioAEmpleadoInactivo() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.INACTIVO);
        assertThrows(ValidacionException.class, () ->
                service.guardarHorario(horario(e.getId(), DiaSemana.LUNES, "08:00", "17:00")));
    }

    @Test
    void noEliminaEmpleadoConHorarioAsignado() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        service.guardarHorario(horario(e.getId(), DiaSemana.LUNES, "08:00", "17:00"));
        assertThrows(ValidacionException.class, () -> service.eliminarEmpleado(e.getId()));
    }

    @Test
    void rechazaHorarioSinEmpleadoOConEmpleadoInexistente() {
        assertAll(
                () -> assertThrows(ValidacionException.class, () -> service.guardarHorario(horario(0, DiaSemana.LUNES, "08:00", "12:00"))),
                () -> assertThrows(ValidacionException.class, () -> service.guardarHorario(horario(999, DiaSemana.LUNES, "08:00", "12:00"))));
    }

    @Test
    void modificaYEliminaUnTurno() throws Exception {
        Empleado e = crearEmpleado("1012345678", EstadoEmpleado.ACTIVO);
        HorarioLaboral manana = horario(e.getId(), DiaSemana.LUNES, "08:00", "12:00");
        service.guardarHorario(manana);
        service.guardarHorario(horario(e.getId(), DiaSemana.LUNES, "14:00", "18:00"));

        manana.setHoraFin(LocalTime.parse("13:00"));
        service.guardarHorario(manana);   // se compara consigo mismo (se salta) y con el turno de la tarde
        assertEquals(LocalTime.parse("13:00"), service.listarHorarios().stream()
                .filter(h -> h.getId() == manana.getId()).findFirst().orElseThrow().getHoraFin());

        service.eliminarHorario(manana.getId());
        assertEquals(1, service.listarHorarios().size());
    }
}
