package com.concesionario.apoyo.model;

/**
 * CU-AP-01 (apoyo): datos mínimos de un vehículo para poder abrirle órdenes de mantenimiento.
 * En el proyecto completo, el vehículo lo administra el área de Inventario y Bodega.
 */
public class Vehiculo {

    private int id;
    private String placa;
    private String marca;
    private String modelo;
    private TipoVehiculo tipo;
    private int anio;
    private EstadoVehiculo estado = EstadoVehiculo.DISPONIBLE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public TipoVehiculo getTipo() { return tipo; }
    public void setTipo(TipoVehiculo tipo) { this.tipo = tipo; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public EstadoVehiculo getEstado() { return estado; }
    public void setEstado(EstadoVehiculo estado) { this.estado = estado; }

    @Override
    public String toString() {
        return placa + " - " + marca + " " + modelo + " (" + estado + ")";
    }
}
