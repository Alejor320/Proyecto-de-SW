package com.concesionario.mantenimiento.view;

import com.concesionario.comun.ValidacionException;
import com.concesionario.mantenimiento.controller.MantenimientoController;
import com.concesionario.comun.view.Campos;
import com.concesionario.comun.view.FormularioCrud;
import com.concesionario.mantenimiento.model.Repuesto;

import java.util.List;
import javax.swing.JTextField;

/** Pantalla del CU-MF-02 Gestionar repuestos de taller. */
public class RepuestoPanel extends FormularioCrud<Repuesto> {

    private final MantenimientoController controller;
    private final JTextField txtCodigo = new JTextField(20);
    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtCantidad = new JTextField(20);
    private final JTextField txtStockMinimo = new JTextField(20);
    private final JTextField txtPrecio = new JTextField(20);

    public RepuestoPanel(MantenimientoController controller) {
        super("CU-MF-02 · Gestionar repuestos de taller",
                "ID", "Código", "Nombre", "Cantidad", "Stock mínimo", "Precio", "¿Reabastecer?");
        this.controller = controller;
        agregarCampo("Código:", txtCodigo);
        agregarCampo("Nombre:", txtNombre);
        agregarCampo("Cantidad disponible:", txtCantidad);
        agregarCampo("Stock mínimo:", txtStockMinimo);
        agregarCampo("Precio unitario:", txtPrecio);
        refrescar();
    }

    @Override
    protected List<Repuesto> cargarDatos() throws Exception {
        return controller.listarRepuestos();
    }

    @Override
    protected Object[] fila(Repuesto r) {
        return new Object[]{r.getId(), r.getCodigo(), r.getNombre(), r.getCantidad(), r.getStockMinimo(),
                r.getPrecio(), r.requiereReabastecimiento() ? "SÍ" : "No"};
    }

    @Override
    protected Repuesto leerFormulario() throws ValidacionException {
        Repuesto r = new Repuesto();
        r.setId(seleccionado == null ? 0 : seleccionado.getId());
        r.setCodigo(Campos.texto(txtCodigo));
        r.setNombre(Campos.texto(txtNombre));
        r.setCantidad(Campos.entero(txtCantidad, "Cantidad disponible"));
        r.setStockMinimo(Campos.entero(txtStockMinimo, "Stock mínimo"));
        r.setPrecio(Campos.decimal(txtPrecio, "Precio unitario"));
        return r;
    }

    @Override
    protected void mostrarEnFormulario(Repuesto r) {
        txtCodigo.setText(r.getCodigo());
        txtNombre.setText(r.getNombre());
        txtCantidad.setText(String.valueOf(r.getCantidad()));
        txtStockMinimo.setText(String.valueOf(r.getStockMinimo()));
        txtPrecio.setText(r.getPrecio().toPlainString());
    }

    @Override
    protected void limpiarFormulario() {
        txtCodigo.setText("");
        txtNombre.setText("");
        txtCantidad.setText("0");
        txtStockMinimo.setText("0");
        txtPrecio.setText("");
    }

    @Override
    protected void guardar(Repuesto r) throws Exception {
        controller.guardarRepuesto(r);
    }

    @Override
    protected void eliminar(Repuesto r) throws Exception {
        controller.eliminarRepuesto(r.getId());
    }
}
