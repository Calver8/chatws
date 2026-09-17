package org.example.chatws.utility;

import org.example.chatws.entity.ChatMessage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Cliente de prueba para conectar al servidor WebSocket y enviar/recibir mensajes.
 * Esta clase simula un cliente que se conecta al chat, se suscribe a mensajes,
 * envía un mensaje y espera recibir respuestas.
 */
public final class ChatBrowser {

    // Constructor privado para evitar instanciación (clase utilitaria)
    private ChatBrowser() {
    }

    /**
     * Método principal que ejecuta el cliente de chat.
     * Realiza las siguientes operaciones:
     * 1. Configura el cliente WebSocket con SockJS
     * 2. Establece conexión con el servidor
     * 3. Se suscribe al canal de mensajes
     * 4. Envía un mensaje de prueba
     * 5. Espera 5 segundos para recibir respuestas
     * 
     * @param args Argumentos de línea de comandos (no utilizados)
     * @throws ExecutionException Si ocurre un error durante la conexión asíncrona
     * @throws InterruptedException Si el hilo es interrumpido durante el sleep
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Configura el transporte WebSocket como lista de transportes disponibles
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        // Crea el cliente SockJS que maneja la conexión con fallback a HTTP si es necesario
        SockJsClient sockJsClient = new SockJsClient(transports);
        // Crea el cliente STOMP sobre WebSocket para el protocolo de mensajería
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        // Configura el convertidor de mensajes JSON para serializar/deserializar objetos
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        System.out.println("Conectando a: " + Constants.BASE_URL + Constants.ENDPOINT);

        // Conecta asíncronamente al servidor WebSocket
        StompSession session = stompClient
                .connectAsync(Constants.BASE_URL + Constants.ENDPOINT,
                        new StompSessionHandlerAdapter() {
                            /**
                             * Callback invocado cuando la conexión se establece exitosamente.
                             * @param session La sesión STOMP establecida
                             * @param connectedHeaders Headers de la conexión
                             */
                            @Override
                            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                System.out.println("Conectado exitosamente al servidor WebSocket");
                            }
                        })
                .get(); // .get() bloquea hasta que la conexión se complete

        System.out.println("Suscrito a: " + Constants.DESTINATION);
        // Se suscribe al destino para recibir mensajes enviados por otros clientes
        session.subscribe(Constants.DESTINATION, new StompFrameHandler() {
            /**
             * Define el tipo de objeto que se espera recibir en los mensajes.
             * El convertidor usará esta clase para deserializar el payload JSON.
             * @param headers Headers del mensaje STOMP
             * @return La clase ChatMessage para deserialización
             */
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            /**
             * Maneja cada mensaje recibido del servidor.
             * @param headers Headers del mensaje STOMP
             * @param payload El objeto ChatMessage deserializado
             */
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Mensaje recibido: " + payload);
            }
        });

        System.out.println("Enviando mensaje a: " + Constants.MESSAGE);
        // Envía un mensaje al servidor a través de la sesión STOMP
        session.send(Constants.MESSAGE,
                new ChatMessage("CHAT", "ChatSystem", "Hola a todos", null));
        System.out.println("Mensaje enviado");

        // Espera 5 segundos para recibir posibles respuestas antes de terminar
        Thread.sleep(5000);
    }
}
