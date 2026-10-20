package com.concesionario.apoyo.repository;

import com.concesionario.apoyo.model.EstadoVehiculo;
import com.concesionario.apoyo.model.TipoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CU-AP-01 (apoyo): acceso a la tabla vehiculo.
 * Es la versión mínima que necesita Mantenimiento y Flota; el ciclo de vida completo
 * del vehículo le corresponde al área de Inventario y Bodega en el proyecto completo.
 */
public class VehiculoRepository {

    public void insertar(Connection c, Vehiculo v) throws SQLException {
        String sql = "INSERT INTO vehiculo (placa, marca, modelo, tipo, anio, estado) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getPlaca());
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            ps.setString(4, v.getTipo().name());
            ps.setInt(5, v.getAnio());
            ps.setString(6, v.getEstado().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setId(rs.getInt(1));
                }
            }
        }
    }

    public void actualizarEstado(Connection c, int idVehiculo, EstadoVehiculo estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE vehiculo SET estado = ? WHERE id_vehiculo = ?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, idVehiculo);
            ps.executeUpdate();
        }
    }

    public Optional<Vehiculo> buscarPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM vehiculo WHERE id_vehiculo = ?")) {
            ps.setInt(1, id);
            return uno(ps);
        }
    }

    public Optional<Vehiculo> buscarPorPlaca(Connection c, String placa) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM vehiculo WHERE placa = ?")) {
            ps.setString(1, placa);
            return uno(ps);
        }
    }

    public List<Vehiculo> listar(Connection c) throws SQLException {
        List<Vehiculo> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM vehiculo ORDER BY placa");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Optional<Vehiculo> uno(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        }
    }

    private Vehiculo mapear(ResultSet rs) throws SQLException {
        Vehiculo v = new Vehiculo();
        v.setId(rs.getInt("id_vehiculo"));
        v.setPlaca(rs.getString("placa"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));
        v.setTipo(TipoVehiculo.valueOf(rs.getString("tipo")));
        v.setAnio(rs.getInt("anio"));
        v.setEstado(EstadoVehiculo.valueOf(rs.getString("estado")));
        return v;
    }
}
