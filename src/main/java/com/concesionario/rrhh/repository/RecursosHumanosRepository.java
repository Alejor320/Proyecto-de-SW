package com.concesionario.rrhh.repository;

import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.EstadoPermiso;
import com.concesionario.rrhh.model.PermisoLaboral;
import com.concesionario.rrhh.model.TipoContrato;

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
 * Acceso a la base de datos de Recursos Humanos (tablas empleado, contrato_laboral y permiso_laboral).
 * Cada método recibe la conexión para que el servicio pueda agrupar varias operaciones en una transacción.
 */
public class RecursosHumanosRepository {

    // =====================================================================
    // CU-RH-01 Empleados
    // =====================================================================

    public void insertarEmpleado(Connection c, Empleado e) throws SQLException {
        String sql = "INSERT INTO empleado (documento, nombres, apellidos, cargo, telefono, correo, fecha_ingreso, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarEmpleado(ps, e);
            ps.executeUpdate();
            e.setId(idGenerado(ps));
        }
    }

    public void actualizarEmpleado(Connection c, Empleado e) throws SQLException {
        String sql = "UPDATE empleado SET documento = ?, nombres = ?, apellidos = ?, cargo = ?, telefono = ?, "
                + "correo = ?, fecha_ingreso = ?, estado = ? WHERE id_empleado = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarEmpleado(ps, e);
            ps.setInt(9, e.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarEmpleado(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM empleado WHERE id_empleado = ?", id);
    }

    public Optional<Empleado> buscarEmpleadoPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM empleado WHERE id_empleado = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearEmpleado(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Empleado> buscarEmpleadoPorDocumento(Connection c, String documento) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM empleado WHERE documento = ?")) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearEmpleado(rs)) : Optional.empty();
            }
        }
    }

    public List<Empleado> listarEmpleados(Connection c) throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM empleado ORDER BY apellidos, nombres");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearEmpleado(rs));
            }
        }
        return lista;
    }

    /**
     * Cuenta contratos, permisos y registros de técnico asociados al empleado.
     * Si hay alguno, el empleado no se puede borrar sin perder información.
     */
    public int contarRegistrosAsociados(Connection c, int idEmpleado) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM contrato_laboral WHERE id_empleado = ?) "
                + "+ (SELECT COUNT(*) FROM permiso_laboral WHERE id_empleado = ?) "
                + "+ (SELECT COUNT(*) FROM tecnico WHERE id_empleado = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEmpleado);
            ps.setInt(2, idEmpleado);
            ps.setInt(3, idEmpleado);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void llenarEmpleado(PreparedStatement ps, Empleado e) throws SQLException {
        ps.setString(1, e.getDocumento());
        ps.setString(2, e.getNombres());
        ps.setString(3, e.getApellidos());
        ps.setString(4, e.getCargo());
        ps.setString(5, e.getTelefono());
        ps.setString(6, e.getCorreo());
        ps.setObject(7, e.getFechaIngreso());
        ps.setString(8, e.getEstado().name());
    }

    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setId(rs.getInt("id_empleado"));
        e.setDocumento(rs.getString("documento"));
        e.setNombres(rs.getString("nombres"));
        e.setApellidos(rs.getString("apellidos"));
        e.setCargo(rs.getString("cargo"));
        e.setTelefono(rs.getString("telefono"));
        e.setCorreo(rs.getString("correo"));
        e.setFechaIngreso(rs.getObject("fecha_ingreso", LocalDate.class));
        e.setEstado(EstadoEmpleado.valueOf(rs.getString("estado")));
        return e;
    }

    // =====================================================================
    // CU-RH-03 Contratos laborales
    // =====================================================================

    private static final String SELECT_CONTRATO = "SELECT k.*, e.nombres || ' ' || e.apellidos AS nombre_empleado "
            + "FROM contrato_laboral k JOIN empleado e ON e.id_empleado = k.id_empleado";

    public void insertarContrato(Connection c, ContratoLaboral k) throws SQLException {
        String sql = "INSERT INTO contrato_laboral (id_empleado, tipo, fecha_inicio, fecha_fin, salario, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarContrato(ps, k);
            ps.executeUpdate();
            k.setId(idGenerado(ps));
        }
    }

    public void actualizarContrato(Connection c, ContratoLaboral k) throws SQLException {
        String sql = "UPDATE contrato_laboral SET id_empleado = ?, tipo = ?, fecha_inicio = ?, fecha_fin = ?, "
                + "salario = ?, estado = ? WHERE id_contrato = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarContrato(ps, k);
            ps.setInt(7, k.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarContrato(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM contrato_laboral WHERE id_contrato = ?", id);
    }

    public List<ContratoLaboral> listarContratos(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_CONTRATO + " ORDER BY k.id_contrato DESC")) {
            return listaContratos(ps);
        }
    }

    public List<ContratoLaboral> listarContratosVigentes(Connection c, int idEmpleado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_CONTRATO + " WHERE k.id_empleado = ? AND k.estado = ?")) {
            ps.setInt(1, idEmpleado);
            ps.setString(2, EstadoContrato.VIGENTE.name());
            return listaContratos(ps);
        }
    }

    private List<ContratoLaboral> listaContratos(PreparedStatement ps) throws SQLException {
        List<ContratoLaboral> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ContratoLaboral k = new ContratoLaboral();
                k.setId(rs.getInt("id_contrato"));
                k.setIdEmpleado(rs.getInt("id_empleado"));
                k.setNombreEmpleado(rs.getString("nombre_empleado"));
                k.setTipo(TipoContrato.valueOf(rs.getString("tipo")));
                k.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
                k.setFechaFin(rs.getObject("fecha_fin", LocalDate.class));
                k.setSalario(rs.getBigDecimal("salario"));
                k.setEstado(EstadoContrato.valueOf(rs.getString("estado")));
                lista.add(k);
            }
        }
        return lista;
    }

    private void llenarContrato(PreparedStatement ps, ContratoLaboral k) throws SQLException {
        ps.setInt(1, k.getIdEmpleado());
        ps.setString(2, k.getTipo().name());
        ps.setObject(3, k.getFechaInicio());
        ps.setObject(4, k.getFechaFin());
        ps.setBigDecimal(5, k.getSalario());
        ps.setString(6, k.getEstado().name());
    }

    // =====================================================================
    // CU-RH-04 Permisos laborales
    // =====================================================================

    private static final String SELECT_PERMISO = "SELECT p.*, e.nombres || ' ' || e.apellidos AS nombre_empleado "
            + "FROM permiso_laboral p JOIN empleado e ON e.id_empleado = p.id_empleado";

    public void insertarPermiso(Connection c, PermisoLaboral p) throws SQLException {
        String sql = "INSERT INTO permiso_laboral (id_empleado, fecha_inicio, fecha_fin, motivo, soporte, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarPermiso(ps, p);
            ps.executeUpdate();
            p.setId(idGenerado(ps));
        }
    }

    public void actualizarPermiso(Connection c, PermisoLaboral p) throws SQLException {
        String sql = "UPDATE permiso_laboral SET id_empleado = ?, fecha_inicio = ?, fecha_fin = ?, motivo = ?, "
                + "soporte = ?, estado = ? WHERE id_permiso = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarPermiso(ps, p);
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarPermiso(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM permiso_laboral WHERE id_permiso = ?", id);
    }

    public Optional<PermisoLaboral> buscarPermisoPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_PERMISO + " WHERE p.id_permiso = ?")) {
            ps.setInt(1, id);
            List<PermisoLaboral> permisos = listaPermisos(ps);
            return permisos.isEmpty() ? Optional.empty() : Optional.of(permisos.get(0));
        }
    }

    public List<PermisoLaboral> listarPermisos(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                SELECT_PERMISO + " ORDER BY p.fecha_inicio DESC, p.id_permiso DESC")) {
            return listaPermisos(ps);
        }
    }

    private List<PermisoLaboral> listaPermisos(PreparedStatement ps) throws SQLException {
        List<PermisoLaboral> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PermisoLaboral p = new PermisoLaboral();
                p.setId(rs.getInt("id_permiso"));
                p.setIdEmpleado(rs.getInt("id_empleado"));
                p.setNombreEmpleado(rs.getString("nombre_empleado"));
                p.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
                p.setFechaFin(rs.getObject("fecha_fin", LocalDate.class));
                p.setMotivo(rs.getString("motivo"));
                p.setSoporte(rs.getString("soporte"));
                p.setEstado(EstadoPermiso.valueOf(rs.getString("estado")));
                lista.add(p);
            }
        }
        return lista;
    }

    private void llenarPermiso(PreparedStatement ps, PermisoLaboral p) throws SQLException {
        ps.setInt(1, p.getIdEmpleado());
        ps.setObject(2, p.getFechaInicio());
        ps.setObject(3, p.getFechaFin());
        ps.setString(4, p.getMotivo());
        ps.setString(5, p.getSoporte());
        ps.setString(6, p.getEstado().name());
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
