package com.concesionario.mantenimiento.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CU-MF-04: servicio realizado a un vehículo. Si idOrden no es null, el registro lo creó el sistema
 * al cerrar esa orden de mantenimiento; si es null, lo registró el técnico a mano.
 */
public class RegistroHistorial {

    private int id;
    private int idVehiculo;
    private String placa;
    private Integer idOrden;
    private LocalDate fecha;
    private int kilometraje;
    private String descripcion;
    private BigDecimal costo;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(int idVehiculo) { this.idVehiculo = idVehiculo; }

    /** Solo para mostrar; viene de la tabla vehiculo. */
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public Integer getIdOrden() { return idOrden; }
    public void setIdOrden(Integer idOrden) { this.idOrden = idOrden; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public int getKilometraje() { return kilometraje; }
    public void setKilometraje(int kilometraje) { this.kilometraje = kilometraje; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }

    public boolean esAutomatico() {
        return idOrden != null;
    }
}
