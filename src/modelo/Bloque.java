package modelo;

import java.util.ArrayList;
import java.util.Date;

public class Bloque {
    private String hash;
    private String hashAnterior;
    private ArrayList<TransaccionInventario> transacciones; // Lista para construir JSON cifrado
    private ArrayList<String> datosEncriptados; // JSON cifrado
    private String llaveAesEncriptada; 
    private long tiempoCreacion;
    private int nonce;
    private int index; 

    // Constructor existente (genera nuevo bloque con tiempo actual)
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

    // ----- Nuevo: constructor completo para replicación exacta -----
    /**
     * Construye un Bloque exactamente con los valores dados (útil para replicación).
     * No recalcula hash: el hash se asigna tal cual se recibe.
     */
    public Bloque(int index, long tiempoCreacion, int nonce, String hash, String hashAnterior,
                  ArrayList<String> datosEncriptados, String llaveAesEncriptada) {
        this.index = index;
        this.tiempoCreacion = tiempoCreacion;
        this.nonce = nonce;
        this.hash = hash != null ? hash : "";
        this.hashAnterior = hashAnterior != null ? hashAnterior : "";
        this.transacciones = new ArrayList<>();
        this.datosEncriptados = datosEncriptados != null ? datosEncriptados : new ArrayList<>();
        this.llaveAesEncriptada = llaveAesEncriptada != null ? llaveAesEncriptada : "";
    }
    // ----------------------------------------------------------------

    // Método para agregar transacciones
    public void agregarTransaccion(TransaccionInventario t) {
        transacciones.add(t);
    }

    // Método para calcular el hash 
    public String calcularHash() {
        String datosBloque = hashAnterior + Long.toString(tiempoCreacion) + Integer.toString(nonce) + llaveAesEncriptada;
        for (String dato : datosEncriptados) {
            datosBloque += dato;
        }
        return CriptoUtil.aplicarSha256(datosBloque);
    }
    
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
    
    // Prueba de trabajo para minar el bloque
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

    // Nuevo setter para hashAnterior
    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
        // NOTA: no recalculamos hash aquí para no invalidar réplicas que traen hash exactamente
    }

    public ArrayList<TransaccionInventario> getTransacciones() { return transacciones; }

    public ArrayList<String> getDatosEncriptados() { return datosEncriptados; }
    public void setDatosEncriptados(ArrayList<String> datosEncriptados) { 
        this.datosEncriptados = datosEncriptados; 
        // NOTA: no recalculamos hash automáticamente
    }
    
    public String getLlaveAesEncriptada() { return llaveAesEncriptada; }
    public void setLlaveAesEncriptada(String llaveAesEncriptada) { 
        this.llaveAesEncriptada = llaveAesEncriptada; 
        // NOTA: no recalculamos hash automáticamente
    }
    
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public long getTimestamp() { return tiempoCreacion; }
    public int getNonce() { return nonce; }

    // ----- Nuevos setters para replicación -----
    /** Forzar nonce (no recalcula hash) */
    public void setNonce(int nonce) { this.nonce = nonce; }

    /** Forzar timestamp (no recalcula hash) */
    public void setTiempoCreacion(long tiempo) { this.tiempoCreacion = tiempo; }

    /** Forzar hash exactamente como vino (útil para replicación) */
    public void setHashDirecto(String hash) { this.hash = hash; }
    // -------------------------------------------

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
