import { PatientsService } from './../../services/patientsService';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { User } from '../../models/User';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Doctor } from '../../models/Doctor';


@Component({
  selector: 'signup',
  imports: [RouterModule, FormsModule, CommonModule],
  templateUrl: './signup.html'
})
export class Signup {

  user: User;

  constructor(private readonly patientsService: PatientsService, private readonly router: Router) {
      this.user = new User();
  }

  onSubmit(): void {
    const userPayload = this.normalizeUserPayload(this.user);

    this.patientsService.createPatient(userPayload).subscribe({
      next: () => {
        setTimeout(() => {
          const doctor = new Doctor();
          if ((doctor as any).id !== undefined) {
            delete (doctor as any).id;
          }
        this.patientsService.createNewDoctor(doctor).subscribe({
            next: () => this.router.navigate(['/login']),
            error: () => this.router.navigate(['/login'])
          });
        }, 2000);
      },
      error: (error) => {
        console.error('User signup failed', error?.error ?? error);
      }
    });
  }
  private normalizeUserPayload(user: User): User {
    const { id, ...userWithoutId } = user;

    return {
      ...userWithoutId,
      profilepicture: userWithoutId.profilepicture?.trim() || 'default-profile.png',
      medical_condition: userWithoutId.medical_condition?.trim() || 'Sin condición reportada',
      carer: userWithoutId.carer?.trim() || 'Sin cuidador asignado'
    } as User;
  }

}
