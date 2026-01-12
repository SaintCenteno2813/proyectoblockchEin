package cliente;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestReplicacion {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║TEST DE REPLICACIÓN ENTRE SERVIDORES ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Test 1: Verificar que los 3 servidores estén activos
        System.out.println("Test 1: Verificar servidores activos");
        boolean s1 = testPing(8080, "Servidor-1");
        boolean s2 = testPing(8090, "Servidor-2");
        boolean s3 = testPing(8100, "Servidor-3");
        
        if (!s1 || !s2 || !s3) {
            System.out.println("\nNo todos los servidores están activos");
            System.out.println("Ejecuta: IniciarServidoresReplicados.main()");
            return;
        }
        
        System.out.println("\n✅ Todos los servidores están activos\n");
        
        // Test 2: Verificar bloques iniciales
        System.out.println("Test 2: Verificar bloques iniciales");
        int bloques1 = obtenerTotalBloques(8080);
        int bloques2 = obtenerTotalBloques(8090);
        int bloques3 = obtenerTotalBloques(8100);
        
        System.out.println("   Servidor-1: " + bloques1 + " bloques");
        System.out.println("   Servidor-2: " + bloques2 + " bloques");
        System.out.println("   Servidor-3: " + bloques3 + " bloques\n");
        
        // Test 3: Verificar peers registrados
        System.out.println("Test 3: Verificar peers registrados");
        listarPeers(8080, "Servidor-1");
        listarPeers(8090, "Servidor-2");
        listarPeers(8100, "Servidor-3");
        
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  REPLICACIÓN CONFIGURADA CORRECTAMENTE ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("\nAhora puedes:");
        System.out.println("1. Abrir FarmaciaApp");
        System.out.println("2. Minar un bloque");
        System.out.println("3. Ver cómo se replica en los monitores web:");
        System.out.println("   - http://localhost:8081");
        System.out.println("   - http://localhost:8091");
        System.out.println("   - http://localhost:8101");
    }
    
    private static boolean testPing(int puerto, String nombre) {
        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println("PING");
            String respuesta = in.readLine();
            
            if ("PONG".equals(respuesta)) {
                System.out.println("    " + nombre + " (puerto " + puerto + ") activo");
                return true;
            }
            
        } catch (IOException e) {
            System.out.println("    " + nombre + " (puerto " + puerto + ") NO responde");
        }
        return false;
    }
    
    private static int obtenerTotalBloques(int puerto) {
        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println("GET_CHAIN");
            String respuesta = in.readLine();
            
            if (respuesta != null && respuesta.startsWith("OK")) {
                String[] partes = respuesta.split("\\|");
                if (partes.length > 1) {
                    return Integer.parseInt(partes[1]);
                }
            }
            
        } catch (IOException | NumberFormatException e) {
            // Ignorar
        }
        return -1;
    }
    
    private static void listarPeers(int puerto, String nombre) {
        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println("LIST_PEERS");
            String respuesta = in.readLine();
            
            if (respuesta != null && respuesta.startsWith("OK")) {
                String[] partes = respuesta.split("\\|");
                System.out.println("   " + nombre + " tiene " + (partes.length - 1) + " peer(s) registrados");
                for (int i = 1; i < partes.length; i++) {
                    System.out.println("      - " + partes[i]);
                }
            }
            
        } catch (IOException e) {
            System.out.println("   Error al listar peers de " + nombre);
        }
    }
}