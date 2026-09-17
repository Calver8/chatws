package org.example.chatws.utility;

/**
 * Clase de utilidad que contiene las constantes de configuración del sistema.
 * Define las URLs, endpoints y destinos utilizados para la comunicación
 * WebSocket entre el cliente y el servidor.
 * 
 * Constantes:
 * - ENDPOINT: URL del endpoint WebSocket para conexión
 * - DESTINATION: Topic público donde se publican los mensajes
 * - MESSAGE: Endpoint para enviar mensajes públicos
 * - PRIVATE_MESSAGE: Endpoint para enviar mensajes privados
 * - BASE_URL: URL base del servidor WebSocket
 */
public final class Constants {
    /** Endpoint WebSocket donde los clientes establecen conexión */
    public static final String ENDPOINT = "/chat";
    /** Topic público donde se publican todos los mensajes del chat */
    public static final String DESTINATION = "/topic/public";
    /** Endpoint para enviar mensajes públicos al servidor */
    public static final String MESSAGE = "/app/chat.sendMessage";
    /** Endpoint para enviar mensajes privados entre usuarios */
    public static final String PRIVATE_MESSAGE = "/app/chat.sendPrivate";
    /** URL base del servidor WebSocket */
    public static final String BASE_URL = "ws://localhost:8080";

    /** Constructor privado para evitar instanciación (clase utilitaria) */
    private Constants() {
    }
}
