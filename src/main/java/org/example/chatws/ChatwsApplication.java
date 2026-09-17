package org.example.chatws;

import org.example.chatws.utility.ChatCliente;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ExecutionException;

/**
 * Clase principal de la aplicación Spring Boot.
 * Esta clase contiene el método main que inicia el contexto de Spring
 * y levanta el servidor WebSocket para el sistema de chat.
 * 
 * La aplicación configura automáticamente:
 * - Servidor web embebido (Tomcat)
 * - Configuración de WebSocket y STOMP
 * - Broker de mensajes en memoria
 * - Endpoints para mensajería en tiempo real
 */
@SpringBootApplication
public class ChatwsApplication {

    /**
     * Método principal que inicia la aplicación Spring Boot.
     * 
     * @param args Argumentos de línea de comandos
     * @throws ExecutionException Si ocurre un error durante la ejecución
     * @throws InterruptedException Si el hilo es interrumpido
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        SpringApplication.run(ChatwsApplication.class, args);
    }

}
