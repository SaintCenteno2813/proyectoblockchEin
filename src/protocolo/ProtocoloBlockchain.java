package protocolo;

public class ProtocoloBlockchain {
    
    // COMANDOS DEL PROTOCOLO (existentes)
    public static final String REGISTRAR_TRANSACCION = "TX_NEW";
    public static final String OBTENER_CADENA = "GET_CHAIN";
    public static final String OBTENER_SALDO = "GET_BALANCE";
    public static final String VALIDAR_CADENA = "VALIDATE_CHAIN";
    public static final String MINAR_BLOQUE = "MINE_BLOCK";
    public static final String OBTENER_BLOQUES = "GET_BLOCKS";
    public static final String PING = "PING";
    
    // ⭐ NUEVOS COMANDOS PARA REPLICACIÓN
    public static final String REPLICAR_BLOQUE = "REPLICATE_BLOCK";
    public static final String OBTENER_CADENA_COMPLETA = "GET_FULL_CHAIN";
    public static final String SINCRONIZAR = "SYNC";
    public static final String REGISTRAR_PEER = "REGISTER_PEER";
    public static final String LISTAR_PEERS = "LIST_PEERS";
    
    // RESPUESTAS DEL PROTOCOLO
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String PONG = "PONG";
    
    // SEPARADORES
    public static final String SEPARADOR = "|";
    public static final String FIN_MENSAJE = "\n";
    public static final String SEPARADOR_INTERNO = ":";
    
    public static String crearMensaje(String comando, String... parametros) {
        StringBuilder mensaje = new StringBuilder(comando);
        for (String param : parametros) {
            if (param != null) {
                mensaje.append(SEPARADOR).append(param);
            }
        }
        mensaje.append(FIN_MENSAJE);
        return mensaje.toString();
    }
    
    public static String[] parsearMensaje(String mensaje) {
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return new String[0];
        }
        return mensaje.trim().split("\\" + SEPARADOR);
    }
}