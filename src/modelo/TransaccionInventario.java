package modelo;
import java.io.Serializable;
import java.util.Date;
/**
 *
 * @author Erick
 */
public class TransaccionInventario implements Serializable {
    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";
    public static final String AJUSTE = "AJUSTE";

    private String farmaciaId;
    private Producto producto;
    private String tipoMovimiento;
    private long tiempoCreacion;
    private String hashTransaccion;

    
    private String responsable;
    private String lote;
    private String fechaCaducidad;

    public TransaccionInventario(String farmaciaId, Producto producto, String tipoMovimiento, String responsable, String lote, String fechaCaducidad) {
        this.farmaciaId = farmaciaId;
        this.producto = producto;
        this.tipoMovimiento = tipoMovimiento;
        this.responsable = responsable;
        this.lote = lote;
        this.fechaCaducidad = fechaCaducidad;
        this.tiempoCreacion = new Date().getTime();
        this.hashTransaccion = calcularHash();
    }

    // Calcula el hash de la transaccion
    public String calcularHash() {
        
        String datosTransaccion = farmaciaId + producto.getNombre() + producto.getCodigo() + Integer.toString(producto.getCantidad()) + tipoMovimiento + responsable + lote + fechaCaducidad + Long.toString(tiempoCreacion);
        return CriptoUtil.aplicarSha256(datosTransaccion);
    }

    // Getters
    public String getFarmaciaId() { return farmaciaId; }
    public Producto getProducto() { return producto; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public long getTiempoCreacion() { return tiempoCreacion; }
    public String getHashTransaccion() { return hashTransaccion; }
    
    //  nuevos campos
    public String getResponsable() { return responsable; }
    public String getLote() { return lote; }
    public String getFechaCaducidad() { return fechaCaducidad; }

    @Override
    public String toString() {
        String estado;
        if (tipoMovimiento.equals(ENTRADA)) {
            estado = "Entrada: +";
        } else if (tipoMovimiento.equals(SALIDA)) {
            estado = "Salida: -";
        } else {
            estado = "Ajuste: ";
        }
        return "ID: " + hashTransaccion.substring(0, 8) + "... | " + estado + producto.getCantidad() + " de " + producto.getNombre() + " (" + producto.getCodigo() + ")";
    }
}