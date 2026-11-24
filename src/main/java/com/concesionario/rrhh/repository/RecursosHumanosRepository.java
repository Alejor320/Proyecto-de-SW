package com.concesionario.rrhh.repository;

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
 * Acceso a la base de datos de Recursos Humanos (tablas empleado, contrato_laboral, horario_laboral,
 * permiso_laboral y solicitud_vacaciones).
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
     * Cuenta contratos, permisos, horarios, vacaciones y registros de técnico asociados al empleado.
     * Si hay alguno, el empleado no se puede borrar sin perder información.
     */
    public int contarRegistrosAsociados(Connection c, int idEmpleado) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM contrato_laboral WHERE id_empleado = ?) "
                + "+ (SELECT COUNT(*) FROM permiso_laboral WHERE id_empleado = ?) "
                + "+ (SELECT COUNT(*) FROM horario_laboral WHERE id_empleado = ?) "
                + "+ (SELECT COUNT(*) FROM solicitud_vacaciones WHERE id_empleado = ? OR id_aprobador = ?) "
                + "+ (SELECT COUNT(*) FROM tecnico WHERE id_empleado = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i++) {
                ps.setInt(i, idEmpleado);
            }
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

    public List<HorarioLaboral> listarHorariosPorEmpleado(Connection c, int idEmpleado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_HORARIO + " WHERE h.id_empleado = ?")) {
            ps.setInt(1, idEmpleado);
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
    // CU-RH-05 Solicitudes de vacaciones
    // =====================================================================

    private static final String SELECT_VACACIONES = "SELECT v.*, e.nombres || ' ' || e.apellidos AS nombre_empleado, "
            + "a.nombres || ' ' || a.apellidos AS nombre_aprobador "
            + "FROM solicitud_vacaciones v JOIN empleado e ON e.id_empleado = v.id_empleado "
            + "LEFT JOIN empleado a ON a.id_empleado = v.id_aprobador";

    public void insertarSolicitud(Connection c, SolicitudVacaciones v) throws SQLException {
        String sql = "INSERT INTO solicitud_vacaciones (id_empleado, fecha_inicio, fecha_fin, dias_habiles, observacion, "
                + "estado, id_aprobador, fecha_decision, motivo_rechazo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarSolicitud(ps, v);
            ps.executeUpdate();
            v.setId(idGenerado(ps));
        }
    }

    public void actualizarSolicitud(Connection c, SolicitudVacaciones v) throws SQLException {
        String sql = "UPDATE solicitud_vacaciones SET id_empleado = ?, fecha_inicio = ?, fecha_fin = ?, dias_habiles = ?, "
                + "observacion = ?, estado = ?, id_aprobador = ?, fecha_decision = ?, motivo_rechazo = ? WHERE id_solicitud = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            llenarSolicitud(ps, v);
            ps.setInt(10, v.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarSolicitud(Connection c, int id) throws SQLException {
        eliminar(c, "DELETE FROM solicitud_vacaciones WHERE id_solicitud = ?", id);
    }

    public Optional<SolicitudVacaciones> buscarSolicitudPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_VACACIONES + " WHERE v.id_solicitud = ?")) {
            ps.setInt(1, id);
            List<SolicitudVacaciones> lista = listaSolicitudes(ps);
            return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
        }
    }

    public List<SolicitudVacaciones> listarSolicitudes(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_VACACIONES + " ORDER BY v.fecha_inicio DESC, v.id_solicitud DESC")) {
            return listaSolicitudes(ps);
        }
    }

    public List<SolicitudVacaciones> listarSolicitudesPorEmpleado(Connection c, int idEmpleado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_VACACIONES + " WHERE v.id_empleado = ?")) {
            ps.setInt(1, idEmpleado);
            return listaSolicitudes(ps);
        }
    }

    /** Días hábiles de las solicitudes del empleado en el estado dado, sin contar la solicitud indicada. */
    public int sumarDiasVacaciones(Connection c, int idEmpleado, EstadoVacaciones estado, int excluirId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(dias_habiles), 0) FROM solicitud_vacaciones "
                + "WHERE id_empleado = ? AND estado = ? AND id_solicitud <> ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEmpleado);
            ps.setString(2, estado.name());
            ps.setInt(3, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<PermisoLaboral> listarPermisosPorEmpleado(Connection c, int idEmpleado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_PERMISO + " WHERE p.id_empleado = ?")) {
            ps.setInt(1, idEmpleado);
            return listaPermisos(ps);
        }
    }

    private List<SolicitudVacaciones> listaSolicitudes(PreparedStatement ps) throws SQLException {
        List<SolicitudVacaciones> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SolicitudVacaciones v = new SolicitudVacaciones();
                v.setId(rs.getInt("id_solicitud"));
                v.setIdEmpleado(rs.getInt("id_empleado"));
                v.setNombreEmpleado(rs.getString("nombre_empleado"));
                v.setFechaInicio(rs.getObject("fecha_inicio", LocalDate.class));
                v.setFechaFin(rs.getObject("fecha_fin", LocalDate.class));
                v.setDiasHabiles(rs.getInt("dias_habiles"));
                v.setObservacion(rs.getString("observacion"));
                v.setEstado(EstadoVacaciones.valueOf(rs.getString("estado")));
                int aprobador = rs.getInt("id_aprobador");
                v.setIdAprobador(rs.wasNull() ? null : aprobador);
                v.setNombreAprobador(rs.getString("nombre_aprobador"));
                v.setFechaDecision(rs.getObject("fecha_decision", LocalDate.class));
                v.setMotivoRechazo(rs.getString("motivo_rechazo"));
                lista.add(v);
            }
        }
        return lista;
    }

    private void llenarSolicitud(PreparedStatement ps, SolicitudVacaciones v) throws SQLException {
        ps.setInt(1, v.getIdEmpleado());
        ps.setObject(2, v.getFechaInicio());
        ps.setObject(3, v.getFechaFin());
        ps.setInt(4, v.getDiasHabiles());
        ps.setString(5, v.getObservacion());
        ps.setString(6, v.getEstado().name());
        ps.setObject(7, v.getIdAprobador());
        ps.setObject(8, v.getFechaDecision());
        ps.setString(9, v.getMotivoRechazo());
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
