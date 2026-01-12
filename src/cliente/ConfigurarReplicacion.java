package cliente;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConfigurarReplicacion {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║ CONFIGURANDO REPLICACIÓN P2P      ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        try {
            // Esperar un poco para asegurar que los servidores estén listos
            Thread.sleep(1000);
            
            // Configurar Servidor-1 (8080) con peers en 8090 y 8100
            System.out.println("Configurando Servidor-1...");
            registrarPeer(8080, "localhost", 8090); // Conectar con Servidor-2
            registrarPeer(8080, "localhost", 8100); // Conectar con Servidor-3
            
            // Configurar Servidor-2 (8090) con peers en 8080 y 8100
            System.out.println("Configurando Servidor-2...");
            registrarPeer(8090, "localhost", 8080); // Conectar con Servidor-1
            registrarPeer(8090, "localhost", 8100); // Conectar con Servidor-3
            
            // Configurar Servidor-3 (8100) con peers en 8080 y 8090
            System.out.println("Configurando Servidor-3...");
            registrarPeer(8100, "localhost", 8080); // Conectar con Servidor-1
            registrarPeer(8100, "localhost", 8090); // Conectar con Servidor-2
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║ REPLICACIÓN P2P CONFIGURADA        ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("\nAhora ejecuta TestReplicacion.main()");
            System.out.println("para verificar la configuración");
            
        } catch (Exception e) {
            System.err.println("Error al configurar replicación: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void registrarPeer(int puertoServidor, String hostPeer, int puertoPeer) {
        try (Socket socket = new Socket("localhost", puertoServidor);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Enviar comando REGISTER_PEER
            String comando = "REGISTER_PEER|" + hostPeer + "|" + puertoPeer;
            out.println(comando);
            
            // Leer respuesta
            String respuesta = in.readLine();
            
            if (respuesta != null && respuesta.startsWith("OK")) {
                System.out.println("   Peer registrado: " + hostPeer + ":" + puertoPeer + 
                                 " en servidor puerto " + puertoServidor);
            } else {
                System.out.println("   Respuesta inesperada: " + respuesta);
            }
            
        } catch (IOException e) {
            System.out.println("   Error al registrar peer " + hostPeer + ":" + puertoPeer + 
                             " en servidor " + puertoServidor);
        }
    }
}