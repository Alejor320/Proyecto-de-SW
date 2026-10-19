package com.concesionario.rrhh.service;

import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.Validar;
import com.concesionario.config.ConexionBD;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.model.TipoContrato;
import com.concesionario.rrhh.repository.RecursosHumanosRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reglas de negocio de Recursos Humanos: CU-RH-01, CU-RH-02 y CU-RH-03. */
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
                throw new ValidacionException("No se puede eliminar el empleado porque tiene contratos, horarios "
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
}
