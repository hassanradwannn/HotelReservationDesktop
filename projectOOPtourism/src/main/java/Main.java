import exceptions.*;
import DatabaseInitializer.DatabaseInitializer;

import javafx.collections.FXCollections;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;

public class Main extends Application {

    private Stage stage;
    private User currentUser;
    private Room selectedRoomForReservation;

    private final int WIDTH = 1280;
    private final int HEIGHT = 720;

    private static final String BG = "#F7EFE5";
    private static final String CARD = "#EFE4D6";
    private static final String CARD_LIGHT = "#F9F3EA";
    private static final String ROSE = "#C74261";
    private static final String ROSE_SOFT = "#E6A4B4";
    private static final String BURGUNDY = "#8F1D3F";
    private static final String GOLD = "#C9AA7C";
    private static final String TEXT = "#2B2421";
    private static final String MUTED = "#8A726B";
    private static final String WHITE = "#FFFFFF";

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Grand Budapest Hotel Reservation System");
        
        // Initialize database and load users from SQL
        DatabaseInitializer.initializeDatabase();
        
        showLoginScreen();
        stage.show();
    }

    private void showLoginScreen() {
        AnchorPane root = new AnchorPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        HBox container = new HBox();
        container.setPrefSize(1000, 600);
        container.setLayoutX(140);
        container.setLayoutY(60);
        container.setStyle("""
                -fx-background-color: #EFE4D6;
                -fx-background-radius: 26;
                -fx-effect: dropshadow(gaussian, rgba(80,45,35,0.18), 28, 0.22, 0, 8);
                """);

        VBox left = new VBox(18);
        left.setPrefWidth(500);
        left.setAlignment(Pos.CENTER);
        left.setPadding(new Insets(45));
        left.setStyle("""
                -fx-background-color: #F9F3EA;
                -fx-background-radius: 26 0 0 26;
                """);

        Label hotelName = smallHotelLabel("GRAND BUDAPEST HOTEL");
        Label title = titleLabel("WELCOME BACK");
        Label subtitle = label("Sign in to continue your reservation experience", 14, MUTED, false);

        TextField usernameField = smallInput("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(350);
        passwordField.setStyle(inputStyle());

        Label message = label("", 13, ROSE, false);

        Button loginBtn = mainButton("SIGN IN", 210, 45);
        loginBtn.setOnAction(e -> {
            try {
                currentUser = Authentication.login(usernameField.getText().trim(), passwordField.getText().trim());

                if (currentUser instanceof Admin admin) {
                    showAdminDashboard(admin);
                } else if (currentUser instanceof Receptionist receptionist) {
                    showReceptionistDashboard(receptionist);
                } else if (currentUser instanceof Guest guest) {
                    showGuestDashboard(guest);
                }

            } catch (InvalidCredentialsException ex) {
                message.setText("Login failed: " + ex.getMessage());
            }
        });

        Button registerBtn = outlineButton("REGISTER AS GUEST", 210, 42);
        registerBtn.setOnAction(e -> showGuestRegisterScreen());

        Label demo = label("Demo users: Admin / Admin@123   |   Hassan / Hassan123   |   Manar / Manar2002",
                11, MUTED, false);
        demo.setWrapText(true);
        demo.setMaxWidth(360);

        left.getChildren().addAll(hotelName, title, subtitle, usernameField, passwordField, loginBtn, registerBtn, message, demo);

        VBox right = hotelRightPanel();

        container.getChildren().addAll(left, right);
        root.getChildren().add(container);

        stage.setScene(new Scene(root, WIDTH, HEIGHT));
    }

    private VBox hotelRightPanel() {
        VBox right = new VBox(16);
        right.setPrefWidth(500);
        right.setAlignment(Pos.CENTER_LEFT);
        right.setPadding(new Insets(65));
        right.setStyle("""
                -fx-background-color: #EFE4D6;
                -fx-background-radius: 0 26 26 0;
                """);

        Label small = smallHotelLabel("EXPERIENCE EGYPTIAN HOTELS LIKE NEVER BEFORE");

        Label title = label("RESERVE YOUR\nSUITE", 42, TEXT, false);
        title.setFont(Font.font("Georgia", FontWeight.NORMAL, 42));

        Label location = label("Cairo, Arab Republic of Egypt", 13, BURGUNDY, false);
        location.setFont(Font.font("Georgia", 13));

        Separator line = new Separator();
        line.setMaxWidth(270);
        line.setStyle("-fx-background-color: #C9AA7C;");

        Label slogan = label("Luxury reservations made simple, elegant, and personal.", 15, MUTED, false);
        slogan.setWrapText(true);
        slogan.setMaxWidth(300);

        Button book = mainButton("BOOK NOW", 130, 38);
        book.setOnAction(e -> alert("Welcome", "Please sign in or register to reserve a suite."));

        right.getChildren().addAll(small, title, location, line, slogan, book);
        return right;
    }

    private void showGuestRegisterScreen() {
        VBox page = basePage("Guest Registration");

        TextField username = smallInput("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password: min 8 chars, uppercase, digit");
        password.setMaxWidth(360);
        password.setStyle(inputStyle());

        TextField dob = smallInput("Date of Birth YYYY-MM-DD");
        TextField balance = smallInput("Balance");
        TextField address = smallInput("Address");

        ComboBox<Gender> gender = new ComboBox<>(FXCollections.observableArrayList(Gender.values()));
        gender.setPromptText("Gender");
        gender.setMaxWidth(360);

        TextField prefs = smallInput("Room Preferences");

        Label msg = label("", 13, ROSE, false);

        Button register = mainButton("CREATE ACCOUNT", 240, 45);
        register.setOnAction(e -> {
            try {
                Authentication.validatePasswordStrength(password.getText().trim());

                Guest guest = new Guest(
                        username.getText().trim(),
                        password.getText().trim(),
                        LocalDate.parse(dob.getText().trim()),
                        Double.parseDouble(balance.getText().trim()),
                        address.getText().trim(),
                        gender.getValue(),
                        prefs.getText().trim()
                );

                guest.register();
                alert("Success", "Account created successfully. You can now log in.");
                showLoginScreen();

            } catch (InvalidCredentialsException ex) {
                msg.setText(ex.getMessage());
            } catch (DateTimeParseException ex) {
                msg.setText("Invalid date format. Use YYYY-MM-DD.");
            } catch (NumberFormatException ex) {
                msg.setText("Balance must be a number.");
            } catch (Exception ex) {
                msg.setText("Please fill all fields correctly.");
            }
        });

        Button back = outlineButton("BACK", 180, 42);
        back.setOnAction(e -> showLoginScreen());

        page.getChildren().addAll(username, password, dob, balance, address, gender, prefs, register, back, msg);
        stage.setScene(new Scene(wrapCenter(page), WIDTH, HEIGHT));
    }

    private void showGuestDashboard(Guest guest) {
        BorderPane root = dashboardBase("Guest Dashboard", guest.getUsername());

        VBox menu = sideMenu();

        Button profile = menuButton("View Profile");
        Button rooms = menuButton("Browse Rooms");
        Button reserve = menuButton("Make Reservation");
        Button myReservations = menuButton("My Reservations");
        Button deposit = menuButton("Pay Deposit");
        Button cancel = menuButton("Cancel Reservation");
        Button time = menuButton("Advance Time");
        Button chatBtn = menuButton("Live Chat");
        Button logout = menuButton("Logout");

        menu.getChildren().addAll(profile, rooms, reserve, myReservations, deposit, cancel, time, chatBtn, logout);
        root.setLeft(menu);

        VBox content = contentBox();
        root.setCenter(content);

        profile.setOnAction(e -> content.getChildren().setAll(
                sectionTitle("My Profile"),
                info("Username: " + guest.getUsername()),
                info("DOB: " + guest.getDateOfBirth()),
                info("Balance: $" + money(guest.getBalance())),
                info("Address: " + guest.getAddress()),
                info("Gender: " + guest.getGender()),
                info("Preferences: " + guest.getRoomPreferences())
        ));

        rooms.setOnAction(e -> showRoomBrowser(content, guest));

        reserve.setOnAction(e -> {
            selectedRoomForReservation = null;
            showMakeReservation(content, guest);
        });

        myReservations.setOnAction(e -> showGuestReservations(content, guest));
        deposit.setOnAction(e -> showPayDeposit(content, guest));
        cancel.setOnAction(e -> showCancelReservation(content, guest));
        time.setOnAction(e -> showAdvanceTime(content));
        chatBtn.setOnAction(e -> showChat(content));
        logout.setOnAction(e -> showLoginScreen());

        profile.fire();
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
    }

    private void showAdminDashboard(Admin admin) {
        BorderPane root = dashboardBase("Admin Dashboard", admin.getUsername());

        VBox menu = sideMenu();

        Button guests = menuButton("View Guests");
        Button rooms = menuButton("View Rooms");
        Button reservations = menuButton("View Reservations");
        Button roomTypes = menuButton("Manage Room Types");
        Button amenities = menuButton("Manage Amenities");
        Button addRoom = menuButton("Add Room");
        Button addReceptionist = menuButton("Add Receptionist");
        Button chatBtn = menuButton("Live Chat");
        Button logout = menuButton("Logout");

        menu.getChildren().addAll(guests, rooms, reservations, roomTypes, amenities, addRoom, addReceptionist, chatBtn, logout);
        root.setLeft(menu);

        VBox content = contentBox();
        root.setCenter(content);

        guests.setOnAction(e -> showList(content, "Guests", Database.getGuests()));
        rooms.setOnAction(e -> showList(content, "Rooms", Database.getRooms()));
        reservations.setOnAction(e -> showList(content, "Reservations", Database.getReservations()));
        roomTypes.setOnAction(e -> showManageRoomTypes(content));
        amenities.setOnAction(e -> showManageAmenities(content));
        addRoom.setOnAction(e -> showAddRoom(content));
        addReceptionist.setOnAction(e -> showAddReceptionist(content, admin));
        chatBtn.setOnAction(e -> showChat(content));
        logout.setOnAction(e -> showLoginScreen());

        guests.fire();
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
    }

    private void showReceptionistDashboard(Receptionist rec) {
        BorderPane root = dashboardBase("Receptionist Dashboard", rec.getUsername());

        VBox menu = sideMenu();

        Button today = menuButton("Today's Reservations");
        Button checkIn = menuButton("Check In");
        Button checkOut = menuButton("Check Out");
        Button allReservations = menuButton("All Reservations");
        Button guests = menuButton("Guests");
        Button rooms = menuButton("Rooms");
        Button time = menuButton("Advance Time");
        Button chatBtn = menuButton("Live Chat");
        Button logout = menuButton("Logout");

        menu.getChildren().addAll(today, checkIn, checkOut, allReservations, guests, rooms, time, chatBtn, logout);
        root.setLeft(menu);

        VBox content = contentBox();
        root.setCenter(content);

        today.setOnAction(e -> showList(content, "Today's Reservations",
                Database.getReservations().stream()
                        .filter(r -> r.getCheckInDate().isEqual(SystemTime.getToday()))
                        .toList()));

        checkIn.setOnAction(e -> showCheckIn(content));
        checkOut.setOnAction(e -> showCheckOut(content));
        allReservations.setOnAction(e -> showList(content, "All Reservations", Database.getReservations()));
        guests.setOnAction(e -> showList(content, "Guests", Database.getGuests()));
        rooms.setOnAction(e -> showList(content, "Rooms", Database.getRooms()));
        time.setOnAction(e -> showAdvanceTime(content));
        chatBtn.setOnAction(e -> showChat(content));
        logout.setOnAction(e -> showLoginScreen());

        today.fire();
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
    }

    private void showRoomBrowser(VBox content, Guest guest) {
        content.getChildren().clear();

        Label title = sectionTitle("Browse Available Rooms");

        ComboBox<RoomType> typeFilter = new ComboBox<>(FXCollections.observableArrayList(Database.getRoomTypes()));
        typeFilter.setPromptText("Filter by Room Type");
        typeFilter.setMaxWidth(260);

        TextField maxPrice = smallInput("Max price per night");
        maxPrice.setMaxWidth(220);

        Button filterBtn = mainButton("FILTER", 120, 40);

        HBox filters = new HBox(12, typeFilter, maxPrice, filterBtn);
        filters.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setPrefHeight(520);

        VBox roomCards = new VBox(15);
        roomCards.setPadding(new Insets(10));

        Runnable loadRooms = () -> {
            roomCards.getChildren().clear();

            List<Room> rooms = Database.getRooms().stream()
                    .filter(r -> ReservationService.isRoomAvailable(r, SystemTime.getToday(), SystemTime.getToday().plusDays(1)))
                    .filter(r -> typeFilter.getValue() == null || r.getRoomType().equals(typeFilter.getValue()))
                    .filter(r -> {
                        if (maxPrice.getText().trim().isEmpty()) return true;
                        try {
                            return r.getRoomType().getPricePerNight() <= Double.parseDouble(maxPrice.getText().trim());
                        } catch (Exception e) {
                            return true;
                        }
                    })
                    .toList();

            if (rooms.isEmpty()) {
                roomCards.getChildren().add(info("No available rooms found."));
                return;
            }

            for (Room room : rooms) {
                roomCards.getChildren().add(roomCard(room, guest, content));
            }
        };

        filterBtn.setOnAction(e -> loadRooms.run());
        scroll.setContent(roomCards);

        content.getChildren().addAll(title, filters, scroll);
        loadRooms.run();
    }

    private HBox roomCard(Room room, Guest guest, VBox content) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.setStyle("""
                -fx-background-color: #FFFFFF;
                -fx-background-radius: 14;
                -fx-border-color: #C9AA7C;
                -fx-border-radius: 14;
                -fx-effect: dropshadow(gaussian, rgba(80,45,35,0.10), 12, 0.2, 0, 4);
                """);

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(170, 120);
        imageBox.setStyle("""
                -fx-background-color: linear-gradient(to bottom right, #EFE4D6, #E6A4B4);
                -fx-background-radius: 12;
                -fx-border-color: #C9AA7C;
                -fx-border-radius: 12;
                """);

        Label imageText = label("ROOM\n" + room.getRoomNumber(), 22, BURGUNDY, true);
        imageText.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        VBox details = new VBox(7);
        details.setPrefWidth(560);

        Label roomTitle = label("Room " + room.getRoomNumber() + " — " + room.getRoomType().getName(), 22, TEXT, true);
        roomTitle.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));

        Label price = label("Price per night: $" + money(room.getRoomType().getPricePerNight()), 14, BURGUNDY, true);
        Label capacity = label("Capacity: " + room.getRoomType().getCapacity() + " guests", 14, TEXT, false);

        Label amenities = label("Amenities: " + room.getAmenities(), 13, MUTED, false);
        amenities.setWrapText(true);

        details.getChildren().addAll(roomTitle, price, capacity, amenities);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);

        Button detailsBtn = outlineButton("DETAILS", 140, 38);
        Button reserveBtn = mainButton("RESERVE", 140, 38);

        detailsBtn.setOnAction(e -> showRoomDetails(content, guest, room));

        reserveBtn.setOnAction(e -> {
            selectedRoomForReservation = room;
            showMakeReservation(content, guest);
        });

        actions.getChildren().addAll(detailsBtn, reserveBtn);

        card.getChildren().addAll(imageBox, details, actions);
        return card;
    }

    private void showRoomDetails(VBox content, Guest guest, Room room) {
        content.getChildren().clear();

        Button back = outlineButton("BACK TO ROOMS", 170, 40);
        back.setOnAction(e -> showRoomBrowser(content, guest));

        Label title = sectionTitle("Room " + room.getRoomNumber());

        StackPane imageBox = new StackPane();
        imageBox.setPrefHeight(230);
        imageBox.setMaxWidth(720);
        imageBox.setStyle("""
                -fx-background-color: linear-gradient(to bottom right, #EFE4D6, #E6A4B4);
                -fx-background-radius: 18;
                -fx-border-color: #C9AA7C;
                -fx-border-radius: 18;
                """);

        Label imageText = label(room.getRoomType().getName() + "\nLuxury Suite Preview", 30, BURGUNDY, true);
        imageText.setFont(Font.font("Georgia", FontWeight.NORMAL, 30));
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        Label details = info(
                "Room Number: " + room.getRoomNumber()
                        + "\nRoom Type: " + room.getRoomType().getName()
                        + "\nCapacity: " + room.getRoomType().getCapacity()
                        + "\nPrice per night: $" + money(room.getRoomType().getPricePerNight())
                        + "\nAmenities: " + room.getAmenities()
        );

        Button reserve = mainButton("RESERVE THIS ROOM", 220, 45);
        reserve.setOnAction(e -> {
            selectedRoomForReservation = room;
            showMakeReservation(content, guest);
        });

        content.getChildren().addAll(back, title, imageBox, details, reserve);
    }

    private void showMakeReservation(VBox content, Guest guest) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Make Reservation"));

        ComboBox<RoomType> typeBox = new ComboBox<>(FXCollections.observableArrayList(Database.getRoomTypes()));
        typeBox.setPromptText("Select Room Type");
        typeBox.setMaxWidth(360);

        TextField guests = smallInput("Number of Guests");
        TextField checkIn = smallInput("Check-in YYYY-MM-DD");
        TextField checkOut = smallInput("Check-out YYYY-MM-DD");

        CheckBox gym = new CheckBox("Add Gym Pass ($200)");
        gym.setTextFill(Color.web(TEXT));

        ComboBox<Room> roomBox = new ComboBox<>();
        roomBox.setPromptText("Search first, then choose room");
        roomBox.setMaxWidth(360);

        Label msg = label("", 13, ROSE, false);

        if (selectedRoomForReservation != null) {
            typeBox.setValue(selectedRoomForReservation.getRoomType());
            roomBox.setItems(FXCollections.observableArrayList(selectedRoomForReservation));
            roomBox.setValue(selectedRoomForReservation);
            msg.setText("Selected Room " + selectedRoomForReservation.getRoomNumber() + " automatically.");
            msg.setTextFill(Color.web(BURGUNDY));
        }

        Button search = mainButton("SEARCH ROOMS", 180, 42);
        search.setOnAction(e -> {
            try {
                if (typeBox.getValue() == null) {
                    msg.setText("Please select a room type.");
                    msg.setTextFill(Color.web(ROSE));
                    return;
                }

                int numGuests = Integer.parseInt(guests.getText().trim());
                LocalDate in = LocalDate.parse(checkIn.getText().trim());
                LocalDate out = LocalDate.parse(checkOut.getText().trim());

                if (!ReservationService.isDateRangeValid(in, out)) {
                    msg.setText("Invalid dates. Check-out must be after check-in.");
                    msg.setTextFill(Color.web(ROSE));
                    return;
                }

                List<Room> available = ReservationService.searchAvailableRooms(in, out, typeBox.getValue(), numGuests);
                roomBox.setItems(FXCollections.observableArrayList(available));

                if (selectedRoomForReservation != null && available.contains(selectedRoomForReservation)) {
                    roomBox.setValue(selectedRoomForReservation);
                }

                msg.setText(available.isEmpty() ? "No rooms available." : available.size() + " rooms found.");
                msg.setTextFill(available.isEmpty() ? Color.web(ROSE) : Color.web(BURGUNDY));

            } catch (NumberFormatException ex) {
                msg.setText("Number of guests must be a number.");
                msg.setTextFill(Color.web(ROSE));
            } catch (DateTimeParseException ex) {
                msg.setText("Use date format YYYY-MM-DD.");
                msg.setTextFill(Color.web(ROSE));
            } catch (Exception ex) {
                msg.setText("Enter valid room type, guests, and dates.");
                msg.setTextFill(Color.web(ROSE));
            }
        });

        Button create = mainButton("CREATE RESERVATION", 220, 42);
        create.setOnAction(e -> {
            try {
                if (roomBox.getValue() == null) {
                    msg.setText("Please choose a room first.");
                    msg.setTextFill(Color.web(ROSE));
                    return;
                }

                LocalDate in = LocalDate.parse(checkIn.getText().trim());
                LocalDate out = LocalDate.parse(checkOut.getText().trim());

                if (!ReservationService.isDateRangeValid(in, out)) {
                    msg.setText("Invalid date range.");
                    msg.setTextFill(Color.web(ROSE));
                    return;
                }
if (ReservationService.hasOverlappingReservation(roomBox.getValue(), in, out)) {
    msg.setText("This room is already reserved during the selected dates.");
    msg.setTextFill(Color.web(ROSE));
    return;
}
                Reservation res = ReservationService.createReservation(guest, roomBox.getValue(), in, out, gym.isSelected());

                selectedRoomForReservation = null;

                alert("Reservation Created",
                        "ID: " + res.getReservationId()
                                + "\nRoom: " + res.getRoom().getRoomNumber()
                                + "\nStatus: " + res.getStatus()
                                + "\nTotal: $" + money(res.getTotalPrice())
                                + "\nDeposit: $" + money(ReservationService.getDepositAmount(res)));

                showGuestReservations(content, guest);

            } catch (Exception ex) {
                msg.setText("Reservation failed: " + ex.getMessage());
                msg.setTextFill(Color.web(ROSE));
            }
        });

        content.getChildren().addAll(typeBox, guests, checkIn, checkOut, gym, search, roomBox, create, msg);
    }

    private void showGuestReservations(VBox content, Guest guest) {
        List<Reservation> list = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                .toList();

        showList(content, "My Reservations", list);
    }

    private void showPayDeposit(VBox content, Guest guest) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Pay Deposit"));

        ComboBox<Reservation> pending = new ComboBox<>(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                        .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                        .toList()
        ));
        pending.setPromptText("Select Pending Reservation");
        pending.setMaxWidth(420);

        Label details = info("Choose a reservation.");
        Button pay = mainButton("PAY DEPOSIT", 180, 42);

        pending.setOnAction(e -> {
            Reservation r = pending.getValue();
            if (r != null) {
                details.setText("Deposit: $" + money(ReservationService.getDepositAmount(r))
                        + " | Your balance: $" + money(guest.getBalance()));
            }
        });

        pay.setOnAction(e -> {
            try {
                if (pending.getValue() == null) {
                    alert("Error", "Please select a reservation first.");
                    return;
                }

                ReservationService.payDeposit(pending.getValue(), guest);
                alert("Success", "Deposit paid. Reservation confirmed.");
                showGuestReservations(content, guest);

            } catch (Exception ex) {
                alert("Payment Failed", ex.getMessage());
            }
        });

        content.getChildren().addAll(pending, details, pay);
    }

    private void showCancelReservation(VBox content, Guest guest) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Cancel Reservation"));

        ComboBox<Reservation> box = new ComboBox<>(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                        .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                        .toList()
        ));
        box.setPromptText("Select Reservation");
        box.setMaxWidth(420);

        Button cancel = mainButton("CANCEL RESERVATION", 220, 42);
        cancel.setOnAction(e -> {
            try {
                if (box.getValue() == null) {
                    alert("Error", "Please select a reservation first.");
                    return;
                }

                ReservationService.cancelReservation(box.getValue().getReservationId());
                alert("Cancelled", "Reservation cancelled successfully.");
                showGuestReservations(content, guest);

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(box, cancel);
    }

    private void showManageRoomTypes(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Manage Room Types"));

        TextField name = smallInput("Room Type Name");
        TextField price = smallInput("Price Per Night");
        TextField cap = smallInput("Capacity");

        Button add = mainButton("ADD ROOM TYPE", 190, 42);
        add.setOnAction(e -> {
            try {
                CatalogService.createRoomType(name.getText().trim(),
                        Double.parseDouble(price.getText().trim()),
                        Integer.parseInt(cap.getText().trim()));

                alert("Success", "Room type added.");
                showManageRoomTypes(content);

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(name, price, cap, add);
        addListView(content, Database.getRoomTypes());
    }

    private void showManageAmenities(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Manage Amenities"));

        TextField name = smallInput("Amenity Name");
        TextField price = smallInput("Price");

        Button add = mainButton("ADD AMENITY", 180, 42);
        add.setOnAction(e -> {
            try {
                CatalogService.createAmenity(name.getText().trim(), Double.parseDouble(price.getText().trim()));
                alert("Success", "Amenity added.");
                showManageAmenities(content);

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(name, price, add);
        addListView(content, Database.getAmenities());
    }

    private void showAddRoom(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Add Room"));

        TextField roomNumber = smallInput("Room Number");

        ComboBox<RoomType> type = new ComboBox<>(FXCollections.observableArrayList(Database.getRoomTypes()));
        type.setPromptText("Room Type");
        type.setMaxWidth(360);

        Button add = mainButton("ADD ROOM", 160, 42);
        add.setOnAction(e -> {
            try {
                if (type.getValue() == null) {
                    alert("Error", "Please select a room type.");
                    return;
                }

                CatalogService.createRoom(roomNumber.getText().trim(), type.getValue());
                alert("Success", "Room added.");
                showList(content, "Rooms", Database.getRooms());

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(roomNumber, type, add);
    }

    private void showAddReceptionist(VBox content, Admin admin) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Register Receptionist"));

        TextField username = smallInput("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setMaxWidth(360);
        password.setStyle(inputStyle());

        TextField dob = smallInput("DOB YYYY-MM-DD");
        TextField hours = smallInput("Working Hours");

        Button add = mainButton("REGISTER", 170, 42);
        add.setOnAction(e -> {
            try {
                admin.registerStaff(username.getText().trim(),
                        password.getText().trim(),
                        LocalDate.parse(dob.getText().trim()),
                        Integer.parseInt(hours.getText().trim()),
                        Role.RECEPTIONIST);

                alert("Success", "Receptionist registered.");

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(username, password, dob, hours, add);
    }

    private void showCheckIn(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Check In Guest"));

        ComboBox<Reservation> box = new ComboBox<>(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                        .filter(r -> r.getCheckInDate().isEqual(SystemTime.getToday()))
                        .toList()
        ));
        box.setPromptText("Select Reservation");
        box.setMaxWidth(420);

        Button check = mainButton("CHECK IN", 170, 42);
        check.setOnAction(e -> {
            try {
                if (box.getValue() == null) {
                    alert("Error", "Please select a reservation first.");
                    return;
                }

                Reservation r = box.getValue();
                ReservationService.checkInGuest(r, r.getGuest());
                alert("Success", "Guest checked in.");
                showList(content, "Reservations", Database.getReservations());

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(box, check);
    }

    private void showCheckOut(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Check Out Guest"));

        ComboBox<Reservation> box = new ComboBox<>(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getStatus() == ReservationStatus.ONGOING)
                        .toList()
        ));
        box.setPromptText("Select Reservation");
        box.setMaxWidth(420);

        ComboBox<PaymentMethod> payment = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        payment.setPromptText("Payment Method");
        payment.setMaxWidth(360);

        Button check = mainButton("CHECK OUT", 170, 42);
        check.setOnAction(e -> {
            try {
                if (box.getValue() == null || payment.getValue() == null) {
                    alert("Error", "Please select reservation and payment method.");
                    return;
                }

                ReservationService.checkOutGuest(box.getValue(), payment.getValue());
                alert("Success", "Guest checked out.");
                showList(content, "Reservations", Database.getReservations());

            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        content.getChildren().addAll(box, payment, check);
    }

    private void showAdvanceTime(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle("Advance System Time"));

        Label today = info("Current date: " + SystemTime.getDate());
        TextField days = smallInput("Days to advance");

        Button advance = mainButton("ADVANCE TIME", 190, 42);
        advance.setOnAction(e -> {
            try {
                int value = Integer.parseInt(days.getText().trim());

                if (value <= 0) {
                    alert("Error", "Enter a positive number.");
                    return;
                }

                SystemTime.advanceDays(value);
                ReservationService.cancelOverdueReservations();

                today.setText("Current date: " + SystemTime.getDate());
                alert("Success", "System date advanced.");

            } catch (Exception ex) {
                alert("Error", "Enter a valid positive number.");
            }
        });

        content.getChildren().addAll(today, days, advance);
    }

    private void showChat(VBox content) {
        content.getChildren().clear();

        Label title = sectionTitle("Live Reception Chat");

        VBox chatCard = new VBox(14);
        chatCard.setPadding(new Insets(18));
        chatCard.setStyle("""
                -fx-background-color: #FFFFFF;
                -fx-background-radius: 18;
                -fx-border-color: #C9AA7C;
                -fx-border-radius: 18;
                -fx-effect: dropshadow(gaussian, rgba(80,45,35,0.10), 16, 0.25, 0, 5);
                """);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(46, 46);
        avatar.setStyle("""
                -fx-background-color: #C74261;
                -fx-background-radius: 50;
                """);

        Label avatarText = label(currentUser.getUsername().substring(0, 1).toUpperCase(), 20, WHITE, true);
        avatar.getChildren().add(avatarText);

        VBox headerText = new VBox(2);
        Label chatName = label("Hotel Live Chat", 18, TEXT, true);
        chatName.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        Label chatStatus = label("Connected to reception desk", 12, MUTED, false);
        headerText.getChildren().addAll(chatName, chatStatus);

        header.getChildren().addAll(avatar, headerText);

        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(390);
        chatArea.setWrapText(true);
        chatArea.setStyle("""
                -fx-control-inner-background: #F9F3EA;
                -fx-text-fill: #2B2421;
                -fx-font-size: 14px;
                -fx-background-radius: 14;
                -fx-border-color: #EFE4D6;
                -fx-border-radius: 14;
                -fx-padding: 10;
                """);

        TextField input = new TextField();
        input.setPromptText("Write your message...");
        input.setStyle(inputStyle());
        HBox.setHgrow(input, Priority.ALWAYS);

        Button send = mainButton("SEND", 110, 42);

        HBox bottom = new HBox(10, input, send);
        bottom.setAlignment(Pos.CENTER);

        try {
            ChatClient client = new ChatClient("localhost", 5000, currentUser.getUsername());
            client.listen(chatArea);

            chatArea.appendText("System: You joined the hotel live chat.\n");

            send.setOnAction(e -> {
                String msg = input.getText().trim();

                if (!msg.isEmpty()) {
                    client.send(msg);
                    input.clear();
                }
            });

            input.setOnAction(e -> send.fire());

        } catch (Exception e) {
            chatStatus.setText("Offline — start ChatServer.java first");
            chatArea.setText("Cannot connect to server.\nMake sure ChatServer.java is running first.");
        }

        chatCard.getChildren().addAll(header, chatArea, bottom);
        content.getChildren().addAll(title, chatCard);
    }

    private void showList(VBox content, String title, List<?> list) {
        content.getChildren().clear();
        content.getChildren().add(sectionTitle(title));
        addListView(content, list);
    }

    private void addListView(VBox content, List<?> list) {
        ListView<String> view = new ListView<>();
        view.setPrefHeight(470);
        view.setItems(FXCollections.observableArrayList(list.stream().map(Object::toString).toList()));
        view.setStyle("""
                -fx-font-size: 14px;
                -fx-background-color: #FFFFFF;
                -fx-control-inner-background: #FFFFFF;
                -fx-text-fill: #2B2421;
                -fx-border-color: #C9AA7C;
                -fx-border-radius: 8;
                """);
        content.getChildren().add(view);
    }

    private BorderPane dashboardBase(String title, String username) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        HBox top = new HBox(22);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(18, 30, 18, 30));
        top.setStyle("""
                -fx-background-color: #F9F3EA;
                -fx-border-color: transparent transparent #C9AA7C transparent;
                -fx-border-width: 0 0 1 0;
                """);

        Label titleLabel = titleLabel(title);

        Label userLabel = label("Logged in as: " + username + "   |   Date: " + SystemTime.getDate(),
                14, BURGUNDY, false);

        top.getChildren().addAll(titleLabel, userLabel);
        root.setTop(top);

        return root;
    }

    private VBox sideMenu() {
        VBox menu = new VBox(12);
        menu.setPadding(new Insets(25));
        menu.setPrefWidth(260);
        menu.setStyle("""
                -fx-background-color: #EFE4D6;
                -fx-border-color: transparent #C9AA7C transparent transparent;
                -fx-border-width: 0 1 0 0;
                """);
        return menu;
    }

    private VBox contentBox() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));
        box.setStyle("""
                -fx-background-color: #F9F3EA;
                -fx-background-radius: 18;
                """);
        return box;
    }

    private VBox basePage(String title) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setMaxWidth(470);
        box.setStyle("""
                -fx-background-color: #F9F3EA;
                -fx-background-radius: 24;
                -fx-effect: dropshadow(gaussian, rgba(80,45,35,0.16), 24, 0.25, 0, 7);
                """);

        box.getChildren().add(smallHotelLabel("GRAND BUDAPEST HOTEL"));
        box.getChildren().add(sectionTitle(title));

        return box;
    }

    private StackPane wrapCenter(VBox content) {
        StackPane pane = new StackPane(content);
        pane.setStyle("-fx-background-color: " + BG + ";");
        return pane;
    }

    private TextField smallInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(360);
        field.setStyle(inputStyle());
        return field;
    }

    private Button menuButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(40);
        b.setAlignment(Pos.CENTER_LEFT);

        String normal = """
                -fx-background-color: transparent;
                -fx-text-fill: #8F1D3F;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-padding: 0 0 0 15;
                -fx-cursor: hand;
                """;

        String hover = """
                -fx-background-color: #E6A4B4;
                -fx-text-fill: #FFFFFF;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-padding: 0 0 0 15;
                -fx-cursor: hand;
                """;

        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));

        return b;
    }

    private Button mainButton(String text, int width, int height) {
        Button b = new Button(text);
        b.setPrefSize(width, height);

        String normal = """
                -fx-background-color: #C74261;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 7;
                -fx-font-size: 13px;
                -fx-cursor: hand;
                """;

        String hover = """
                -fx-background-color: #8F1D3F;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 7;
                -fx-font-size: 13px;
                -fx-cursor: hand;
                """;

        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));

        return b;
    }

    private Button outlineButton(String text, int width, int height) {
        Button b = new Button(text);
        b.setPrefSize(width, height);
        b.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: #C74261;
                -fx-border-radius: 7;
                -fx-text-fill: #C74261;
                -fx-font-weight: bold;
                -fx-cursor: hand;
                """);
        return b;
    }

    private String inputStyle() {
        return """
                -fx-background-color: #FFFFFF;
                -fx-background-radius: 8;
                -fx-border-color: #C9AA7C;
                -fx-border-radius: 8;
                -fx-padding: 10;
                -fx-font-size: 14px;
                -fx-text-fill: #2B2421;
                -fx-prompt-text-fill: #8A726B;
                """;
    }

    private Label sectionTitle(String text) {
        Label l = label(text, 29, TEXT, true);
        l.setFont(Font.font("Georgia", FontWeight.NORMAL, 29));
        return l;
    }

    private Label titleLabel(String text) {
        Label l = label(text, 34, TEXT, true);
        l.setFont(Font.font("Georgia", FontWeight.NORMAL, 34));
        return l;
    }

    private Label smallHotelLabel(String text) {
        Label l = label(text, 13, BURGUNDY, false);
        l.setFont(Font.font("Georgia", 13));
        return l;
    }

    private Label info(String text) {
        return label(text, 15, TEXT, false);
    }

    private Label label(String text, int size, String color, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private String money(double value) {
        return String.format("%.2f", value);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }


    public static void main(String[] args) {
    

    Database.getGuests().clear();
    Database.getStaffMembers().clear();

    ArrayList<User> loadedUsers = UserDatabase.loadUsersFromDatabase();

    for (User user : loadedUsers) {
        Database.addUser(user);
    }
DatabaseSync.syncDefaultDataToMySQL();
    System.out.println("Users loaded from MySQL: " + loadedUsers.size());
    System.out.println("Guests: " + Database.getGuests().size());
    System.out.println("Staff: " + Database.getStaffMembers().size());

    launch(args);
}
}
