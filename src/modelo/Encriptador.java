package modelo;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileReader;
// *** Importaciones necesarias para Bouncy Castle (Si se usa la versión cifrada) ***
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
// *********************************************************
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

public class Encriptador {
    
    // Método para generar una llave simétrica AES (sin cambios)
    public static SecretKey generarLlaveAes() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // Tamaño de la llave en bits
        return keyGen.generateKey();
    }
    
    // Método para encriptar datos con AES (sin cambios)
    public static byte[] cifrarAes(byte[] datos, SecretKey llave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, llave);
        return cipher.doFinal(datos);
    }
    
    // Método para desencriptar datos con AES (sin cambios)
    public static byte[] descifrarAes(byte[] datosCifrados, SecretKey llave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, llave);
        return cipher.doFinal(datosCifrados);
    }

    // Método para cifrar una llave simétrica con RSA (sin cambios)
    public static byte[] cifrarLlaveRsa(SecretKey llaveAes, PublicKey llavePublica) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, llavePublica);
        return cipher.doFinal(llaveAes.getEncoded());
    }
    
    // Método para descifrar una llave simétrica con RSA (sin cambios)
    public static SecretKey descifrarLlaveRsa(byte[] llaveCifrada, PrivateKey llavePrivada) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, llavePrivada);
        byte[] llaveDescifrada = cipher.doFinal(llaveCifrada);
        return new javax.crypto.spec.SecretKeySpec(llaveDescifrada, "AES");
    }

    // Cargar llave pública desde un archivo PEM (sin cambios)
    public static PublicKey cargarLlavePublica(String rutaArchivo) throws Exception {
        try (InputStream is = new FileInputStream(rutaArchivo)) {
            byte[] keyBytes = is.readAllBytes();
            String keyPem = new String(keyBytes, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(keyPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
    }

    
    public static PrivateKey cargarLlavePrivada(String rutaArchivo) throws Exception {
        try (InputStream is = new FileInputStream(rutaArchivo)) {
            byte[] keyBytes = is.readAllBytes();
            String keyPem = new String(keyBytes, StandardCharsets.UTF_8);
            
            
            keyPem = keyPem.replaceAll("-----BEGIN (?:ENCRYPTED )?PRIVATE KEY-----", "")
                           .replaceAll("-----END (?:ENCRYPTED )?PRIVATE KEY-----", "")
                           .replaceAll("\\s", ""); // Quita saltos de línea y espacios
                           
            byte[] decoded = Base64.getDecoder().decode(keyPem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }
    }
    
   
    public static PrivateKey cargarLlavePrivada(String rutaArchivo, char[] contrasenia) throws Exception {
        if (contrasenia == null) {
            throw new IllegalArgumentException("Se requiere una contraseña para la llave cifrada.");
        }
        
        try (PEMParser parser = new PEMParser(new FileReader(rutaArchivo))) {
            Object objetoPEM = parser.readObject();

            if (objetoPEM instanceof PKCS8EncryptedPrivateKeyInfo) {
                PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) objetoPEM;

                // Crea el proveedor de descifrado usando la contraseña y el proveedor "BC"
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .setProvider("BC") 
                        .build(contrasenia); 

                // Descifra la información de la llave privada
                return new JcaPEMKeyConverter()
                        .setProvider("BC")
                        .getPrivateKey(encryptedInfo.decryptPrivateKeyInfo(decryptorProvider));
                
            } else {
                // Si el objeto no es la llave cifrada esperada, lo tratamos como si fuera sin cifrar:
                return cargarLlavePrivada(rutaArchivo); 
            }
        } catch (Exception e) {
            throw new Exception("Error al cargar o descifrar la llave privada (Verifique la contraseña o el formato): " + e.getMessage(), e);
        }
    }

    // Método para pedir la contraseña al usuario (sin cambios)
    public static char[] pedirContrasenia(String mensaje) {
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(null, passwordField, mensaje, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            return passwordField.getPassword();
        }
        return null;
    }
}