package org.example.chatws.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Entidad que representa un mensaje de chat.
 * Esta clase encapsula la información de los mensajes que se envían
 * a través del WebSocket, incluyendo soporte para mensajes públicos,
 * privados y señales WebRTC para comunicación en tiempo real.
 * 
 * Campos:
 * - tipo: Tipo de mensaje (CHAT, JOIN, LEAVE)
 * - usuario: Nombre del usuario que envía el mensaje
 * - contenido: Texto del mensaje
 * - destinatario: Usuario destinatario para mensajes privados (opcional)
 * - webRTCSignal: Datos de señalización WebRTC para video/audio (opcional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    @JsonProperty("tipo")
    private String tipo;
    @JsonProperty("usuario")
    private String usuario;
    @JsonProperty("contenido")
    private String contenido;
    @JsonProperty("destinatario")
    private String destinatario;
    @JsonProperty("webRTCSignal")
    private Object webRTCSignal;

    public ChatMessage() {
    }

    public ChatMessage(String tipo, String usuario, String contenido, String destinatario) {
        this.tipo = tipo;
        this.usuario = usuario;
        this.contenido = contenido;
        this.destinatario = destinatario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public Object getWebRTCSignal() {
        return webRTCSignal;
    }

    public void setWebRTCSignal(Object webRTCSignal) {
        this.webRTCSignal = webRTCSignal;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "tipo='" + tipo + '\'' +
                ", usuario='" + usuario + '\'' +
                ", contenido='" + contenido + '\'' +
                ", destinatario='" + destinatario + '\'' +
                ", webRTCSignal=" + webRTCSignal +
                '}';
    }
}
