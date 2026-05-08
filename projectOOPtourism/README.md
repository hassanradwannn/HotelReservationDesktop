# Hotel Reservation System - OOP Project

## Overview
This is a JavaFX-based hotel reservation system built using Object-Oriented Programming principles. The system allows guests, receptionists, and administrators to manage hotel operations including room reservations, check-ins/check-outs, payments, and system configurations.

## Technologies Used
- **Java 17** - Core programming language
- **JavaFX 17** - GUI framework
- **Maven** - Build and dependency management
- **MySQL** - Database for persistent storage
- **FXML** - Declarative UI markup

## Project Structure
```
projectOOPtourism/
|-- src/
|   `-- main/
|       |-- java/
|       |   |-- Controllers/ - JavaFX controller classes
|       |   |-- Models/ - Entity classes (Guest, Room, Reservation, etc.)
|       |   |-- Services/ - Business logic services
|       |   |-- Repositories/ - Data access layers
|       |   |-- Utils/ - Utility classes and exceptions
|       |   `-- Main.java - Application entry point
|       `-- resources/
|           |-- [FXML files] - UI layouts
|           `-- styles.css - Styling
|-- pom.xml - Maven configuration
`-- README.md - This file
```

## Key Components

### 1. Application Entry Point (`Main.java`)
- Extends `javafx.application.Application`
- Manages application state (current user, selected room, etc.)
- Handles scene switching between different views
- Implements auto-refresh mechanism for real-time data synchronization
- Initializes database and checks for first-time setup

### 2. Database Layer (`Database.java`)
- Centralized data management using the Singleton pattern
- Handles all database operations (CRUD) for:
  - RoomTypes, Amenities, Rooms, Guests, Staff, Reservations, Invoices
- Implements lazy loading and caching of data
- Provides methods for data synchronization between instances
- Includes utility methods for checking table emptiness and data versioning

### 3. Model Classes
- **User Hierarchy**: `User` (abstract) -> `Guest`, `Staff` -> `Admin`, `Receptionist`
- **Hotel Entities**: `Room`, `RoomType`, `Amenity`
- **Reservation System**: `Reservation`, `ReservationStatus`, `Invoice`
- **Support Classes**: `SystemTime`, `Gender`, `PaymentMethod`, `Payable`

### 4. Services
- **Authentication**: Handles user login/logout and session management
- **AuthService**: Manages authentication logic and password hashing
- **AvailabilityService**: Checks room availability for given dates
- **PricingService**: Calculates reservation costs
- **ReservationService**: Handles reservation lifecycle operations
- **CatalogService**: Manages room types and amenities catalog
- **GuestPreferenceRanker**: Ranks room recommendations based on guest preferences

### 5. Controllers (JavaFX)
Each FXML file has a corresponding controller class that handles:
- User interactions (button clicks, form submissions)
- Data binding between UI and model objects
- Navigation between different views
- Validation of user input
- Examples:
  - `LoginController`: Handles user authentication
  - `MakeReservationController`: Manages the reservation creation flow
  - `GuestHomeController`: Main interface for guest users
  - `AdminMenuController`: Navigation for admin functions
  - `GenericListController`: Reusable controller for displaying lists of entities

### 6. Repositories
- Data access objects that encapsulate SQL queries:
  - `UserRepository`, `RoomRepository`, `ReservationRepository`, etc.
  - Handle mapping between database records and Java objects
  - Provide methods for finding, saving, updating, and deleting records

### 7. Utilities
- **DatabaseConnection**: Manages JDBC connections to MySQL
- **DatabaseInitializer**: Sets up default data on first run
- **DatabaseSaver**: Handles asynchronous database updates
- **DatabaseSync**: Synchronizes data between multiple application instances
- **FxNodeSync**: Synchronizes JavaFX node properties
- **Exception Classes**: Custom exceptions for business logic validation

## How the Application Works

### Startup Sequence
1. `Main.start()` initializes the application
2. `DatabaseInitializer.initializeDatabase()` sets up default data if needed
3. `Database.loadAll()` fetches all data from the database into memory
4. Login screen is displayed (`/Login.fxml`)

### User Roles and Dashboards
- **Guest**: Views available rooms, makes reservations, manages profile
- **Receptionist**: Handles check-ins/check-outs, manages reservations, processes payments
- **Administrator**: Manages room types, amenities, staff, and system settings

### Data Synchronization
- The application uses a versioning system (`SystemSettingsRepository`) to detect changes
- Each instance polls the database periodically (via `Timeline`) for updates
- When changes are detected, local caches are refreshed and active views are updated
- This allows multiple instances to stay in sync without requiring a database trigger

### Reservation Flow
1. Guest searches for available rooms via `AvailabilityService`
2. Selects a room and room type
3. Enters guest information and reservation dates
4. System calculates pricing and creates a `Reservation` object
5. Reservation is saved to database and appears in relevant views
6. On check-in date, status automatically updates to `CHECKING_IN`
7. On check-out date, status updates to `CHECKING_OUT` then to `COMPLETED` after payment

## Configuration
- Database connection details are in `src/main/java/Repositories/database/DatabaseConnection.java`
- JavaFX module path and VM arguments are configured in `nbactions.xml`
- Maven dependencies are managed in `pom.xml`

## Running the Application
1. Ensure MySQL is running and create the database schema
2. Update database credentials in `DatabaseConnection.java` if needed
3. Run: `mvn javafx:run` or use the configured Maven actions in your IDE

## Design Patterns Used
- **MVC**: Separation of concerns (Model-View-Controller)
- **Singleton**: Database and SystemSettings classes
- **Factory**: Object creation in various services
- **Observer**: Property change listeners in JavaFX bindings
- **Strategy**: Different pricing algorithms in `PricingService`
- **Template Method**: Common workflows in controller classes

## Future Improvements
- Implement proper internationalization (i18n)
- Add unit and integration tests
- Enhance security with stronger password hashing
- Add reporting and analytics features
- Implement mobile-responsive design
