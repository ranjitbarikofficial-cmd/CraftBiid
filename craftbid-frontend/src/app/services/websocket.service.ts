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
  private reconnectTimeout: any = null;
  private subscriptions: Map<string, Subject<WebSocketEvent>> = new Map();
  private pendingSubTopics: Set<string> = new Set();
  private subCounter = 0;
  private topicToSubId: Map<string, string> = new Map();

  constructor(private zone: NgZone) {
    this.initConnection();
  }

  private initConnection(): void {
    if (typeof window === 'undefined') return;

    try {
      const wsUrl = getWsBaseUrl();
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        this.zone.run(() => {
          // Send STOMP CONNECT frame
          const connectFrame = 'CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\n\n\0';
          this.socket?.send(connectFrame);
        });
      };

      this.socket.onmessage = (event) => {
        this.zone.run(() => {
          this.handleIncomingMessage(event.data);
        });
      };

      this.socket.onerror = (err) => {
        // Silent connection attempt - will fallback or retry
      };

      this.socket.onclose = () => {
        this.zone.run(() => {
          this.connected = false;
          this.topicToSubId.clear();
          this.scheduleReconnect();
        });
      };
    } catch (e) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    this.reconnectTimeout = setTimeout(() => {
      this.initConnection();
    }, 4000);
  }

  private handleIncomingMessage(raw: string): void {
    if (!raw) return;

    // Handle CONNECTED frame
    if (raw.startsWith('CONNECTED')) {
      this.connected = true;
      // Resubscribe to all pending topics
      this.subscriptions.forEach((_, topic) => {
        this.sendSubscribeFrame(topic);
      });
      return;
    }

    // Handle MESSAGE frame
    if (raw.startsWith('MESSAGE')) {
      const headerEnd = raw.indexOf('\n\n');
      if (headerEnd !== -1) {
        const headerPart = raw.substring(0, headerEnd);
        const bodyPart = raw.substring(headerEnd + 2).replace(/\0$/, '');

        // Extract destination topic
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
            // Non-json message body
          }
        }
      }
    }
  }

  private sendSubscribeFrame(topic: string): void {
    if (!this.connected || !this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    const subId = 'sub-' + (++this.subCounter);
    this.topicToSubId.set(topic, subId);
    const frame = `SUBSCRIBE\nid:${subId}\ndestination:${topic}\nack:auto\n\n\0`;
    this.socket.send(frame);
  }

  public subscribeToAuction(auctionId: number): Observable<WebSocketEvent> {
    const topic = `/topic/auctions/${auctionId}`;
    if (!this.subscriptions.has(topic)) {
      const subject = new Subject<WebSocketEvent>();
      this.subscriptions.set(topic, subject);
      this.sendSubscribeFrame(topic);
    }
    return this.subscriptions.get(topic)!.asObservable();
  }

  public subscribeToUser(userId: number): Observable<WebSocketEvent> {
    const topic = `/topic/users/${userId}`;
    if (!this.subscriptions.has(topic)) {
      const subject = new Subject<WebSocketEvent>();
      this.subscriptions.set(topic, subject);
      this.sendSubscribeFrame(topic);
    }
    return this.subscriptions.get(topic)!.asObservable();
  }

  public unsubscribe(topic: string): void {
    const subId = this.topicToSubId.get(topic);
    if (subId && this.connected && this.socket?.readyState === WebSocket.OPEN) {
      const frame = `UNSUBSCRIBE\nid:${subId}\n\n\0`;
      this.socket.send(frame);
    }
    this.subscriptions.delete(topic);
    this.topicToSubId.delete(topic);
  }
}
