package modelo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * ServidorBlockchain adaptado a la API real de Bloque (constructor Bloque(String hashAnterior))
 *
 * - Crea Bloque usando new Bloque(hashAnterior)
 * - Usa setDatosEncriptados(ArrayList<String>), setLlaveAesEncriptada(String), setIndex(int)
 * - Responde a PING, GET_CHAIN, LIST_PEERS, REPLICATE_BLOCK
 *
 * Nota: si tu Blockchain.agregarBloque necesita el nonce/timestamp exacto, entonces
 *       habrá que añadir setters en Bloque o un constructor completo. Más abajo explico las opciones.
 */
public class ServidorBlockchain implements Runnable {

    private final String nombre;
    private final int puertoReplicacion;
    private final int puertoMonitorWeb;
    private final Blockchain blockchain;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    private volatile boolean running = true;
    private ServerSocket serverSocketReplicacion;
    private HttpServer httpServerMonitor;

    private final ServicioReplicacion servicioReplicacion;

    public ServidorBlockchain(String nombre, int puertoReplicacion, int puertoMonitorWeb,
                              Blockchain blockchain, PublicKey pubKey, PrivateKey privKey) {
        this.nombre = nombre;
        this.puertoReplicacion = puertoReplicacion;
        this.puertoMonitorWeb = puertoMonitorWeb;
        this.blockchain = blockchain;
        this.publicKey = pubKey;
        this.privateKey = privKey;
        this.servicioReplicacion = new ServicioReplicacion();
    }

    @Override
    public void run() {
        try {
            iniciarReplicacion();
        } catch (Exception e) {
            System.err.println("[" + nombre + "] Error al iniciar replicación:");
            e.printStackTrace();
        }

        try {
            iniciarMonitorWeb();
        } catch (Exception e) {
            System.err.println("[" + nombre + "] Error al iniciar monitor web:");
            e.printStackTrace();
        }
    }

    // -----------------------
    // Servidor TCP replicación
    // -----------------------
    private void iniciarReplicacion() throws Exception {
        try {
            serverSocketReplicacion = new ServerSocket(puertoReplicacion);
            System.out.println("[" + nombre + "] Replicación escuchando en puerto " + puertoReplicacion);
        } catch (BindException be) {
            System.err.println("[" + nombre + "] BindException replicación en puerto " + puertoReplicacion + ": " + be.getMessage());
            throw be;
        } catch (Exception ex) {
            System.err.println("[" + nombre + "] No se pudo iniciar ServerSocket replicación: " + ex.getMessage());
            throw ex;
        }

        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket cliente = serverSocketReplicacion.accept();
                    new Thread(() -> procesarConexionReplicacion(cliente)).start();
                } catch (Exception e) {
                    if (running) {
                        System.err.println("[" + nombre + "] Error accept() replicacion: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }, nombre + "-Replicacion-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void procesarConexionReplicacion(Socket cliente) {
        String remote = cliente.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String linea = in.readLine();
            if (linea == null) {
                out.println("ERR|Empty");
                return;
            }
            linea = linea.trim();

            if ("PING".equalsIgnoreCase(linea)) {
                out.println("PONG");
                System.out.println("[" + nombre + "] PING -> PONG desde " + remote);
                return;
            }

            if ("GET_CHAIN".equalsIgnoreCase(linea)) {
                int total = blockchain != null && blockchain.getCadena() != null ? blockchain.getCadena().size() : 0;
                out.println("OK|" + total);
                System.out.println("[" + nombre + "] GET_CHAIN -> " + total);
                return;
            }

            if ("LIST_PEERS".equalsIgnoreCase(linea)) {
                List<String> peers = servicioReplicacion.listarPeers();
                StringBuilder sb = new StringBuilder("OK");
                for (String p : peers) sb.append("|").append(p);
                out.println(sb.toString());
                System.out.println("[" + nombre + "] LIST_PEERS -> " + peers.size());
                return;
            }

            if (linea.startsWith("REPLICATE_BLOCK|")) {
                // Parseamos y construimos Bloque usando la API disponible
                String[] partes = linea.split("\\|", 9); // límite para capturar el resto
                if (partes.length < 8) {
                    out.println("ERR|Malformed");
                    System.err.println("[" + nombre + "] REPLICATE_BLOCK malformado desde " + remote + " -> " + linea);
                    return;
                }

                try {
                    int index = Integer.parseInt(partes[1]);
                    String hash = partes[2];
                    String hashAnterior = partes[3];
                    long nonce = parseLongSafe(partes[4]);
                    long timestamp = parseLongSafe(partes[5]);
                    String llaveAes = partes[6];
                    String datosEncConcatenados = partes.length > 7 ? partes[7] : "";

                    // Convertir datosEnc concatenados (si se usó ":" como separador)
                    ArrayList<String> listaDatos = new ArrayList<>();
                    if (!datosEncConcatenados.isEmpty()) {
                        // si usas ":" para separar múltiples datos en mensaje, dividir
                        if (datosEncConcatenados.contains(":")) {
                            String[] arr = datosEncConcatenados.split(":");
                            Collections.addAll(listaDatos, arr);
                        } else {
                            listaDatos.add(datosEncConcatenados);
                        }
                    }

                    // Construir bloque con constructor existente
                    Bloque b = new Bloque(hashAnterior); // usa constructor Bloque(String)
                    // setear datos encriptados y llave AES
                    b.setDatosEncriptados(listaDatos);
                    b.setLlaveAesEncriptada(llaveAes);
                    b.setIndex(index);
                    // NOTA: no existe setter para nonce ni para timestamp ni para hash
                    // Por eso no intentamos fijarlos aquí (si los necesitas, añade setters en Bloque).

                    // Insertar en la blockchain local (asume que agregarBloque acepta este Bloque)
                    try {
                        blockchain.agregarBloque(b);
                        System.out.println("[" + nombre + "] Bloque replicado agregado index=" + index + " (hash recibido: " + shortHash(hash) + ")");
                        out.println("OK|ADDED");
                    } catch (Exception exAdd) {
                        System.err.println("[" + nombre + "] Error agregando bloque replicado: " + exAdd.getMessage());
                        exAdd.printStackTrace();
                        out.println("ERR|ADD_FAILED");
                    }

                } catch (Exception ex) {
                    System.err.println("[" + nombre + "] Error parseando REPLICATE_BLOCK: " + ex.getMessage());
                    ex.printStackTrace();
                    out.println("ERR|" + ex.getMessage());
                }
                return;
            }
            
            // Manejar registro remoto de peers (ej: ConfigurarReplicacion envía esto)
if (linea.startsWith("REGISTER_PEER|")) {
    try {
        String[] parts = linea.split("\\|");
        if (parts.length < 3) {
            out.println("ERR|MALFORMED");
            return;
        }
        String host = parts[1];
        int port = Integer.parseInt(parts[2]);
        servicioReplicacion.registrarPeer(host, port);
        out.println("OK|REGISTERED|" + host + ":" + port);
        System.out.println("[" + nombre + "] REGISTER_PEER -> " + host + ":" + port);
    } catch (NumberFormatException nfe) {
        out.println("ERR|BAD_PORT");
    } catch (Exception ex) {
        out.println("ERR|FAILED");
        System.err.println("[" + nombre + "] Error REGISTER_PEER: " + ex.getMessage());
        ex.printStackTrace();
    }
    return;
}


            out.println("ERR|UNKNOWN_CMD");
            System.out.println("[" + nombre + "] Comando desconocido desde " + remote + ": " + linea);

        } catch (Exception e) {
            System.err.println("[" + nombre + "] Error procesando conexión replicacion desde " + remote + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { cliente.close(); } catch (Exception ignored) {}
        }
    }

    private long parseLongSafe(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }

    private String shortHash(String s) {
        if (s == null) return "";
        return s.length() > 12 ? s.substring(0, 12) + "..." : s;
    }

    // -----------------------
    // Monitor web HTTP
    // -----------------------
    private void iniciarMonitorWeb() throws Exception {
        try {
            httpServerMonitor = HttpServer.create(new InetSocketAddress(puertoMonitorWeb), 0);
            httpServerMonitor.createContext("/", new RootHandler());
            httpServerMonitor.createContext("/chain", new ChainHandler());
            httpServerMonitor.setExecutor(Executors.newFixedThreadPool(4));
            httpServerMonitor.start();
            System.out.println("[" + nombre + "] Monitor web iniciado en http://localhost:" + puertoMonitorWeb + "/");
        } catch (BindException be) {
            System.err.println("[" + nombre + "] BindException monitor web puerto " + puertoMonitorWeb + ": " + be.getMessage());
            throw be;
        } catch (Exception ex) {
            System.err.println("[" + nombre + "] Error iniciando monitor web: " + ex.getMessage());
            throw ex;
        }
    }

    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String respuesta = "<html><body>"
                    + "<h2>" + nombre + " - Monitor</h2>"
                    + "<p>replicación: " + puertoReplicacion + "</p>"
                    + "<p>web: " + puertoMonitorWeb + "</p>"
                    + "<p>timestamp: " + Instant.now().toString() + "</p>"
                    + "<p><a href=\"/chain\">Ver cadena (JSON)</a></p>"
                    + "</body></html>";
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private class ChainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"chain\":[");
            boolean first = true;
            for (Bloque b : blockchain.getCadena()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{");
                sb.append("\"index\":").append(b.getIndex()).append(",");
                sb.append("\"hashAnterior\":\"").append(escapeJson(b.getHashAnterior())).append("\",");
                sb.append("\"nonce\":").append(b.getNonce()).append(",");
                sb.append("\"timestamp\":").append(b.getTimestamp());
                sb.append("}");
            }
            sb.append("]}");
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private String escapeJson(String s) { if (s == null) return ""; return s.replace("\"", "\\\""); }

    // -----------------------
    // ServicioReplicacion
    // -----------------------
    public class ServicioReplicacion {
        private final Map<String, Integer> peers = new HashMap<>();

        public void registrarPeer(String host, int puerto) {
            String key = host + ":" + puerto;
            peers.put(key, puerto);
            System.out.println("[" + nombre + "] Peer registrado: " + key + " | Total peers: " + peers.size());
        }

        public void eliminarPeer(String host, int puerto) { peers.remove(host + ":" + puerto); }

        public List<String> listarPeers() { return new ArrayList<>(peers.keySet()); }

        public void replicarBloque(Bloque bloque) {
            String datosEnc = "";
            if (bloque.getDatosEncriptados() != null && !bloque.getDatosEncriptados().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < bloque.getDatosEncriptados().size(); i++) {
                    sb.append(bloque.getDatosEncriptados().get(i));
                    if (i < bloque.getDatosEncriptados().size() - 1) sb.append(":");
                }
                datosEnc = sb.toString();
            }

            String mensaje = "REPLICATE_BLOCK|"
                    + bloque.getIndex() + "|"
                    + (bloque.getHash() != null ? bloque.getHash() : "") + "|"
                    + (bloque.getHashAnterior() != null ? bloque.getHashAnterior() : "") + "|"
                    + bloque.getNonce() + "|"
                    + bloque.getTimestamp() + "|"
                    + (bloque.getLlaveAesEncriptada() != null ? bloque.getLlaveAesEncriptada() : "") + "|"
                    + datosEnc;

            for (String key : peers.keySet()) {
                String[] parts = key.split(":");
                if (parts.length < 2) continue;
                String host = parts[0];
                int port;
                try { port = Integer.parseInt(parts[1]); } catch (NumberFormatException nfe) { continue; }

                new Thread(() -> {
                    try (Socket socket = new Socket(host, port);
                         PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                        out.println(mensaje);
                        String respuesta = in.readLine();
                        if (respuesta != null && respuesta.startsWith("OK")) {
                            System.out.println("[" + nombre + "] Bloque replicado OK a " + host + ":" + port + " -> " + respuesta);
                        } else {
                            System.err.println("[" + nombre + "] Replicación a " + host + ":" + port + " respondió: " + respuesta);
                        }
                    } catch (Exception e) {
                        System.err.println("[" + nombre + "] No se pudo replicar a " + host + ":" + port + " -> " + e.getMessage());
                    }
                }).start();
            }
        }
    }

    public ServicioReplicacion getServicioReplicacion() { return this.servicioReplicacion; }

    public void stop() {
        running = false;
        try { if (serverSocketReplicacion != null && !serverSocketReplicacion.isClosed()) serverSocketReplicacion.close(); } catch (Exception ignored) {}
        try { if (httpServerMonitor != null) httpServerMonitor.stop(0); } catch (Exception ignored) {}
        System.out.println("[" + nombre + "] Servidor detenido.");
    }
}
