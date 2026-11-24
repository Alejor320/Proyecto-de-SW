package com.concesionario.mantenimiento.repository;

import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.RepuestoOrden;
import com.concesionario.mantenimiento.model.Tecnico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la base de datos de Mantenimiento y Flota (tablas tecnico, repuesto,
 * orden_mantenimiento, orden_repuesto e historial_mantenimiento).
 * Cada método recibe la conexión para que el servicio pueda agrupar varias operaciones en una transacción.
 */
public class MantenimientoRepository {

    // =====================================================================
    // CU-MF-03 Técnicos
    // =====================================================================

    private static final String SELECT_TECNICO = "SELECT t.*, e.nombres || ' ' || e.apellidos AS nombre_empleado "
            + "FROM tecnico t JOIN empleado e ON e.id_empleado = t.id_empleado";

    public void insertarTecnico(Connection c, Tecnico t) throws SQLException {
        String sql = "INSERT INTO tecnico (id_empleado, especialidad, disponible) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getIdEmpleado());
            ps.setString(2, t.getEspecialidad());
            ps.setBoolean(3, t.isDisponible());
            ps.executeUpdate();
            t.setId(idGenerado(ps));
        }
    }

    public void actualizarTecnico(Connection c, Tecnico t) throws SQLException {
        String sql = "UPDATE tecnico SET id_empleado = ?, especialidad = ?, disponible = ? WHERE id_tecnico = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getIdEmpleado());
            ps.setString(2, t.getEspecialidad());
            ps.setBoolean(3, t.isDisponible());
            ps.setInt(4, t.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarTecnico(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM tecnico WHERE id_tecnico = ?", id);
    }

    public Optional<Tecnico> buscarTecnicoPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_TECNICO + " WHERE t.id_tecnico = ?")) {
            ps.setInt(1, id);
            return unTecnico(ps);
        }
    }

    public Optional<Tecnico> buscarTecnicoPorEmpleado(Connection c, int idEmpleado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_TECNICO + " WHERE t.id_empleado = ?")) {
            ps.setInt(1, idEmpleado);
            return unTecnico(ps);
        }
    }

    public List<Tecnico> listarTecnicos(Connection c) throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_TECNICO + " ORDER BY e.apellidos, e.nombres");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearTecnico(rs));
            }
        }
        return lista;
    }

    public int contarOrdenesDeTecnico(Connection c, int idTecnico) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM orden_mantenimiento WHERE id_tecnico = ?")) {
            ps.setInt(1, idTecnico);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Optional<Tecnico> unTecnico(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapearTecnico(rs)) : Optional.empty();
        }
    }

    private Tecnico mapearTecnico(ResultSet rs) throws SQLException {
        Tecnico t = new Tecnico();
        t.setId(rs.getInt("id_tecnico"));
        t.setIdEmpleado(rs.getInt("id_empleado"));
        t.setNombreEmpleado(rs.getString("nombre_empleado"));
        t.setEspecialidad(rs.getString("especialidad"));
        t.setDisponible(rs.getBoolean("disponible"));
        return t;
    }

    // =====================================================================
    // CU-MF-02 Repuestos
    // =====================================================================

    public void insertarRepuesto(Connection c, Repuesto r) throws SQLException {
        String sql = "INSERT INTO repuesto (codigo, nombre, cantidad, stock_minimo, precio) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarRepuesto(ps, r);
            ps.executeUpdate();
            r.setId(idGenerado(ps));
        }
    }

    public void actualizarRepuesto(Connection c, Repuesto r) throws SQLException {
        String sql = "UPDATE repuesto SET codigo = ?, nombre = ?, cantidad = ?, stock_minimo = ?, precio = ? WHERE id_repuesto = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarRepuesto(ps, r);
            ps.setInt(6, r.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarRepuesto(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM repuesto WHERE id_repuesto = ?", id);
    }

    public Optional<Repuesto> buscarRepuestoPorCodigo(Connection c, String codigo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repuesto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearRepuesto(rs)) : Optional.empty();
            }
        }
    }

    public List<Repuesto> listarRepuestos(Connection c) throws SQLException {
        List<Repuesto> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repuesto ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearRepuesto(rs));
            }
        }
        return lista;
    }

    private void llenarRepuesto(PreparedStatement ps, Repuesto r) throws SQLException {
        ps.setString(1, r.getCodigo());
        ps.setString(2, r.getNombre());
        ps.setInt(3, r.getCantidad());
        ps.setInt(4, r.getStockMinimo());
        ps.setBigDecimal(5, r.getPrecio());
    }

    private Repuesto mapearRepuesto(ResultSet rs) throws SQLException {
        Repuesto r = new Repuesto();
        r.setId(rs.getInt("id_repuesto"));
        r.setCodigo(rs.getString("codigo"));
        r.setNombre(rs.getString("nombre"));
        r.setCantidad(rs.getInt("cantidad"));
        r.setStockMinimo(rs.getInt("stock_minimo"));
        r.setPrecio(rs.getBigDecimal("precio"));
        return r;
    }

    // =====================================================================
    // CU-MF-01 Órdenes de mantenimiento
    // =====================================================================

    private static final String SELECT_ORDEN = "SELECT o.*, v.placa, e.nombres || ' ' || e.apellidos AS nombre_tecnico "
            + "FROM orden_mantenimiento o "
            + "JOIN vehiculo v ON v.id_vehiculo = o.id_vehiculo "
            + "JOIN tecnico t ON t.id_tecnico = o.id_tecnico "
            + "JOIN empleado e ON e.id_empleado = t.id_empleado";

    public void insertarOrden(Connection c, OrdenMantenimiento o) throws SQLException {
        String sql = "INSERT INTO orden_mantenimiento (id_vehiculo, id_tecnico, fecha_apertura, fecha_cierre, motivo, "
                + "kilometraje, prioridad, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarOrden(ps, o);
            ps.executeUpdate();
            o.setId(idGenerado(ps));
        }
    }

    public void actualizarOrden(Connection c, OrdenMantenimiento o) throws SQLException {
        String sql = "UPDATE orden_mantenimiento SET id_vehiculo = ?, id_tecnico = ?, fecha_apertura = ?, fecha_cierre = ?, "
                + "motivo = ?, kilometraje = ?, prioridad = ?, estado = ? WHERE id_orden = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarOrden(ps, o);
            ps.setInt(9, o.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarOrden(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM orden_mantenimiento WHERE id_orden = ?", id);
    }

    public Optional<OrdenMantenimiento> buscarOrdenPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_ORDEN + " WHERE o.id_orden = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearOrden(rs)) : Optional.empty();
            }
        }
    }

    public List<OrdenMantenimiento> listarOrdenes(Connection c) throws SQLException {
        List<OrdenMantenimiento> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ORDEN + " ORDER BY o.id_orden DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearOrden(rs));
            }
        }
        return lista;
    }

    private void llenarOrden(PreparedStatement ps, OrdenMantenimiento o) throws SQLException {
        ps.setInt(1, o.getIdVehiculo());
        ps.setInt(2, o.getIdTecnico());
        ps.setObject(3, o.getFechaApertura());
        ps.setObject(4, o.getFechaCierre());
        ps.setString(5, o.getMotivo());
        ps.setInt(6, o.getKilometraje());
        ps.setString(7, o.getPrioridad().name());
        ps.setString(8, o.getEstado().name());
    }

    private OrdenMantenimiento mapearOrden(ResultSet rs) throws SQLException {
        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setId(rs.getInt("id_orden"));
        o.setIdVehiculo(rs.getInt("id_vehiculo"));
        o.setPlaca(rs.getString("placa"));
        o.setIdTecnico(rs.getInt("id_tecnico"));
        o.setNombreTecnico(rs.getString("nombre_tecnico"));
        o.setFechaApertura(rs.getObject("fecha_apertura", LocalDate.class));
        o.setFechaCierre(rs.getObject("fecha_cierre", LocalDate.class));
        o.setMotivo(rs.getString("motivo"));
        o.setKilometraje(rs.getInt("kilometraje"));
        o.setPrioridad(Prioridad.valueOf(rs.getString("prioridad")));
        o.setEstado(EstadoOrden.valueOf(rs.getString("estado")));
        o.setDiagnostico(rs.getString("diagnostico"));
        o.setHorasManoObra(rs.getBigDecimal("horas_mano_obra"));
        o.setObservacionCalidad(rs.getString("observacion_calidad"));
        return o;
    }

    // =====================================================================
    // CU-MF-05 Diagnosticar y reparar vehículo
    // =====================================================================

    public void actualizarDiagnostico(Connection c, int idOrden, String diagnostico, BigDecimal horas,
                                      Prioridad prioridad, EstadoOrden estado) throws SQLException {
        String sql = "UPDATE orden_mantenimiento SET diagnostico = ?, horas_mano_obra = ?, prioridad = ?, estado = ? "
                + "WHERE id_orden = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, diagnostico);
            ps.setBigDecimal(2, horas);
            ps.setString(3, prioridad.name());
            ps.setString(4, estado.name());
            ps.setInt(5, idOrden);
            ps.executeUpdate();
        }
    }

    public void actualizarEstadoOrden(Connection c, int idOrden, EstadoOrden estado, LocalDate fechaCierre,
                                      String observacionCalidad) throws SQLException {
        String sql = "UPDATE orden_mantenimiento SET estado = ?, fecha_cierre = ?, observacion_calidad = ? WHERE id_orden = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setObject(2, fechaCierre);
            ps.setString(3, observacionCalidad);
            ps.setInt(4, idOrden);
            ps.executeUpdate();
        }
    }

    public Optional<Repuesto> buscarRepuestoPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repuesto WHERE id_repuesto = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearRepuesto(rs)) : Optional.empty();
            }
        }
    }

    /** Suma (o resta, si delta es negativo) unidades al inventario de un repuesto. */
    public void ajustarCantidadRepuesto(Connection c, int idRepuesto, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE repuesto SET cantidad = cantidad + ? WHERE id_repuesto = ?")) {
            ps.setInt(1, delta);
            ps.setInt(2, idRepuesto);
            ps.executeUpdate();
        }
    }

    public int contarUsosDeRepuesto(Connection c, int idRepuesto) throws SQLException {
        return contar(c, "SELECT COUNT(*) FROM orden_repuesto WHERE id_repuesto = ?", idRepuesto);
    }

    public int contarRepuestosDescontados(Connection c, int idOrden) throws SQLException {
        return contar(c, "SELECT COUNT(*) FROM orden_repuesto WHERE id_orden = ? AND descontado = TRUE", idOrden);
    }

    public void insertarRepuestoOrden(Connection c, RepuestoOrden r) throws SQLException {
        String sql = "INSERT INTO orden_repuesto (id_orden, id_repuesto, cantidad, descontado) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getIdOrden());
            ps.setInt(2, r.getIdRepuesto());
            ps.setInt(3, r.getCantidad());
            ps.setBoolean(4, r.isDescontado());
            ps.executeUpdate();
            r.setId(idGenerado(ps));
        }
    }

    public void marcarRepuestosDescontados(Connection c, int idOrden) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orden_repuesto SET descontado = TRUE WHERE id_orden = ?")) {
            ps.setInt(1, idOrden);
            ps.executeUpdate();
        }
    }

    /** Borra los repuestos de la orden que todavía no han salido del inventario. */
    public void eliminarRepuestosPendientes(Connection c, int idOrden) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM orden_repuesto WHERE id_orden = ? AND descontado = FALSE")) {
            ps.setInt(1, idOrden);
            ps.executeUpdate();
        }
    }

    public List<RepuestoOrden> listarRepuestosDeOrden(Connection c, int idOrden) throws SQLException {
        String sql = "SELECT x.*, r.codigo, r.nombre, r.precio FROM orden_repuesto x "
                + "JOIN repuesto r ON r.id_repuesto = x.id_repuesto WHERE x.id_orden = ? ORDER BY x.id_orden_repuesto";
        List<RepuestoOrden> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RepuestoOrden r = new RepuestoOrden();
                    r.setId(rs.getInt("id_orden_repuesto"));
                    r.setIdOrden(rs.getInt("id_orden"));
                    r.setIdRepuesto(rs.getInt("id_repuesto"));
                    r.setCodigo(rs.getString("codigo"));
                    r.setNombreRepuesto(rs.getString("nombre"));
                    r.setPrecioUnitario(rs.getBigDecimal("precio"));
                    r.setCantidad(rs.getInt("cantidad"));
                    r.setDescontado(rs.getBoolean("descontado"));
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    // =====================================================================
    // CU-MF-04 Historial de mantenimiento del vehículo
    // =====================================================================

    private static final String SELECT_HISTORIAL = "SELECT h.*, v.placa FROM historial_mantenimiento h "
            + "JOIN vehiculo v ON v.id_vehiculo = h.id_vehiculo";

    public void insertarHistorial(Connection c, RegistroHistorial h) throws SQLException {
        String sql = "INSERT INTO historial_mantenimiento (id_vehiculo, id_orden, fecha, kilometraje, descripcion, costo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarHistorial(ps, h);
            ps.executeUpdate();
            h.setId(idGenerado(ps));
        }
    }

    public void actualizarHistorial(Connection c, RegistroHistorial h) throws SQLException {
        String sql = "UPDATE historial_mantenimiento SET id_vehiculo = ?, id_orden = ?, fecha = ?, kilometraje = ?, "
                + "descripcion = ?, costo = ? WHERE id_registro = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarHistorial(ps, h);
            ps.setInt(7, h.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarHistorial(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM historial_mantenimiento WHERE id_registro = ?", id);
    }

    public Optional<RegistroHistorial> buscarHistorialPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_HISTORIAL + " WHERE h.id_registro = ?")) {
            ps.setInt(1, id);
            List<RegistroHistorial> lista = listaHistorial(ps);
            return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
        }
    }

    public List<RegistroHistorial> listarHistorial(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_HISTORIAL + " ORDER BY v.placa, h.fecha DESC, h.id_registro DESC")) {
            return listaHistorial(ps);
        }
    }

    public List<RegistroHistorial> listarHistorialPorVehiculo(Connection c, int idVehiculo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_HISTORIAL + " WHERE h.id_vehiculo = ? ORDER BY h.fecha DESC, h.id_registro DESC")) {
            ps.setInt(1, idVehiculo);
            return listaHistorial(ps);
        }
    }

    private List<RegistroHistorial> listaHistorial(PreparedStatement ps) throws SQLException {
        List<RegistroHistorial> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RegistroHistorial h = new RegistroHistorial();
                h.setId(rs.getInt("id_registro"));
                h.setIdVehiculo(rs.getInt("id_vehiculo"));
                h.setPlaca(rs.getString("placa"));
                int orden = rs.getInt("id_orden");
                h.setIdOrden(rs.wasNull() ? null : orden);
                h.setFecha(rs.getObject("fecha", LocalDate.class));
                h.setKilometraje(rs.getInt("kilometraje"));
                h.setDescripcion(rs.getString("descripcion"));
                h.setCosto(rs.getBigDecimal("costo"));
                lista.add(h);
            }
        }
        return lista;
    }

    private void llenarHistorial(PreparedStatement ps, RegistroHistorial h) throws SQLException {
        ps.setInt(1, h.getIdVehiculo());
        ps.setObject(2, h.getIdOrden());
        ps.setObject(3, h.getFecha());
        ps.setInt(4, h.getKilometraje());
        ps.setString(5, h.getDescripcion());
        ps.setBigDecimal(6, h.getCosto());
    }

    private int contar(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private void eliminar(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private int idGenerado(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
