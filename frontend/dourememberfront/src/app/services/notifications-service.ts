import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environments } from '../../environments/environment';
import { NotificationModel } from '../models/Notification';

@Injectable({
  providedIn: 'root'
})
export class NotificationsService {
  private readonly baseUrl = `${environments.apiUrl}/api/v1/notifications`;

  constructor(private readonly http: HttpClient) {}

  getByUser(userId: number, unreadOnly: boolean = false): Observable<NotificationModel[]> {
    const params = new HttpParams().set('unreadOnly', unreadOnly);
    return this.http.get<NotificationModel[]>(`${this.baseUrl}/user/${userId}`, { params });
  }

  markAsRead(notificationId: number, userId: number): Observable<NotificationModel> {
    const params = new HttpParams().set('userId', userId);
    return this.http.put<NotificationModel>(`${this.baseUrl}/${notificationId}/read`, {}, { params });
  }

  stream(userId: number): EventSource {
    return new EventSource(`${this.baseUrl}/stream/${userId}`);
  }
}