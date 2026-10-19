package com.concesionario.rrhh.repository;

import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.DiaSemana;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoContrato;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.model.TipoContrato;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la base de datos de Recursos Humanos (tablas empleado, contrato_laboral y horario_laboral).
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
     * Cuenta contratos, horarios y registros de técnico asociados al empleado.
     * Si hay alguno, el empleado no se puede borrar sin perder información.
     */
    public int contarRegistrosAsociados(Connection c, int idEmpleado) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM contrato_laboral WHERE id_empleado = ?) "
                + "+ (SELECT COUNT(*) FROM horario_laboral WHERE id_empleado = ?) "
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
    // CU-RH-02 Horarios laborales
    // =====================================================================

    private static final String SELECT_HORARIO = "SELECT h.*, e.nombres || ' ' || e.apellidos AS nombre_empleado "
            + "FROM horario_laboral h JOIN empleado e ON e.id_empleado = h.id_empleado";

    public void insertarHorario(Connection c, HorarioLaboral h) throws SQLException {
        String sql = "INSERT INTO horario_laboral (id_empleado, dia, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarHorario(ps, h);
            ps.executeUpdate();
            h.setId(idGenerado(ps));
        }
    }

    public void actualizarHorario(Connection c, HorarioLaboral h) throws SQLException {
        String sql = "UPDATE horario_laboral SET id_empleado = ?, dia = ?, hora_inicio = ?, hora_fin = ? WHERE id_horario = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarHorario(ps, h);
            ps.setInt(5, h.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarHorario(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM horario_laboral WHERE id_horario = ?", id);
    }

    public List<HorarioLaboral> listarHorarios(Connection c) throws SQLException {
        String orden = " ORDER BY e.apellidos, e.nombres, CASE h.dia WHEN 'LUNES' THEN 1 WHEN 'MARTES' THEN 2 "
                + "WHEN 'MIERCOLES' THEN 3 WHEN 'JUEVES' THEN 4 WHEN 'VIERNES' THEN 5 WHEN 'SABADO' THEN 6 ELSE 7 END, "
                + "h.hora_inicio";
        try (PreparedStatement ps = c.prepareStatement(SELECT_HORARIO + orden)) {
            return listaHorarios(ps);
        }
    }

    public List<HorarioLaboral> listarHorariosPorEmpleadoYDia(Connection c, int idEmpleado, DiaSemana dia) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_HORARIO + " WHERE h.id_empleado = ? AND h.dia = ?")) {
            ps.setInt(1, idEmpleado);
            ps.setString(2, dia.name());
            return listaHorarios(ps);
        }
    }

    private List<HorarioLaboral> listaHorarios(PreparedStatement ps) throws SQLException {
        List<HorarioLaboral> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                HorarioLaboral h = new HorarioLaboral();
                h.setId(rs.getInt("id_horario"));
                h.setIdEmpleado(rs.getInt("id_empleado"));
                h.setNombreEmpleado(rs.getString("nombre_empleado"));
                h.setDia(DiaSemana.valueOf(rs.getString("dia")));
                h.setHoraInicio(rs.getObject("hora_inicio", LocalTime.class));
                h.setHoraFin(rs.getObject("hora_fin", LocalTime.class));
                lista.add(h);
            }
        }
        return lista;
    }

    private void llenarHorario(PreparedStatement ps, HorarioLaboral h) throws SQLException {
        ps.setInt(1, h.getIdEmpleado());
        ps.setString(2, h.getDia().name());
        ps.setObject(3, h.getHoraInicio());
        ps.setObject(4, h.getHoraFin());
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
