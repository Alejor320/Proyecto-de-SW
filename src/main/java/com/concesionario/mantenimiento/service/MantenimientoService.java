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
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.RepuestoOrden;
import com.concesionario.mantenimiento.model.ResultadoDiagnostico;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.repository.MantenimientoRepository;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.repository.RecursosHumanosRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de Mantenimiento y Flota: CU-MF-01 a CU-MF-05
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

    public void eliminarRepuesto(int id) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            Validar.condicion(repository.contarUsosDeRepuesto(c, id) == 0,
                    "No se puede eliminar el repuesto porque está asociado a órdenes de mantenimiento.");
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
            } catch (SQLException | ValidacionException | RuntimeException ex) {
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
                // EN_PROCESO y PENDIENTE_REPUESTO solo los asigna el diagnóstico (CU-MF-05).
                boolean estadoDeDiagnostico = o.getEstado() == EstadoOrden.EN_PROCESO
                        || o.getEstado() == EstadoOrden.PENDIENTE_REPUESTO;
                Validar.condicion(o.getEstado() == actual.getEstado() || !estadoDeDiagnostico,
                        "Los estados EN_PROCESO y PENDIENTE_REPUESTO los asigna el diagnóstico del vehículo (CU-MF-05).");
                // El vehículo y la fecha de apertura no cambian después de abrir la orden.
                o.setIdVehiculo(actual.getIdVehiculo());
                o.setFechaApertura(actual.getFechaApertura());

                if (o.getEstado() == EstadoOrden.CERRADA) {
                    o.setFechaCierre(LocalDate.now());
                    repository.eliminarRepuestosPendientes(c, o.getId());
                    vehiculoRepository.actualizarEstado(c, actual.getIdVehiculo(), EstadoVehiculo.DISPONIBLE);
                } else {
                    o.setFechaCierre(null);
                }
                repository.actualizarOrden(c, o);
                if (o.getEstado() == EstadoOrden.CERRADA) {
                    registrarCierreEnHistorial(c, repository.buscarOrdenPorId(c, o.getId()).orElseThrow(), "Cierre de la orden");
                }
                c.commit();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
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
                Validar.condicion(repository.contarRepuestosDescontados(c, id) == 0,
                        "La orden ya tiene repuestos usados en una reparación; no se puede eliminar, ciérrela.");
                repository.eliminarRepuestosPendientes(c, id);
                repository.eliminarOrden(c, id);
                vehiculoRepository.actualizarEstado(c, actual.getIdVehiculo(), EstadoVehiculo.DISPONIBLE);
                c.commit();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
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

    // =====================================================================
    // CU-MF-04 Gestionar historial de mantenimiento del vehículo
    // =====================================================================

    public List<RegistroHistorial> listarHistorial() throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarHistorial(c);
        }
    }

    public List<RegistroHistorial> listarHistorialDeVehiculo(int idVehiculo) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarHistorialPorVehiculo(c, idVehiculo);
        }
    }

    /** Crea o modifica un registro manual del historial (por ejemplo, un servicio hecho fuera del taller). */
    public void guardarRegistroHistorial(RegistroHistorial h) throws SQLException, ValidacionException {
        Validar.condicion(h.getIdVehiculo() > 0, "Seleccione un vehículo.");
        Validar.requerido(h.getFecha(), "Fecha");
        Validar.condicion(!h.getFecha().isAfter(LocalDate.now()), "La fecha del servicio no puede ser futura.");
        Validar.condicion(h.getKilometraje() >= 0, "El kilometraje no puede ser negativo.");
        Validar.texto(h.getDescripcion(), "Descripción del servicio");
        Validar.condicion(h.getDescripcion().length() <= 300, "La descripción no puede superar los 300 caracteres.");
        Validar.condicion(h.getCosto() != null && h.getCosto().signum() >= 0, "El costo no puede ser negativo.");

        try (Connection c = ConexionBD.obtener()) {
            vehiculoRepository.buscarPorId(c, h.getIdVehiculo())
                    .orElseThrow(() -> new ValidacionException("El vehículo seleccionado no existe."));
            if (h.getId() != 0) {
                RegistroHistorial actual = repository.buscarHistorialPorId(c, h.getId())
                        .orElseThrow(() -> new ValidacionException("El registro seleccionado no existe."));
                Validar.condicion(!actual.esAutomatico(),
                        "Los registros creados al cerrar una orden no se pueden modificar.");
            }
            h.setIdOrden(null);
            validarKilometrajeCoherente(c, h);
            if (h.getId() == 0) {
                repository.insertarHistorial(c, h);
            } else {
                repository.actualizarHistorial(c, h);
            }
        }
    }

    public void eliminarRegistroHistorial(int id) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            RegistroHistorial actual = repository.buscarHistorialPorId(c, id)
                    .orElseThrow(() -> new ValidacionException("El registro seleccionado no existe."));
            Validar.condicion(!actual.esAutomatico(),
                    "Los registros creados al cerrar una orden no se pueden eliminar.");
            repository.eliminarHistorial(c, id);
        }
    }

    /** El kilometraje no puede bajar respecto a un servicio anterior ni superar a uno posterior del mismo vehículo. */
    private void validarKilometrajeCoherente(Connection c, RegistroHistorial h) throws SQLException, ValidacionException {
        for (RegistroHistorial otro : repository.listarHistorialPorVehiculo(c, h.getIdVehiculo())) {
            if (otro.getId() == h.getId()) {
                continue;
            }
            if (otro.getFecha().isBefore(h.getFecha()) && otro.getKilometraje() > h.getKilometraje()) {
                throw new ValidacionException("El kilometraje es menor que el de un servicio anterior ("
                        + otro.getKilometraje() + " km el " + otro.getFecha() + ").");
            }
            if (otro.getFecha().isAfter(h.getFecha()) && otro.getKilometraje() < h.getKilometraje()) {
                throw new ValidacionException("El kilometraje es mayor que el de un servicio posterior ("
                        + otro.getKilometraje() + " km el " + otro.getFecha() + ").");
            }
        }
    }

    // =====================================================================
    // CU-MF-05 Diagnosticar y reparar vehículo (No CRUD)
    // Cada operación es una transacción: o se guardan todos sus cambios, o ninguno (RNF-04).
    // =====================================================================

    /** Tarifa por hora de mano de obra del taller, usada para el costo que queda en el historial. */
    public static final BigDecimal TARIFA_HORA_MANO_OBRA = new BigDecimal("45000");

    /** Órdenes que todavía no se han cerrado, es decir, las que el técnico puede diagnosticar o reparar. */
    public List<OrdenMantenimiento> listarOrdenesActivas() throws SQLException {
        return listarOrdenes().stream().filter(o -> o.getEstado() != EstadoOrden.CERRADA).toList();
    }

    public List<RepuestoOrden> listarRepuestosDeOrden(int idOrden) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            return repository.listarRepuestosDeOrden(c, idOrden);
        }
    }

    /**
     * Pasos 3 a 6 del flujo básico: registra el diagnóstico, los repuestos y la mano de obra.
     * Si hay inventario para todos los repuestos, se descuentan y la orden pasa a EN_PROCESO;
     * si falta alguno, no se descuenta nada y la orden queda PENDIENTE_REPUESTO (flujo 6A).
     * Un daño severo marca la orden como prioridad CRITICA (flujo 3A).
     */
    public ResultadoDiagnostico registrarDiagnostico(int idOrden, String diagnostico, boolean danoSevero,
                                                     BigDecimal horasManoObra, List<RepuestoOrden> repuestos)
            throws SQLException, ValidacionException {
        Validar.texto(diagnostico, "Diagnóstico");
        Validar.condicion(diagnostico.length() <= 500, "El diagnóstico no puede superar los 500 caracteres.");
        Validar.condicion(horasManoObra != null && horasManoObra.signum() >= 0
                && horasManoObra.compareTo(new BigDecimal("200")) <= 0,
                "Las horas de mano de obra deben estar entre 0 y 200.");
        Set<Integer> vistos = new HashSet<>();
        for (RepuestoOrden r : repuestos) {
            Validar.condicion(r.getCantidad() > 0, "La cantidad de cada repuesto debe ser mayor que cero.");
            Validar.condicion(vistos.add(r.getIdRepuesto()), "Un repuesto aparece repetido en la lista; únalo en una sola línea.");
        }

        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                OrdenMantenimiento orden = ordenNoCerrada(c, idOrden);
                Validar.condicion(orden.getEstado() == EstadoOrden.ABIERTA || orden.getEstado() == EstadoOrden.PENDIENTE_REPUESTO,
                        "La orden está EN_PROCESO; registre el control de calidad antes de volver a diagnosticar.");

                repository.eliminarRepuestosPendientes(c, idOrden);
                for (RepuestoOrden r : repuestos) {
                    repository.buscarRepuestoPorId(c, r.getIdRepuesto())
                            .orElseThrow(() -> new ValidacionException("Uno de los repuestos seleccionados no existe."));
                    RepuestoOrden fila = new RepuestoOrden(r.getIdRepuesto(), r.getCantidad());
                    fila.setIdOrden(idOrden);
                    fila.setDescontado(false);
                    repository.insertarRepuestoOrden(c, fila);
                }
                Prioridad prioridad = danoSevero ? Prioridad.CRITICA : orden.getPrioridad();
                List<String> faltantes = tomarRepuestosDelInventario(c, idOrden);
                EstadoOrden estado = faltantes.isEmpty() ? EstadoOrden.EN_PROCESO : EstadoOrden.PENDIENTE_REPUESTO;
                repository.actualizarDiagnostico(c, idOrden, diagnostico.trim(), horasManoObra, prioridad, estado);
                c.commit();
                return new ResultadoDiagnostico(estado, faltantes);
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Flujo 6A: cuando llega inventario, vuelve a intentar tomar los repuestos que faltaban. */
    public ResultadoDiagnostico reintentarRepuestos(int idOrden) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                OrdenMantenimiento orden = ordenNoCerrada(c, idOrden);
                Validar.condicion(orden.getEstado() == EstadoOrden.PENDIENTE_REPUESTO,
                        "La orden no está esperando repuestos.");
                List<String> faltantes = tomarRepuestosDelInventario(c, idOrden);
                EstadoOrden estado = faltantes.isEmpty() ? EstadoOrden.EN_PROCESO : EstadoOrden.PENDIENTE_REPUESTO;
                repository.actualizarEstadoOrden(c, idOrden, estado, null, orden.getObservacionCalidad());
                c.commit();
                return new ResultadoDiagnostico(estado, faltantes);
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /**
     * Pasos 8 a 10: el control de calidad se aprueba. La orden queda CERRADA, el vehículo DISPONIBLE
     * y se agrega el servicio al historial del vehículo con su costo.
     */
    public void aprobarControlCalidad(int idOrden) throws SQLException, ValidacionException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                OrdenMantenimiento orden = ordenEnReparacion(c, idOrden);
                repository.actualizarEstadoOrden(c, idOrden, EstadoOrden.CERRADA, LocalDate.now(), null);
                vehiculoRepository.actualizarEstado(c, orden.getIdVehiculo(), EstadoVehiculo.DISPONIBLE);
                registrarCierreEnHistorial(c, repository.buscarOrdenPorId(c, idOrden).orElseThrow(),
                        "Reparación con control de calidad aprobado");
                c.commit();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Flujo 8A: el control de calidad se rechaza; la orden vuelve a diagnóstico y el vehículo sigue en el taller. */
    public void rechazarControlCalidad(int idOrden, String observacion) throws SQLException, ValidacionException {
        Validar.texto(observacion, "Observación del control de calidad");
        Validar.condicion(observacion.length() <= 300, "La observación no puede superar los 300 caracteres.");
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                ordenEnReparacion(c, idOrden);
                repository.actualizarEstadoOrden(c, idOrden, EstadoOrden.ABIERTA, null, observacion.trim());
                c.commit();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /**
     * Excepción E2: la reparación no es viable. La orden se cierra, el vehículo queda NO_DISPONIBLE
     * y el historial deja constancia de que se requiere autorización para darlo de baja.
     */
    public void declararIrreparable(int idOrden, String observacion) throws SQLException, ValidacionException {
        Validar.texto(observacion, "Observación");
        Validar.condicion(observacion.length() <= 300, "La observación no puede superar los 300 caracteres.");
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                OrdenMantenimiento orden = ordenNoCerrada(c, idOrden);
                Validar.condicion(orden.getDiagnostico() != null,
                        "Registre primero el diagnóstico que justifica que el vehículo no se puede reparar.");
                repository.eliminarRepuestosPendientes(c, idOrden);
                repository.actualizarEstadoOrden(c, idOrden, EstadoOrden.CERRADA, LocalDate.now(), observacion.trim());
                vehiculoRepository.actualizarEstado(c, orden.getIdVehiculo(), EstadoVehiculo.NO_DISPONIBLE);
                registrarCierreEnHistorial(c, repository.buscarOrdenPorId(c, idOrden).orElseThrow(),
                        "Daño irreparable, requiere autorización para dar de baja el vehículo");
                c.commit();
            } catch (SQLException | ValidacionException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Si hay inventario para todos los repuestos pendientes de la orden, los descuenta; si no, devuelve los que faltan. */
    private List<String> tomarRepuestosDelInventario(Connection c, int idOrden) throws SQLException {
        List<String> faltantes = new ArrayList<>();
        List<RepuestoOrden> pendientes = repository.listarRepuestosDeOrden(c, idOrden).stream()
                .filter(r -> !r.isDescontado()).toList();
        for (RepuestoOrden r : pendientes) {
            Repuesto inventario = repository.buscarRepuestoPorId(c, r.getIdRepuesto()).orElseThrow();
            if (inventario.getCantidad() < r.getCantidad()) {
                faltantes.add(inventario.getCodigo() + " " + inventario.getNombre()
                        + " (faltan " + (r.getCantidad() - inventario.getCantidad()) + ")");
            }
        }
        if (faltantes.isEmpty()) {
            for (RepuestoOrden r : pendientes) {
                repository.ajustarCantidadRepuesto(c, r.getIdRepuesto(), -r.getCantidad());
            }
            repository.marcarRepuestosDescontados(c, idOrden);
        }
        return faltantes;
    }

    private OrdenMantenimiento ordenNoCerrada(Connection c, int idOrden) throws SQLException, ValidacionException {
        OrdenMantenimiento orden = repository.buscarOrdenPorId(c, idOrden)
                .orElseThrow(() -> new ValidacionException("La orden seleccionada no existe."));
        Validar.condicion(orden.getEstado() != EstadoOrden.CERRADA,
                "La orden N.º " + idOrden + " ya está cerrada.");
        return orden;
    }

    /** El control de calidad solo aplica a órdenes diagnosticadas y con los repuestos ya tomados del inventario. */
    private OrdenMantenimiento ordenEnReparacion(Connection c, int idOrden) throws SQLException, ValidacionException {
        OrdenMantenimiento orden = ordenNoCerrada(c, idOrden);
        Validar.condicion(orden.getEstado() != EstadoOrden.PENDIENTE_REPUESTO,
                "La orden está esperando repuestos; no se puede hacer el control de calidad todavía.");
        Validar.condicion(orden.getEstado() == EstadoOrden.EN_PROCESO,
                "Registre primero el diagnóstico y la reparación del vehículo.");
        return orden;
    }

    /**
     * Agrega al historial del vehículo el servicio de una orden cerrada, con el costo de repuestos y mano de obra.
     * Se llama después de quitar los repuestos pendientes, así que todos los que quedan ya salieron del inventario.
     * Las horas nunca son nulas: la columna horas_mano_obra es NOT NULL con valor por defecto 0.
     */
    private void registrarCierreEnHistorial(Connection c, OrdenMantenimiento orden, String resultado) throws SQLException {
        BigDecimal costo = orden.getHorasManoObra().multiply(TARIFA_HORA_MANO_OBRA);
        for (RepuestoOrden r : repository.listarRepuestosDeOrden(c, orden.getId())) {
            costo = costo.add(r.getPrecioUnitario().multiply(BigDecimal.valueOf(r.getCantidad())));
        }
        String descripcion = "Orden N.º " + orden.getId() + ": " + orden.getMotivo() + ". " + resultado + "."
                + (orden.getDiagnostico() == null ? "" : " Diagnóstico: " + orden.getDiagnostico());
        RegistroHistorial h = new RegistroHistorial();
        h.setIdVehiculo(orden.getIdVehiculo());
        h.setIdOrden(orden.getId());
        h.setFecha(LocalDate.now());
        h.setKilometraje(orden.getKilometraje());
        h.setDescripcion(descripcion.length() > 300 ? descripcion.substring(0, 297) + "..." : descripcion);
        h.setCosto(costo);
        repository.insertarHistorial(c, h);
    }
}
