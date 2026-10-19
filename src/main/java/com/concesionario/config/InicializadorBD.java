package com.concesionario.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/** Crea las tablas a partir del archivo schema.sql. */
public final class InicializadorBD {

    private InicializadorBD() {
    }

    /** Crea las tablas que no existan. No borra datos. */
    public static void inicializar() throws SQLException, IOException {
        ejecutarScript();
    }

    /** Borra todo y vuelve a crear las tablas. Se usa en las pruebas. */
    public static void reiniciar() throws SQLException, IOException {
        try (Connection c = ConexionBD.obtener(); Statement s = c.createStatement()) {
            s.execute("DROP ALL OBJECTS");
        }
        ejecutarScript();
    }

    private static void ejecutarScript() throws SQLException, IOException {
        String script;
        try (InputStream in = InicializadorBD.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IOException("No se encontró el archivo schema.sql");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String sinComentarios = script.lines()
                .filter(linea -> !linea.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
        try (Connection c = ConexionBD.obtener(); Statement s = c.createStatement()) {
            for (String sentencia : sinComentarios.split(";")) {
                if (!sentencia.isBlank()) {
                    s.execute(sentencia);
                }
            }
        }
    }
}
