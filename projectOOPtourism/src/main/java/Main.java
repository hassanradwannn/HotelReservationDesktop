import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage stage;
    private User currentUser;
    private Room selectedRoomForReservation;
    private VBox currentContentArea;

    private final int WIDTH = 1280;
    private final int HEIGHT = 720;

    private static final String BG = "#F7EFE5";
    private static final String CARD = "#EFE4D6";
    private static final String CARD_LIGHT = "#F9F3EA";
    private static final String ROSE = "#C74261";
    private static final String ROSE_SOFT = "#E6A4B4";
    private static final String BURGUNDY = "#8F1D3F";
    private static final String GOLD = "#C9AA7C";
    private static final String TEXTDARK = "#2B2421";
    private static final String TEXT = "#FFFFFF";
    private static final String MUTED = "#8A726B";
    private static final String WHITE = "#FFFFFF";

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Grand Budapest Hotel Reservation System");
        showLoginScreen();
        stage.show();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            AnchorPane root = loader.load();
            
            // Pass a reference of Main to the controller so it can navigate
            LoginController controller = loader.getController();
            controller.setMainApp(this);
            
            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showGuestRegisterScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Register.fxml"));
            AnchorPane root = loader.load();
            
            RegisterController controller = loader.getController();
            controller.setMainApp(this);
            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchDashboardContent(VBox contentArea, String fxmlFile, Object data) {
        this.currentContentArea = contentArea;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Node node = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof DashboardContentController contentController) {
                contentController.initData(this, data);
            }
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            System.out.println("Could not load FXML: " + fxmlFile);
            e.printStackTrace();
        }
    }

    public VBox getCurrentContentArea() {
        return currentContentArea;
    }

    public Room getSelectedRoomForReservation() {
        return selectedRoomForReservation;
    }

    public void setSelectedRoomForReservation(Room room) {
        this.selectedRoomForReservation = room;
    }

    public void showGuestDashboard(Guest guest) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            BorderPane root = loader.load();
            
            DashboardController controller = loader.getController();
            controller.setMainApp(this);
            controller.setTitle("Guest Dashboard");
            controller.setUserInfo("Logged in as: " + guest.getUsername() + "   |   Date: " + SystemTime.getDate());

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();

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

        profile.setOnAction(e -> switchDashboardContent(content, "/GuestProfile.fxml", guest));

        rooms.setOnAction(e -> switchDashboardContent(content, "/RoomBrowser.fxml", guest));
        reserve.setOnAction(e -> switchDashboardContent(content, "/MakeReservation.fxml", guest));

        myReservations.setOnAction(e ->  switchDashboardContent(content, "/GuestReservations.fxml", guest));
        deposit.setOnAction(e -> switchDashboardContent(content, "/PayDeposit.fxml", guest));
        cancel.setOnAction(e -> switchDashboardContent(content, "/CancelReservation.fxml", guest));
        time.setOnAction(e -> switchDashboardContent(content, "/AdvanceTime.fxml", guest));
        chatBtn.setOnAction(e -> switchDashboardContent(content, "/LiveChat.fxml", guest));
        logout.setOnAction(e -> showLoginScreen());

        profile.fire();
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void showAdminDashboard(Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            BorderPane root = loader.load();
            
            DashboardController controller = loader.getController();
            controller.setMainApp(this);
            controller.setTitle("Admin Dashboard");
            controller.setUserInfo("Logged in as: " + admin.getUsername() + "   |   Date: " + SystemTime.getDate());

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();

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
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void showReceptionistDashboard(Receptionist rec) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            BorderPane root = loader.load();
            
            DashboardController controller = loader.getController();
            controller.setMainApp(this);
            controller.setTitle("Receptionist Dashboard");
            controller.setUserInfo("Logged in as: " + rec.getUsername() + "   |   Date: " + SystemTime.getDate());

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();

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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showRoomBrowser(VBox content, Guest guest) {
        content.getChildren().clear();

        Label title = sectionTitle("Browse Available Rooms");
        title.setStyle("-fx-text-fill: #000000;");
        

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

        Label imageText = label("ROOM\n" + room.getRoomNumber(), 22, TEXTDARK, true);
        imageText.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        VBox details = new VBox(7);
        details.setPrefWidth(560);

        Label roomTitle = label("Room " + room.getRoomNumber() + " — " + room.getRoomType().getName(), 22, TEXTDARK, true);
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
        TextField checkIn = smallInput("Check-in DD-MM-YYYY");
        TextField checkOut = smallInput("Check-out DD-MM-YYYY");

        CheckBox gym = new CheckBox("Add Gym Pass ($200)");
        gym.setTextFill(Color.web(TEXTDARK));

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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
                LocalDate in = LocalDate.parse(checkIn.getText().trim(), formatter);
                LocalDate out = LocalDate.parse(checkOut.getText().trim(), formatter);

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
                msg.setText("Use date format DD-MM-YYYY.");
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

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
                LocalDate in = LocalDate.parse(checkIn.getText().trim(), formatter);
                LocalDate out = LocalDate.parse(checkOut.getText().trim(), formatter);

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

                switchDashboardContent(content, "/GuestReservations.fxml", guest);

            } catch (Exception ex) {
                msg.setText("Reservation failed: " + ex.getMessage());
                msg.setTextFill(Color.web(ROSE));
            }
        });

        content.getChildren().addAll(typeBox, guests, checkIn, checkOut, gym, search, roomBox, create, msg);
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

        TextField dob = smallInput("DOB DD-MM-YYYY");
        TextField hours = smallInput("Working Hours");

        Button add = mainButton("REGISTER", 170, 42);
        add.setOnAction(e -> {
            try {
                admin.registerStaff(username.getText().trim(),
                        password.getText().trim(),
                        LocalDate.parse(dob.getText().trim(), DateTimeFormatter.ofPattern("d-M-yyyy")),
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
        Label chatName = label("Hotel Live Chat", 18, TEXTDARK, true);
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

    public TextField smallInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(360);
        field.setStyle(inputStyle());
        return field;
    }

    public Button menuButton(String text) {
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

    public Button mainButton(String text, int width, int height) {
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

    public Button outlineButton(String text, int width, int height) {
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

    public String inputStyle() {
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

    public Label sectionTitle(String text) {
        Label l = label(text, 29, TEXTDARK, true);
        l.setFont(Font.font("Georgia", FontWeight.NORMAL, 29));
        return l;
    }

    public Label titleLabel(String text) {
        Label l = label(text, 34, TEXTDARK, true);
        l.setFont(Font.font("Georgia", FontWeight.NORMAL, 34));
        return l;
    }

    public Label smallHotelLabel(String text) {
        Label l = label(text, 13, BURGUNDY, false);
        l.setFont(Font.font("Georgia", 13));
        return l;
    }

    public Label info(String text) {
        return label(text, 15, TEXTDARK, false);
    }

    public Label label(String text, int size, String color, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(color));
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    public String money(double value) {
        return String.format("%.2f", value);
    }

    public void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void main(String[] args) {
        // DatabaseSync.syncDefaultDataToMySQL();
        launch(args);
    }
}