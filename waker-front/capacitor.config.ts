/* eslint-disable @typescript-eslint/naming-convention */
import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.waker.front',
  appName: 'waker-front',
  webDir: 'www',
  bundledWebRuntime: false,
  plugins: {
    SplashScreen: {
      launchShowDuration: 0
    },
    GoogleAuth: {
      scopes: [
        'profile',
        'email'
      ],
      serverClientId: '',
      forceCodeForRefreshToken: true
    }
  }
};

export default config;
