package Main;

import CONFIG.dbConnect;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class CustomerDashboard {

    private final Scanner sc;
    private final dbConnect con;
    private final int customerId;
    
    // Basic date pattern YYYY-MM-DD
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    // Constructor...
    public CustomerDashboard(Scanner sc, dbConnect con, int customerId) {
        this.sc = sc;
        this.con = con;
        this.customerId = customerId;
    }
    
    // ------------------------------------
    // --- UTILITY METHODS ----------------
    // ------------------------------------
    
    // Helper method to create separator lines 
    private static String createSeparator(int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append("-");
        }
        return sb.toString();
    }
    
    // View Treks (NOW CUSTOM ALIGNED) 🎯
    private void viewTreks() {
        String query = "SELECT t_id, t_code, t_desc, t_difficulty, t_price FROM tables_treks";
        
        List<Map<String, Object>> result = con.fetchRecords(query); // Assumes fetchRecords handles query without parameters
        
        if (result.isEmpty()) {
            System.out.println("No treks available.");
            return;
        }

        // Define fixed widths for alignment:
        // ID: 7, Code: 8, Description: 50, Difficulty: 15, Price: 10
        // Total Width: 1 + 7 + 3 + 8 + 3 + 50 + 3 + 15 + 3 + 10 + 1 = 101
        int totalWidth = 101;
        String separator = createSeparator(totalWidth);
        
        // Format string for output rows
        String format = "| %-7s | %-8s | %-50s | %-15s | %-10s |%n";
        
        System.out.println("\n===== AVAILABLE TREKS =====");
        System.out.println(separator);
        // Print Headers
        System.out.printf(format, 
                          "Trek ID", "Code", "Description", "Difficulty", "Price (PHP)");
        System.out.println(separator);
        
        // Print Data
        for (Map<String, Object> row : result) {
            System.out.printf(format,
                              row.get("t_id"), 
                              row.get("t_code"), 
                              row.get("t_desc"), 
                              row.get("t_difficulty"), 
                              row.get("t_price"));
        }
        System.out.println(separator);
    }
    
    // View Customer's Bookings (Kept as previously fixed)
    private void viewMyBookings() {
        String Query = "SELECT b.b_id, b.t_id, b.guide_id, b.b_date, b.b_status, t.t_desc AS trek_name " +
                        "FROM table_bookings b " +
                        "JOIN tables_treks t ON b.t_id = t.t_id " + 
                        "WHERE b.u_id = ?"; // Filters by the Customer's ID
        
        List<Map<String, Object>> result = con.fetchRecords(Query, customerId);
        
        if (result.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        
        int totalWidth = 112; 
        String separator = createSeparator(totalWidth);
        
        String format = "| %-13s | %-8s | %-10s | %-12s | %-15s | %-40s |%n";
        
        System.out.println("\n===== MY BOOKINGS =====");
        System.out.println(separator);
        System.out.printf(format, 
                          "Booking ID", "Trek ID", "Guide ID", "Date", "Status", "Trek");
        System.out.println(separator);
        for (Map<String, Object> row : result) {
            System.out.printf(format,
                              row.get("b_id"), row.get("t_id"), row.get("guide_id"), row.get("b_date"), 
                              row.get("b_status"), row.get("trek_name"));
        }
        System.out.println(separator);
    }

    // ------------------------------------
    // --- MAIN EXECUTION METHOD ----------
    // ------------------------------------

    public void start() {
        int custOpt;
        do {
            System.out.println("\n===== 🚶 CUSTOMER DASHBOARD 🚶 =====");
            System.out.println("1. View Treks | 2. Book Trek | 3. View My Bookings | 4. Log Out");
            System.out.print("Enter choice: ");

            if (sc.hasNextInt()) {
                custOpt = sc.nextInt();
                sc.nextLine();  
            } else {
                System.out.println("❌ Invalid input. Please enter a number (1-4).");
                sc.nextLine(); 
                custOpt = 0;
                continue;
            }

            switch (custOpt) {
                case 1:
                    viewTreks();
                    break;
                case 2: // Book Trek
                    handleBookTrek();
                    break;
                case 3:
                    viewMyBookings();
                    break;
                case 4:
                    System.out.println("Logging out from Customer Dashboard...");
                    break;
                default:
                    System.out.println("Invalid customer option.");
            }
        } while (custOpt != 4);
    }
    
    private void handleBookTrek() {
        viewTreks(); // Show treks first
        
        System.out.print("Enter Trek ID to Book: ");
        int bookTid;

        // 1. Validate Trek ID format (must be integer)
        if (sc.hasNextInt()) {
            bookTid = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Trek ID format. Must be a number.");
            sc.nextLine();
            return;
        }
        
        // 2. Validate Trek ID existence
        String checkTrekQuery = "SELECT t_id FROM tables_treks WHERE t_id = ?";
        List<Map<String, Object>> trekResult = con.fetchRecords(checkTrekQuery, bookTid);

        if (trekResult.isEmpty()) {
            System.out.println("❌ Trek ID " + bookTid + " not found. Please choose an available trek.");
            return;
        }
        
        // 3. Validate Booking Date format and content
        String bookDate;
        do {
            System.out.print("Enter Booking Date (YYYY-MM-DD): ");
            bookDate = sc.nextLine().trim();
            
            if (bookDate.isEmpty()) {
                System.out.println("❌ Booking Date cannot be empty.");
            } else if (!DATE_PATTERN.matcher(bookDate).matches()) {
                System.out.println("❌ Invalid date format. Please use YYYY-MM-DD (e.g., 2024-12-31).");
            } else {
                // Future: Add logic to check if the date is in the future.
                break; 
            }
        } while (true);
        
        
        // Insertion happens only after all validations pass
        String bookSql = "INSERT INTO table_bookings (u_id, t_id, b_date, b_status) VALUES (?, ?, ?, ?)";
        con.addRecord(bookSql, customerId, bookTid, bookDate, "Pending");
        System.out.println("✅ Booking created successfully! (Status: Pending - Await admin approval)");
    }
}