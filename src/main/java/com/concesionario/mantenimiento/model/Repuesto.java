package com.concesionario.mantenimiento.model;

import java.math.BigDecimal;

/** CU-MF-02: repuesto del taller. */
public class Repuesto {

    private int id;
    private String codigo;
    private String nombre;
    private int cantidad;
    private int stockMinimo;
    private BigDecimal precio;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    /** Un repuesto necesita reabastecerse cuando su cantidad llega al stock mínimo o menos. */
    public boolean requiereReabastecimiento() {
        return cantidad <= stockMinimo;
    }
}
