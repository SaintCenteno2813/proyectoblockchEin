package modelo;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Erick
 */
public class Bloque {
    private String hash;
    private String hashAnterior;
    private ArrayList<TransaccionInventario> transacciones; // Lista que usamos para construir el JSON cifrado
    private ArrayList<String> datosEncriptados; // Aquí se guarda el JSON cifrado
    private String llaveAesEncriptada; 
    private long tiempoCreacion;
    private int nonce;
    private int index; 

    public Bloque(String hashAnterior) {
        this.hashAnterior = hashAnterior;
        this.tiempoCreacion = new Date().getTime();
        this.transacciones = new ArrayList<>();
        this.datosEncriptados = new ArrayList<>();
        this.llaveAesEncriptada = "";
        this.nonce = 0;
        this.hash = calcularHash();
        this.index = 0;
    }
    
    // Método para agregar transacciones
    public void agregarTransaccion(TransaccionInventario t) {
        transacciones.add(t);
    }

    // método para calcular el hash (usa datos encriptados para la validación)
    public String calcularHash() {
        String datosBloque = hashAnterior + Long.toString(tiempoCreacion) + Integer.toString(nonce) + llaveAesEncriptada;
        for (String dato : datosEncriptados) {
            datosBloque += dato;
        }
        return CriptoUtil.aplicarSha256(datosBloque);
    }
    
    
    /**
     * Recalcula el hash del bloque usando un nonce específico (obtenido de la BD).
     */
    public String calcularHashConNonce(long nonceDelBD) {
        String datos = hashAnterior + 
                       Long.toString(tiempoCreacion) +
                       Long.toString(nonceDelBD) + 
                       llaveAesEncriptada;
                       
        for (String dato : datosEncriptados) {
            datos += dato;
        }
        
        return CriptoUtil.aplicarSha256(datos); 
    }
    
    // prueba de trabajo para minar el bloque
    public void minarBloque(int dificultad) {
        String prefijoDificultad = new String(new char[dificultad]).replace('\0', '0');
        while (!hash.substring(0, dificultad).equals(prefijoDificultad)) {
            nonce++;
            hash = calcularHash();
        }
        System.out.println("Bloque minado: " + hash);
    }

    // Getters y Setters
    public String getHash() { return hash; }
    public String getHashAnterior() { return hashAnterior; }
    public ArrayList<TransaccionInventario> getTransacciones() { return transacciones; }
    
    public ArrayList<String> getDatosEncriptados() { return datosEncriptados; }
    public void setDatosEncriptados(ArrayList<String> datosEncriptados) { 
        this.datosEncriptados = datosEncriptados; 
        this.hash = calcularHash();
    }
    
    public String getLlaveAesEncriptada() { return llaveAesEncriptada; }
    public void setLlaveAesEncriptada(String llaveAesEncriptada) { 
        this.llaveAesEncriptada = llaveAesEncriptada; 
        this.hash = calcularHash();
    }

    public int getIndex() { return index; }
    public long getTimestamp() { return tiempoCreacion; }
    public int getNonce() { return nonce; }
    
    public void setIndex(int index) { this.index = index; }
    
    
 
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"index\": ").append(index).append(",\n");
        sb.append("  \"timestamp\": ").append(tiempoCreacion).append(",\n");
        
        

        sb.append("  \"nonce\": ").append(nonce).append(",\n");
        sb.append("  \"hashAnterior\": \"").append(hashAnterior).append("\",\n");
        sb.append("  \"hash\": \"").append(hash).append("\",\n");
        sb.append("  \"llaveAesEncriptada\": \"").append(llaveAesEncriptada).append("\",\n"); 
        sb.append("  \"datosEncriptados\": ["); 
        
        // El campo de datos encriptados se muestra como un array de strings (Base64)
        for (int i = 0; i < datosEncriptados.size(); i++) {
            sb.append("\"").append(datosEncriptados.get(i)).append("\"");
            if (i < datosEncriptados.size() - 1) {
                sb.append(",");
            }
        }
        
        sb.append("]\n");
        sb.append("}");

        return sb.toString();
    }
}