package org.example.chatws.controller;

import org.example.chatws.entity.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Recibe los mensajes que mandan los clientes y los reparte a todos los conectados.
 */
@Controller
public class ChatController {

    // Ruta para avisar que un usuario se suma al chat
    public static final String CHAT_ADD_USER = "/chat.addUser";
    // Destino comun al que estan suscriptos todos los clientes
    public static final String PATH = "/topic/public";

    /**
     * Cliente -> /app/chat.sendMessage -> este metodo -> /topic/public -> todos los clientes.
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo(PATH)
    public ChatMessage enviarMensaje(ChatMessage mensaje) {
        System.out.println("Mensaje recibido en servidor: " + mensaje);
        return mensaje;
    }

    /**
     * Mismo recorrido, pero para avisar que alguien entro al chat.
     */
    @MessageMapping(CHAT_ADD_USER)
    @SendTo(PATH)
    public ChatMessage agregarUsuario(ChatMessage mensaje) {
        return mensaje;
    }
}
