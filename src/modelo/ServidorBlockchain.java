package modelo;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorBlockchain implements Runnable {
    
    private Blockchain blockchain;
    private final int PUERTO = 8080;
    private final int PUERTO_WEB = 8081; //Puerto para el monitor web
    
    private static final int MAX_CONEXIONES_POR_IP = 3;
    private static ConcurrentHashMap<String, AtomicInteger> conexionesPorIP = new ConcurrentHashMap<>();
    
    private PublicKey llavePublica;
    private PrivateKey llavePrivada; 
    
    private static final String RUTA_LLAVE_PUBLICA = "public_key_descifrada.pem";
    private static final String RUTA_LLAVE_PRIVADA = "private_key_descifrada.pem";
    
    public ServidorBlockchain(Blockchain blockchain, PublicKey pubKey, PrivateKey privKey) {
        this.blockchain = blockchain;
        this.llavePublica = pubKey;
        this.llavePrivada = privKey;
    }
    
    // Iniciar servidor web de monitoreo
    private void iniciarMonitorWeb() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO_WEB), 0);
            
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String response = generarHTMLEstado();
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            });
            
            server.setExecutor(null);
            server.start();
            System.out.println("🌐 Monitor Web disponible en: http://localhost:" + PUERTO_WEB);
            
        } catch (IOException e) {
            System.err.println("❌ Error al iniciar monitor web: " + e.getMessage());
        }
    }
    
    //Generar HTML con estado del servidor
    private String generarHTMLEstado() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta http-equiv='refresh' content='5'>"); // Auto-refresh cada 5 seg
        html.append("<title>Monitor Servidor Blockchain</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #1a1a2e; color: #eee; }");
        html.append("h1 { color: #0f3460; }");
        html.append(".card { background: #16213e; padding: 20px; border-radius: 10px; margin: 20px 0; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }");
        html.append(".ip { color: #e94560; font-weight: bold; }");
        html.append(".count { color: #00ff88; font-size: 24px; }");
        html.append(".status { background: #0f3460; padding: 10px; border-radius: 5px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #0f3460; }");
        html.append("th { background: #0f3460; color: #00ff88; }");
        html.append("</style></head><body>");
        
        html.append("<h1>🔗 Monitor del Servidor Blockchain</h1>");
        
        html.append("<div class='card'>");
        html.append("<div class='status'>");
        html.append("🟢 <strong>Estado:</strong> Servidor activo en puerto <strong>" + PUERTO + "</strong><br>");
        html.append("📊 <strong>Total conexiones activas:</strong> <span class='count'>" + getTotalConexiones() + "</span>");
        html.append("</div>");
        html.append("</div>");
        
        html.append("<div class='card'>");
        html.append("<h2>📋 Conexiones por IP</h2>");
        
        if (conexionesPorIP.isEmpty()) {
            html.append("<p>No hay conexiones activas actualmente.</p>");
        } else {
            html.append("<table>");
            html.append("<tr><th>IP Cliente</th><th>Conexiones Activas</th><th>Estado</th></tr>");
            
            for (var entry : conexionesPorIP.entrySet()) {
                String ip = entry.getKey();
                int count = entry.getValue().get();
                String estado = count >= MAX_CONEXIONES_POR_IP ? "⚠️ LÍMITE ALCANZADO" : "✅ Normal";
                
                html.append("<tr>");
                html.append("<td class='ip'>" + ip + "</td>");
                html.append("<td class='count'>" + count + "</td>");
                html.append("<td>" + estado + "</td>");
                html.append("</tr>");
            }
            html.append("</table>");
        }
        html.append("</div>");
        
        html.append("<div class='card'>");
        html.append("<h2>⛓️ Estado del Blockchain</h2>");
        html.append("<p><strong>Bloques en la cadena:</strong> " + blockchain.getCadena().size() + "</p>");
        html.append("<p><strong>Dificultad:</strong> " + blockchain.getDificultad() + "</p>");
        html.append("<p><strong>Hash último bloque:</strong> <code>" + blockchain.obtenerUltimoBloque().getHash() + "</code></p>");
        html.append("</div>");
        
        html.append("<p style='text-align: center; color: #666; margin-top: 40px;'>");
        html.append("🔄 Actualizando cada 5 segundos...");
        html.append("</p>");
        
        html.append("</body></html>");
        return html.toString();
    }
    
    private int getTotalConexiones() {
        return conexionesPorIP.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
    }
    
    @Override
    public void run() {
        //Iniciar el monitor web
        iniciarMonitorWeb();
        
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("✅ Servidor Blockchain iniciado en el puerto " + PUERTO);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                
                AtomicInteger contador = conexionesPorIP.computeIfAbsent(clientIP, k -> new AtomicInteger(0));
                
                if (contador.get() >= MAX_CONEXIONES_POR_IP) {
                    System.out.println("⚠️ Demasiadas conexiones desde: " + clientIP + " (Rechazada)");
                    clientSocket.close();
                    continue;
                }
                
                contador.incrementAndGet();
                System.out.println("✅ Cliente conectado desde: " + clientIP + " (Total: " + contador.get() + ")");
                
                new Thread(new ClientHandler(clientSocket, blockchain, llavePublica, llavePrivada, contador, clientIP)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Error al iniciar el servidor: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            PublicKey pubKey = Encriptador.cargarLlavePublica(RUTA_LLAVE_PUBLICA);
            PrivateKey privKey = Encriptador.cargarLlavePrivada(RUTA_LLAVE_PRIVADA); 
            
            Blockchain bc = new Blockchain(4);
            
            new Thread(new ServidorBlockchain(bc, pubKey, privKey)).start();

        } catch (Exception e) {
            System.err.println("❌ Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Blockchain blockchain;
        private PublicKey llavePublica;
        private PrivateKey llavePrivada;
        private AtomicInteger contadorConexiones;
        private String clientIP;

        public ClientHandler(Socket socket, Blockchain bc, PublicKey pubKey, PrivateKey privKey, 
                           AtomicInteger contador, String ip) {
            this.clientSocket = socket;
            this.blockchain = bc;
            this.llavePublica = pubKey;
            this.llavePrivada = privKey;
            this.contadorConexiones = contador;
            this.clientIP = ip;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                clientSocket.setSoTimeout(30000);
                
                String line;
                StringBuilder request = new StringBuilder();
                
                while ((line = in.readLine()) != null) {
                    if (line.isEmpty()) break;
                    request.append(line).append("\n");
                }
                
                String comando = request.toString().trim();
                
                if (comando.startsWith("SEND_TX:")) {
                    String jsonBloqueTexto = comando.substring("SEND_TX:".length()).trim();
                    out.println("SUCCESS: Bloque recibido. Minería en curso.");
                    
                } else if (comando.equals("PING")) {
                    out.println("PONG");
                    
                } else {
                    out.println("ERROR: Comando no reconocido.");
                }

            } catch (IOException e) {
                System.err.println("⚠️ Error de comunicación con " + clientIP + ": " + e.getMessage());
            } finally {
                try { 
                    clientSocket.close(); 
                    contadorConexiones.decrementAndGet();
                    
                   
                    if (contadorConexiones.get() == 0) {
                        conexionesPorIP.remove(clientIP);
                    }
                    
                    System.out.println("🔌 Cliente desconectado: " + clientIP + " (Restantes: " + contadorConexiones.get() + ")");
                } catch (IOException ignored) {}
            }
        }
    }
}
