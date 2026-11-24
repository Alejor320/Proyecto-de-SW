package com.concesionario.rrhh.service;

import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.Validar;
import com.concesionario.config.ConexionBD;
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
import com.concesionario.rrhh.repository.RecursosHumanosRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reglas de negocio de Recursos Humanos: CU-RH-01, CU-RH-02, CU-RH-03, CU-RH-04 y CU-RH-05. */
public class RecursosHumanosService {

    private static final Pattern DOCUMENTO = Pattern.compile("\\d{5,15}");
    private static final Pattern TELEFONO = Pattern.compile("\\d{7,15}");
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final RecursosHumanosRepository repository = new RecursosHumanosRepository();

    // =====================================================================
    // CU-RH-01 Gestionar ciclo laboral de empleados
    // =====================================================================

    public List<Empleado> listarEmpleados() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarEmpleados(c);
        }
    }

    public List<Empleado> listarEmpleadosActivos() throws SQLException {
        return listarEmpleados().stream().filter(e -> e.getEstado() == EstadoEmpleado.ACTIVO).toList();
    }

    /** Crea el empleado si es nuevo (id = 0) o lo actualiza si ya existe. */
    public void guardarEmpleado(Empleado e) throws SQLException, ValidacionException {
        validarEmpleado(e);
        try (Connection c = ConexionBD.obtener()) {
            Optional<Empleado> otro = repository.buscarEmpleadoPorDocumento(c, e.getDocumento());
            if (otro.isPresent() && otro.get().getId() != e.getId()) {
                throw new ValidacionException("Ya existe un empleado con el documento " + e.getDocumento() + ".");
            }
            if (e.getId() == 0) {
                repository.insertarEmpleado(c, e);
            } else {
                repository.actualizarEmpleado(c, e);
            }
        }
    }

    public void eliminarEmpleado(int id) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            if (repository.contarRegistrosAsociados(c, id) > 0) {
                throw new ValidacionException("No se puede eliminar el empleado porque tiene contratos, permisos "
                        + "o está registrado como técnico. Cambie su estado a INACTIVO.");
            }
            repository.eliminarEmpleado(c, id);
        }
    }

    private void validarEmpleado(Empleado e) throws ValidacionException {
        Validar.condicion(!Validar.vacio(e.getDocumento()) && DOCUMENTO.matcher(e.getDocumento()).matches(),
                "El documento es obligatorio y debe tener entre 5 y 15 dígitos, sin puntos ni espacios.");
        Validar.texto(e.getNombres(), "Nombres");
        Validar.texto(e.getApellidos(), "Apellidos");
        Validar.texto(e.getCargo(), "Cargo");
        if (!Validar.vacio(e.getTelefono())) {
            Validar.condicion(TELEFONO.matcher(e.getTelefono()).matches(), "El teléfono debe tener entre 7 y 15 dígitos.");
        }
        if (!Validar.vacio(e.getCorreo())) {
            Validar.condicion(CORREO.matcher(e.getCorreo()).matches(), "El correo no tiene un formato válido.");
        }
        Validar.requerido(e.getFechaIngreso(), "Fecha de ingreso");
        Validar.condicion(!e.getFechaIngreso().isAfter(LocalDate.now()), "La fecha de ingreso no puede ser futura.");
        Validar.requerido(e.getEstado(), "Estado");
    }

    // =====================================================================
    // CU-RH-03 Gestionar contratos laborales
    // =====================================================================

    public List<ContratoLaboral> listarContratos() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarContratos(c);
        }
    }

    public void guardarContrato(ContratoLaboral k) throws SQLException, ValidacionException {
        validarContrato(k);
        try (Connection c = ConexionBD.obtener()) {
            Empleado empleado = repository.buscarEmpleadoPorId(c, k.getIdEmpleado())
                    .orElseThrow(() -> new ValidacionException("El empleado seleccionado no existe."));
            if (k.getEstado() == EstadoContrato.VIGENTE) {
                Validar.condicion(empleado.getEstado() == EstadoEmpleado.ACTIVO,
                        "Solo se pueden registrar contratos vigentes para empleados activos.");
                for (ContratoLaboral otro : repository.listarContratosVigentes(c, k.getIdEmpleado())) {
                    if (otro.getId() != k.getId() && contratosSeCruzan(k, otro)) {
                        throw new ValidacionException("El empleado ya tiene un contrato vigente que se cruza con esas fechas "
                                + "(contrato N.º " + otro.getId() + ").");
                    }
                }
            }
            if (k.getId() == 0) {
                repository.insertarContrato(c, k);
            } else {
                repository.actualizarContrato(c, k);
            }
        }
    }

    public void eliminarContrato(int id) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            repository.eliminarContrato(c, id);
        }
    }

    private void validarContrato(ContratoLaboral k) throws ValidacionException {
        Validar.condicion(k.getIdEmpleado() > 0, "Seleccione un empleado.");
        Validar.requerido(k.getTipo(), "Tipo de contrato");
        Validar.requerido(k.getFechaInicio(), "Fecha de inicio");
        if (k.getTipo() != TipoContrato.INDEFINIDO) {
            Validar.condicion(k.getFechaFin() != null, "Los contratos que no son indefinidos deben tener fecha de fin.");
        }
        if (k.getFechaFin() != null) {
            Validar.condicion(k.getFechaFin().isAfter(k.getFechaInicio()),
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        Validar.condicion(k.getSalario() != null && k.getSalario().signum() > 0, "El salario debe ser mayor que cero.");
        Validar.requerido(k.getEstado(), "Estado");
    }

    /** Dos contratos se cruzan si sus periodos comparten al menos un día. Sin fecha de fin = indefinido. */
    static boolean contratosSeCruzan(ContratoLaboral a, ContratoLaboral b) {
        LocalDate finA = a.getFechaFin() == null ? LocalDate.MAX : a.getFechaFin();
        LocalDate finB = b.getFechaFin() == null ? LocalDate.MAX : b.getFechaFin();
        return !a.getFechaInicio().isAfter(finB) && !b.getFechaInicio().isAfter(finA);
    }

    // =====================================================================
    // CU-RH-04 Gestionar permisos laborales
    // =====================================================================

    public List<PermisoLaboral> listarPermisos() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarPermisos(c);
        }
    }

    public void guardarPermiso(PermisoLaboral p) throws SQLException, ValidacionException {
        if (p.getId() == 0) {
            p.setEstado(EstadoPermiso.PENDIENTE);
        }
        validarPermiso(p);
        try (Connection c = ConexionBD.obtener()) {
            Empleado empleado = repository.buscarEmpleadoPorId(c, p.getIdEmpleado())
                    .orElseThrow(() -> new ValidacionException("El empleado seleccionado no existe."));
            if (p.getId() == 0) {
                Validar.condicion(empleado.getEstado() == EstadoEmpleado.ACTIVO,
                        "Solo se pueden registrar permisos para empleados activos.");
                repository.insertarPermiso(c, p);
            } else {
                PermisoLaboral actual = repository.buscarPermisoPorId(c, p.getId())
                        .orElseThrow(() -> new ValidacionException("El permiso seleccionado no existe."));
                Validar.condicion(actual.getIdEmpleado() == p.getIdEmpleado(),
                        "No se puede cambiar el empleado de un permiso ya registrado.");
                repository.actualizarPermiso(c, p);
            }
        }
    }

    public void eliminarPermiso(int id) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            repository.eliminarPermiso(c, id);
        }
    }

    private void validarPermiso(PermisoLaboral p) throws ValidacionException {
        Validar.condicion(p.getIdEmpleado() > 0, "Seleccione un empleado.");
        Validar.requerido(p.getFechaInicio(), "Fecha de inicio");
        Validar.requerido(p.getFechaFin(), "Fecha de fin");
        Validar.condicion(!p.getFechaFin().isBefore(p.getFechaInicio()),
                "La fecha de fin no puede ser anterior a la fecha de inicio.");
        Validar.texto(p.getMotivo(), "Motivo");
        Validar.condicion(p.getMotivo().length() <= 200, "El motivo no puede superar los 200 caracteres.");
        Validar.condicion(p.getSoporte() == null || p.getSoporte().length() <= 255,
                "El soporte no puede superar los 255 caracteres.");
        Validar.requerido(p.getEstado(), "Estado");
    }

    // =====================================================================
    // CU-RH-02 Gestionar asignación de horarios laborales
    // =====================================================================

    public List<HorarioLaboral> listarHorarios() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarHorarios(c);
        }
    }

    public void guardarHorario(HorarioLaboral h) throws SQLException, ValidacionException {
        Validar.condicion(h.getIdEmpleado() > 0, "Seleccione un empleado.");
        Validar.requerido(h.getDia(), "Día");
        Validar.requerido(h.getHoraInicio(), "Hora de inicio");
        Validar.requerido(h.getHoraFin(), "Hora de fin");
        Validar.condicion(h.getHoraFin().isAfter(h.getHoraInicio()), "La hora de fin debe ser posterior a la hora de inicio.");

        try (Connection c = ConexionBD.obtener()) {
            Empleado empleado = repository.buscarEmpleadoPorId(c, h.getIdEmpleado())
                    .orElseThrow(() -> new ValidacionException("El empleado seleccionado no existe."));
            Validar.condicion(empleado.getEstado() == EstadoEmpleado.ACTIVO,
                    "Solo se pueden asignar horarios a empleados activos.");
            for (HorarioLaboral otro : repository.listarHorariosPorEmpleadoYDia(c, h.getIdEmpleado(), h.getDia())) {
                if (otro.getId() != h.getId() && horariosSeCruzan(h, otro)) {
                    throw new ValidacionException("El horario se cruza con otro turno del mismo día ("
                            + otro.getHoraInicio() + " a " + otro.getHoraFin() + ").");
                }
            }
            if (h.getId() == 0) {
                repository.insertarHorario(c, h);
            } else {
                repository.actualizarHorario(c, h);
            }
        }
    }

    public void eliminarHorario(int id) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            repository.eliminarHorario(c, id);
        }
    }

    /** Dos turnos se cruzan si uno empieza antes de que el otro termine. Turnos seguidos (8-12 y 12-17) no se cruzan. */
    static boolean horariosSeCruzan(HorarioLaboral a, HorarioLaboral b) {
        return a.getHoraInicio().isBefore(b.getHoraFin()) && b.getHoraInicio().isBefore(a.getHoraFin());
    }

    // =====================================================================
    // CU-RH-05 Gestionar solicitud y aprobación de vacaciones (No CRUD)
    // =====================================================================

    /** Días hábiles de vacaciones que gana un empleado por cada año trabajado. */
    public static final int DIAS_VACACIONES_POR_ANIO = 15;

    public List<SolicitudVacaciones> listarSolicitudesVacaciones() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarSolicitudes(c);
        }
    }

    /** Saldo disponible: días ganados menos los aprobados y los que están pendientes de aprobación. */
    public int consultarSaldoVacaciones(int idEmpleado) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            Empleado empleado = repository.buscarEmpleadoPorId(c, idEmpleado)
                    .orElseThrow(() -> new ValidacionException("El empleado seleccionado no existe."));
            return saldoDisponible(c, empleado, 0, true);
        }
    }

    /**
     * Pasos 1 a 7 del flujo básico: registra una solicitud nueva (o modifica una pendiente) en estado PENDIENTE,
     * después de validar contrato, horario, días hábiles, saldo y cruces con otras ausencias.
     */
    public void solicitarVacaciones(SolicitudVacaciones v) throws SQLException, ValidacionException {
        Validar.condicion(v.getIdEmpleado() > 0, "Seleccione un empleado.");
        Validar.requerido(v.getFechaInicio(), "Fecha de inicio");
        Validar.requerido(v.getFechaFin(), "Fecha de fin");
        Validar.condicion(!v.getFechaFin().isBefore(v.getFechaInicio()),
                "La fecha de fin no puede ser anterior a la fecha de inicio.");
        Validar.condicion(!v.getFechaInicio().isBefore(LocalDate.now()),
                "Las vacaciones deben solicitarse para fechas desde hoy en adelante.");
        Validar.condicion(v.getObservacion() == null || v.getObservacion().length() <= 200,
                "La observación no puede superar los 200 caracteres.");

        try (Connection c = ConexionBD.obtener()) {
            Empleado empleado = repository.buscarEmpleadoPorId(c, v.getIdEmpleado())
                    .orElseThrow(() -> new ValidacionException("El empleado seleccionado no existe."));
            Validar.condicion(empleado.getEstado() == EstadoEmpleado.ACTIVO,
                    "Solo los empleados activos pueden solicitar vacaciones.");
            if (v.getId() != 0) {
                SolicitudVacaciones actual = repository.buscarSolicitudPorId(c, v.getId())
                        .orElseThrow(() -> new ValidacionException("La solicitud seleccionada no existe."));
                Validar.condicion(actual.getEstado() == EstadoVacaciones.PENDIENTE,
                        "Solo se pueden modificar solicitudes en estado PENDIENTE.");
                Validar.condicion(actual.getIdEmpleado() == v.getIdEmpleado(),
                        "No se puede cambiar el empleado de una solicitud ya registrada.");
            }
            Validar.condicion(tieneContratoVigente(c, v.getIdEmpleado(), v.getFechaInicio(), v.getFechaFin()),
                    "El empleado no tiene un contrato vigente que cubra las fechas solicitadas.");

            int dias = diasHabiles(c, v.getIdEmpleado(), v.getFechaInicio(), v.getFechaFin());
            v.setDiasHabiles(dias);

            int saldo = saldoDisponible(c, empleado, v.getId(), true);
            if (dias > saldo) {
                throw new ValidacionException("No dispone de suficientes días de vacaciones para el período solicitado "
                        + "(saldo: " + saldo + " días, solicitados: " + dias + ").");
            }
            validarSinCruces(c, v);

            v.setEstado(EstadoVacaciones.PENDIENTE);
            v.setIdAprobador(null);
            v.setFechaDecision(null);
            v.setMotivoRechazo(null);
            if (v.getId() == 0) {
                repository.insertarSolicitud(c, v);
            } else {
                repository.actualizarSolicitud(c, v);
            }
        }
    }

    /**
     * Pasos 8 y 9: el jefe inmediato aprueba la solicitud. Se vuelven a revisar contrato y saldo dentro de una
     * transacción; si algo falla no queda ningún cambio (excepciones E1 y E2 del caso de uso).
     */
    public void aprobarVacaciones(int idSolicitud, int idJefe) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                SolicitudVacaciones v = solicitudPendiente(c, idSolicitud);
                validarJefe(c, v, idJefe);
                Validar.condicion(tieneContratoVigente(c, v.getIdEmpleado(), v.getFechaInicio(), v.getFechaFin()),
                        "La solicitud no puede ser aprobada porque el contrato del empleado no se encuentra vigente.");
                Empleado empleado = repository.buscarEmpleadoPorId(c, v.getIdEmpleado()).orElseThrow();
                int saldo = saldoDisponible(c, empleado, v.getId(), false);
                Validar.condicion(v.getDiasHabiles() <= saldo,
                        "El empleado ya no tiene saldo suficiente para aprobar esta solicitud (saldo: " + saldo + " días).");

                v.setEstado(EstadoVacaciones.APROBADA);
                v.setIdAprobador(idJefe);
                v.setFechaDecision(LocalDate.now());
                v.setMotivoRechazo(null);
                repository.actualizarSolicitud(c, v);
                c.commit();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Flujo alternativo 8A: el jefe rechaza la solicitud con un motivo; el saldo no cambia. */
    public void rechazarVacaciones(int idSolicitud, int idJefe, String motivo) throws SQLException, ValidacionException {
        Validar.texto(motivo, "Motivo del rechazo");
        Validar.condicion(motivo.length() <= 200, "El motivo del rechazo no puede superar los 200 caracteres.");
        try (Connection c = ConexionBD.obtener()) {
            SolicitudVacaciones v = solicitudPendiente(c, idSolicitud);
            validarJefe(c, v, idJefe);
            v.setEstado(EstadoVacaciones.RECHAZADA);
            v.setIdAprobador(idJefe);
            v.setFechaDecision(LocalDate.now());
            v.setMotivoRechazo(motivo.trim());
            repository.actualizarSolicitud(c, v);
        }
    }

    /** Solo se pueden cancelar (eliminar) solicitudes que siguen pendientes. */
    public void cancelarSolicitudVacaciones(int idSolicitud) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            solicitudPendiente(c, idSolicitud);
            repository.eliminarSolicitud(c, idSolicitud);
        }
    }

    private SolicitudVacaciones solicitudPendiente(Connection c, int idSolicitud) throws SQLException, ValidacionException {
        SolicitudVacaciones v = repository.buscarSolicitudPorId(c, idSolicitud)
                .orElseThrow(() -> new ValidacionException("La solicitud seleccionada no existe."));
        Validar.condicion(v.getEstado() == EstadoVacaciones.PENDIENTE,
                "La solicitud ya fue " + v.getEstado().name().toLowerCase() + " y no se puede cambiar.");
        return v;
    }

    private void validarJefe(Connection c, SolicitudVacaciones v, int idJefe) throws SQLException, ValidacionException {
        Validar.condicion(idJefe > 0, "Seleccione el jefe inmediato que decide la solicitud.");
        Validar.condicion(idJefe != v.getIdEmpleado(), "Un empleado no puede decidir sobre sus propias vacaciones.");
        Empleado jefe = repository.buscarEmpleadoPorId(c, idJefe)
                .orElseThrow(() -> new ValidacionException("El jefe seleccionado no existe."));
        Validar.condicion(jefe.getEstado() == EstadoEmpleado.ACTIVO, "El jefe que decide debe ser un empleado activo.");
    }

    /** Hay un contrato VIGENTE cuyo período cubre todas las fechas solicitadas. */
    private boolean tieneContratoVigente(Connection c, int idEmpleado, LocalDate inicio, LocalDate fin) throws SQLException {
        for (ContratoLaboral k : repository.listarContratosVigentes(c, idEmpleado)) {
            boolean empiezaAntes = !k.getFechaInicio().isAfter(inicio);
            boolean terminaDespues = k.getFechaFin() == null || !k.getFechaFin().isBefore(fin);
            if (empiezaAntes && terminaDespues) {
                return true;
            }
        }
        return false;
    }

    /** Días del período que coinciden con los días de la semana en que el empleado tiene horario. */
    private int diasHabiles(Connection c, int idEmpleado, LocalDate inicio, LocalDate fin)
            throws SQLException, ValidacionException {
        List<HorarioLaboral> horarios = repository.listarHorariosPorEmpleado(c, idEmpleado);
        Validar.condicion(!horarios.isEmpty(),
                "El empleado no tiene un horario laboral asignado; asígnelo antes de solicitar vacaciones.");
        Set<DiaSemana> diasLaborales = EnumSet.noneOf(DiaSemana.class);
        horarios.forEach(h -> diasLaborales.add(h.getDia()));
        int dias = contarDiasLaborales(inicio, fin, diasLaborales);
        Validar.condicion(dias > 0, "El período solicitado no incluye días laborales del empleado.");
        return dias;
    }

    static int contarDiasLaborales(LocalDate inicio, LocalDate fin, Set<DiaSemana> diasLaborales) {
        int dias = 0;
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            if (diasLaborales.contains(DiaSemana.values()[d.getDayOfWeek().getValue() - 1])) {
                dias++;
            }
        }
        return dias;
    }

    /** 15 días hábiles por año trabajado, proporcional a los días desde el ingreso. */
    static int diasGanados(LocalDate fechaIngreso, LocalDate hoy) {
        long trabajados = Math.max(0, ChronoUnit.DAYS.between(fechaIngreso, hoy));
        return (int) (trabajados * DIAS_VACACIONES_POR_ANIO / 365);
    }

    private int saldoDisponible(Connection c, Empleado e, int excluirId, boolean contarPendientes) throws SQLException {
        int saldo = diasGanados(e.getFechaIngreso(), LocalDate.now())
                - repository.sumarDiasVacaciones(c, e.getId(), EstadoVacaciones.APROBADA, excluirId);
        if (contarPendientes) {
            saldo -= repository.sumarDiasVacaciones(c, e.getId(), EstadoVacaciones.PENDIENTE, excluirId);
        }
        return Math.max(0, saldo);
    }

    /** Flujo alternativo 6A: no puede haber otra solicitud activa ni un permiso en las mismas fechas. */
    private void validarSinCruces(Connection c, SolicitudVacaciones v) throws SQLException, ValidacionException {
        for (SolicitudVacaciones otra : repository.listarSolicitudesPorEmpleado(c, v.getIdEmpleado())) {
            if (otra.getId() != v.getId() && otra.getEstado() != EstadoVacaciones.RECHAZADA
                    && periodosSeCruzan(v.getFechaInicio(), v.getFechaFin(), otra.getFechaInicio(), otra.getFechaFin())) {
                throw new ValidacionException("Existe una ausencia registrada que coincide con el período solicitado "
                        + "(vacaciones del " + otra.getFechaInicio() + " al " + otra.getFechaFin() + ").");
            }
        }
        for (PermisoLaboral p : repository.listarPermisosPorEmpleado(c, v.getIdEmpleado())) {
            if (p.getEstado() != EstadoPermiso.RECHAZADO
                    && periodosSeCruzan(v.getFechaInicio(), v.getFechaFin(), p.getFechaInicio(), p.getFechaFin())) {
                throw new ValidacionException("Existe una ausencia registrada que coincide con el período solicitado "
                        + "(permiso del " + p.getFechaInicio() + " al " + p.getFechaFin() + ").");
            }
        }
    }

    static boolean periodosSeCruzan(LocalDate inicioA, LocalDate finA, LocalDate inicioB, LocalDate finB) {
        return !inicioA.isAfter(finB) && !inicioB.isAfter(finA);
    }
}
