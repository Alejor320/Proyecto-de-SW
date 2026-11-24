package com.concesionario.rrhh.model;

import java.time.LocalDate;

/** CU-RH-05: solicitud de vacaciones de un empleado, que aprueba o rechaza su jefe inmediato. */
public class SolicitudVacaciones {

    private int id;
    private int idEmpleado;
    private String nombreEmpleado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int diasHabiles;
    private String observacion;
    private EstadoVacaciones estado = EstadoVacaciones.PENDIENTE;
    private Integer idAprobador;
    private String nombreAprobador;
    private LocalDate fechaDecision;
    private String motivoRechazo;

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

    /** Días laborales del período según el horario del empleado. Lo calcula el servicio. */
    public int getDiasHabiles() { return diasHabiles; }
    public void setDiasHabiles(int diasHabiles) { this.diasHabiles = diasHabiles; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public EstadoVacaciones getEstado() { return estado; }
    public void setEstado(EstadoVacaciones estado) { this.estado = estado; }

    /** Jefe inmediato que aprobó o rechazó la solicitud; null mientras está pendiente. */
    public Integer getIdAprobador() { return idAprobador; }
    public void setIdAprobador(Integer idAprobador) { this.idAprobador = idAprobador; }

    /** Solo para mostrar en la tabla; no se guarda. */
    public String getNombreAprobador() { return nombreAprobador; }
    public void setNombreAprobador(String nombreAprobador) { this.nombreAprobador = nombreAprobador; }

    public LocalDate getFechaDecision() { return fechaDecision; }
    public void setFechaDecision(LocalDate fechaDecision) { this.fechaDecision = fechaDecision; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
}
