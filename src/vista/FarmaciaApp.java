package vista;

import modelo.Bloque;
import modelo.Blockchain;
import modelo.Encriptador;
import modelo.Medicamento;
import modelo.Producto;
import modelo.TransaccionInventario;
import modelo.ConexionPostgres; // Importación necesaria para BD

import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException; // Importación SQL
import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import org.bouncycastle.jce.provider.BouncyCastleProvider; 


/**
 *
 * @author Erick
 */
public class FarmaciaApp extends JFrame {
    
  
    private Blockchain blockchain;
    private Bloque bloquePendiente;
    private ArrayList<String> listaFarmacias;


private static final String RUTA_LLAVE_PUBLICA = "public_key_descifrada.pem"; 
private static final String RUTA_LLAVE_PRIVADA = "private_key_descifrada.pem";

    private JComboBox<String> comboFarmacias;
    private DefaultListModel<Bloque> listModelBloques;
    private JList<Bloque> listaBloques;
    private DefaultListModel<TransaccionInventario> listModelTransaccionesPendientes;
    private JList<TransaccionInventario> listaTransaccionesPendientes;
    private DefaultListModel<Producto> listModelInventario;
    private JList<Producto> listaInventario;
    private JTextArea areaContenidoBloque;
    private JLabel labelEstado;
 

    public FarmaciaApp() {

        blockchain = new Blockchain(4);
        bloquePendiente = new Bloque(blockchain.obtenerUltimoBloque().getHash());
        listaFarmacias = new ArrayList<>();

        listaFarmacias.add("Farmacia A");
        listaFarmacias.add("Farmacia B");

        setTitle("Inventario de Farmacias con Blockchain");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        JPanel panelDerecho = new JPanel(new BorderLayout());

        comboFarmacias = new JComboBox<>(listaFarmacias.toArray(new String[0]));
        comboFarmacias.addActionListener(e -> actualizarInventario());
        panelIzquierdo.add(comboFarmacias, BorderLayout.NORTH);

        // Inicialización de ListModels y JLists
        listModelInventario = new DefaultListModel<>();
        listaInventario = new JList<>(listModelInventario);
        listaInventario.setBorder(BorderFactory.createTitledBorder("Inventario Actual"));
        panelIzquierdo.add(new JScrollPane(listaInventario), BorderLayout.CENTER);

        listModelBloques = new DefaultListModel<>();
        listaBloques = new JList<>(listModelBloques);
        listaBloques.setBorder(BorderFactory.createTitledBorder("Bloques de la Cadena"));
        listModelBloques.addElement(blockchain.getCadena().get(0));

        listModelTransaccionesPendientes = new DefaultListModel<>();
        listaTransaccionesPendientes = new JList<>(listModelTransaccionesPendientes);
        listaTransaccionesPendientes.setBorder(BorderFactory.createTitledBorder("Transacciones Pendientes"));

        areaContenidoBloque = new JTextArea();
        areaContenidoBloque.setBorder(BorderFactory.createTitledBorder("Contenido del Bloque (Desencriptado)"));
        areaContenidoBloque.setEditable(false);

        JSplitPane splitPaneBloquesTransacciones = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(listaBloques), new JScrollPane(listaTransaccionesPendientes));
        splitPaneBloquesTransacciones.setDividerLocation(300);

        JSplitPane splitPaneDerecho = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                splitPaneBloquesTransacciones, new JScrollPane(areaContenidoBloque));
        splitPaneDerecho.setDividerLocation(300);

        panelDerecho.add(splitPaneDerecho, BorderLayout.CENTER);

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

        labelEstado = new JLabel("Estado: Listo para registrar transacciones.");
        labelEstado.setHorizontalAlignment(SwingConstants.CENTER);

        panelPrincipal.add(panelIzquierdo, BorderLayout.WEST);
        panelPrincipal.add(panelDerecho, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);
        panelPrincipal.add(labelEstado, BorderLayout.NORTH);

        add(panelPrincipal);
    }

    private void registrarMovimiento(String tipo) {
        String farmaciaSeleccionada = (String) comboFarmacias.getSelectedItem();
        if (farmaciaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una farmacia.");
            return;
        }

        String nombreProducto = JOptionPane.showInputDialog("Nombre del producto:");
        String codigoProducto = JOptionPane.showInputDialog("Código del producto:");
        int cantidad = Integer.parseInt(JOptionPane.showInputDialog("Cantidad a " + (tipo.equals(TransaccionInventario.ENTRADA) ? "ingresar" : "retirar") + ":"));

        String responsable = JOptionPane.showInputDialog("Responsable:");
        String lote = JOptionPane.showInputDialog("Lote:");
        String fechaCaducidad = JOptionPane.showInputDialog("Fecha de caducidad (DD/MM/AAAA):");

        Producto nuevoProducto = new Medicamento(nombreProducto, codigoProducto, cantidad, "250mg");

        TransaccionInventario transaccion = new TransaccionInventario(farmaciaSeleccionada, nuevoProducto, tipo, responsable, lote, fechaCaducidad);
        bloquePendiente.agregarTransaccion(transaccion);
        listModelTransaccionesPendientes.addElement(transaccion);
        labelEstado.setText("Estado: Transacción agregada. A la espera de ser minada.");
    }

    private void minarNuevoBloque(ActionEvent e) {
        if (listModelTransaccionesPendientes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay transacciones pendientes para minar.");
            return;
        }

        try {
            SecretKey llaveAes = Encriptador.generarLlaveAes();
            
            StringBuilder jsonTransacciones = new StringBuilder("[");
            for (int i = 0; i < bloquePendiente.getTransacciones().size(); i++) {
                TransaccionInventario t = bloquePendiente.getTransacciones().get(i);
                jsonTransacciones.append("{");
                jsonTransacciones.append("\"farmaciaId\":\"").append(t.getFarmaciaId()).append("\",");
                jsonTransacciones.append("\"productoNombre\":\"").append(t.getProducto().getNombre()).append("\",");
                jsonTransacciones.append("\"productoCodigo\":\"").append(t.getProducto().getCodigo()).append("\",");
                jsonTransacciones.append("\"cantidad\":").append(t.getProducto().getCantidad()).append(",");
                jsonTransacciones.append("\"tipoMovimiento\":\"").append(t.getTipoMovimiento()).append("\",");
                jsonTransacciones.append("\"responsable\":\"").append(t.getResponsable()).append("\",");
                jsonTransacciones.append("\"lote\":\"").append(t.getLote()).append("\",");
                jsonTransacciones.append("\"fechaCaducidad\":\"").append(t.getFechaCaducidad()).append("\"");
                jsonTransacciones.append("}");
                if (i < bloquePendiente.getTransacciones().size() - 1) {
                    jsonTransacciones.append(",");
                }
            }
            jsonTransacciones.append("]");

            byte[] datosCifradosAes = Encriptador.cifrarAes(jsonTransacciones.toString().getBytes(StandardCharsets.UTF_8), llaveAes);
            String datosBase64 = Base64.getEncoder().encodeToString(datosCifradosAes);
            
            ArrayList<String> listaDatosEncriptados = new ArrayList<>();
            listaDatosEncriptados.add(datosBase64);

            PublicKey llavePublica = Encriptador.cargarLlavePublica(RUTA_LLAVE_PUBLICA);
            byte[] llaveAesCifrada = Encriptador.cifrarLlaveRsa(llaveAes, llavePublica);
            String llaveAesBase64 = Base64.getEncoder().encodeToString(llaveAesCifrada);

            bloquePendiente.setDatosEncriptados(listaDatosEncriptados);
            bloquePendiente.setLlaveAesEncriptada(llaveAesBase64);
            bloquePendiente.setIndex(blockchain.getCadena().size());

            // Ejecuta la minería y agregar el bloque
            labelEstado.setText("Estado: Minando el nuevo bloque...");
            bloquePendiente.minarBloque(blockchain.getDificultad());
            
            blockchain.agregarBloque(bloquePendiente);
            listModelBloques.addElement(bloquePendiente);
            

            try {
                ConexionPostgres db = new ConexionPostgres();
                db.guardarNonce(
                    bloquePendiente.getIndex(),
                    bloquePendiente.getHash(),
                    (long)bloquePendiente.getNonce()
                );
            } catch (SQLException ex) {
                // El error de conexión o driver se capturará aquí si la BD no está encendida.
                JOptionPane.showMessageDialog(this, "Error al guardar el nonce en BD. Verifique la conexión: " + ex.getMessage());
                ex.printStackTrace();
            }
 

            guardarBloqueComoJSON(bloquePendiente, blockchain.getCadena().size() - 1);

            listModelTransaccionesPendientes.clear();
            bloquePendiente = new Bloque(blockchain.obtenerUltimoBloque().getHash());

            actualizarInventario();
            labelEstado.setText("Estado: Bloque minado con éxito.");
            JOptionPane.showMessageDialog(this, "¡Bloque minado con éxito!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al minar: " + ex.getMessage(), "Error de Proceso", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void validarCadena(ActionEvent e) {
        if (!blockchain.esCadenaValida()) {
            labelEstado.setText("Estado: La cadena de bloques es inválida (Estructura rota). ❌");
            JOptionPane.showMessageDialog(this, "¡La cadena de bloques es inválida (hashes no coinciden)!");
            return;
        }

 
        try {
            ConexionPostgres db = new ConexionPostgres();
            
            for (Bloque bloque : blockchain.getCadena()) {
                if (bloque.getIndex() == 0) continue; // Saltar el Génesis
                
                Long nonceGuardado = db.obtenerNonce(bloque.getIndex());
                
                if (nonceGuardado == null) {
                    JOptionPane.showMessageDialog(this, "Error: Nonce del Bloque #" + bloque.getIndex() + " no encontrado en BD. Cadena no confiable.");
                    return;
                }

                // Recalcular el hash usando el nonce recuperado
                String hashRecalculado = bloque.calcularHashConNonce(nonceGuardado); 

                // Comparar si el hash recalculado coincide con el hash almacenado
                if (!hashRecalculado.equals(bloque.getHash())) {
                    labelEstado.setText("Estado: La cadena es INCONFIABLE. El bloque #" + bloque.getIndex() + " ha sido alterado.");
                    JOptionPane.showMessageDialog(this, "¡Error de confianza! El Nonce de la BD no genera el Hash del Bloque.");
                    return; 
                }
            }

            labelEstado.setText("Estado: La cadena es válida y CONFIABLE (Verificada con BD). ✔️");
            JOptionPane.showMessageDialog(this, "¡La cadena de bloques es válida y la Prueba de Trabajo coincide con la BD!");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión/SQL al validar con la base de datos. Verifique el driver y la conexión: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    private void verContenidoBloque(ActionEvent e) {
        Bloque bloqueSeleccionado = listaBloques.getSelectedValue();
        if (bloqueSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un bloque de la lista.");
            return;
        }
        
        ArrayList<String> datosBase64List = bloqueSeleccionado.getDatosEncriptados();
        String datosBase64 = datosBase64List.get(0);
        String llaveAesBase64 = bloqueSeleccionado.getLlaveAesEncriptada();

        if (datosBase64 == null || datosBase64.isEmpty() || llaveAesBase64 == null || llaveAesBase64.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Este bloque no contiene datos encriptados.");
            return;
        }

        try {

            char[] contrasenia = Encriptador.pedirContrasenia("Ingresa la contraseña para la llave privada:");
            
            // Carga la llave privada cifrada con la contraseña
            PrivateKey llavePrivada = Encriptador.cargarLlavePrivada(RUTA_LLAVE_PRIVADA, contrasenia);
            
            // Desencriptar la llave AES con la llave privada (RSA)
            byte[] llaveAesCifrada = Base64.getDecoder().decode(llaveAesBase64);
            SecretKey llaveAes = Encriptador.descifrarLlaveRsa(llaveAesCifrada, llavePrivada);

            //  Desencriptar los datos con la llave AES
            byte[] datosCifradosAes = Base64.getDecoder().decode(datosBase64);
            byte[] datosDesencriptados = Encriptador.descifrarAes(datosCifradosAes, llaveAes);
            String jsonOriginal = new String(datosDesencriptados, StandardCharsets.UTF_8);

            areaContenidoBloque.setText(jsonOriginal);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al desencriptar: " + ex.getMessage(), "Error de Desencriptación", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            areaContenidoBloque.setText("Error al desencriptar el contenido.");
        }
    }

    private void actualizarInventario() {
        String farmaciaSeleccionada = (String) comboFarmacias.getSelectedItem();
        if (farmaciaSeleccionada == null) return;

        listModelInventario.clear();
        HashMap<String, Producto> inventarioCalculado = new HashMap<>();

        for (Bloque bloque : blockchain.getCadena()) {
            if (bloque.getTransacciones().isEmpty() && !bloque.getDatosEncriptados().isEmpty()) {
                continue; 
            }
            
            for (TransaccionInventario transaccion : bloque.getTransacciones()) {
                if (transaccion.getFarmaciaId().equals(farmaciaSeleccionada)) {
                    String codigo = transaccion.getProducto().getCodigo();

                    if (!inventarioCalculado.containsKey(codigo)) {
                        inventarioCalculado.put(codigo, new Producto(transaccion.getProducto().getNombre(), codigo, 0) {
                            @Override
                            public String getTipo() {
                                return "Desconocido";
                            }
                        });
                    }

                    Producto producto = inventarioCalculado.get(codigo);
                    int nuevaCantidad = producto.getCantidad();

                    if (transaccion.getTipoMovimiento().equals(TransaccionInventario.ENTRADA)) {
                        nuevaCantidad += transaccion.getProducto().getCantidad();
                    } else if (transaccion.getTipoMovimiento().equals(TransaccionInventario.SALIDA)) {
                        nuevaCantidad -= transaccion.getProducto().getCantidad();
                    }
                    producto.setCantidad(nuevaCantidad);
                }
            }
        }

        for (Producto p : inventarioCalculado.values()) {
            listModelInventario.addElement(p);
        }
    }

    private void guardarBloqueComoJSON(Bloque bloque, int index) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Bloque como JSON");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.endsWith(".json")) {
                filePath += ".json";
            }

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath), "UTF-8"))) {
                
                writer.write(bloque.toString());
                JOptionPane.showMessageDialog(this, "Bloque guardado con éxito en: " + filePath);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("Bouncy Castle registrado para manejar llaves cifradas.");
        }

        SwingUtilities.invokeLater(() -> new FarmaciaApp().setVisible(true));
    }
}