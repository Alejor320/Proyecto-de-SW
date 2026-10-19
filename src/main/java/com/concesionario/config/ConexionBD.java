package com.concesionario.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Entrega conexiones a la base de datos H2 embebida.
 * Por defecto crea el archivo en la carpeta "data" del proyecto.
 * Las pruebas cambian la URL a una base en memoria con la propiedad del sistema.
 */
public final class ConexionBD {

    public static final String PROPIEDAD_URL = "concesionario.db.url";
    private static final String URL_POR_DEFECTO = "jdbc:h2:./data/concesionario";

    private ConexionBD() {
    }

    public static Connection obtener() throws SQLException {
        String url = System.getProperty(PROPIEDAD_URL, URL_POR_DEFECTO);
        return DriverManager.getConnection(url, "sa", "");
    }
}
