export interface NotificationModel {
  id: number;
  title: string;
  message?: string;
  description?: string;
  createdAt?: string;
  eventAt?: string;
  readAt?: string | null;
  read?: boolean;
}