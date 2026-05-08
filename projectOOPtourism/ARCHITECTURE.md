# Hotel Reservation System - Technical Architecture

## Overview
This document explains the architectural structure, key relationships, and connections between components in the Hotel Reservation System codebase. It serves as a technical reference for understanding how the system is organized and how different parts interact.

## Architectural Patterns

### 1. Layered Architecture
The system follows a layered architecture pattern:
- **Presentation Layer**: JavaFX FXML files + Controllers
- **Application Layer**: Services (ReservationService, AuthService, etc.)
- **Domain Layer**: Model classes (User, Guest, Room, Reservation, etc.)
- **Data Access Layer**: Repositories + Database class
- **Infrastructure Layer**: Database connection, utilities

### 2. Model-View-Controller (MVC)
- **Model**: Entity classes in `src/main/java/` (e.g., Guest.java, Room.java)
- **View**: FXML files in `src/main/resources/` (e.g., Login.fxml, GuestHome.fxml)
- **Controller**: Controller classes in `src/main/java/` (e.g., LoginController.java, GuestHomeController.java)

### 3. Singleton Pattern
- `Database.java`: Centralized data management
- `SystemSettingsRepository.java`: Manages application state and versioning
- `Main.java`: Application entry point with static instance access

### 4. Service Layer
Business logic is encapsulated in service classes:
- `ReservationService.java`: Handles reservation lifecycle
- `AuthService.java`: Manages authentication and authorization
- `AvailabilityService.java`: Checks room availability
- `PricingService.java`: Calculates costs
- `CatalogService.java`: Manages room types and amenities

### 5. Repository Pattern
Data access is abstracted through repository interfaces:
- `UserRepository.java`, `RoomRepository.java`, `ReservationRepository.java`, etc.
- Implementations handle SQL queries and object-relational mapping

## Core Component Relationships

### 1. User Hierarchy
```
User (abstract)
├── Guest
│   ├── balance, address, gender, roomPreferences
│   ├── methods: getBalance(), setRoomPreferences(), prefersAmenity()
│   └── register() -> Authentication.register()
├── Staff (abstract)
│   ├── role, workingHours
│   ├── Admin
│   │   ├── role = ADMIN
│   │   ├── methods: registerGuest(), registerStaff(), CRUD operations
│   │   └── delegates to CatalogService for entity management
│   └── Receptionist
│       ├── role = RECEPTIONIST
│       ├── methods: viewReservations(), checkInGuest(), checkOutGuest()
│       └── uses ReservationService for operations
```

### 2. Service Dependencies
```
Main.java
├── uses Database.initializeDatabase() and Database.loadAll()
├── manages user sessions and current views
├── coordinates auto-refresh via Timeline
└── delegates to role-specific dashboard methods

ReservationService
├── depends on AvailabilityService (composition)
├── depends on ReservationPaymentService (composition)
├── uses Database.getRooms() and Database.getReservations()
├── uses DatabaseSaver for persistence
└── called by controllers (MakeReservationController, Receptionist controllers)

AuthService
├── uses UserDatabase for user lookup
├── uses Database.addUser() for registration
├── uses DatabaseSaver.saveUser() for persistence
├── updates in-memory cache via Database.addUser()
└── called by LoginController, RegisterController

CatalogService
├── static methods for managing RoomType and Amenity entities
├── uses Database.getRoomTypes() and Database.getAmenities()
├── validates uniqueness and handles normalization
└── called by Admin controller methods and Controllers

AvailabilityService
├── checks date validity and room availability
├── uses Database.getReservations() for overlap detection
├── used by ReservationService and MakeReservationController
└── handles amenity matching logic
```

### 3. Data Flow
```
User Action (UI) 
        ↓
Controller (handles event)
        ↓
Service Layer (business logic)
        ↓
Repository/Data Layer (persistence)
        ↓
Database (MySQL)
        ↓
[Changes trigger version update]
        ↓
SystemSettingsRepository.notifyDataChanged()
        ↓
Main.autoRefreshTimeline detects version change
        ↓
Main.refreshRuntimeDataFromDatabase()
        ↓
Controllers refresh views via currentViewRefresher
```

### 4. Controller-FXML Mapping
Each FXML file has a corresponding controller:
- `Login.fxml` ↔ `LoginController.java`
- `GuestHome.fxml` ↔ `GuestHomeController.java`
- `MakeReservation.fxml` ↔ `MakeReservationController.java`
- `AdminMenu.fxml` ↔ `AdminMenuController.java`
- `GenericList.fxml` ↔ `GenericListController.java` (reusable)
- `RoomDetails.fxml` ↔ `RoomDetailsController.java`
- And so on for all UI screens

### 5. Database-Centric Design
The `Database.java` class acts as an in-memory cache and facade:
- Maintains static lists: `roomTypes`, `amenities`, `rooms`, `guests`, `staffMembers`, `reservations`, `invoices`
- Provides getter methods for all entities
- Handles loading/synchronizing with MySQL database
- Implements change notification via `SystemSettingsRepository`
- Provides CRUD operations that update both memory and database

### 6. Key Utility Classes
- `SystemTime.java`: Manages application time (can be advanced for testing)
- `SystemSettingsRepository.java`: Tracks data version for synchronization
- `DatabaseSaver.java`: Handles asynchronous database writes
- `DatabaseSync.java`: Synchronizes data between multiple instances
- `FxNodeSync.java`: Synchronizes JavaFX node properties
- `GuestPreferenceRanker.java`: Ranks room recommendations
- Exception classes: Custom exceptions for business logic validation

## Important Interactions

### 1. Login Process
```
LoginController.handleLoginButton()
        ↓
AuthService.login(username, password)
        ↓
UserDatabase.findUser(username)
        ↓
[if credentials valid]
        ↓
UserDatabase.setUserLoggedIn(username, true)
        ↓
AuthService.updateInMemoryCache(user) → Database.addUser()
        ↓
Main.show[Role]Dashboard(user)
        ↓
Main sets up auto-refresh timeline
        ↓
App navigates to appropriate dashboard
```

### 2. Reservation Creation
```
MakeReservationController.handleCreateReservation()
        ↓
ReservationService.createReservation(guest, room, checkIn, checkOut, addGym)
        ↓
[validates dates, availability, no overlaps]
        ↓
Creates Reservation object with UUID ID
        ↓
Adds to Database.getReservations() (memory)
        ↓
DatabaseSaver.saveReservation(...) → persists to MySQL
        ↓
Returns reservation object to controller
        ↓
Controller navigates to confirmation view
```

### 3. Check-in/Check-out Process
```
ReceptionistController.handleCheckIn()
        ↓
ReservationService.checkInGuest(reservation, guest)
        ↓
[validates status and date]
        ↓
Sets reservation status to ONGOING
        ↓
DatabaseSaver.updateReservationStatus(...) → persists to MySQL
        ↓
[Similar flow for check-out with payment processing]
```

### 4. Data Synchronization Mechanism
```
Main.autoRefreshTimeline (runs every 150ms for guests, 500ms for staff)
        ↓
Checks Database.getLatestDataVersion() vs localDataVersion
        ↓
[if newer version exists]
        ↓
Main.refreshRuntimeDataFromDatabase()
        ↓
[reloads all data from MySQL into memory lists]
        ↓
SystemTime.syncFromDatabase()
        ↓
Main.getInstance().refreshActiveView()
        ↓
Current view's refresher runs (if defined)
        ↓
UI updates with latest data
```

### 5. Admin Operations
```
AdminController.handleCreateRoomType()
        ↓
Admin.createRoomType(name, price, capacity)
        ↓
Delegates to CatalogService.createRoomType(name, price, capacity)
        ↓
[validates doesn't already exist]
        ↓
Database.getRoomTypes().add(new RoomType(...))
        ↓
DatabaseSaver.saveRoomType(...) → persists to MySQL
        ↓
CatalogService.listRoomTypes() reflects change immediately
        ↓
notifyDataChanged() triggers sync for other instances
```

## Cross-Cutting Concerns

### 1. Threading
- Database operations often run on background threads to avoid blocking UI
- `DatabaseSaver` uses separate threads for persistence
- Auto-refresh timeline runs on JavaFX Application Thread but spawns background threads for DB work
- Proper synchronization using `synchronized (Database.class)` blocks

### 2. Error Handling
- Custom exception types for business rules (InUseException, AlreadyExistsException, etc.)
- Services throw `IllegalArgumentException` for invalid operations
- Controllers catch exceptions and show user-friendly error dialogs
- Database operations catch SQLException and print stack traces

### 3. Data Consistency
- In-memory lists are cleared and reloaded during refresh operations
- Versioning system prevents stale data
- Database operations use transactions where appropriate (e.g., amenity deletion)
- Cache invalidation happens on data changes

## Extension Points

### 1. Adding New Entity Types
1. Create model class (e.g., `Promotion.java`)
2. Add repository interface and implementation
3. Add service class if business logic needed
4. Add FXML views and controllers for CRUD operations
5. Update Database class with new list and loading methods
6. Add menu items to relevant dashboards

### 2. Adding New Features
1. Identify which service would contain the business logic
2. Create or extend service class
3. Create controller to handle UI interactions
4. Create FXML view
5. Wire up in Main.java dashboard methods if needed
6. Add to service dependencies as needed

### 3. Changing Persistence Mechanism
1. Modify DatabaseConnection.java for new DB type
2. Update repository implementations to use new SQL dialect
3. Change Database.java loading/saving methods as needed
4. No changes needed to service or controller layers if repositories maintain same interfaces

## Conclusion
This architecture provides a clean separation of concerns while maintaining practical simplicity. The use of services, repositories, and a centralized database facade makes the system maintainable and extensible. The real-time synchronization mechanism allows multiple instances to work together seamlessly, which is essential for a hotel reservation system where front desk staff, managers, and guests might be using the system simultaneously.