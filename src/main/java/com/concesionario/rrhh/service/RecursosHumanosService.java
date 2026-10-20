package com.concesionario.rrhh.service;

import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.Validar;
import com.concesionario.config.ConexionBD;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.EstadoPermiso;
import com.concesionario.rrhh.model.PermisoLaboral;
import com.concesionario.rrhh.model.TipoContrato;
import com.concesionario.rrhh.repository.RecursosHumanosRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reglas de negocio de Recursos Humanos: CU-RH-01, CU-RH-03 y CU-RH-04. */
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
}
