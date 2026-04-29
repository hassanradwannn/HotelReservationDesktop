import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class SystemTime {
    private static LocalDate today = LocalDate.now();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static LocalDate getToday() {
        return today;
    }

    public static String getDate() {
        return today.format(formatter);
    }

    public static void advanceDays(int days) {
        today = today.plusDays(days);
        System.out.println("FAST FORWARD: The date is now " + getDate());
    }

}
