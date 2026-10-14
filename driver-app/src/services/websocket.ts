import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class WebSocketService {
  private client: Client | null = null;
  private connected: boolean = false;
  private listeners: Map<string, (data: any) => void> = new Map();

  connect(token?: string | null): void {
    if (this.client && this.connected) return;

    const socketFactory = () => {
      try {
        return new SockJS('http://localhost:8080/ws');
      } catch {
        return new WebSocket('ws://localhost:8080/ws');
      }
    };

    this.client = new Client({
      webSocketFactory: socketFactory,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (msg) => {
        // Optional debug logging in dev mode
        if (import.meta.env.DEV) {
          console.debug('[STOMP]', msg);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      this.connected = true;
      console.log('✅ Connected to SwiftPulse STOMP WebSocket');

      // Re-subscribe all registered listeners
      this.listeners.forEach((callback, topic) => {
        this.client?.subscribe(topic, (message) => {
          try {
            const parsed = JSON.parse(message.body);
            callback(parsed);
          } catch {
            callback(message.body);
          }
        });
      });
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP Error:', frame.headers['message'], frame.body);
    };

    this.client.onWebSocketClose = () => {
      this.connected = false;
    };

    try {
      this.client.activate();
    } catch (err) {
      console.warn('STOMP activation error (backend may be offline):', err);
    }
  }

  subscribe(topic: string, callback: (data: any) => void): () => void {
    this.listeners.set(topic, callback);

    if (this.connected && this.client) {
      const subscription = this.client.subscribe(topic, (message) => {
        try {
          const parsed = JSON.parse(message.body);
          callback(parsed);
        } catch {
          callback(message.body);
        }
      });

      return () => {
        subscription.unsubscribe();
        this.listeners.delete(topic);
      };
    }

    return () => {
      this.listeners.delete(topic);
    };
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected = false;
    }
  }

  isConnected(): boolean {
    return this.connected;
  }
}

export const wsService = new WebSocketService();
