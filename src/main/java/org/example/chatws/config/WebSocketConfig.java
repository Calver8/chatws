package org.example.chatws.config;

import org.example.chatws.utility.Constants;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuracion del WebSocket para la mensajeria en tiempo real.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Prefijo de los destinos a los que se suscriben los clientes
    public static final String TOPIC = "/topic";
    // Prefijo de los destinos a los que el cliente manda mensajes
    public static final String APP = "/app";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en memoria: todo lo que se publique en /topic se reparte a los suscriptos
        config.enableSimpleBroker(TOPIC);
        // Lo que llega a /app lo atiende un @MessageMapping del controller
        config.setApplicationDestinationPrefixes(APP);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // withSockJS deja conectar por HTTP a los navegadores que no soportan WebSocket
        registry.addEndpoint(Constants.ENDPOINT).withSockJS();
    }
}
