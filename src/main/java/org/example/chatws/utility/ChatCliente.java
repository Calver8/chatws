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

import java.util.List;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

public class ChatCliente {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        System.out.println("Conectando a: " + Constants.BASE_URL + Constants.ENDPOINT);

        StompSession session = stompClient
                .connectAsync(Constants.BASE_URL+ Constants.ENDPOINT,
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                System.out.println("Conectado exitosamente al servidor WebSocket");
                            }
                        })
                .get();

        System.out.println("Suscrito a: " + Constants.DESTINATION);
        session.subscribe(Constants.DESTINATION, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Mensaje recibido: " + payload);
            }
        });

        System.out.println("Enviando mensaje a: " + Constants.MESSAGE);
        session.send(Constants.MESSAGE,
                new ChatMessage("CHAT", "ana", "Hola a todos", null));
        System.out.println("Mensaje enviado");

        Thread.sleep(5000);
    }
}
