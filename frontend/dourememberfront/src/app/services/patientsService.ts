import { Injectable} from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { HttpClient } from '@angular/common/http';
import { User } from '../models/User';
import { Doctor } from '../models/Doctor';

@Injectable({
  providedIn: 'root'
})

export class PatientsService{

  empty = 'nobody28'

  private readonly url: string = 'http://localhost:8080/api/v1/users'
    constructor(private readonly http: HttpClient){}

  getAllUserInformation(id: number | null): Observable<any>{
    return this.http.get<any>(`${this.url}/${id}`)
  }

  createPatient(user: User): Observable<User>{
    console.log('1')
    return this.http.post<User>(this.url, user);
  }

    createDoctor(): Observable<any>{
    console.log('2')
    return this.http.post(`${this.url}/createDoctor`, this.empty);
  }

  private decodeJwtPayload(token: string): any | null {
    try {
      const payloadBase64 = token.split('.')[1];
      if (!payloadBase64) {
        return null;
      }

      const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
      return JSON.parse(atob(padded));
    } catch {
      return null;
    }
  }

  getUserId(): number | null {
    const token = sessionStorage.getItem('token');
    if (token) {
      const payload = this.decodeJwtPayload(token);
      const tokenId = payload?.id;
      if (typeof tokenId === 'number') {
        return tokenId;
      }
      if (typeof tokenId === 'string' && tokenId.trim() !== '' && !Number.isNaN(Number(tokenId))) {
        return Number(tokenId);
      }
    }

    const loginRaw = sessionStorage.getItem('login');
    if (loginRaw) {
      try {
        const login = JSON.parse(loginRaw);
        const loginId = login?.user?.id;
        if (typeof loginId === 'number') {
          return loginId;
        }
        if (typeof loginId === 'string' && loginId.trim() !== '' && !Number.isNaN(Number(loginId))) {
          return Number(loginId);
        }
      } catch {
        return null;
      }
    }

    return null;
  }

  findAllUserSessionsByUserIdPage(userId: number, page: number): Observable<any> {
    return this.http.get<any>(`${this.url}/findAllByUserId/${userId}/sessions/${page}`);
  }

  getAllUserSessionsById(id: number): Observable<any> {
    return this.http.get<any>(`${this.url}/getAllUserSessionsById/${id}`);
  }

  createNewDoctor(doctor: Doctor): Observable<Doctor> {
    console.log('3')
    return this.http.post<Doctor>(`${this.url}/createNewDoctor`, doctor);
  }

  getAllDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.url}/doctors`);
  }

}
