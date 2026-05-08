package Utils;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class SystemTime {
    private static LocalDate today = LocalDate.now();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SystemSettingsRepository SETTINGS = new SystemSettingsRepository();

    public static LocalDate getToday() {
        return today;
    }

    public static String getDate() {
        return today.format(formatter);
    }

    public static void advanceDays(int days) {
        today = today.plusDays(days);
        try {
            if (SETTINGS.updateCurrentDate(today)) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to sync date to database: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("FAST FORWARD: The date is now " + getDate());
    }

    public static void resetToRealToday() {
        today = LocalDate.now();
        try {
            if (SETTINGS.updateCurrentDate(today)) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to reset date to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void syncFromDatabase() {
        try {
            LocalDate syncedDate = SETTINGS.loadCurrentDateCatchingUp();
            if (syncedDate != null) {
                today = syncedDate;
            }
        } catch (Exception e) {
            System.out.println("Could not fetch date from database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
