package modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConexionPostgres {
    
    private static final String URL = "jdbc:postgresql://localhost:5432/inventario_db"; 
    private static final String USER = "postgres"; 
    private static final String PASSWORD = "centeno"; 

    private Connection getConnection() throws SQLException {
        // Carga el driver JDBC 
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Error: Driver JDBC de PostgreSQL no encontrado. Verifique la librería JAR.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Guarda el nonce y el hash del bloque minado en la BD.
     */
    public void guardarNonce(int indice, String hash, long nonce) throws SQLException {
        // Usamos ON CONFLICT DO UPDATE para actualizar el registro si ya existe (útil para el Bloque Génesis si se modifica)
        String sql = "INSERT INTO registro_bloques (indice, hash_bloque, nonce) VALUES (?, ?, ?) ON CONFLICT (indice) DO UPDATE SET hash_bloque = EXCLUDED.hash_bloque, nonce = EXCLUDED.nonce, fecha_minado = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, indice);
            pstmt.setString(2, hash);
            pstmt.setLong(3, nonce);
            pstmt.executeUpdate();
            System.out.println("Nonce guardado en BD para Bloque #" + indice);
        }
    }

    /**
     * Recupera el nonce guardado para la verificación del PoW.
     */
    public Long obtenerNonce(int indice) throws SQLException {
        String sql = "SELECT nonce FROM registro_bloques WHERE indice = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, indice);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("nonce");
                }
            }
        }
        return null;
    }
}