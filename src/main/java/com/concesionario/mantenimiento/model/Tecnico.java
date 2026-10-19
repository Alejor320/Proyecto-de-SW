package com.concesionario.mantenimiento.model;

/** CU-MF-03: técnico de mantenimiento. Cada técnico es un empleado registrado en Recursos Humanos. */
public class Tecnico {

    private int id;
    private int idEmpleado;
    private String nombreEmpleado;
    private String especialidad;
    private boolean disponible = true;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }

    /** Solo para mostrar; viene de la tabla empleado. */
    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    @Override
    public String toString() {
        return nombreEmpleado + " - " + especialidad + (disponible ? "" : " (no disponible)");
    }
}
