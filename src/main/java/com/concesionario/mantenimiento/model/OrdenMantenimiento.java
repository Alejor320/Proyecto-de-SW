package com.concesionario.mantenimiento.model;

import java.time.LocalDate;

/** CU-MF-01: orden de mantenimiento de un vehículo, asignada a un técnico. */
public class OrdenMantenimiento {

    private int id;
    private int idVehiculo;
    private String placa;
    private int idTecnico;
    private String nombreTecnico;
    private LocalDate fechaApertura;
    private LocalDate fechaCierre;
    private String motivo;
    private int kilometraje;
    private Prioridad prioridad;
    private EstadoOrden estado = EstadoOrden.ABIERTA;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(int idVehiculo) { this.idVehiculo = idVehiculo; }

    /** Solo para mostrar; viene de la tabla vehiculo. */
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public int getIdTecnico() { return idTecnico; }
    public void setIdTecnico(int idTecnico) { this.idTecnico = idTecnico; }

    /** Solo para mostrar; viene de las tablas tecnico y empleado. */
    public String getNombreTecnico() { return nombreTecnico; }
    public void setNombreTecnico(String nombreTecnico) { this.nombreTecnico = nombreTecnico; }

    public LocalDate getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDate fechaApertura) { this.fechaApertura = fechaApertura; }

    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public int getKilometraje() { return kilometraje; }
    public void setKilometraje(int kilometraje) { this.kilometraje = kilometraje; }

    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }
}
