let stompClient = null;

function connect() {
    const serverIp = document.getElementById('serverIp').value;
    const username = document.getElementById('username').value;
    const socket = new SockJS('http://' + serverIp + ':8080/chat');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/public', function (message) {
            showMessage(JSON.parse(message.body));
        });
        stompClient.subscribe('/queue/' + username, function (message) {
            showMessage(JSON.parse(message.body));
        });
    }, function (error) {
        console.error('Error: ' + error);
        setConnected(false);
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function setConnected(connected) {
    const status = document.getElementById('status');
    if (connected) {
        status.className = 'connected';
        status.textContent = 'Conectado';
    } else {
        status.className = 'disconnected';
        status.textContent = 'Desconectado';
    }
    document.getElementById('message').disabled = !connected;
}

function toggleMessageType() {
    const messageType = document.querySelector('input[name="messageType"]:checked').value;
    const recipientContainer = document.getElementById('recipientContainer');
    if (messageType === 'private') {
        recipientContainer.style.display = 'block';
        switchTab('private');
    } else {
        recipientContainer.style.display = 'none';
        switchTab('public');
    }
}

function switchTab(tab) {
    const publicTab = document.querySelector('.tab-button:nth-child(1)');
    const privateTab = document.querySelector('.tab-button:nth-child(2)');
    const publicMessages = document.getElementById('publicMessages');
    const privateMessages = document.getElementById('privateMessages');
    
    if (tab === 'public') {
        publicTab.classList.add('active');
        privateTab.classList.remove('active');
        publicMessages.classList.add('active');
        privateMessages.classList.remove('active');
        document.querySelector('input[name="messageType"][value="public"]').checked = true;
        document.getElementById('recipientContainer').style.display = 'none';
    } else {
        privateTab.classList.add('active');
        publicTab.classList.remove('active');
        privateMessages.classList.add('active');
        publicMessages.classList.remove('active');
        document.querySelector('input[name="messageType"][value="private"]').checked = true;
        document.getElementById('recipientContainer').style.display = 'block';
    }
}

function sendMessage() {
    const username = document.getElementById('username').value;
    const content = document.getElementById('message').value;
    const messageType = document.querySelector('input[name="messageType"]:checked').value;
    
    if (content.trim() === '') return;
    
    if (messageType === 'private') {
        const recipient = document.getElementById('recipient').value;
        if (recipient.trim() === '') {
            alert('Por favor ingresa un destinatario para el mensaje privado');
            return;
        }
        stompClient.send("/app/chat.sendPrivate", {}, JSON.stringify({
            tipo: 'CHAT',
            usuario: username,
            contenido: content,
            destinatario: recipient
        }));
    } else {
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
            tipo: 'CHAT',
            usuario: username,
            contenido: content,
            destinatario: null
        }));
    }
    
    document.getElementById('message').value = '';
}

function showMessage(message) {
    if (message.tipo === 'WEBRTC_SIGNAL') {
        handleWebRTCSignal(message);
        return;
    }
    
    const messages = message.destinatario ? document.getElementById('privateMessages') : document.getElementById('publicMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message';
    messageDiv.innerHTML = '<strong>' + message.usuario + ':</strong> ' + message.contenido;
    messages.appendChild(messageDiv);
    messages.scrollTop = messages.scrollHeight;
}

// Allow sending message with Enter key
document.getElementById('message').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
});

// WebRTC variables
let localStream = null;
let peerConnection = null;
let remoteStream = null;
let currentCallRecipient = null;
let pendingOffer = null;
let pendingCaller = null;

const iceServers = {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' }
    ]
};

async function startVideo() {
    try {
        const constraints = {
            video: true,
            audio: true
        };
        
        localStream = await navigator.mediaDevices.getUserMedia(constraints);
        
        const localVideo = document.getElementById('localVideo');
        localVideo.srcObject = localStream;
        
        document.getElementById('videoContainer').style.display = 'block';
        document.getElementById('startVideoBtn').disabled = true;
        document.getElementById('stopVideoBtn').disabled = false;
        document.getElementById('callBtn').disabled = false;
        
        console.log('Cámara y audio iniciados');
    } catch (error) {
        console.error('Error al acceder a cámara/microfono:', error);
        alert('No se pudo acceder a la cámara o el microfono. Verifica los permisos.');
    }
}

function stopVideo() {
    if (localStream) {
        localStream.getTracks().forEach(track => track.stop());
        localStream = null;
        
        const localVideo = document.getElementById('localVideo');
        localVideo.srcObject = null;
        
        document.getElementById('videoContainer').style.display = 'none';
        document.getElementById('startVideoBtn').disabled = false;
        document.getElementById('stopVideoBtn').disabled = true;
        document.getElementById('callBtn').disabled = true;
        
        if (peerConnection) {
            peerConnection.close();
            peerConnection = null;
        }
        
        console.log('Cámara y audio detenidos');
    }
}

function createPeerConnection() {
    peerConnection = new RTCPeerConnection(iceServers);
    
    peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
            sendWebRTCSignal(currentCallRecipient, {
                type: 'ice-candidate',
                candidate: event.candidate
            });
        }
    };
    
    peerConnection.ontrack = (event) => {
        const remoteVideo = document.getElementById('remoteVideo');
        remoteVideo.srcObject = event.streams[0];
        console.log('Stream remoto recibido');
    };
    
    peerConnection.onconnectionstatechange = () => {
        console.log('Estado de conexión:', peerConnection.connectionState);
        if (peerConnection.connectionState === 'disconnected' || 
            peerConnection.connectionState === 'failed' || 
            peerConnection.connectionState === 'closed') {
            endCall();
        }
    };
    
    localStream.getTracks().forEach(track => {
        peerConnection.addTrack(track, localStream);
    });
}

async function startCall() {
    const recipient = document.getElementById('callRecipient').value.trim();
    if (!recipient) {
        alert('Por favor ingresa el usuario a llamar');
        return;
    }
    
    currentCallRecipient = recipient;
    createPeerConnection();
    
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    
    sendWebRTCSignal(recipient, {
        type: 'offer',
        offer: offer
    });
    
    document.getElementById('callBtn').disabled = true;
    document.getElementById('endCallBtn').disabled = false;
    console.log('Llamada iniciada a:', recipient);
}

async function handleWebRTCSignal(message) {
    if (message.tipo === 'WEBRTC_SIGNAL') {
        const signal = message.webRTCSignal;
        
        switch (signal.type) {
            case 'offer':
                if (!peerConnection) {
                    pendingOffer = signal.offer;
                    pendingCaller = message.usuario;
                    showIncomingCallNotification(message.usuario);
                }
                console.log('Offer recibida de:', message.usuario);
                break;
                
            case 'answer':
                await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.answer));
                console.log('Answer recibida de:', message.usuario);
                break;
                
            case 'ice-candidate':
                await peerConnection.addIceCandidate(new RTCIceCandidate(signal.candidate));
                console.log('ICE candidate recibido de:', message.usuario);
                break;
                
            case 'call-rejected':
                alert(message.usuario + ' rechazó la llamada');
                endCall();
                break;
        }
    }
}

function sendWebRTCSignal(recipient, signal) {
    const username = document.getElementById('username').value;
    stompClient.send("/app/chat.sendPrivate", {}, JSON.stringify({
        tipo: 'WEBRTC_SIGNAL',
        usuario: username,
        contenido: '',
        destinatario: recipient,
        webRTCSignal: signal
    }));
}

function endCall() {
    if (peerConnection) {
        peerConnection.close();
        peerConnection = null;
    }
    
    const remoteVideo = document.getElementById('remoteVideo');
    remoteVideo.srcObject = null;
    
    document.getElementById('callBtn').disabled = false;
    document.getElementById('endCallBtn').disabled = true;
    currentCallRecipient = null;
    
    console.log('Llamada terminada');
}

function showIncomingCallNotification(caller) {
    document.getElementById('callerName').textContent = caller;
    document.getElementById('incomingCallNotification').style.display = 'flex';
    console.log('Notificación de llamada entrante de:', caller);
}

async function acceptCall() {
    if (!pendingOffer || !pendingCaller) {
        console.error('No hay oferta pendiente para aceptar');
        return;
    }
    
    document.getElementById('incomingCallNotification').style.display = 'none';
    
    if (!localStream) {
        await startVideo();
    }
    
    currentCallRecipient = pendingCaller;
    createPeerConnection();
    
    await peerConnection.setRemoteDescription(new RTCSessionDescription(pendingOffer));
    const answer = await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(answer);
    sendWebRTCSignal(pendingCaller, {
        type: 'answer',
        answer: answer
    });
    
    document.getElementById('callBtn').disabled = true;
    document.getElementById('endCallBtn').disabled = false;
    
    pendingOffer = null;
    pendingCaller = null;
    
    console.log('Llamada aceptada de:', currentCallRecipient);
}

function rejectCall() {
    document.getElementById('incomingCallNotification').style.display = 'none';
    
    sendWebRTCSignal(pendingCaller, {
        type: 'call-rejected'
    });
    
    pendingOffer = null;
    pendingCaller = null;
    
    console.log('Llamada rechazada');
}
