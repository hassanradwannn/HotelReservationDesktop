import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private static final Scanner scanner = new Scanner(System.in);

    public static void show(Admin admin) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Admin Menu [" + admin.getUsername() + "] ---");
            System.out.println("1. View All Guests");
            System.out.println("2. View All Rooms");
            System.out.println("3. View All Reservations");
            System.out.println("4. Manage Rooms (CRUD)");
            System.out.println("5. Manage Room Types (CRUD)");
            System.out.println("6. Manage Amenities (CRUD)");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> admin.viewGuests();
                case "2" -> admin.viewRooms();
                case "3" -> admin.viewReservations();
                case "4" -> manageRooms(admin);
                case "5" -> manageRoomTypes(admin);
                case "6" -> manageAmenities(admin);
                case "0" -> {
                    System.out.println("Logged out.");
                    active = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void manageRooms(Admin admin) {
        System.out.println("\n--- Manage Rooms ---");
        System.out.println("1. Add Room");
        System.out.println("2. Update Room");
        System.out.println("3. Delete Room");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Room number: ");
                String num = scanner.nextLine().trim();

                System.out.println("Select Room Type:");
                List<RoomType> types = Database.getRoomTypes();
                for (int i = 0; i < types.size(); i++)
                    System.out.println((i + 1) + ". " + types.get(i).getName());
                System.out.print("Choice: ");
                int t;
                try {
                    t = Integer.parseInt(scanner.nextLine().trim()) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid.");
                    return;
                }

                Room newRoom = new Room(num, types.get(t));
                admin.createRoom(newRoom);
                System.out.println("Room " + num + " added.");
            }
            case "2" -> {
                System.out.print("Room number to update: ");
                String num = scanner.nextLine().trim();

                Room room = findRoom(num);
                if (room == null) {
                    System.out.println("Room not found.");
                    return;
                }

                System.out.print("New room number (or same): ");
                String newNum = scanner.nextLine().trim();

                System.out.println("Select new Room Type:");
                List<RoomType> types = Database.getRoomTypes();
                for (int i = 0; i < types.size(); i++)
                    System.out.println((i + 1) + ". " + types.get(i).getName());
                System.out.print("Choice: ");
                int t;
                try {
                    t = Integer.parseInt(scanner.nextLine().trim()) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid.");
                    return;
                }

                admin.updateRoom(room, newNum, types.get(t), room.isAvailable());
                System.out.println("Room updated.");
            }
            case "3" -> {
                System.out.print("Room number to delete: ");
                String num = scanner.nextLine().trim();

                Room room = findRoom(num);
                if (room == null) {
                    System.out.println("Room not found.");
                    return;
                }
                admin.deleteRoom(room);
                System.out.println("Room " + num + " deleted.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void manageRoomTypes(Admin admin) {
        System.out.println("\n--- Manage Room Types ---");
        System.out.println("1. Add Room Type");
        System.out.println("2. Update Room Type");
        System.out.println("3. Delete Room Type");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Price per night: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                System.out.print("Capacity: ");
                int cap;
                try {
                    cap = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid capacity.");
                    return;
                }

                admin.createRoomType(new RoomType(name, price, cap));
                System.out.println("Room type '" + name + "' added.");
            }
            case "2" -> {
                System.out.print("Current room type name: ");
                String name = scanner.nextLine().trim();
                RoomType rt = findRoomType(name);
                if (rt == null) {
                    System.out.println("Room type not found.");
                    return;
                }

                System.out.print("New name: ");
                String newName = scanner.nextLine().trim();
                System.out.print("New price per night: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                System.out.print("New capacity: ");
                int cap;
                try {
                    cap = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid capacity.");
                    return;
                }

                admin.updateRoomType(rt, newName, cap, price);
                System.out.println("Room type updated.");
            }
            case "3" -> {
                System.out.print("Room type name to delete: ");
                String name = scanner.nextLine().trim();
                RoomType rt = findRoomType(name);
                if (rt == null) {
                    System.out.println("Room type not found.");
                    return;
                }
                admin.deleteRoomType(rt);
                System.out.println("Room type '" + name + "' deleted.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void manageAmenities(Admin admin) {
        System.out.println("\n--- Manage Amenities ---");
        System.out.println("1. Add Amenity");
        System.out.println("2. Update Amenity");
        System.out.println("3. Delete Amenity");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Price: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                admin.createAmenity(name, price);
                System.out.println("Amenity '" + name + "' added.");
            }
            case "2" -> {
                System.out.print("Current amenity name: ");
                String name = scanner.nextLine().trim();
                Amenity a = findAmenity(name);
                if (a == null) {
                    System.out.println("Amenity not found.");
                    return;
                }
                System.out.print("New name: ");
                String newName = scanner.nextLine().trim();
                System.out.print("New price: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                admin.updateAmenity(a, newName, price);
                System.out.println("Amenity updated.");
            }
            case "3" -> {
                System.out.print("Amenity name to delete: ");
                String name = scanner.nextLine().trim();
                Amenity a = findAmenity(name);
                if (a == null) {
                    System.out.println("Amenity not found.");
                    return;
                }
                admin.deleteAmenity(a);
                System.out.println("Amenity '" + name + "' deleted.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static Room findRoom(String number) {
        return Database.getRooms().stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(number))
                .findFirst().orElse(null);
    }

    private static RoomType findRoomType(String name) {
        return Database.getRoomTypes().stream()
                .filter(rt -> rt.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private static Amenity findAmenity(String name) {
        return Database.getAmenities().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}