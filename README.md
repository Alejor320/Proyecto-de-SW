# Concesionario Integrado – Recursos Humanos y Mantenimiento y Flota

Aplicación de escritorio en **Java 17 + Swing + H2** (Entrega 3).

## Casos de uso implementados (10)

| Área | Caso de uso | Entrega | Responsable |
|---|---|---|---|
| Recursos Humanos | CU-RH-01 Gestionar ciclo laboral de empleados | 2 | Alejandro Ramírez |
| Recursos Humanos | CU-RH-02 Gestionar asignación de horarios laborales | 3 | Alejandro Ramírez |
| Recursos Humanos | CU-RH-03 Gestionar contratos laborales | 2 | Alejandro Ramírez |
| Recursos Humanos | CU-RH-04 Gestionar permisos laborales | 2 | Alejandro Ramírez |
| Recursos Humanos | CU-RH-05 Gestionar solicitud y aprobación de vacaciones | 3 | Alejandro Ramírez |
| Mantenimiento y Flota | CU-MF-01 Gestionar órdenes de mantenimiento | 2 | Jaime Molina |
| Mantenimiento y Flota | CU-MF-02 Gestionar repuestos de taller | 2 | Jaime Molina |
| Mantenimiento y Flota | CU-MF-03 Gestionar técnicos de mantenimiento | 2 | Jaime Molina |
| Mantenimiento y Flota | CU-MF-04 Gestionar historial de mantenimiento del vehículo | 3 | Jaime Molina |
| Mantenimiento y Flota | CU-MF-05 Diagnosticar y reparar vehículo | 3 | Jaime Molina |

La pantalla **Vehículos (apoyo)** se conserva como dependencia mínima para poder abrir órdenes de
mantenimiento (CU-AP-01).

## Estructura (arquitectura por capas, como en el SDD)

```
src/main/java/com/concesionario/
├── App.java                     punto de entrada
├── config/                      conexión a H2 y creación de tablas            (acceso a datos)
├── comun/                       validaciones y pantalla base compartida
│   └── view/                    FormularioCrud, Refrescable, Campos, VentanaPrincipal
├── rrhh/
│   ├── view/                    pantallas Swing                                 (presentación)
│   ├── controller/              fachada que usan las pantallas                  (presentación)
│   ├── service/                 RecursosHumanosService: reglas de negocio       (lógica de negocio)
│   ├── repository/              RecursosHumanosRepository: JDBC + H2            (acceso a datos)
│   └── model/                   Empleado, ContratoLaboral, HorarioLaboral, PermisoLaboral, SolicitudVacaciones
├── mantenimiento/
│   ├── view/                    incluye DiagnosticoPanel e HistorialPanel
│   ├── controller/
│   ├── service/                 MantenimientoService
│   ├── repository/              MantenimientoRepository
│   └── model/                   OrdenMantenimiento, RepuestoOrden, Repuesto, Tecnico, RegistroHistorial
└── apoyo/
    ├── model/                   Vehiculo
    └── repository/              VehiculoRepository
src/main/resources/schema.sql    tablas de la base de datos (11)
src/test/java/                   pruebas JUnit 5 (90, de integración)
.github/workflows/pruebas.yml    pruebas y cobertura en GitHub Actions
```

Flujo de una operación: pantalla → controller → service → repository → H2.
Cada capa solo llama a la de abajo. El controller no tiene reglas de negocio.

## Cómo ejecutarlo

1. Abrir la carpeta como **proyecto Maven** en IntelliJ IDEA, NetBeans o Eclipse.
2. Esperar a que el IDE descargue las dependencias (H2 y JUnit).
3. Ejecutar la clase `com.concesionario.App`.

Desde la terminal: `mvn compile exec:java`

La base de datos se crea sola en la carpeta `data/`. Si ya existía una base de la Entrega 2, las tablas y
columnas nuevas se agregan solas al abrir la aplicación.

## Pruebas automáticas y cobertura

`mvn verify` compila, ejecuta las 90 pruebas JUnit y mide la cobertura con JaCoCo.
Las pruebas son de integración: llaman al servicio, que usa los repositorios reales sobre una base H2
en memoria, así que no tocan los datos de `data/`.

- El reporte de cobertura queda en `target/site/jacoco/index.html`.
- El `pom.xml` exige **100 % de instrucciones, líneas y ramas** en las clases de los paquetes
  `com.concesionario.*.service`. Si falta cubrir algo, `mvn verify` falla y dice qué clase no cumple.
- `mvn test` solo corre las pruebas, sin revisar la regla de cobertura.

| Clase de prueba | Pruebas | Qué prueba |
|---|---|---|
| RecursosHumanosServiceTest | 20 | CU-RH-01, CU-RH-03, CU-RH-04 |
| HorarioLaboralTest | 8 | CU-RH-02 |
| SolicitudVacacionesTest | 18 | CU-RH-05 |
| MantenimientoServiceTest | 19 | CU-MF-01, CU-MF-02, CU-MF-03, CU-AP-01 |
| HistorialMantenimientoTest | 8 | CU-MF-04 |
| DiagnosticoReparacionTest | 13 | CU-MF-05 |
| ConfiabilidadTest | 2 | RNF-04: una falla a mitad del cierre de una orden no deja datos a medias |
| RendimientoTest | 2 | RNF-05: tiempo de respuesta menor a 1,5 s con volumen de datos |

## GitHub Actions

El workflow `.github/workflows/pruebas.yml` ("Pruebas y cobertura") corre `mvn -B verify` con JDK 17 en cada
push a `main`, `develop` y `feature/**`, y en cada pull request hacia `main` o `develop`. También se puede lanzar
a mano con **Run workflow** en la pestaña Actions.

En cada ejecución:

- el resumen muestra una tabla con la cobertura de cada clase de servicio;
- el reporte completo de JaCoCo queda como artefacto `reporte-jacoco` (se descarga, se descomprime y se abre `index.html`).

Si una prueba falla o la cobertura baja del 100 %, la ejecución queda en rojo. No hacemos merge hasta que vuelva a verde.

## Pruebas manuales (casos CP-01 a CP-12 del PTP)

Hay un caso por cada escenario de los casos de uso no triviales (CU-RH-05 y CU-MF-05): flujo básico,
flujos alternativos y excepciones.

1. Cerrar la aplicación si está abierta.
2. Ejecutar `src/test/java/com/concesionario/DatosPruebasManuales.java` (clic derecho → Run).
   **Ojo:** borra la base de `data/` y carga los datos de prueba.
3. Abrir la aplicación con `App` y seguir los pasos del Plan de Pruebas en orden (CP-01 a CP-12).

### Provocar una falla de la base de datos (CP-06 y CP-11)

Con la aplicación abierta y la solicitud u orden ya seleccionada:

1. Marcar `data/concesionario.mv.db` como de solo lectura.
   - Windows: clic derecho → Propiedades → marcar "Solo lectura" → Aceptar.
   - macOS / Linux: `chmod a-w data/concesionario.mv.db`
2. Dar clic en **Aprobar** (vacaciones) o **Aprobar control de calidad** (mantenimiento).
   Debe salir el mensaje de error del SRS y no debe cambiar nada.
3. Quitar el solo lectura (desmarcar la casilla, o `chmod u+w data/concesionario.mv.db`) y cambiar de pestaña
   para que la pantalla vuelva a leer los datos.

Funciona porque la aplicación abre una conexión nueva en cada operación: si el archivo no se puede escribir,
H2 lo abre en modo de solo lectura y la actualización falla.
