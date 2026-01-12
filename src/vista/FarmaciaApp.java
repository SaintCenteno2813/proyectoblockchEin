package vista;

// ──────────────────────────────────────────────────────────
// Imports del proyecto
// ──────────────────────────────────────────────────────────
import modelo.Bloque;
import modelo.Blockchain;
import modelo.Encriptador;
import modelo.Medicamento;
import modelo.Producto;
import modelo.TransaccionInventario;
import modelo.ConexionPostgres;
import java.net.Socket;
// ──────────────────────────────────────────────────────────
// Imports de Java
// ──────────────────────────────────────────────────────────
import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

// ──────────────────────────────────────────────────────────
// Imports externos
// ──────────────────────────────────────────────────────────
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author Erick
 */
public class FarmaciaApp extends JFrame {

    // ──────────────────────────────────────────────────────────
    // Atributos principales
    // ──────────────────────────────────────────────────────────
    private Blockchain blockchain;
    private Bloque bloquePendiente;
    private ArrayList<String> listaFarmacias;

    // Rutas de llaves
    private static final String RUTA_LLAVE_PUBLICA = "public_key_descifrada.pem";
       private static final String RUTA_LLAVE_PRIVADA = "private_key_descifrada.pem";

    // Componentes UI
    private JComboBox<String> comboFarmacias;
    private DefaultListModel<Bloque> listModelBloques;
    private JList<Bloque> listaBloques;

    private DefaultListModel<TransaccionInventario> listModelTransaccionesPendientes;
    private JList<TransaccionInventario> listaTransaccionesPendientes;

    private DefaultListModel<Producto> listModelInventario;
    private JList<Producto> listaInventario;

    private JTextArea areaContenidoBloque;
    private JLabel labelEstado;

    // ──────────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────────
    public FarmaciaApp() {

        // Inicialización
        blockchain = new Blockchain(4);
        bloquePendiente = new Bloque(blockchain.obtenerUltimoBloque().getHash());

        listaFarmacias = new ArrayList<>();
        listaFarmacias.add("Farmacia A");
        listaFarmacias.add("Farmacia B");

        configurarVentana();
        configurarPaneles();
    }

    // ──────────────────────────────────────────────────────────
    // UI - Configuración principal
    // ──────────────────────────────────────────────────────────
    private void configurarVentana() {
        setTitle("Inventario de Farmacias con Blockchain");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void configurarPaneles() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        JPanel panelDerecho = new JPanel(new BorderLayout());

        // ─────────────────────────────
        // Combo de farmacias
        // ─────────────────────────────
        comboFarmacias = new JComboBox<>(listaFarmacias.toArray(new String[0]));
        comboFarmacias.addActionListener(e -> actualizarInventario());
        panelIzquierdo.add(comboFarmacias, BorderLayout.NORTH);

        // ─────────────────────────────
        // Inventario
        // ─────────────────────────────
        listModelInventario = new DefaultListModel<>();
        listaInventario = new JList<>(listModelInventario);
        listaInventario.setBorder(BorderFactory.createTitledBorder("Inventario Actual"));
        panelIzquierdo.add(new JScrollPane(listaInventario), BorderLayout.CENTER);

        // ─────────────────────────────
        // Bloques
        // ─────────────────────────────
        listModelBloques = new DefaultListModel<>();
        listaBloques = new JList<>(listModelBloques);
        listaBloques.setBorder(BorderFactory.createTitledBorder("Bloques de la Cadena"));
        listModelBloques.addElement(blockchain.getCadena().get(0)); // Génesis

        // ─────────────────────────────
        // Transacciones pendientes
        // ─────────────────────────────
        listModelTransaccionesPendientes = new DefaultListModel<>();
        listaTransaccionesPendientes = new JList<>(listModelTransaccionesPendientes);
        listaTransaccionesPendientes.setBorder(
                BorderFactory.createTitledBorder("Transacciones Pendientes")
        );

        // ─────────────────────────────
        // Contenido desencriptado
        // ─────────────────────────────
        areaContenidoBloque = new JTextArea();
        areaContenidoBloque.setEditable(false);
        areaContenidoBloque.setBorder(
                BorderFactory.createTitledBorder("Contenido del Bloque (Desencriptado)")
        );

        // ─────────────────────────────
        // Split panes
        // ─────────────────────────────
        JSplitPane splitBloquesTransacciones = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(listaBloques),
                new JScrollPane(listaTransaccionesPendientes)
        );
        splitBloquesTransacciones.setDividerLocation(300);

        JSplitPane splitDerecho = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                splitBloquesTransacciones,
                new JScrollPane(areaContenidoBloque)
        );
        splitDerecho.setDividerLocation(300);

        panelDerecho.add(splitDerecho, BorderLayout.CENTER);

        // ─────────────────────────────
        // Panel de botones
        // ─────────────────────────────
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton btnRegistrarEntrada = new JButton("Registrar Entrada");
        JButton btnRegistrarSalida = new JButton("Registrar Salida");
        JButton btnMinarBloque = new JButton("Minar Nuevo Bloque");
        JButton btnValidarCadena = new JButton("Validar Cadena");
        JButton btnVerContenido = new JButton("Ver Contenido del Bloque");

        btnRegistrarEntrada.addActionListener(e -> registrarMovimiento(TransaccionInventario.ENTRADA));
        btnRegistrarSalida.addActionListener(e -> registrarMovimiento(TransaccionInventario.SALIDA));
        btnMinarBloque.addActionListener(this::minarNuevoBloque);
        btnValidarCadena.addActionListener(this::validarCadena);
        btnVerContenido.addActionListener(this::verContenidoBloque);

        panelBotones.add(btnRegistrarEntrada);
        panelBotones.add(btnRegistrarSalida);
        panelBotones.add(btnMinarBloque);
        panelBotones.add(btnValidarCadena);
        panelBotones.add(btnVerContenido);

        // ─────────────────────────────
        // Estado
        // ─────────────────────────────
        labelEstado = new JLabel("Estado: Listo para registrar transacciones.", SwingConstants.CENTER);

        // ─────────────────────────────
        // Ensamblar todo
        // ─────────────────────────────
        panelPrincipal.add(panelIzquierdo, BorderLayout.WEST);
        panelPrincipal.add(panelDerecho, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(labelEstado, BorderLayout.NORTH);

        add(panelPrincipal);
    }

    // ──────────────────────────────────────────────────────────
    // Registrar movimientos
    // ──────────────────────────────────────────────────────────
    private void registrarMovimiento(String tipo) {
        String farmaciaSeleccionada = (String) comboFarmacias.getSelectedItem();

        if (farmaciaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una farmacia.");
            return;
        }

        String nombreProducto = JOptionPane.showInputDialog("Nombre del producto:");
        String codigoProducto = JOptionPane.showInputDialog("Código del producto:");
        int cantidad = Integer.parseInt(
                JOptionPane.showInputDialog("Cantidad a " + (tipo.equals("ENTRADA") ? "ingresar" : "retirar") + ":")
        );

        String responsable = JOptionPane.showInputDialog("Responsable:");
        String lote = JOptionPane.showInputDialog("Lote:");
        String fechaCaducidad = JOptionPane.showInputDialog("Fecha de caducidad (DD/MM/AAAA):");

        Producto producto = new Medicamento(nombreProducto, codigoProducto, cantidad, "250mg");
        TransaccionInventario transaccion = new TransaccionInventario(
                farmaciaSeleccionada, producto, tipo, responsable, lote, fechaCaducidad
        );

        bloquePendiente.agregarTransaccion(transaccion);
        listModelTransaccionesPendientes.addElement(transaccion);

        labelEstado.setText("Estado: Transacción agregada.");
    }

    // ──────────────────────────────────────────────────────────
    // MINAR NUEVO BLOQUE
    // ──────────────────────────────────────────────────────────
    private void minarNuevoBloque(ActionEvent e) {
        if (listModelTransaccionesPendientes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay transacciones para minar.");
            return;
        }

        try {
            // Generar AES
            SecretKey llaveAes = Encriptador.generarLlaveAes();

            // Convertir transacciones a JSON manual
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < bloquePendiente.getTransacciones().size(); i++) {
                TransaccionInventario t = bloquePendiente.getTransacciones().get(i);
                json.append("{")
                        .append("\"farmaciaId\":\"").append(t.getFarmaciaId()).append("\",")
                        .append("\"productoNombre\":\"").append(t.getProducto().getNombre()).append("\",")
                        .append("\"productoCodigo\":\"").append(t.getProducto().getCodigo()).append("\",")
                        .append("\"cantidad\":").append(t.getProducto().getCantidad()).append(",")
                        .append("\"tipoMovimiento\":\"").append(t.getTipoMovimiento()).append("\",")
                        .append("\"responsable\":\"").append(t.getResponsable()).append("\",")
                        .append("\"lote\":\"").append(t.getLote()).append("\",")
                        .append("\"fechaCaducidad\":\"").append(t.getFechaCaducidad()).append("\"")
                        .append("}");
                if (i < bloquePendiente.getTransacciones().size() - 1) json.append(",");
            }
            json.append("]");

            // Cifrar datos con AES
            byte[] datosCifrados = Encriptador.cifrarAes(
                    json.toString().getBytes(StandardCharsets.UTF_8),
                    llaveAes
            );
            String datosBase64 = Base64.getEncoder().encodeToString(datosCifrados);

            // Cifrar llave AES con RSA
            PublicKey llavePublica = Encriptador.cargarLlavePublica(RUTA_LLAVE_PUBLICA);
            byte[] llaveAesCifrada = Encriptador.cifrarLlaveRsa(llaveAes, llavePublica);
            String llaveAesBase64 = Base64.getEncoder().encodeToString(llaveAesCifrada);

            // Guardar en el bloque
            ArrayList<String> listaDatos = new ArrayList<>();
            listaDatos.add(datosBase64);

            bloquePendiente.setDatosEncriptados(listaDatos);
            bloquePendiente.setLlaveAesEncriptada(llaveAesBase64);
            bloquePendiente.setIndex(blockchain.getCadena().size());

            // Minar
            labelEstado.setText("Estado: Minando bloque...");
            bloquePendiente.minarBloque(blockchain.getDificultad());

            // Agregar a la cadena
            blockchain.agregarBloque(bloquePendiente);
            listModelBloques.addElement(bloquePendiente);

            // Replicar
            try {
                replicarBloqueAServidor(bloquePendiente, "localhost", 8080);
            } catch (Exception ex) {
                System.err.println("⚠ No se pudo replicar: " + ex.getMessage());
            }

            // Guardar nonce en BD
            try {
                ConexionPostgres db = new ConexionPostgres();
                db.guardarNonce(
                        bloquePendiente.getIndex(),
                        bloquePendiente.getHash(),
                        (long) bloquePendiente.getNonce()
                );

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error al guardar nonce en BD: " + ex.getMessage()
                );
            }

            // Guardar archivo JSON
            guardarBloqueComoJSON(bloquePendiente, blockchain.getCadena().size() - 1);

            // Reset
            listModelTransaccionesPendientes.clear();
            bloquePendiente = new Bloque(blockchain.obtenerUltimoBloque().getHash());

            actualizarInventario();
            labelEstado.setText("Estado: Bloque minado correctamente.");
            JOptionPane.showMessageDialog(this, "Bloque minado con éxito.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al minar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────────────────
    // VALIDACIÓN DE CADENA + BD
    // ──────────────────────────────────────────────────────────
    private void validarCadena(ActionEvent e) {

        if (!blockchain.esCadenaValida()) {
            JOptionPane.showMessageDialog(this, "La cadena es inválida.");
            labelEstado.setText("Estado: Cadena inválida.");
            return;
        }

        try {
            ConexionPostgres db = new ConexionPostgres();

            for (Bloque bloque : blockchain.getCadena()) {

                if (bloque.getIndex() == 0) continue;   // Saltar génesis

                Long nonceBD = db.obtenerNonce(bloque.getIndex());

                if (nonceBD == null) {
                    JOptionPane.showMessageDialog(this, "Nonce no encontrado en BD.");
                    return;
                }

                String hashRecalculado = bloque.calcularHashConNonce(nonceBD);

                if (!hashRecalculado.equals(bloque.getHash())) {
                    JOptionPane.showMessageDialog(this, "Hash no coincide con el nonce guardado.");
                    return;
                }
            }

            JOptionPane.showMessageDialog(this, "Cadena válida y verificada con BD.");
            labelEstado.setText("Estado: Cadena confiable ✔");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────
    // VER CONTENIDO DE BLOQUE (DESCIFRAR)
    // ──────────────────────────────────────────────────────────
    private void verContenidoBloque(ActionEvent e) {

        Bloque bloque = listaBloques.getSelectedValue();
        if (bloque == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un bloque.");
            return;
        }

        try {
            String datosBase64 = bloque.getDatosEncriptados().get(0);
            String llaveAesBase64 = bloque.getLlaveAesEncriptada();

            char[] contrasenia = Encriptador.pedirContrasenia("Ingrese la contraseña:");

            PrivateKey llavePrivada = Encriptador.cargarLlavePrivada(RUTA_LLAVE_PRIVADA, contrasenia);

            SecretKey llaveAes = Encriptador.descifrarLlaveRsa(
                    Base64.getDecoder().decode(llaveAesBase64),
                    llavePrivada
            );

            byte[] datosDes = Encriptador.descifrarAes(
                    Base64.getDecoder().decode(datosBase64),
                    llaveAes
            );

            areaContenidoBloque.setText(new String(datosDes));

        } catch (Exception ex) {
            areaContenidoBloque.setText("Error al desencriptar.");
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────
    // ACTUALIZAR INVENTARIO
    // ──────────────────────────────────────────────────────────
    private void actualizarInventario() {

        String farmacia = (String) comboFarmacias.getSelectedItem();
        if (farmacia == null) return;

        listModelInventario.clear();
        HashMap<String, Producto> inventario = new HashMap<>();

        for (Bloque bloque : blockchain.getCadena()) {

            for (TransaccionInventario t : bloque.getTransacciones()) {

                if (!t.getFarmaciaId().equals(farmacia)) continue;

                String codigo = t.getProducto().getCodigo();

                inventario.putIfAbsent(
                        codigo,
                        new Producto(t.getProducto().getNombre(), codigo, 0) {
                            @Override public String getTipo() { return "Desconocido"; }
                        }
                );

                Producto p = inventario.get(codigo);
                int cantidad = p.getCantidad();

                cantidad += t.getTipoMovimiento().equals(TransaccionInventario.ENTRADA)
                        ? t.getProducto().getCantidad()
                        : -t.getProducto().getCantidad();

                p.setCantidad(cantidad);
            }
        }

        inventario.values().forEach(listModelInventario::addElement);
    }

    // ──────────────────────────────────────────────────────────
    // GUARDAR BLOQUE COMO ARCHIVO JSON
    // ──────────────────────────────────────────────────────────
   private void guardarBloqueComoJSON(Bloque bloque, int numero) {
    try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(
                    new FileOutputStream("bloque_" + numero + ".json"),
                    StandardCharsets.UTF_8
            )
    )) {
        writer.write(bloque.toString()); // ⭐ Cambiar toJSON() por toString()
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error guardando JSON: " + e.getMessage());
    }
}

    // ──────────────────────────────────────────────────────────
    // REPLICACIÓN ENTRE SERVIDORES (llamada externa)
    // ──────────────────────────────────────────────────────────
   private void replicarBloqueAServidor(Bloque bloque, String host, int puerto) throws Exception {
    try (Socket socket = new Socket(host, puerto);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
        
        // Serializar datos encriptados
        StringBuilder datosEnc = new StringBuilder();
        for (int i = 0; i < bloque.getDatosEncriptados().size(); i++) {
            datosEnc.append(bloque.getDatosEncriptados().get(i));
            if (i < bloque.getDatosEncriptados().size() - 1) {
                datosEnc.append(":");
            }
        }
        
        String mensaje = "REPLICATE_BLOCK|" + 
                        bloque.getIndex() + "|" +
                        bloque.getHash() + "|" +
                        bloque.getHashAnterior() + "|" +
                        bloque.getNonce() + "|" +
                        bloque.getTimestamp() + "|" +
                        bloque.getLlaveAesEncriptada() + "|" +
                        datosEnc.toString();
        
        out.println(mensaje);
        String respuesta = in.readLine();
        
        if (respuesta != null && respuesta.startsWith("OK")) {
            System.out.println("✅ Bloque replicado al servidor " + host + ":" + puerto);
        }
        
    } catch (IOException e) {
        System.err.println("⚠️ Error al replicar: " + e.getMessage());
        throw e;
    }
}
   
   public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(() -> {
        FarmaciaApp app = new FarmaciaApp();
        app.setVisible(true);
    });
}

}


