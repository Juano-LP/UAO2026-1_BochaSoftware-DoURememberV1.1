import { SharingData } from './../services/sharing-data-service';
import { Component, signal } from '@angular/core';
import { Navbar } from './navbar/navbar';
import { Router, RouterOutlet } from '@angular/router';
import { Auth } from '../services/auth';
import { ToastrService } from 'ngx-toastr';


@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    Navbar],
  templateUrl: './app.html'
})

export class App {

  constructor(
    private toastr: ToastrService,
    private readonly SharingData: SharingData,
    private readonly authService: Auth,
    private readonly router: Router){

    this.handlerLogin()
  }

  // service or any TS file

 getUserId(): number | null {
  const token = sessionStorage.getItem("token"); // 0. retrieve token
  if (!token) return null;

  const payloadBase64 = token.split(".")[1]; // part after first dot
  if (!payloadBase64) return null;

  // 1. decode Base64URL → 2. JSON
  const json = JSON.parse(
    atob(payloadBase64.replace(/-/g, "+").replace(/_/g, "/"))
  );

  // 3. get id
  return json.id ?? null;
}


  handlerLogin(){
    this.SharingData.handlerLoginEventEmitter.subscribe(({username, password}) =>
      {

      this.authService.loginUser({username,password}).subscribe({
        next: response => {

          const token = response.token;
          const payload = this.authService.getpayload(token);   // for taking the first part of the jwt, and in this part we take the payload [1]
          const user = { id: payload.id ?? null, username: payload.sub }
          const login = { user, isAuth: true, isAdmin: payload.isAdmin } //is doctor?

          //session storage
          this.authService.token = token
          this.authService.user = login // under the hood invokes the set method
          // here i should change if it is doctor or if its patient using the jwt claims

          const userId = payload.id ?? this.getUserId()

          if(login.isAdmin != true){this.router.navigate([`/patient/${userId}`]);}
          else{this.router.navigate([`/doctor/${userId}/patients`]);}


        }, error: error => {if (error.status == 401){this.showLoginError()} else{throw error}}
          }
        )
      }
    )
  }

    showLoginError() {
  this.toastr.error('Login error', 'Username or password invalid');}
}
