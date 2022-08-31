import { Injectable } from '@angular/core';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  constructor() { }

  async googleSignIn() {
    GoogleAuth.initialize({
      scopes: ['profile', 'email'],
      grantOfflineAccess: true,
      clientId: environment.serverClientId
    });
    const googleUser = await GoogleAuth.signIn();
    console.log(googleUser);
  }
}
