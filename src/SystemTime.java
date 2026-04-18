import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class SystemTime {
    private static LocalDate today = LocalDate.now();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    
    public static void setDate(int year, int month, int day) {
        today = LocalDate.of(year, month, day);
        System.out.println("Date reset to: " + getDate());
    }

}
