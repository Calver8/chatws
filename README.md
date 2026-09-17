# Chat WebSocket - Guía para Alumnos

Este proyecto implementa un sistema de chat en tiempo real utilizando **WebSocket** y el protocolo **STOMP** con Spring Boot. A continuación se explica el flujo completo de funcionamiento y los conceptos clave.

## 📋 Índice

1. [Descripción del Proyecto](#descripción-del-proyecto)
2. [Arquitectura y Flujo](#arquitectura-y-flujo)
3. [Componentes del Sistema](#componentes-del-sistema)
4. [Flujo de Mensajes](#flujo-de-mensajes)
5. [Glosario de Conceptos](#glosario-de-conceptos)
6. [Ejecución del Proyecto](#ejecución-del-proyecto)

---

## 📝 Descripción del Proyecto

ChatWS es una aplicación de chat en tiempo real que permite a múltiples usuarios conectarse, enviar mensajes y recibir actualizaciones instantáneas sin necesidad de recargar la página. Utiliza una arquitectura basada en eventos donde los mensajes se propagan automáticamente a todos los clientes conectados.

**Tecnologías principales:**
- Spring Boot 4.1.1
- WebSocket para comunicación bidireccional
- STOMP como protocolo de mensajería
- SockJS para compatibilidad con navegadores antiguos
- Jackson para serialización JSON
- Soporte para mensajes privados (queues)
- Soporte para señalización WebRTC (video/audio)

---

## 🏗️ Arquitectura y Flujo

### Diagrama de Arquitectura

```
┌─────────────────┐         WebSocket          ┌──────────────────┐
│   Cliente Web   │ ◄────────────────────────► │   Servidor       │
│  (Navegador)    │     (ws://localhost:8080)  │   Spring Boot    │
└─────────────────┘                            └──────────────────┘
       │                                                 │
       │ 1. Conexión WebSocket                          │
       │ 2. Suscripción a /topic/public                 │
       │ 3. Envío a /app/chat.sendMessage               │
       │ 4. Envío a /app/chat.sendPrivate (opcional)     │
       │                                                 │
       │                                                 │
       │ 5. Recepción de mensajes                       │
       │    (broadcast desde /topic/public)             │
       │ 6. Recepción de mensajes privados              │
       │    (desde /queue/{usuario})                    │
```

### Flujo Completo de un Mensaje

1. **Conexión:** El cliente web establece una conexión WebSocket con el servidor
2. **Suscripción:** El cliente se suscribe al canal `/topic/public` para mensajes públicos y `/queue/{usuario}` para mensajes privados
3. **Envío público:** El cliente envía un mensaje a `/app/chat.sendMessage` para broadcast
4. **Envío privado:** El cliente envía un mensaje a `/app/chat.sendPrivate` para comunicación directa
5. **Procesamiento:** El `ChatController` recibe y procesa los mensajes
6. **Broadcast:** Los mensajes públicos se reenvían a todos los suscriptores de `/topic/public`
7. **Envío privado:** Los mensajes privados se envían solo a `/queue/{destinatario}` y `/queue/{usuario}`
8. **Recepción:** Los clientes reciben mensajes según sus suscripciones

---

## 🔧 Componentes del Sistema

### 1. Configuración WebSocket (`WebSocketConfig.java`)

Esta clase configura la infraestructura de mensajería:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
```

**Responsabilidades:**
- Habilita el broker de mensajes en memoria
- Configura el prefijo `/topic` para suscripciones públicas
- Configura el prefijo `/queue` para mensajes privados
- Configura el prefijo `/app` para mensajes del cliente
- Registra el endpoint `/chat` para conexiones WebSocket
- Habilita SockJS como fallback

### 2. Controlador de Chat (`ChatController.java`)

Maneja el flujo de mensajes:

```java
@Controller
public class ChatController {
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage enviarMensaje(ChatMessage mensaje)
}
```

**Responsabilidades:**
- Recibe mensajes enviados por los clientes
- Reenvía los mensajes públicos a todos los suscriptores
- Envía mensajes privados a destinatarios específicos
- Gestiona la entrada de nuevos usuarios
- Soporta señalización WebRTC para video/audio

### 3. Entidad de Mensaje (`ChatMessage.java`)

Representa la estructura de un mensaje:

```java
public record ChatMessage(String tipo, String usuario, String contenido)
```

**Campos:**
- `tipo`: Tipo de mensaje (CHAT, JOIN, LEAVE)
- `usuario`: Nombre del usuario que envía el mensaje
- `contenido`: Texto del mensaje
- `destinatario`: Usuario destinatario para mensajes privados (opcional)
- `webRTCSignal`: Datos de señalización WebRTC para video/audio (opcional)

### 4. Cliente Web (`index.html` + `script.js`)

Interfaz de usuario para el chat:

**Funciones principales:**
- `connect()`: Establece conexión WebSocket con SockJS y STOMP
- `disconnect()`: Cierra la conexión
- `sendMessage()`: Envía mensajes públicos al servidor
- `sendPrivateMessage()`: Envía mensajes privados a un usuario específico
- `showMessage()`: Muestra mensajes recibidos en la UI
- `handleWebRTCSignal()`: Procesa señales WebRTC para video/audio

### 5. Cliente de Prueba (`ChatBrowser.java`)

Cliente Java para pruebas automatizadas:

**Funcionalidades:**
- Conecta al servidor WebSocket programáticamente
- Se suscribe al canal de mensajes
- Envía mensajes de prueba
- Recibe y procesa respuestas

---

## 🔄 Flujo de Mensajes Detallado

### Paso 1: Conexión del Cliente

**Cliente (JavaScript):**
```javascript
const socket = new SockJS('http://localhost:8080/chat');
stompClient = Stomp.over(socket);
stompClient.connect({}, function (frame) {
    stompClient.subscribe('/topic/public', function (message) {
        showMessage(JSON.parse(message.body));
    });
});
```

**Servidor (WebSocketConfig):**
- Registra endpoint `/chat` con soporte SockJS
- Configura broker para `/topic/*`

### Paso 2: Envío de Mensaje

**Cliente (JavaScript):**
```javascript
stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
    tipo: 'CHAT',
    usuario: username,
    contenido: content
}));
```

**Servidor (ChatController):**
```java
@MessageMapping("/chat.sendMessage")
@SendTo("/topic/public")
public ChatMessage enviarMensaje(ChatMessage mensaje) {
    return mensaje; // Se envía automáticamente a /topic/public
}
```

### Paso 3: Recepción por Todos los Clientes

Cada cliente suscrito a `/topic/public` recibe el mensaje automáticamente a través del callback de suscripción.

### Paso 4: Mensajes Privados (Opcional)

**Cliente (JavaScript):**
```javascript
stompClient.send("/app/chat.sendPrivate", {}, JSON.stringify({
    tipo: 'CHAT',
    usuario: username,
    contenido: content,
    destinatario: 'otroUsuario'
}));
```

**Servidor (ChatController):**
```java
@MessageMapping("/chat.sendPrivate")
public void enviarMensajePrivado(ChatMessage mensaje) {
    messagingTemplate.convertAndSend("/queue/" + mensaje.getDestinatario(), mensaje);
    messagingTemplate.convertAndSend("/queue/" + mensaje.getUsuario(), mensaje);
}
```

**Suscripción a mensajes privados:**
```javascript
stompClient.subscribe('/queue/' + username, function (message) {
    showMessage(JSON.parse(message.body));
});
```

---

## 📚 Glosario de Conceptos

### WebSocket
**Definición:** Protocolo de comunicación que proporciona canales full-duplex (bidireccionales) sobre una sola conexión TCP.

**En este proyecto:** Permite comunicación en tiempo real entre el servidor y los clientes sin necesidad de recargar la página. A diferencia de HTTP, donde el cliente debe solicitar información, WebSocket permite que el servidor envíe mensajes activamente a los clientes.

**Características clave:**
- Comunicación bidireccional
- Baja latencia
- Mantenimiento de conexión persistente
- Ahorro de ancho de banda (sin headers HTTP repetidos)

---

### STOMP (Simple Text Oriented Messaging Protocol)
**Definición:** Protocolo de mensajería simple y basado en texto que define el formato y la semántica para intercambiar mensajes entre clientes y servidores.

**En este proyecto:** Actúa como el protocolo de aplicación sobre WebSocket. Define cómo se estructuran los mensajes, cómo se suscriben los clientes a canales y cómo se envían mensajes al servidor.

**Conceptos clave de STOMP:**
- **FRAME:** Unidad básica de comunicación en STOMP (ej: SEND, SUBSCRIBE, MESSAGE)
- **DESTINATION:** Donde se envía o de donde se recibe un mensaje (ej: `/topic/public`)
- **SUBSCRIPTION:** Registro de interés en recibir mensajes de un destino específico

**Ejemplo de frame STOMP:**
```
SEND
destination:/app/chat.sendMessage
content-type:application/json

{"tipo":"CHAT","usuario":"Juan","contenido":"Hola"}
```

---

### Broker de Mensajes
**Definición:** Componente intermedio que recibe mensajes de productores y los distribuye a consumidores interesados. Actúa como un intermediario que desacopla a emisores y receptores.

**En este proyecto:** Spring Boot proporciona un broker simple en memoria (`enableSimpleBroker`) que gestiona la distribución de mensajes a los clientes suscritos.

**Tipos de destinos que maneja:**
- **Topics (`/topic/*`):** Mensajes enviados a múltiples suscriptores (broadcast)
- **Queues (`/queue/*`):** Mensajes enviados a un solo consumidor (mensajes privados)

**Flujo a través del broker:**
```
Cliente A → /app/chat.sendMessage → Broker → /topic/public → Cliente B, C, D
Cliente A → /app/chat.sendPrivate → Broker → /queue/UsuarioB → Solo UsuarioB
```

---

### SockJS
**Definición:** Biblioteca JavaScript que proporciona una capa de abstracción sobre WebSocket, ofreciendo alternativas de transporte cuando WebSocket no está disponible.

**En este proyecto:** Se usa como fallback para navegadores que no soportan WebSocket nativo o cuando hay firewalls/proxies que bloquean conexiones WebSocket.

**Transportes que SockJS puede usar (en orden de preferencia):**
1. WebSocket (transporte nativo)
2. XHR Streaming
3. XHR Polling
4. Iframe-based methods

**Uso en el código:**
```java
registry.addEndpoint("/chat").withSockJS();
```

---

### Topic
**Definición:** Tipo de destino en un sistema de mensajería donde los mensajes se distribuyen a todos los suscriptores activos (patrón publish-subscribe).

**En este proyecto:** `/topic/public` es el topic donde se publican todos los mensajes del chat. Todos los clientes conectados se suscriben a este topic para recibir los mensajes.

**Diferencia con Queue:**
- **Topic:** Un mensaje va a múltiples suscriptores (broadcast)
- **Queue:** Un mensaje va a un solo consumidor (point-to-point)

---

### Endpoint
**Definición:** Punto de entrada específico en una API o servicio donde los clientes pueden enviar solicitudes o establecer conexiones.

**En este proyecto:** `/chat` es el endpoint WebSocket donde los clientes establecen la conexión inicial.

**Jerarquía de endpoints en el proyecto:**
- `/chat` - Endpoint de conexión WebSocket
- `/app/chat.sendMessage` - Endpoint para enviar mensajes públicos (desde cliente)
- `/app/chat.sendPrivate` - Endpoint para enviar mensajes privados (desde cliente)
- `/topic/public` - Topic para recibir mensajes públicos (hacia clientes)
- `/queue/{usuario}` - Queue para recibir mensajes privados (hacia cliente específico)

---

### Message Mapping
**Definición:** Anotación en Spring que mapea mensajes entrantes a métodos específicos del controlador basándose en su destino.

**En este proyecto:** `@MessageMapping("/chat.sendMessage")` indica que los mensajes enviados a `/app/chat.sendMessage` se procesan en el método `enviarMensaje`.

**Relación con @RequestMapping:**
- `@RequestMapping`: Para endpoints HTTP REST
- `@MessageMapping`: Para endpoints WebSocket/STOMP

---

### SendTo
**Definición:** Anotación que especifica el destino donde se enviará el valor de retorno de un método del controlador.

**En este proyecto:** `@SendTo("/topic/public")` hace que el mensaje retornado por el método se envíe automáticamente a todos los suscriptores del topic público.

**Flujo:**
```
Cliente → @MessageMapping → Método del Controller → @SendTo → Topic → Clientes
```

---

### Full-Duplex
**Definición:** Capacidad de un canal de comunicación para transmitir datos en ambas direcciones simultáneamente.

**En este proyecto:** WebSocket es full-duplex, lo que significa que el servidor y el cliente pueden enviar mensajes al mismo tiempo sin interferencia.

**Comparación:**
- **Half-Duplex (HTTP tradicional):** Solo una dirección a la vez
- **Full-Duplex (WebSocket):** Ambas direcciones simultáneas

---

### JSON (JavaScript Object Notation)
**Definición:** Formato ligero de intercambio de datos basado en texto, fácil de leer y escribir para humanos, y fácil de parsear y generar para máquinas.

**En este proyecto:** Se usa para serializar los objetos `ChatMessage` al enviarlos por la red y deserializarlos al recibirlos.

**Ejemplo de mensaje JSON (público):**
```json
{
  "tipo": "CHAT",
  "usuario": "Ana",
  "contenido": "Hola a todos"
}
```

**Ejemplo de mensaje JSON (privado):**
```json
{
  "tipo": "CHAT",
  "usuario": "Ana",
  "contenido": "Hola Juan",
  "destinatario": "Juan"
}
```

**Ejemplo de mensaje JSON (WebRTC):**
```json
{
  "tipo": "WEBRTC_SIGNAL",
  "usuario": "Ana",
  "destinatario": "Juan",
  "webRTCSignal": {
    "type": "offer",
    "sdp": "..."
  }
}
```

---

### Jackson
**Definición:** Biblioteca Java para procesamiento JSON, usada para serializar objetos Java a JSON y viceversa.

**En este proyecto:** Spring usa Jackson automáticamente (a través de `MappingJackson2MessageConverter`) para convertir los objetos `ChatMessage` a JSON al enviar mensajes y convertir JSON a objetos al recibirlos.

---

## 🚀 Ejecución del Proyecto

### Requisitos Previos
- Java 26 o superior
- Maven 3.6+
- Navegador web moderno

### Pasos para Ejecutar

1. **Clonar o descargar el proyecto**

2. **Compilar el proyecto:**
   ```bash
   ./mvnw clean install
   ```

3. **Ejecutar el servidor:**
   ```bash
   ./mvnw spring-boot:run
   ```
   El servidor se ejecutará en HTTPS en el puerto 8443.

4. **Abrir el cliente web:**
   - Navegar a `https://localhost:8443/index.html`
   - Aceptar la advertencia de certificado (el certificado es self-signed para desarrollo)
   - Ingresar nombre de usuario
   - Hacer clic en "Conectar"

### Acceso desde Android (WiFi)

Para acceder a la cámara desde un dispositivo Android conectado vía WiFi:

1. **Obtener la IP del servidor:**
   - En macOS: `ifconfig` (buscar inet en en0)
   - En Linux: `ip addr show`
   - En Windows: `ipconfig`

2. **Configurar el firewall:**
   - Asegúrate de que el puerto 8443 esté abierto en el firewall

3. **Acceder desde Android:**
   - Navegar a `https://[IP_DEL_SERVIDOR]:8443/index.html`
   - Aceptar la advertencia de certificado (Chrome mostrará "Su conexión no es privada")
   - **Importante:** Los navegadores Android requieren HTTPS para acceder a la cámara y el microfono

4. **Si el certificado no es aceptado:**
   - Abre Chrome en Android
   - Navega a `chrome://flags/#unsafely-treat-insecure-origin-as-secure`
   - Habilita la opción y agrega `https://[IP_DEL_SERVIDOR]:8443`
   - Reinicia Chrome

5. **Probar el cliente Java (opcional):**
   ```bash
   # Ejecutar la clase ChatBrowser
   java -cp target/classes:target/dependency/* org.example.chatws.utility.ChatBrowser
   ```

### Probar con Múltiples Clientes

1. Abre varias pestañas del navegador
2. Conecta cada una con diferentes nombres de usuario
3. Envía mensajes desde una pestaña
4. Observa cómo los mensajes aparecen en todas las pestañas en tiempo real

---

## 📁 Estructura del Proyecto

```
chatws/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/chatws/
│   │   │       ├── ChatwsApplication.java       # Clase principal
│   │   │       ├── config/
│   │   │       │   └── WebSocketConfig.java      # Configuración WebSocket
│   │   │       ├── controller/
│   │   │       │   └── ChatController.java       # Controlador de mensajes
│   │   │       ├── entity/
│   │   │       │   └── ChatMessage.java          # Entidad de mensaje
│   │   │       └── utility/
│   │   │           ├── Constants.java           # Constantes del sistema
│   │   │           └── ChatBrowser.java          # Cliente de prueba
│   │   └── resources/
│   │       ├── application.yaml                  # Configuración Spring
│   │       └── static/
│   │           ├── index.html                    # Interfaz web
│   │           ├── script.js                     # Lógica cliente
│   │           └── style.css                     # Estilos
├── pom.xml                                        # Dependencias Maven
└── README.md                                      # Este archivo
```

---

## 💡 Conceptos Adicionales

### Patrón Publish-Subscribe
El chat implementa el patrón publish-subscribe donde:
- **Publisher:** El cliente que envía un mensaje
- **Subscriber:** Los clientes que se suscriben al topic
- **Topic:** El canal `/topic/public` que distribuye los mensajes

### Mensajes Privados (Point-to-Point)
Para comunicación directa entre usuarios:
- Los mensajes se envían a `/queue/{usuario}`
- Solo el usuario específico recibe el mensaje
- Útil para conversaciones privadas

### WebRTC (Video/Audio)
El sistema soporta señalización WebRTC para:
- Videollamadas peer-to-peer
- Llamadas de audio
- Compartición de pantalla
- El campo `webRTCSignal` transporta ofertas, respuestas y candidatos ICE

### Asincronía
La comunicación es completamente asíncrona:
- Los clientes no bloquean esperando respuestas
- Los mensajes se entregan cuando están disponibles
- Múltiples mensajes pueden estar en tránsito simultáneamente

### Escalabilidad (Consideraciones)
El broker simple en memoria es adecuado para:
- Desarrollo y pruebas
- Aplicaciones con pocos usuarios
- Despliegues de un solo servidor

Para producción con muchos usuarios, considerar:
- Broker externo (RabbitMQ, ActiveMQ, Kafka)
- Redis para pub/sub distribuido
- Clustering de servidores

---

## 🔍 Referencias Útiles

- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP Protocol Specification](https://stomp.github.io/)
- [SockJS Documentation](https://github.com/sockjs/sockjs-client)
- [WebSocket MDN Guide](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)

---

## 📝 Notas para Alumnos

### Preguntas Frecuentes

**¿Por qué usar STOMP sobre WebSocket?**
WebSocket solo proporciona el canal de comunicación. STOMP define el protocolo para estructurar los mensajes, gestionar suscripciones y manejar errores, haciendo el desarrollo más sencillo.

**¿Qué pasa si un navegador no soporta WebSocket?**
SockJS detecta automáticamente la capacidad del navegador y usa transportes alternativos (como HTTP polling) si WebSocket no está disponible.

**¿Cómo se manejan múltiples usuarios?**
Cada conexión WebSocket es independiente. El broker mantiene registro de todas las suscripciones y distribuye los mensajes a los suscriptores correspondientes automáticamente.

**¿Por qué usar `/app`, `/topic` y `/queue`?**
- `/app`: Prefijo para mensajes que el cliente envía al servidor (para procesamiento)
- `/topic`: Prefijo para mensajes que el servidor envía a múltiples clientes (broadcast)
- `/queue`: Prefijo para mensajes que el servidor envía a un cliente específico (privado)
Esta separación clarifica la dirección y el alcance del flujo de mensajes.

**¿Cómo funcionan los mensajes privados?**
Los mensajes privados usan el prefijo `/queue` que implementa el patrón point-to-point. Cuando un cliente envía un mensaje privado, el servidor lo envía solo a la cola del destinatario específico (`/queue/{destinatario}`), asegurando que solo ese usuario reciba el mensaje.

**¿Qué es WebRTC y cómo se usa aquí?**
WebRTC (Web Real-Time Communication) es una tecnología que permite comunicación de audio/video directamente entre navegadores sin servidor intermediario. En este proyecto, el campo `webRTCSignal` en `ChatMessage` se usa para transportar la señalización (ofertas, respuestas, candidatos ICE) necesaria para establecer conexiones P2P.

---

**¡Explora el código, experimenta con diferentes configuraciones y aprende cómo funciona la comunicación en tiempo real!** 🎓
