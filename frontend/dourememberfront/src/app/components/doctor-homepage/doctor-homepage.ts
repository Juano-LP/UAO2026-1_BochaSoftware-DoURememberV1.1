import { PatientsService } from './../../services/patientsService';
import { ServiceDoctors } from './../../services/service-doctors';
import { Component, signal } from '@angular/core';
import { User } from '../../models/User';
import { PatientCard } from '../patient-card/patient-card';

@Component({
  selector: 'doctor-homepage',
  imports: [PatientCard],
  templateUrl: './doctor-homepage.html',
})

export class DoctorHomepage {

  patients = signal<User[]>([]);

  doctorInfo =  signal<User>({
    id: null,
    profilepicture: '',
    name: '',
    lastname: '',
    email: '',
    username: '',
    password: '',
    role: '',
    medical_condition: '',
    carer: '',
    doctor : {id: 2 }
  })

  userId: number | null = 0;

  doctorName = signal<string>('Angular refreh error, please refresh')

  doctorImage: string | null = null;

  constructor(private readonly ServiceDoctors: ServiceDoctors, private readonly PatientsService: PatientsService){
    this.userId = this.PatientsService.getUserId();

    if (this.userId !== null && !Number.isNaN(this.userId)) {
      this.PatientsService.getAllUserInformation(this.userId).subscribe(userInfo => {
        this.doctorInfo.set(userInfo)
        this.doctorName.set( this.doctorInfo().name)
        this.doctorImage = this.doctorInfo().profilepicture
      })
      this.getAllPatients()
    }
  }

  getAllPatients(){
    this.ServiceDoctors.getAllDoctorPatients(this.userId).subscribe(allDoctorPatients => {
      this.patients.set(allDoctorPatients)
    })
  }

  /**
   * Open the user's default mail client with a prefilled invitation.
   * Uses mailto: so the user can choose the recipient and send the invite.
   */
  invitePatientEmail(){
    const subject = `Invitation to join DoURemember`;
    const signupLink = 'https://douremember.example.com';
    const doctor = this.doctorName ? this.doctorName() : '';
    const doctorEmail = this.doctorInfo ? this.doctorInfo().email : '';
    const body = `Hello. ${doctor} has invited you to join DoURemember. Sign up here: http://douremember.com/signup, Remember! the doctor ID is = ${this.userId}  you have questions, reply to ${doctorEmail} Best regards,${doctor}`;

    const mailto = `mailto:?subject=${encodeURIComponent(subject)}&body=${body}`;
    // Use window.location.href to open the default mail client
    window.location.href = mailto;
  }

}
