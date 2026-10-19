package com.concesionario.rrhh.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** CU-RH-03: contrato laboral de un empleado. */
public class ContratoLaboral {

    private int id;
    private int idEmpleado;
    private String nombreEmpleado;
    private TipoContrato tipo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal salario;
    private EstadoContrato estado = EstadoContrato.VIGENTE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }

    /** Solo para mostrar en la tabla; no se guarda. */
    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public TipoContrato getTipo() { return tipo; }
    public void setTipo(TipoContrato tipo) { this.tipo = tipo; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    /** Null cuando el contrato es indefinido. */
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }

    public EstadoContrato getEstado() { return estado; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }
}
