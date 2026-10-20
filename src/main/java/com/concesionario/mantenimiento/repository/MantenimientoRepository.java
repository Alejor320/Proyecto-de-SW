package com.concesionario.mantenimiento.repository;

import com.concesionario.mantenimiento.model.EstadoOrden;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.Tecnico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la base de datos de Mantenimiento y Flota (tablas tecnico, repuesto y orden_mantenimiento).
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
        return o;
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
