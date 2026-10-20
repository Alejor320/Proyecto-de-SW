package com.concesionario.comun.view;

import com.concesionario.comun.ValidacionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.function.ToIntFunction;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/** Convierte lo que el usuario escribe en los campos a tipos de Java, con mensajes claros. */
public final class Campos {

    private Campos() {
    }

    public static String texto(JTextField campo) {
        return campo.getText().trim();
    }

    /** Devuelve null si el campo está vacío. */
    public static String textoOpcional(JTextField campo) {
        String valor = campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    public static LocalDate fecha(JTextField campo, String nombre, boolean obligatoria) throws ValidacionException {
        String valor = campo.getText().trim();
        if (valor.isEmpty()) {
            if (obligatoria) {
                throw new ValidacionException("La " + nombre + " es obligatoria.");
            }
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new ValidacionException("La " + nombre + " no es válida. Use el formato aaaa-mm-dd, por ejemplo 2026-10-18.");
        }
    }

    public static LocalTime hora(JTextField campo, String nombre) throws ValidacionException {
        String valor = campo.getText().trim();
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException e) {
            throw new ValidacionException("La " + nombre + " no es válida. Use el formato HH:mm de 24 horas, por ejemplo 08:00 o 17:30.");
        }
    }

    public static int entero(JTextField campo, String nombre) throws ValidacionException {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo \"" + nombre + "\" debe ser un número entero.");
        }
    }

    public static BigDecimal decimal(JTextField campo, String nombre) throws ValidacionException {
        try {
            return new BigDecimal(campo.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo \"" + nombre + "\" debe ser un número, por ejemplo 1500000 o 25000.50.");
        }
    }

    public static <E> E seleccion(JComboBox<E> combo, String nombre) throws ValidacionException {
        int indice = combo.getSelectedIndex();
        if (indice < 0) {
            throw new ValidacionException("Seleccione un valor en \"" + nombre + "\".");
        }
        E valor = combo.getItemAt(indice);
        if (valor == null) {
            throw new ValidacionException("Seleccione un valor en \"" + nombre + "\".");
        }
        return valor;
    }

    /** Selecciona en el combo el elemento cuyo id coincide. */
    public static <E> void seleccionarPorId(JComboBox<E> combo, ToIntFunction<E> id, int buscado) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (id.applyAsInt(combo.getItemAt(i)) == buscado) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    public static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
