package modelo;

import java.io.IOException;
import java.net.Socket;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.time.Instant;

/**
 * IniciarServidoresReplicados (mejorado)
 *
 * - Crea 3 ServidorBlockchain, los arranca en hilos separados.
 * - Espera a que los puertos estén up (replicación + web).
 * - Registra peers directamente vía getServicioReplicacion().registrarPeer(...)
 *   evitando así dependencia de comandos TCP de registro.
 */
public class IniciarServidoresReplicados {

    private static final String RUTA_LLAVE_PUBLICA = "public_key_descifrada.pem";
    private static final String RUTA_LLAVE_PRIVADA = "private_key_descifrada.pem";

    public static void main(String[] args) {
        try {
            System.out.println("╔═══════════════════════════════════════════╗");
            System.out.println("║  🚀 INICIANDO 3 SERVIDORES REPLICADOS    ║");
            System.out.println("╚═══════════════════════════════════════════╝\n");

            // Configurar BouncyCastle
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            // Cargar las llaves
            System.out.println("🔑 Cargando llaves criptográficas...");
            PublicKey pubKey = Encriptador.cargarLlavePublica(RUTA_LLAVE_PUBLICA);
            PrivateKey privKey = Encriptador.cargarLlavePrivada(RUTA_LLAVE_PRIVADA);
            System.out.println("✅ Llaves cargadas\n");

            // Crear blockchains independientes (ajusta dificultad si quieres)
            System.out.println("⛓️  Creando blockchains...");
            Blockchain bc1 = new Blockchain(4);
            Blockchain bc2 = new Blockchain(4);
            Blockchain bc3 = new Blockchain(4);
            System.out.println("✅ Blockchains creados\n");

            System.out.println("🚀 Iniciando servidores en hilos separados...\n");

            // --- Crear instancias de ServidorBlockchain (guardamos referencias) ---
            final ServidorBlockchain s1 = new ServidorBlockchain("Servidor-1", 8080, 8081, bc1, pubKey, privKey);
            final ServidorBlockchain s2 = new ServidorBlockchain("Servidor-2", 8090, 8091, bc2, pubKey, privKey);
            final ServidorBlockchain s3 = new ServidorBlockchain("Servidor-3", 8100, 8101, bc3, pubKey, privKey);

            // Lanzar hilos que ejecutan run() de cada servidor
            Thread t1 = new Thread(() -> {
                try {
                    System.out.println("🟢 [HILO-1] Iniciando Servidor-1 en puerto 8080...");
                    s1.run();
                } catch (Exception e) {
                    System.err.println("❌ [HILO-1] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            t1.setName("Servidor-1-Thread");
            t1.setDaemon(false);
            t1.start();

            Thread.sleep(800);

            Thread t2 = new Thread(() -> {
                try {
                    System.out.println("🟢 [HILO-2] Iniciando Servidor-2 en puerto 8090...");
                    s2.run();
                } catch (Exception e) {
                    System.err.println("❌ [HILO-2] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            t2.setName("Servidor-2-Thread");
            t2.setDaemon(false);
            t2.start();

            Thread.sleep(800);

            Thread t3 = new Thread(() -> {
                try {
                    System.out.println("🟢 [HILO-3] Iniciando Servidor-3 en puerto 8100...");
                    s3.run();
                } catch (Exception e) {
                    System.err.println("❌ [HILO-3] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            t3.setName("Servidor-3-Thread");
            t3.setDaemon(false);
            t3.start();

            // Esperar a que los servidores abran sus puertos (replicación + web)
            System.out.println("\n⏳ Esperando que puertos de replicación y web estén arriba...");
            boolean s1Ready = esperarPuertoArriba("localhost", 8080, 20, 250);
            boolean s1WebReady = esperarPuertoArriba("localhost", 8081, 20, 250);
            boolean s2Ready = esperarPuertoArriba("localhost", 8090, 20, 250);
            boolean s2WebReady = esperarPuertoArriba("localhost", 8091, 20, 250);
            boolean s3Ready = esperarPuertoArriba("localhost", 8100, 20, 250);
            boolean s3WebReady = esperarPuertoArriba("localhost", 8101, 20, 250);

            System.out.println("\n📣 Estado de readiness:");
            System.out.println("  Servidor-1 replicación: " + (s1Ready ? "UP" : "NO") + " | web: " + (s1WebReady ? "UP" : "NO"));
            System.out.println("  Servidor-2 replicación: " + (s2Ready ? "UP" : "NO") + " | web: " + (s2WebReady ? "UP" : "NO"));
            System.out.println("  Servidor-3 replicación: " + (s3Ready ? "UP" : "NO") + " | web: " + (s3WebReady ? "UP" : "NO"));

            // Solo registrar peers si los servidores están listos (al menos puerto replicación)
            if (s1Ready && s2Ready && s3Ready) {
                System.out.println("\n🔗 Registrando peers directamente en los servicios (por referencia)...");
                // Servidor 1 conoce a 2 y 3
                s1.getServicioReplicacion().registrarPeer("localhost", 8090);
                s1.getServicioReplicacion().registrarPeer("localhost", 8100);

                // Servidor 2 conoce a 1 y 3
                s2.getServicioReplicacion().registrarPeer("localhost", 8080);
                s2.getServicioReplicacion().registrarPeer("localhost", 8100);

                // Servidor 3 conoce a 1 y 2
                s3.getServicioReplicacion().registrarPeer("localhost", 8080);
                s3.getServicioReplicacion().registrarPeer("localhost", 8090);

                System.out.println("✅ Peers registrados por referencia.");
            } else {
                System.err.println("⚠ Algunos servidores de replicación no están listos. No se registraron peers automáticamente.");
            }

            System.out.println("\n╔═══════════════════════════════════════════╗");
            System.out.println("║  ✅ 3 SERVIDORES INICIADOS                ║");
            System.out.println("╚═══════════════════════════════════════════╝");
            System.out.println("\n📊 Monitores web disponibles (si están UP):");
            System.out.println("   • Servidor-1: http://localhost:8081");
            System.out.println("   • Servidor-2: http://localhost:8091");
            System.out.println("   • Servidor-3: http://localhost:8101");
            System.out.println("\n⏳ Espera 5 segundos y luego ejecuta:");
            System.out.println("   TestReplicacion.main()  para comprobar la configuración\n");

            // Mantener vivos los hilos (join)
            t1.join();
            t2.join();
            t3.join();

        } catch (Exception e) {
            System.err.println("❌ Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Intenta conectar por TCP al host:port repetidamente.
     * @param host host (ej: "localhost")
     * @param port puerto
     * @param maxAttempts número máximo de intentos
     * @param delayMs retraso entre intentos en ms
     * @return true si pudo conectar al menos 1 vez
     */
    private static boolean esperarPuertoArriba(String host, int port, int maxAttempts, long delayMs) {
        for (int i = 1; i <= maxAttempts; i++) {
            try (Socket s = new Socket(host, port)) {
                // conectado -> puerto arriba
                return true;
            } catch (IOException ignored) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) { /* ignore */ }
            }
        }
        return false;
    }
}
