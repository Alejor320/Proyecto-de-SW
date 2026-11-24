package com.concesionario;

import com.concesionario.apoyo.model.TipoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.config.InicializadorBD;
import com.concesionario.mantenimiento.model.OrdenMantenimiento;
import com.concesionario.mantenimiento.model.Prioridad;
import com.concesionario.mantenimiento.model.RegistroHistorial;
import com.concesionario.mantenimiento.model.Repuesto;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;
import com.concesionario.rrhh.model.ContratoLaboral;
import com.concesionario.rrhh.model.DiaSemana;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoPermiso;
import com.concesionario.rrhh.model.HorarioLaboral;
import com.concesionario.rrhh.model.PermisoLaboral;
import com.concesionario.rrhh.model.TipoContrato;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Deja la base de datos lista para ejecutar los casos de prueba manuales CP-01 a CP-12 del PTP.
 *
 * <p>OJO: borra todo lo que haya en la base del proyecto (carpeta data/) y carga los datos de prueba.
 * Se ejecuta desde el IDE (clic derecho → Run) y después se abre la aplicación con App.
 */
public final class DatosPruebasManuales {

    private static final RecursosHumanosService RRHH = new RecursosHumanosService();
    private static final MantenimientoService MF = new MantenimientoService();

    private DatosPruebasManuales() {
    }

    public static void main(String[] args) throws Exception {
        InicializadorBD.reiniciar();
        cargar();
        System.out.println("Datos de las pruebas manuales cargados. Ya puede abrir la aplicación (App).");
    }

    public static void cargar() throws Exception {
        Empleado laura = empleado("1012345678", "Laura", "Gómez", "Analista de RRHH", LocalDate.of(2024, 2, 1), true);
        empleado("1023456789", "Carlos", "Rodríguez", "Jefe de taller", LocalDate.of(2023, 5, 15), true);
        Empleado diana = empleado("1034567890", "Diana", "Martínez", "Técnica de mantenimiento",
                LocalDate.of(2026, 9, 21), true);
        empleado("1045678901", "Andrés", "Pérez", "Asesor comercial", LocalDate.of(2025, 3, 10), false);
        Empleado juan = empleado("1056789012", "Juan", "Torres", "Técnico de mantenimiento", LocalDate.of(2022, 8, 1), true);

        PermisoLaboral permiso = new PermisoLaboral();
        permiso.setIdEmpleado(laura.getId());
        permiso.setFechaInicio(LocalDate.of(2026, 12, 16));
        permiso.setFechaFin(LocalDate.of(2026, 12, 16));
        permiso.setMotivo("Cita médica");
        permiso.setSoporte("Orden médica EPS");
        permiso.setEstado(EstadoPermiso.APROBADO);
        RRHH.guardarPermiso(permiso);

        Tecnico tecnicaDiana = tecnico(diana, "Frenos y suspensión");
        Tecnico tecnicoJuan = tecnico(juan, "Motor y carrocería");

        Vehiculo abc = vehiculo("ABC123", "Renault", "Logan", TipoVehiculo.AUTOMOVIL, 2022);
        Vehiculo def = vehiculo("DEF456", "Chevrolet", "Onix", TipoVehiculo.AUTOMOVIL, 2023);
        Vehiculo ghi = vehiculo("GHI789", "Mazda", "3", TipoVehiculo.AUTOMOVIL, 2021);
        vehiculo("XYZ45B", "Yamaha", "NMAX", TipoVehiculo.MOTOCICLETA, 2024);

        RegistroHistorial anterior = new RegistroHistorial();
        anterior.setIdVehiculo(abc.getId());
        anterior.setFecha(LocalDate.of(2026, 5, 10));
        anterior.setKilometraje(45000);
        anterior.setDescripcion("Cambio de aceite y filtro (taller externo)");
        anterior.setCosto(new BigDecimal("160000"));
        MF.guardarRegistroHistorial(anterior);

        repuesto("FIL-001", "Filtro de aceite", 10, 3, "35000");
        repuesto("FRE-010", "Pastillas de freno", 4, 2, "120000");
        repuesto("ACE-020", "Aceite 5W-30 (litro)", 20, 5, "25000");
        repuesto("BAT-005", "Batería 12V", 1, 1, "380000");

        orden(abc, tecnicaDiana, "Ruido al frenar", 52000);
        orden(def, tecnicaDiana, "Revisión de frenos y suspensión", 31000);
        orden(ghi, tecnicoJuan, "Golpe frontal por colisión", 88000);
    }

    private static Empleado empleado(String documento, String nombres, String apellidos, String cargo,
                                     LocalDate ingreso, boolean conHorario) throws Exception {
        Empleado e = new Empleado();
        e.setDocumento(documento);
        e.setNombres(nombres);
        e.setApellidos(apellidos);
        e.setCargo(cargo);
        e.setTelefono("3001234567");
        e.setCorreo(nombres.toLowerCase() + "@concesionario.com");
        e.setFechaIngreso(ingreso);
        RRHH.guardarEmpleado(e);

        ContratoLaboral contrato = new ContratoLaboral();
        contrato.setIdEmpleado(e.getId());
        contrato.setTipo(TipoContrato.INDEFINIDO);
        contrato.setFechaInicio(ingreso);
        contrato.setSalario(new BigDecimal("2800000"));
        RRHH.guardarContrato(contrato);

        if (conHorario) {
            for (DiaSemana dia : new DiaSemana[] {DiaSemana.LUNES, DiaSemana.MARTES, DiaSemana.MIERCOLES,
                    DiaSemana.JUEVES, DiaSemana.VIERNES}) {
                HorarioLaboral h = new HorarioLaboral();
                h.setIdEmpleado(e.getId());
                h.setDia(dia);
                h.setHoraInicio(LocalTime.of(8, 0));
                h.setHoraFin(LocalTime.of(17, 0));
                RRHH.guardarHorario(h);
            }
        }
        return e;
    }

    private static Tecnico tecnico(Empleado empleado, String especialidad) throws Exception {
        Tecnico t = new Tecnico();
        t.setIdEmpleado(empleado.getId());
        t.setEspecialidad(especialidad);
        MF.guardarTecnico(t);
        return t;
    }

    private static Vehiculo vehiculo(String placa, String marca, String modelo, TipoVehiculo tipo, int anio)
            throws Exception {
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setMarca(marca);
        v.setModelo(modelo);
        v.setTipo(tipo);
        v.setAnio(anio);
        MF.registrarVehiculo(v);
        return v;
    }

    private static void repuesto(String codigo, String nombre, int cantidad, int minimo, String precio)
            throws Exception {
        Repuesto r = new Repuesto();
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setCantidad(cantidad);
        r.setStockMinimo(minimo);
        r.setPrecio(new BigDecimal(precio));
        MF.guardarRepuesto(r);
    }

    private static void orden(Vehiculo v, Tecnico t, String motivo, int kilometraje) throws Exception {
        OrdenMantenimiento o = new OrdenMantenimiento();
        o.setIdVehiculo(v.getId());
        o.setIdTecnico(t.getId());
        o.setMotivo(motivo);
        o.setKilometraje(kilometraje);
        o.setPrioridad(Prioridad.MEDIA);
        MF.guardarOrden(o);
    }
}
