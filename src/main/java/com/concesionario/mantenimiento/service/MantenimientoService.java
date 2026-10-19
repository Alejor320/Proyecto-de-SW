package com.concesionario.mantenimiento.service;

import com.concesionario.apoyo.model.EstadoVehiculo;
import com.concesionario.apoyo.model.TipoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.apoyo.repository.VehiculoRepository;
import com.concesionario.comun.ValidacionException;
import com.concesionario.comun.Validar;
import com.concesionario.config.ConexionBD;
import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.repository.MantenimientoRepository;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.repository.RecursosHumanosRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de Mantenimiento y Flota: CU-MF-01, CU-MF-02, CU-MF-03
 * y el caso de apoyo CU-AP-01 (registro mínimo de vehículos).
 */
public class MantenimientoService {

    private static final Pattern PLACA_AUTOMOVIL = Pattern.compile("[A-Z]{3}\\d{3}");
    private static final Pattern PLACA_MOTOCICLETA = Pattern.compile("[A-Z]{3}\\d{2}[A-Z]");

    private final MantenimientoRepository repository = new MantenimientoRepository();
    private final VehiculoRepository vehiculoRepository = new VehiculoRepository();
    private final RecursosHumanosRepository rrhhRepository = new RecursosHumanosRepository();

    // =====================================================================
    // CU-AP-01 Registrar y consultar vehículos (versión mínima de apoyo)
    // =====================================================================

    public List<Vehiculo> listarVehiculos() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return vehiculoRepository.listar(c);
        }
    }

    public void registrarVehiculo(Vehiculo v) throws SQLException, ValidacionException {
        Validar.condicion(v.getId() == 0, "El registro mínimo de vehículos solo permite registrar vehículos nuevos.");
        Validar.texto(v.getPlaca(), "Placa");
        v.setPlaca(v.getPlaca().toUpperCase().replace("-", "").replace(" ", ""));
        Validar.requerido(v.getTipo(), "Tipo");
        if (v.getTipo() == TipoVehiculo.AUTOMOVIL) {
            Validar.condicion(PLACA_AUTOMOVIL.matcher(v.getPlaca()).matches(),
                    "La placa de un automóvil debe tener 3 letras y 3 números, por ejemplo ABC123.");
        } else {
            Validar.condicion(PLACA_MOTOCICLETA.matcher(v.getPlaca()).matches(),
                    "La placa de una motocicleta debe tener 3 letras, 2 números y 1 letra, por ejemplo ABC12D.");
        }
        Validar.texto(v.getMarca(), "Marca");
        Validar.texto(v.getModelo(), "Modelo");
        int maximo = Year.now().getValue() + 1;
        Validar.condicion(v.getAnio() >= 1950 && v.getAnio() <= maximo, "El año debe estar entre 1950 y " + maximo + ".");

        try (Connection c = ConexionBD.obtener()) {
            Validar.condicion(vehiculoRepository.buscarPorPlaca(c, v.getPlaca()).isEmpty(),
                    "Ya existe un vehículo con la placa " + v.getPlaca() + ".");
            v.setEstado(EstadoVehiculo.DISPONIBLE);
            vehiculoRepository.insertar(c, v);
        }
    }

    // =====================================================================
    // CU-MF-03 Gestionar técnicos de mantenimiento
    // Todo técnico debe ser un empleado activo de Recursos Humanos.
    // =====================================================================

    public List<Tecnico> listarTecnicos() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarTecnicos(c);
        }
    }

    public List<Tecnico> listarTecnicosDisponibles() throws SQLException {
        return listarTecnicos().stream().filter(Tecnico::isDisponible).toList();
    }

    public void guardarTecnico(Tecnico t) throws SQLException, ValidacionException {
        Validar.condicion(t.getIdEmpleado() > 0, "Seleccione el empleado que será técnico.");
        Validar.texto(t.getEspecialidad(), "Especialidad");

        try (Connection c = ConexionBD.obtener()) {
            Empleado empleado = rrhhRepository.buscarEmpleadoPorId(c, t.getIdEmpleado())
                    .orElseThrow(() -> new ValidacionException("El empleado seleccionado no existe."));
            Validar.condicion(empleado.getEstado() == EstadoEmpleado.ACTIVO,
                    "Solo un empleado activo puede registrarse como técnico.");
            Optional<Tecnico> otro = repository.buscarTecnicoPorEmpleado(c, t.getIdEmpleado());
            if (otro.isPresent() && otro.get().getId() != t.getId()) {
                throw new ValidacionException("El empleado " + empleado.getNombreCompleto() + " ya está registrado como técnico.");
            }
            if (t.getId() == 0) {
                repository.insertarTecnico(c, t);
            } else {
                repository.actualizarTecnico(c, t);
            }
        }
    }

    public void eliminarTecnico(int id) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            if (repository.contarOrdenesDeTecnico(c, id) > 0) {
                throw new ValidacionException("No se puede eliminar el técnico porque tiene órdenes de mantenimiento "
                        + "asociadas. Márquelo como no disponible.");
            }
            repository.eliminarTecnico(c, id);
        }
    }

    // =====================================================================
    // CU-MF-02 Gestionar repuestos de taller
    // =====================================================================

    public List<Repuesto> listarRepuestos() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarRepuestos(c);
        }
    }

    /** Repuestos cuya cantidad llegó al stock mínimo o menos. */
    public List<Repuesto> listarRepuestosParaReabastecer() throws SQLException {
        return listarRepuestos().stream().filter(Repuesto::requiereReabastecimiento).toList();
    }

    public void guardarRepuesto(Repuesto r) throws SQLException, ValidacionException {
        Validar.texto(r.getCodigo(), "Código");
        r.setCodigo(r.getCodigo().trim().toUpperCase());
        Validar.texto(r.getNombre(), "Nombre");
        Validar.condicion(r.getCantidad() >= 0, "La cantidad no puede ser negativa.");
        Validar.condicion(r.getStockMinimo() >= 0, "El stock mínimo no puede ser negativo.");
        Validar.condicion(r.getPrecio() != null && r.getPrecio().signum() > 0, "El precio debe ser mayor que cero.");

        try (Connection c = ConexionBD.obtener()) {
            Optional<Repuesto> otro = repository.buscarRepuestoPorCodigo(c, r.getCodigo());
            if (otro.isPresent() && otro.get().getId() != r.getId()) {
                throw new ValidacionException("Ya existe un repuesto con el código " + r.getCodigo() + ".");
            }
            if (r.getId() == 0) {
                repository.insertarRepuesto(c, r);
            } else {
                repository.actualizarRepuesto(c, r);
            }
        }
    }

    public void eliminarRepuesto(int id) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            repository.eliminarRepuesto(c, id);
        }
    }

    // =====================================================================
    // CU-MF-01 Gestionar órdenes de mantenimiento
    // Abrir, cerrar o eliminar una orden también cambia el estado del vehículo,
    // por eso esas operaciones van en una sola transacción (RNF-04).
    // =====================================================================

    public List<OrdenMantenimiento> listarOrdenes() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarOrdenes(c);
        }
    }

    public void guardarOrden(OrdenMantenimiento o) throws SQLException, ValidacionException {
        if (o.getId() == 0) {
            abrirOrden(o);
        } else {
            actualizarOrden(o);
        }
    }

    /** Abre una orden nueva y deja el vehículo EN_MANTENIMIENTO. */
    public void abrirOrden(OrdenMantenimiento o) throws SQLException, ValidacionException {
        Validar.condicion(o.getIdVehiculo() > 0, "Seleccione un vehículo.");
        validarOrden(o);
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                Vehiculo v = vehiculoRepository.buscarPorId(c, o.getIdVehiculo())
                        .orElseThrow(() -> new ValidacionException("El vehículo seleccionado no existe."));
                Validar.condicion(v.getEstado() == EstadoVehiculo.DISPONIBLE,
                        "El vehículo " + v.getPlaca() + " no está disponible (estado actual: " + v.getEstado() + ").");
                validarTecnicoDisponible(c, o.getIdTecnico());

                o.setFechaApertura(LocalDate.now());
                o.setFechaCierre(null);
                o.setEstado(EstadoOrden.ABIERTA);
                repository.insertarOrden(c, o);
                vehiculoRepository.actualizarEstado(c, v.getId(), EstadoVehiculo.EN_MANTENIMIENTO);
                c.commit();
            } catch (SQLException | ValidacionException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Modifica una orden existente. Si pasa a CERRADA, el vehículo vuelve a quedar DISPONIBLE. */
    public void actualizarOrden(OrdenMantenimiento o) throws SQLException, ValidacionException {
        validarOrden(o);
        Validar.requerido(o.getEstado(), "Estado");
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                OrdenMantenimiento actual = repository.buscarOrdenPorId(c, o.getId())
                        .orElseThrow(() -> new ValidacionException("La orden seleccionada no existe."));
                Validar.condicion(actual.getEstado() != EstadoOrden.CERRADA,
                        "La orden N.º " + actual.getId() + " ya está cerrada y no se puede modificar.");
                if (o.getIdTecnico() != actual.getIdTecnico()) {
                    validarTecnicoDisponible(c, o.getIdTecnico());
                }
                // El vehículo y la fecha de apertura no cambian después de abrir la orden.
                o.setIdVehiculo(actual.getIdVehiculo());
                o.setFechaApertura(actual.getFechaApertura());

                if (o.getEstado() == EstadoOrden.CERRADA) {
                    o.setFechaCierre(LocalDate.now());
                    vehiculoRepository.actualizarEstado(c, actual.getIdVehiculo(), EstadoVehiculo.DISPONIBLE);
                } else {
                    o.setFechaCierre(null);
                }
                repository.actualizarOrden(c, o);
                c.commit();
            } catch (SQLException | ValidacionException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Solo se eliminan órdenes recién abiertas (sin trabajo hecho). El vehículo vuelve a DISPONIBLE. */
    public void eliminarOrden(int id) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                OrdenMantenimiento actual = repository.buscarOrdenPorId(c, id)
                        .orElseThrow(() -> new ValidacionException("La orden seleccionada no existe."));
                Validar.condicion(actual.getEstado() == EstadoOrden.ABIERTA,
                        "Solo se pueden eliminar órdenes en estado ABIERTA. Si ya se trabajó en ella, ciérrela.");
                repository.eliminarOrden(c, id);
                vehiculoRepository.actualizarEstado(c, actual.getIdVehiculo(), EstadoVehiculo.DISPONIBLE);
                c.commit();
            } catch (SQLException | ValidacionException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private void validarOrden(OrdenMantenimiento o) throws ValidacionException {
        Validar.condicion(o.getIdTecnico() > 0, "Seleccione un técnico.");
        Validar.texto(o.getMotivo(), "Motivo");
        Validar.condicion(o.getMotivo().length() <= 200, "El motivo no puede superar los 200 caracteres.");
        Validar.condicion(o.getKilometraje() >= 0, "El kilometraje no puede ser negativo.");
        Validar.requerido(o.getPrioridad(), "Prioridad");
    }

    private void validarTecnicoDisponible(Connection c, int idTecnico) throws SQLException, ValidacionException {
        Tecnico t = repository.buscarTecnicoPorId(c, idTecnico)
                .orElseThrow(() -> new ValidacionException("El técnico seleccionado no existe."));
        Validar.condicion(t.isDisponible(), "El técnico " + t.getNombreEmpleado() + " no está disponible.");
    }
}
