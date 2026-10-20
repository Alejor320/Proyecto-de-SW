package com.concesionario.rrhh.model;

import java.time.LocalDate;

/** CU-RH-04: permiso laboral solicitado por un empleado. */
public class PermisoLaboral {

    private int id;
    private int idEmpleado;
    private String nombreEmpleado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
    private String soporte;
    private EstadoPermiso estado = EstadoPermiso.PENDIENTE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }

    /** Solo para mostrar en la tabla; no se guarda. */
    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    /** Ruta, enlace o referencia textual del soporte; puede ser null. */
    public String getSoporte() { return soporte; }
    public void setSoporte(String soporte) { this.soporte = soporte; }

    public EstadoPermiso getEstado() { return estado; }
    public void setEstado(EstadoPermiso estado) { this.estado = estado; }
}
