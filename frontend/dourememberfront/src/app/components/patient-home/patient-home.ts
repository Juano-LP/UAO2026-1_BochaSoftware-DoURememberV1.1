import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NotificationModel } from '../../models/Notification';
import { User } from '../../models/User';

import { NotificationsService } from '../../services/notifications-service';
import { PatientsService } from '../../services/patientsService';

@Component({
  selector: 'patient-home',
  imports: [RouterModule, CommonModule],
  templateUrl: './patient-home.html'
})

export class PatientHome implements OnInit, OnDestroy {
  userId!: number | null;
  notifications = signal<NotificationModel[]>([]);

  private eventSource: EventSource | null = null;

  user = signal<User>({
    id: 0,
    profilepicture: '',
    name: '',
    lastname: '',
    email: '',
    username: '',
    password: '',
    role: '',
    medical_condition: '',
    carer: '',
    doctor: { id: 2 }
  });

  constructor(
    private readonly patientsService: PatientsService,
    private readonly notificationsService: NotificationsService
  ) {
    this.userId = this.patientsService.getUserId();
    this.findUserById(this.userId);
  }

  ngOnInit(): void {
    this.findUserById(this.userId);
    this.loadNotifications();
    this.startNotificationsStream();
  }

  ngOnDestroy(): void {
    this.eventSource?.close();
  }

  findUserById(id: number | null): void {
    this.patientsService.getAllUserInformation(id).subscribe((userInfo) => {
      this.user.set(userInfo);
    });
  }

  loadNotifications(): void {
    if (!this.userId) {
      return;
    }

    this.notificationsService.getByUser(this.userId).subscribe((notifications: NotificationModel[]) => {
      this.notifications.set(notifications);
    });
  }

  markAsRead(notificationId: number): void {
    if (!this.userId) {
      return;
    }

    this.notificationsService.markAsRead(notificationId, this.userId).subscribe((updated: NotificationModel) => {
      const current = this.notifications();
      this.notifications.set(current.map((n) => (n.id === updated.id ? updated : n)));
    });
  }

 get unreadCount(): number {
    return this.notifications().filter((n) => {
      if (n.readAt !== undefined) {
        return !n.readAt;
      }
    

      return !n.read;
    }).length;
  }


  private startNotificationsStream(): void {
    if (!this.userId) {
      return;
    }

    this.eventSource = this.notificationsService.stream(this.userId);
    this.eventSource.addEventListener('notification', () => {
      this.loadNotifications();
    });
  }
}