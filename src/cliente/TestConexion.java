
package cliente;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestConexion {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║     🧪 TEST DE CONEXIÓN AL SERVIDOR   ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // TEST 1: PING
        System.out.println("📡 Test 1: PING");
        if (testPing()) {
            System.out.println("   ✅ PING exitoso\n");
        } else {
            System.out.println("   ❌ PING falló\n");
            return;
        }
        
        // TEST 2: GET_CHAIN
        System.out.println("📡 Test 2: Obtener total de bloques");
        int bloques = testGetChain();
        if (bloques > 0) {
            System.out.println("   ✅ Total bloques: " + bloques + "\n");
        } else {
            System.out.println("   ❌ No se pudo obtener bloques\n");
        }
        
        // TEST 3: VALIDATE_CHAIN
        System.out.println("📡 Test 3: Validar blockchain");
        if (testValidateChain()) {
            System.out.println("   ✅ Blockchain válido\n");
        } else {
            System.out.println("   ⚠️ Blockchain inválido\n");
        }
        
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  ✅ COMUNICACIÓN SERVIDOR-CLIENTE OK  ║");
        System.out.println("╚════════════════════════════════════════╝");
    }
    
    private static boolean testPing() {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println("PING");
            String respuesta = in.readLine();
            
            System.out.println("   Enviado: PING");
            System.out.println("   Recibido: " + respuesta);
            
            return "PONG".equals(respuesta);
            
        } catch (IOException e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    private static int testGetChain() {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println("GET_CHAIN");
            String respuesta = in.readLine();
            
            System.out.println("   Enviado: GET_CHAIN");
            System.out.println("   Recibido: " + respuesta);
            
            if (respuesta != null && respuesta.startsWith("OK")) {
                String[] partes = respuesta.split("\\|");
                if (partes.length > 1) {
                    return Integer.parseInt(partes[1]);
                }
            }
            
            return -1;
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return -1;
        }
    }
    
    private static boolean testValidateChain() {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            out.println("VALIDATE_CHAIN");
            String respuesta = in.readLine();
            
            System.out.println("   Enviado: VALIDATE_CHAIN");
            System.out.println("   Recibido: " + respuesta);
            
            return respuesta != null && respuesta.startsWith("OK");
            
        } catch (IOException e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
}
