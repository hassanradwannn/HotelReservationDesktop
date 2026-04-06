# 🏨 Desktop Hotel Reservation System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white)
![Sockets](https://img.shields.io/badge/Networking-Sockets-blue?style=for-the-badge)
![OOP](https://img.shields.io/badge/Architecture-OOP-success?style=for-the-badge)

## 📖 Project Overview
[cite_start]This project is a comprehensive **Desktop Hotel Reservation System** developed for the CSE241 Object-Oriented Computer Programming course (2nd Semester 2025/2026) at the Faculty of Engineering, Ain Shams University[cite: 1, 6, 7]. 

[cite_start]The system provides a complete end-to-end management solution for hotel operations, built entirely in Java[cite: 19]. [cite_start]It features a robust Object-Oriented backend, a dynamic JavaFX graphical user interface, multi-threaded background operations, and a real-time client-server live chat[cite: 21, 22].

---

## ✨ Key Features

### 👥 User Roles & Capabilities
* [cite_start]**Guests:** Can register, log in, browse available rooms with filters (type, price, amenities), make and cancel reservations, and securely check out/pay invoices [cite: 42-48, 97].
* [cite_start]**Receptionists:** Can view all hotel data, manage guest check-ins, process check-outs, and communicate with guests via live chat [cite: 55-58, 119].
* [cite_start]**Administrators:** Have full system control with CRUD capabilities for rooms, room types, and amenities[cite: 57, 63].

### 💻 Technical Highlights
* [cite_start]**Strict OOP Design:** Built utilizing advanced Object-Oriented principles including encapsulation, inheritance, abstract classes, and interface contracts (`Payable`, `Manageable`)[cite: 72, 88].
* [cite_start]**Interactive GUI:** Fully designed using JavaFX (FXML and CSS) with distinct screens for dashboards, room browsing, and reservation management[cite: 23, 96, 101, 102].
* [cite_start]**Multi-threading:** Utilizes Java threads/JavaFX Tasks for real-time room availability updates without freezing the main UI thread [cite: 111-115].
* [cite_start]**Client-Server Networking:** Implements Java Sockets to support a concurrent, real-time live chat feature between guests and receptionists[cite: 116, 117, 119, 121].
* [cite_start]**Data Validation:** Comprehensive input validation and custom exception handling (`RoomNotAvailableException`, `InvalidPaymentException`) ensure system stability[cite: 73, 74].

---

## 🛠️ Technology Stack
* **Language:** Java (JDK X.X)
* [cite_start]**GUI Framework:** JavaFX (No Swing used) [cite: 23]
* [cite_start]**Networking:** `java.net.Socket`, `java.net.ServerSocket` [cite: 121]
* [cite_start]**Concurrency:** Java Threads & `Platform.runLater()` [cite: 112, 115]
* [cite_start]**Version Control:** Git & GitHub [cite: 28]

---

## 📐 System Architecture & UML

The system architecture is divided into clear logical modules. 
*(Note: Replace the placeholder links below with the actual paths to the PDF/Image UML diagrams generated for the report).*

* **[View Core Data & Enums UML](./docs/uml_part1.pdf)**
* **[View Staff Inheritance Hierarchy UML](./docs/uml_part2.pdf)**
* **[View Booking & Room Aggregation UML](./docs/uml_part3.pdf)**

---

## 🚀 Getting Started

### Prerequisites
* Java Development Kit (JDK) installed.
* JavaFX SDK configured in your IDE.
* An IDE (IntelliJ IDEA, Eclipse, or VS Code).

### Installation & Execution
1.  Clone the repository:
    ```bash
    git clone [https://github.com/YourUsername/Hotel-Reservation-System.git](https://github.com/YourUsername/Hotel-Reservation-System.git)
    ```
2.  Open the project in your preferred IDE.
3.  Ensure JavaFX libraries are correctly linked in your project structure.
4.  Run the `Main.java` file (or the file containing your primary JavaFX `Application` class).
5.  *Optional:* To test networking, launch the Server class first, followed by multiple Client instances.

---

## 👨‍💻 Team Members

This project was collaboratively developed by:

1.  **[Student 1 Name]** - *[Brief description of contribution]*
2.  **[Student 2 Name]** - *[Brief description of contribution]*
3.  **[Student 3 Name]** - *[Brief description of contribution]*
4.  **[Student 4 Name]** - *[Brief description of contribution]*
5.  **[Student 5 Name]** - *[Brief description of contribution]*

[cite_start]*(Contributions are documented in detail within the final project report [cite: 27]).*
