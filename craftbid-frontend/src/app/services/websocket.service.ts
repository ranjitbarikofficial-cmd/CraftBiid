import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { getWsBaseUrl } from './api-config';

export interface WebSocketEvent {
  eventType: string;
  auctionId?: number;
  userId?: number;
  timestamp: number;
  data: any;
}

@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  private socket: WebSocket | null = null;
  private connected = false;
  private isConnecting = false;
  private reconnectTimeout: any = null;
  private reconnectAttempts = 0;
  private readonly maxReconnectDelay = 10000;

  // Active topic subscriptions (persisted across reconnects)
  private subscriptions: Map<string, Subject<WebSocketEvent>> = new Map();
  private topicToSubId: Map<string, string> = new Map();
  private subCounter = 0;

  constructor(private zone: NgZone) {
    this.connect();
  }

  public connect(): void {
    if (typeof window === 'undefined') return;

    // Guard: Prevent duplicate sockets if already OPEN or CONNECTING
    if (
      this.socket &&
      (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)
    ) {
      return;
    }

    this.isConnecting = true;

    try {
      const wsUrl = getWsBaseUrl();
      console.debug('[WS] Initializing connection to:', wsUrl);
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        this.zone.run(() => {
          console.debug('[WS] Transport open. Sending STOMP CONNECT frame...');
          const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null;
          let connectFrame = 'CONNECT\naccept-version:1.2,1.1,1.0\nhost:/\nheart-beat:10000,10000\n';
          if (token && token.trim() && token !== 'null' && token !== 'undefined') {
            connectFrame += `Authorization:Bearer ${token.trim()}\n`;
          }
          connectFrame += '\n\0';
          this.socket?.send(connectFrame);
        });
      };

      this.socket.onmessage = (event) => {
        this.zone.run(() => {
          this.handleIncomingMessage(event.data);
        });
      };

      this.socket.onerror = (err) => {
        console.debug('[WS] Transport error event received');
      };

      this.socket.onclose = (event) => {
        this.zone.run(() => {
          console.debug('[WS] Closed:', {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean,
          });
          this.connected = false;
          this.isConnecting = false;
          this.topicToSubId.clear();
          this.scheduleReconnect();
        });
      };
    } catch (e) {
      console.debug('[WS] Connection exception:', e);
      this.isConnecting = false;
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    this.reconnectAttempts++;
    // Exponential backoff with jitter: 1.5s, 2.25s, 3.37s ... capped at 10s
    const delay = Math.min(1000 * Math.pow(1.5, this.reconnectAttempts - 1), this.maxReconnectDelay);
    console.debug(`[WS] Scheduling reconnect attempt #${this.reconnectAttempts} in ${Math.round(delay)}ms`);

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null;
      this.connect();
    }, delay);
  }

  private handleIncomingMessage(raw: string): void {
    if (!raw) return;

    // Handle heartbeats (single newline)
    if (raw === '\n' || raw === '\r\n') {
      return;
    }

    // Split on null byte for batched STOMP frames
    const frames = raw.split('\0');
    for (const frame of frames) {
      const trimmed = frame.trim();
      if (!trimmed) continue;

      // 1. Handle STOMP CONNECTED frame
      if (trimmed.startsWith('CONNECTED')) {
        this.connected = true;
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        console.debug('[WS] STOMP Protocol Handshake SUCCESS: Connected to broker.');

        // Resubscribe to all active topics
        this.subscriptions.forEach((_, topic) => {
          this.sendSubscribeFrame(topic);
        });
        continue;
      }

      // 2. Handle STOMP ERROR frame
      if (trimmed.startsWith('ERROR')) {
        console.warn('[WS] STOMP Broker ERROR frame received:', trimmed.split('\n')[0]);
        continue;
      }

      // 3. Handle STOMP MESSAGE frame
      if (trimmed.startsWith('MESSAGE')) {
        const headerEnd = trimmed.indexOf('\n\n');
        if (headerEnd !== -1) {
          const headerPart = trimmed.substring(0, headerEnd);
          const bodyPart = trimmed.substring(headerEnd + 2);

          const destMatch = headerPart.match(/destination:(.+)/i);
          if (destMatch && destMatch[1]) {
            const destination = destMatch[1].trim();
            try {
              const parsed = JSON.parse(bodyPart) as WebSocketEvent;
              const sub = this.subscriptions.get(destination);
              if (sub) {
                sub.next(parsed);
              }
            } catch (e) {
              // Non-JSON message payload
            }
          }
        }
      }
    }
  }

  private sendSubscribeFrame(topic: string): void {
    if (!this.connected || !this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    // Only send subscribe frame if not already subscribed
    if (this.topicToSubId.has(topic)) {
      return;
    }
    const subId = 'sub-' + (++this.subCounter);
    this.topicToSubId.set(topic, subId);
    console.debug(`[WS] Subscribing to: ${topic} (id: ${subId})`);
    const frame = `SUBSCRIBE\nid:${subId}\ndestination:${topic}\nack:auto\n\n\0`;
    this.socket.send(frame);
  }

  public subscribeToAuction(auctionId: number): Observable<WebSocketEvent> {
    const topic = `/topic/auctions/${auctionId}`;
    if (!this.subscriptions.has(topic)) {
      const subject = new Subject<WebSocketEvent>();
      this.subscriptions.set(topic, subject);
    }
    this.sendSubscribeFrame(topic);
    return this.subscriptions.get(topic)!.asObservable();
  }

  public subscribeToUser(userId: number): Observable<WebSocketEvent> {
    const topic = `/topic/users/${userId}`;
    if (!this.subscriptions.has(topic)) {
      const subject = new Subject<WebSocketEvent>();
      this.subscriptions.set(topic, subject);
    }
    this.sendSubscribeFrame(topic);
    return this.subscriptions.get(topic)!.asObservable();
  }

  public subscribeToOrder(orderId: number): Observable<WebSocketEvent> {
    const topic = `/topic/orders/${orderId}`;
    if (!this.subscriptions.has(topic)) {
      const subject = new Subject<WebSocketEvent>();
      this.subscriptions.set(topic, subject);
    }
    this.sendSubscribeFrame(topic);
    return this.subscriptions.get(topic)!.asObservable();
  }

  public unsubscribe(topic: string): void {
    const subId = this.topicToSubId.get(topic);
    if (subId && this.connected && this.socket?.readyState === WebSocket.OPEN) {
      console.debug(`[WS] Unsubscribing from: ${topic} (id: ${subId})`);
      const frame = `UNSUBSCRIBE\nid:${subId}\n\n\0`;
      this.socket.send(frame);
    }
    this.topicToSubId.delete(topic);
    this.subscriptions.delete(topic);
  }

  public isSocketConnected(): boolean {
    return this.connected && this.socket?.readyState === WebSocket.OPEN;
  }
}
