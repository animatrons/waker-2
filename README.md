# waker-2
A hybride mobile app, for web and android
## To get started:
Make sure to have **node v16+**, **ionic/angular**, **android studio** + **android skd** and latest **java** installed.
In the front-end directory run 
```
npm install
```
-----------------------------------------
###### For global dependencies in the front-end here's how i started ***(this is just to document the proper setup process because it can get confusing with all the different deps and versions, skip this if everything works fine)***
  1. Uninstall the old ionic version and install the latest cli (angular is included) along with native-run:
    
```
npm uninstall -g ionic
npm install -g @ionic/cli@latest native-run
```
  
  2. (if you haven't already) Start a ionic app, and choose angular when prompted:
   
```
ionic start
```

Or:
  
  2.5. if you already have an app setup and capacitor added, update capacitor and angular
    
```
npm i @capacitor/{core,cli,ios,android,filesystem}@latest
ng update --all #will update core, cli and others
```

jump to step 6-

  3. Drop capacitor in the root of the app:
    
```
npm i @capacitor/core
npm i -D @capacitor/cli
```
  
  4. Init capacitor config:
    
```
npx cap init
```
  
  5. Install android and ios platforms:
    
```
npm i @capacitor/android @capacitor/ios
```
  
  6. Add your facorite capacitor plugin (here i am installing google auth, using npm)
    
```
npm i @codetrix-studio/capacitor-google-auth@latest
```
----------------------------

To use and emulated device, open android studio, add a device in device manager run it, and run:
```
npx ionic cap run android -l --external
```
You will be prompted to slect a device from a list, select the emulated one, if your machine is cennected to a physical device, enable dev mode and authorize the machine, and it will show in the list.

## For the backend:
These environment variables must be configured (in IDE lauch config):
```
mongodbHost: localhost
mongodbPort: 27017
PORT: 8888
MAIN_EMAIL_ADDRESS: ***(private domain email address, linked to a approved Posmark account)***
SECONDARY_EMAIL_ADDRESS: ***(the google workspace domain email address, with domain wide delegation for the google service account enabled)***
POSTMART_API_KEY: ***private api key***
```
In mongo shell create the db ```wakerdb```.

Add a user with ```username: wakerman``` ```password: 1111```.

Run ```mvn clean install``` and then run the Java app.


# What is this app for anyway?
Good question:
A *no-reward, only-punishment* system of alarm and task management
----------------

**Synopsis**: just an app for waking people up from slumber, ***very effectively***
