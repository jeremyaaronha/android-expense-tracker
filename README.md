Overview

As a software engineer, I created this project to learn more about cloud databases and user login systems. My goal was to build a real mobile app that connects to a cloud database and can create, read, update, and delete data.

This software is a Personal Expense Tracker Android app built using Kotlin and Compose. Users can register, log in, and manage their transactions like income and expenses. All data is stored in Firebase Firestore, which is a cloud database. The app listens to real time changes in the database and updates the screen automatically.

https://youtu.be/kgGgtcbMPeY

Development Environment

This project uses Firebase Firestore as the cloud database. Firestore is a NoSQL database that stores data in collections and documents.

The structure of the database is:

Collection: users - Each user has a document with their user id.

Inside each user document there is a subcollection: transactions

Each transaction contains:
description
amount
date
type (income or expense)

The app uses a snapshot listener to get data in real time from Firestore. User login is handled by Firebase Authentication using email and password.

I used Android Studio to build and run the application. I tested the app using the Android Emulator.
The programming language used in this project is Kotlin. The app is built using Compose for the user interface and Navigation Compose for screen navigation.

⸻

Useful Websites

https://kotlinlang.org/docs/home.html

https://developer.android.com/jetpack/compose

https://firebase.google.com/docs

https://developer.android.com/jetpack/compose/navigation

⸻

Future Work

Add categories for transactions like food or rent.
Add charts to show spending.
Improve input validation.
Improve the design of the app.
