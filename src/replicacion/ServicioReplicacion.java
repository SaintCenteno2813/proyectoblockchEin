
package replicacion;

import modelo.Bloque;
import modelo.Blockchain;
import protocolo.ProtocoloBlockchain;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio para manejar la replicación de bloques entre servidores
 */
public class ServicioReplicacion {
    
    private Blockchain blockchain;
    private List<String> servidoresPeers; // Lista de servidores conectados
    
    public ServicioReplicacion(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.servidoresPeers = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Registra un nuevo servidor peer
     */
    public void registrarPeer(String host, int puerto) {
        String peer = host + ":" + puerto;
        if (!servidoresPeers.contains(peer)) {
            servidoresPeers.add(peer);
            System.out.println("🔗 Peer registrado: " + peer);
            System.out.println("📊 Total peers: " + servidoresPeers.size());
        }
    }
    
    /**
     * Elimina un peer desconectado
     */
    public void eliminarPeer(String host, int puerto) {
        String peer = host + ":" + puerto;
        servidoresPeers.remove(peer);
        System.out.println("🔌 Peer eliminado: " + peer);
    }
    
    /**
     * Obtiene la lista de peers
     */
    public List<String> obtenerPeers() {
        return new ArrayList<>(servidoresPeers);
    }
    
    /**
     * Replica un bloque a todos los peers conectados
     */
    public void replicarBloque(Bloque bloque) {
        if (servidoresPeers.isEmpty()) {
            System.out.println("⚠️  No hay peers para replicar");
            return;
        }
        
        System.out.println("📤 Replicando bloque #" + bloque.getIndex() + " a " + servidoresPeers.size() + " peer(s)");
        
        for (String peer : servidoresPeers) {
            try {
                String[] partes = peer.split(":");
                String host = partes[0];
                int puerto = Integer.parseInt(partes[1]);
                
                enviarBloqueAPeer(bloque, host, puerto);
                
            } catch (Exception e) {
                System.err.println("❌ Error al replicar a " + peer + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Envía un bloque a un peer específico
     */
    private void enviarBloqueAPeer(Bloque bloque, String host, int puerto) {
        try (Socket socket = new Socket(host, puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Serializar el bloque como mensaje
            String mensaje = ProtocoloBlockchain.crearMensaje(
                ProtocoloBlockchain.REPLICAR_BLOQUE,
                String.valueOf(bloque.getIndex()),
                bloque.getHash(),
                bloque.getHashAnterior(),
                String.valueOf(bloque.getNonce()),
                String.valueOf(bloque.getTimestamp()),
                bloque.getLlaveAesEncriptada(),
                serializarDatosEncriptados(bloque.getDatosEncriptados())
            );
            
            out.println(mensaje);
            String respuesta = in.readLine();
            
            if (respuesta != null && respuesta.startsWith(ProtocoloBlockchain.OK)) {
                System.out.println("✅ Bloque #" + bloque.getIndex() + " replicado a " + host + ":" + puerto);
            } else {
                System.err.println("⚠️  Error al replicar: " + respuesta);
            }
            
        } catch (IOException e) {
            System.err.println("❌ No se pudo conectar a " + host + ":" + puerto);
        }
    }
    
    /**
     * Sincroniza la cadena con otros servidores
     */
    public void sincronizarCadena() {
        if (servidoresPeers.isEmpty()) {
            System.out.println("⚠️  No hay peers para sincronizar");
            return;
        }
        
        System.out.println("🔄 Iniciando sincronización con peers...");
        
        Blockchain cadenaMasLarga = blockchain;
        
        for (String peer : servidoresPeers) {
            try {
                String[] partes = peer.split(":");
                String host = partes[0];
                int puerto = Integer.parseInt(partes[1]);
                
                Blockchain cadenaRemota = obtenerCadenaRemota(host, puerto);
                
                if (cadenaRemota != null && cadenaRemota.getCadena().size() > cadenaMasLarga.getCadena().size()) {
                    if (cadenaRemota.esCadenaValida()) {
                        cadenaMasLarga = cadenaRemota;
                        System.out.println("✅ Cadena más larga encontrada en " + peer);
                    }
                }
                
            } catch (Exception e) {
                System.err.println("❌ Error al sincronizar con " + peer + ": " + e.getMessage());
            }
        }
        
        // Si encontramos una cadena más larga y válida, la adoptamos
        if (cadenaMasLarga.getCadena().size() > blockchain.getCadena().size()) {
            blockchain.setCadena(new ArrayList<>(cadenaMasLarga.getCadena()));
            System.out.println("🔄 Cadena actualizada. Nuevos bloques: " + blockchain.getCadena().size());
        } else {
            System.out.println("✅ Cadena local está actualizada");
        }
    }
    
    /**
     * Obtiene la cadena completa de un servidor remoto
     */
    private Blockchain obtenerCadenaRemota(String host, int puerto) {
        try (Socket socket = new Socket(host, puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println(ProtocoloBlockchain.OBTENER_CADENA);
            String respuesta = in.readLine();
            
            if (respuesta != null && respuesta.startsWith(ProtocoloBlockchain.OK)) {
                String[] partes = ProtocoloBlockchain.parsearMensaje(respuesta);
                if (partes.length > 1) {
                    int totalBloques = Integer.parseInt(partes[1]);
                    System.out.println("📊 Servidor " + host + ":" + puerto + " tiene " + totalBloques + " bloques");
                }
            }
            
            // Por ahora retornamos null (implementación completa requiere más código)
            return null;
            
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Serializa los datos encriptados para envío
     */
    private String serializarDatosEncriptados(ArrayList<String> datosEncriptados) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < datosEncriptados.size(); i++) {
            sb.append(datosEncriptados.get(i));
            if (i < datosEncriptados.size() - 1) {
                sb.append(ProtocoloBlockchain.SEPARADOR_INTERNO);
            }
        }
        return sb.toString();
    }
    
    /**
     * Deserializa los datos encriptados recibidos
     */
    public static ArrayList<String> deserializarDatosEncriptados(String datosSerializados) {
        ArrayList<String> datos = new ArrayList<>();
        if (datosSerializados != null && !datosSerializados.isEmpty()) {
            String[] partes = datosSerializados.split(ProtocoloBlockchain.SEPARADOR_INTERNO);
            for (String parte : partes) {
                datos.add(parte);
            }
        }
        return datos;
    }
}