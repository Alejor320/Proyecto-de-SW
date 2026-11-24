package com.concesionario.mantenimiento.model;

import java.util.List;

/** CU-MF-05: estado en que quedó la orden después del diagnóstico y los repuestos que faltan, si hay. */
public class ResultadoDiagnostico {

    private final EstadoOrden estado;
    private final List<String> faltantes;

    public ResultadoDiagnostico(EstadoOrden estado, List<String> faltantes) {
        this.estado = estado;
        this.faltantes = List.copyOf(faltantes);
    }

    public EstadoOrden getEstado() { return estado; }

    /** Descripción de cada repuesto que no alcanzó, por ejemplo "FIL-001 Filtro de aceite (faltan 2)". */
    public List<String> getFaltantes() { return faltantes; }

    public boolean hayFaltantes() {
        return !faltantes.isEmpty();
    }
}
