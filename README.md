Desktop Hotel Reservation System

Project Overview
This project is a comprehensive Desktop Hotel Reservation System developed for the CSE241 Object-Oriented Computer Programming course (2nd Semester 2025/2026) at the Faculty of Engineering, Ain Shams University.

- Key Features
User Roles & Capabilities
Guests: Can register, log in, browse available rooms with filters (type, price, amenities), make and cancel reservations, and securely check out/pay invoices.

Receptionists: Can view all hotel data, manage guest check-ins, process check-outs, and communicate with guests via live chat.

Administrators: Have full system control with CRUD capabilities for rooms, room types, and amenities.

💻 Technical Highlights
Strict OOP Design: Built utilizing advanced Object-Oriented principles including encapsulation, inheritance, abstract classes, and interface contracts (Payable, Manageable).

Interactive GUI: Fully designed using JavaFX (FXML and CSS) with distinct screens for dashboards, room browsing, and reservation management.

Multi-threading: Utilizes Java threads/JavaFX Tasks for real-time room availability updates without freezing the main UI thread.

Client-Server Networking: Implements Java Sockets to support a concurrent, real-time live chat feature between guests and receptionists.

Data Validation: Comprehensive input validation and custom exception handling (RoomNotAvailableException, InvalidPaymentException) ensure system stability.

🛠️ Technology Stack
Language: Java

GUI Framework: JavaFX

Networking: java.net.Socket, java.net.ServerSocket

Concurrency: Java Threads & Platform.runLater()

Version Control: Git & GitHub

