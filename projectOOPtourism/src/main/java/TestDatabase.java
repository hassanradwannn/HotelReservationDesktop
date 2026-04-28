package DatabaseInitializer;

public class TestDatabase {
    public static void main(String[] args) {
        try {
            DatabaseConnection.getConnection();
            System.out.println("Connected successfully to hotel_db!");
        } catch (Exception e) {
            System.out.println("Connection failed.");
            e.printStackTrace();
        }
    }
}