// Client STOMP/SockJS de capture pour valider /topic/status (Phase 5/6 E2E)
const SockJS = require('sockjs-client');
const { Client } = require('@stomp/stompjs');

const socket = new SockJS('http://localhost:8080/ws');
const client = new Client({
  webSocketFactory: () => socket,
  reconnectDelay: 0,
  onConnect: () => {
    console.log('CONNECTED to /ws');
    client.subscribe('/topic/status', (msg) => {
      console.log('STATUS_MSG: ' + msg.body);
    });
  },
  onWebSocketClose: () => console.log('WS CLOSED'),
  onStompError: (f) => console.log('STOMP_ERROR: ' + f.headers.message),
});

client.activate();

setTimeout(() => { console.log('LISTEN_DONE'); process.exit(0); }, 480000);