package com.concesionario;

import com.concesionario.apoyo.model.TipoVehiculo;
import com.concesionario.apoyo.model.Vehiculo;
import com.concesionario.config.ConexionBD;
import com.concesionario.config.InicializadorBD;
import com.concesionario.mantenimiento.model.Tecnico;
import com.concesionario.mantenimiento.service.MantenimientoService;
import com.concesionario.rrhh.model.Empleado;
import com.concesionario.rrhh.model.EstadoEmpleado;
import com.concesionario.rrhh.service.RecursosHumanosService;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;

/** Base de las pruebas: usa una base H2 en memoria que se reinicia antes de cada prueba. */
public abstract class PruebaBase {

    @BeforeEach
    public void prepararBaseDeDatos() throws Exception {
        System.setProperty(ConexionBD.PROPIEDAD_URL, "jdbc:h2:mem:pruebas;DB_CLOSE_DELAY=-1");
        InicializadorBD.reiniciar();
    }

    protected Empleado crearEmpleado(String documento, EstadoEmpleado estado) throws Exception {
        Empleado e = new Empleado();
        e.setDocumento(documento);
        e.setNombres("Nombre" + documento);
        e.setApellidos("Apellido");
        e.setCargo("Técnico");
        e.setFechaIngreso(LocalDate.of(2024, 1, 15));
        e.setEstado(estado);
        new RecursosHumanosService().guardarEmpleado(e);
        return e;
    }

    protected Vehiculo crearVehiculo(String placa) throws Exception {
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setMarca("Renault");
        v.setModelo("Logan");
        v.setTipo(TipoVehiculo.AUTOMOVIL);
        v.setAnio(2022);
        new MantenimientoService().registrarVehiculo(v);
        return v;
    }

    protected Tecnico crearTecnico(String documento, boolean disponible) throws Exception {
        Empleado e = crearEmpleado(documento, EstadoEmpleado.ACTIVO);
        Tecnico t = new Tecnico();
        t.setIdEmpleado(e.getId());
        t.setEspecialidad("Motor");
        t.setDisponible(disponible);
        new MantenimientoService().guardarTecnico(t);
        return t;
    }
}
