# Concesionario Integrado – Recursos Humanos y Mantenimiento y Flota

Aplicación de escritorio en **Java 17 + Swing + H2** (Entrega 2).

## Casos de uso implementados

| Área | Caso de uso | Responsable |
|---|---|---|
| Recursos Humanos | CU-RH-01 Gestionar ciclo laboral de empleados | Alejandro Ramírez |
| Recursos Humanos | CU-RH-02 Gestionar asignación de horarios laborales | Alejandro Ramírez |
| Recursos Humanos | CU-RH-03 Gestionar contratos laborales | Alejandro Ramírez |
| Mantenimiento y Flota | CU-MF-01 Gestionar órdenes de mantenimiento | Jaime Molina |
| Mantenimiento y Flota | CU-MF-02 Gestionar repuestos de taller | Jaime Molina |
| Mantenimiento y Flota | CU-MF-03 Gestionar técnicos de mantenimiento | Jaime Molina |
| Apoyo | CU-AP-01 Registrar y consultar vehículos (versión mínima) | Jaime Molina |

CU-RH-04 (permisos) y CU-RH-05 (vacaciones) ya tienen su clase en `rrhh/model`, pero se implementan en una entrega posterior.

## Estructura (capas, como en el SDD)

```
src/main/java/com/concesionario/
├── App.java                     punto de entrada
├── config/                      conexión a H2 y creación de tablas
├── comun/                       validaciones y pantalla base compartida
│   └── view/                    FormularioCrud, Campos, VentanaPrincipal
├── rrhh/
│   ├── model/                   Empleado, ContratoLaboral, HorarioLaboral, ...
│   ├── view/                    RecursosHumanosView y una pantalla por caso de uso
│   ├── controller/              RecursosHumanosController
│   ├── service/                 RecursosHumanosService (reglas de negocio)
│   └── repository/              RecursosHumanosRepository (JDBC + H2)
├── mantenimiento/
│   ├── model/                   OrdenMantenimiento, Repuesto, Tecnico, ...
│   ├── view/                    MantenimientoView y una pantalla por caso de uso
│   ├── controller/              MantenimientoController
│   ├── service/                 MantenimientoService (reglas de negocio)
│   └── repository/              MantenimientoRepository (JDBC + H2)
└── apoyo/
    ├── model/                   Vehiculo
    └── repository/              VehiculoRepository
src/main/resources/schema.sql    tablas de la base de datos
src/test/java/                   pruebas JUnit por área
```

Flujo de una operación: **View → Controller → Service → Repository → H2**.

## Cómo ejecutarlo

1. Abrir la carpeta como **proyecto Maven** en IntelliJ IDEA, NetBeans o Eclipse.
2. Esperar a que el IDE descargue las dependencias (H2 y JUnit).
3. Ejecutar la clase `com.concesionario.App`.

Desde la terminal: `mvn compile exec:java`

La base de datos se crea sola en la carpeta `data/`. Para empezar de cero, cierre la aplicación y borre esa carpeta.

## Pruebas

`mvn test` ejecuta las pruebas JUnit de `RecursosHumanosService` y `MantenimientoService`. Usan una base H2 en memoria, así que no tocan los datos reales.

## Orden sugerido para la demostración

1. Recursos Humanos → Empleados: crear 2 o 3 empleados.
2. Contratos y Horarios: asignarles contrato y horario (probar un cruce de fechas u horas para ver la validación).
3. Mantenimiento → Técnicos: convertir un empleado en técnico.
4. Vehículos (apoyo): registrar un vehículo.
5. Órdenes de mantenimiento: abrir una orden (el vehículo pasa a EN_MANTENIMIENTO) y luego cerrarla (vuelve a DISPONIBLE).
