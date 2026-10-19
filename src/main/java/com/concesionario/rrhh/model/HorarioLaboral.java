package com.concesionario.rrhh.model;

import java.time.LocalTime;

/** CU-RH-02: franja de trabajo de un empleado en un día de la semana. */
public class HorarioLaboral {

    private int id;
    private int idEmpleado;
    private String nombreEmpleado;
    private DiaSemana dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }

    /** Solo para mostrar en la tabla; no se guarda. */
    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public DiaSemana getDia() { return dia; }
    public void setDia(DiaSemana dia) { this.dia = dia; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
}
