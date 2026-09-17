package org.example.chatws.controller;

import org.example.chatws.entity.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Controlador que maneja los mensajes de chat enviados a través de WebSocket.
 * Define los endpoints para recibir y reenviar mensajes a los clientes conectados.
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Ruta para agregar un nuevo usuario al chat
    public static final String CHAT_ADD_USER = "/chat.addUser";
    // Destino donde se envían los mensajes para todos los clientes suscritos
    public static final String PATH = "/topic/public";

    /**
     * Recibe un mensaje de chat de un cliente y lo reenvía a todos los suscriptores.
     * - @MessageMapping: Mapea mensajes enviados a /app/chat.sendMessage (el prefijo /app
     *   se configura en WebSocketConfig) a este método.
     * - @SendTo: El mensaje retornado se envía automáticamente a todos los clientes
     *   suscritos al destino /topic/public.
     * 
     * Flujo: Cliente -> /app/chat.sendMessage -> este método -> /topic/public -> Todos los clientes
     * 
     * @param mensaje El objeto ChatMessage recibido del cliente
     * @return El mismo mensaje para ser reenviado a todos los suscriptores
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo(PATH)
    public ChatMessage enviarMensaje(ChatMessage mensaje) {
        System.out.println("Mensaje recibido en servidor: " + mensaje);
        return mensaje;
    }

    /**
     * Recibe un mensaje de un nuevo usuario uniéndose al chat y lo notifica a todos.
     * - @MessageMapping: Mapea mensajes enviados a /app/chat.addUser a este método.
     * - @SendTo: El mensaje del nuevo usuario se envía a /topic/public para que
     *   todos los clientes sepan que alguien se unió.
     * 
     * Flujo: Cliente -> /app/chat.addUser -> este método -> /topic/public -> Todos los clientes
     * 
     * @param mensaje El objeto ChatMessage con información del nuevo usuario
     * @return El mensaje para ser reenviado a todos los suscriptores
     */
    @MessageMapping(CHAT_ADD_USER)
    @SendTo(PATH)
    public ChatMessage agregarUsuario(ChatMessage mensaje) {
        return mensaje;
    }

    /**
     * Recibe un mensaje privado y lo envía solo al destinatario específico.
     * - @MessageMapping: Mapea mensajes enviados a /app/chat.sendPrivate a este método.
     * - SimpMessagingTemplate: Envía el mensaje dinámicamente a /queue/{destinatario}
     *   para que solo el usuario especificado reciba el mensaje.
     * 
     * Flujo: Cliente -> /app/chat.sendPrivate -> este método -> /queue/{destinatario} -> Solo el destinatario
     * 
     * @param mensaje El objeto ChatMessage con el mensaje privado
     */
    @MessageMapping("/chat.sendPrivate")
    public void enviarMensajePrivado(ChatMessage mensaje) {
        System.out.println("Mensaje privado recibido en servidor: " + mensaje);
        messagingTemplate.convertAndSend("/queue/" + mensaje.destinatario(), mensaje);
    }
}
