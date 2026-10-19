package com.concesionario.comun;

/**
 * Error de negocio o de datos ingresados por el usuario.
 * La vista lo muestra como advertencia, sin detalles técnicos.
 */
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
