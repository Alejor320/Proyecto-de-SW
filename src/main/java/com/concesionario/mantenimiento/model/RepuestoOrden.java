package com.concesionario.mantenimiento.model;

import java.math.BigDecimal;

/**
 * CU-MF-05: repuesto que necesita una orden de mantenimiento.
 * descontado = true cuando ya salió del inventario para la reparación.
 */
public class RepuestoOrden {

    private int id;
    private int idOrden;
    private int idRepuesto;
    private String codigo;
    private String nombreRepuesto;
    private BigDecimal precioUnitario;
    private int cantidad;
    private boolean descontado;

    public RepuestoOrden() {
    }

    public RepuestoOrden(int idRepuesto, int cantidad) {
        this.idRepuesto = idRepuesto;
        this.cantidad = cantidad;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }

    public int getIdRepuesto() { return idRepuesto; }
    public void setIdRepuesto(int idRepuesto) { this.idRepuesto = idRepuesto; }

    /** Solo para mostrar; viene de la tabla repuesto. */
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    /** Solo para mostrar; viene de la tabla repuesto. */
    public String getNombreRepuesto() { return nombreRepuesto; }
    public void setNombreRepuesto(String nombreRepuesto) { this.nombreRepuesto = nombreRepuesto; }

    /** Solo para mostrar y calcular costos; viene de la tabla repuesto. */
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public boolean isDescontado() { return descontado; }
    public void setDescontado(boolean descontado) { this.descontado = descontado; }
}
