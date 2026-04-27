# THECLASS
A Classroom Attendance System Based on Bluetooth Beacon and Face Recognition

## Features

- User login (student/admin)
- Bluetooth beacon based attendance
- Face recognition using TensorFlow Lite
- Course management
- Firebase cloud database

## Tech Stack

- **Language**: Java
- **Model**: TensorFlow Lite and OpenCV
- **Database**: Firebase Realtime Database
- **Bluetooth**: BLE beacon scanning

## Project Structure
- app/src/main/java/com/example/theclass/
    * MainActivity.java: The Login screen
    * Function.java: The sign-in screen for users
    * Manage.java:The admin screen
    * TeachableMachine.java: Face recognition model
    * Face.java: OpenCV face detection

- app/src/main/res/layout/
    * activity_main.xml
    * activity_function.xml
    * activity_manage.xml

- app/src/main/assets/
    * model_unquant.tflite - TensorFlow Lite model
    * labels.txt - Model labels
    * haarcascade_frontalface_default.xml: The OpenCV face detector

## Setup Instructions

1. Prerequisites
    - Android Studio
    - JDK 8 or higher
    - Android device with Android 6.0+

2. Default Login Credentials

 -  Username: admin    Password: 00000    Role: Admin
 -  Username: user1    Password: 12345    Role: Student
 -  Username: user2    Password: 54321    Role: Student
 -  Username: user3    Password: 54321    Role: Student
 -  Username: user4    Password: 54321    Role: Student



## Permissions Required (AndroidManifest.xml)

- CAMERA
- BLUETOOTH
- BLUETOOTH_ADMIN
- ACCESS_COARSE_LOCATION
- INTERNET

## How It Works

Student Flow:
1. Login
2. App scans for the Bluetooth beacon
3. When it connect with the beacon, "Sign In" buttons become active
4. Tap "Sign In" buttons then take photo
5. Face recognition verifies identity
6. If matched, attendance recorded to Firebase, it can watch on index.html.

Admin Flow:
1. Login as "admin"
2. Add new students 
3. Add new courses

Database Structure of Firebase:
- "attendance": {
- "user1": {
- "EIE444": true,
- "EIE555": false
- },
- "user2": { ... }
- },
- "signin_records": {
- "-Nx7kQ2pLm8...": {
- "username": "user1",
- "course": "EIE444",
- "time": "2024-01-15 10:30:00",
- "status": "success"
- }
- }
- }

## Future Improvements
Add liveness detection to prevent photo attacks
