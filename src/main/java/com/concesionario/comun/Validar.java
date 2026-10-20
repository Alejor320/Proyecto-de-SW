package com.concesionario.comun;

/** Validaciones de uso frecuente en la capa de lógica. */
public final class Validar {

    private Validar() {
    }

    public static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public static void texto(String valor, String campo) throws ValidacionException {
        if (vacio(valor)) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
    }

    public static void requerido(Object valor, String campo) throws ValidacionException {
        if (valor == null) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
    }

    public static void condicion(boolean seCumple, String mensaje) throws ValidacionException {
        if (!seCumple) {
            throw new ValidacionException(mensaje);
        }
    }
}
