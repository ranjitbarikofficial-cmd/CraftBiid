import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-config';

export interface SupportTicketItem {
  id: number;
  name: string;
  email: string;
  phone?: string;
  category: string;
  subject: string;
  message: string;
  ticketRef: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';
  resolutionNotes?: string;
  createdAt: string;
}

export interface CreateSupportTicketPayload {
  name?: string;
  email?: string;
  phone?: string;
  category: string;
  subject: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class SupportService {
  private apiUrl = `${getApiBaseUrl()}/api/support`;

  constructor(private http: HttpClient) {}

  createTicket(payload: CreateSupportTicketPayload): Observable<SupportTicketItem> {
    return this.http.post<SupportTicketItem>(`${this.apiUrl}/ticket`, payload);
  }

  getMyTickets(): Observable<SupportTicketItem[]> {
    return this.http.get<SupportTicketItem[]>(`${this.apiUrl}/my-tickets`);
  }

  getTicketByRef(ref: string): Observable<SupportTicketItem> {
    return this.http.get<SupportTicketItem>(`${this.apiUrl}/ticket/${ref}`);
  }
}
