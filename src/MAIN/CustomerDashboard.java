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
    
    // Basic date pattern YYYY-MM-DD (not strictly used here, but kept from original code)
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
    
    // View Available Schedules
    private List<Map<String, Object>> viewAvailableSchedules() {
        // Fetches schedules that are in the future and have capacity > 0
        String query = "SELECT s.s_id, t.t_code, t.t_desc, t.t_difficulty, s.schedule_date, " +
                        "s.capacity AS available_slots, t.t_price, u.u_name AS guide_name " +
                        "FROM table_schedules s " +
                        "JOIN tables_treks t ON s.t_id = t.t_id " +
                        "JOIN table_user u ON s.guide_id = u.u_id " +
                        "WHERE s.capacity > 0 AND s.schedule_date >= DATE('now') " + 
                        "ORDER BY s.schedule_date";
        
        List<Map<String, Object>> result = con.fetchRecords(query); 
        
        if (result.isEmpty()) {
            System.out.println("No available treks scheduled at this time.");
            return result;
        }

        // Define fixed widths for alignment:
        int totalWidth = 160;
        String separator = createSeparator(totalWidth);
        
        // Format string for output rows
        String format = "| %-10s | %-8s | %-40s | %-12s | %-12s | %-10s | %-15s | %-25s |%n";
        
        System.out.println("\n===== AVAILABLE TREK SCHEDULES =====");
        System.out.println(separator);
        // Print Headers
        System.out.printf(format, 
                          "Sched ID", "Code", "Description", "Difficulty", "Date", 
                          "Slots", "Price (PHP)", "Guide");
        System.out.println(separator);
        
        // Print Data
        for (Map<String, Object> row : result) {
            System.out.printf(format,
                              row.get("s_id"), 
                              row.get("t_code"), 
                              row.get("t_desc"), 
                              row.get("t_difficulty"), 
                              row.get("schedule_date"), 
                              row.get("available_slots"), 
                              row.get("t_price"),
                              row.get("guide_name"));
        }
        System.out.println(separator);
        return result;
    }
    
    // View Customer's Bookings - UPDATED to show guide and guests
    private void viewMyBookings() {
        String query = "SELECT b.b_id, s.s_id, t.t_desc AS trek_name, s.schedule_date, " +
                        "b.b_guests, b.b_total_price, b.b_status, u.u_name AS assigned_guide " +
                        "FROM table_bookings b " +
                        "JOIN table_schedules s ON b.s_id = s.s_id " + 
                        "JOIN tables_treks t ON s.t_id = t.t_id " + 
                        "LEFT JOIN table_user u ON s.guide_id = u.u_id " + 
                        "WHERE b.u_id = ?"; 
        
        List<Map<String, Object>> result = con.fetchRecords(query, customerId);
        
        if (result.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        
        int totalWidth = 145; 
        String separator = createSeparator(totalWidth);
        
        String format = "| %-13s | %-10s | %-40s | %-12s | %-10s | %-15s | %-10s | %-15s |%n";
        
        System.out.println("\n===== MY BOOKINGS =====");
        System.out.println(separator);
        System.out.printf(format, 
                          "Booking ID", "Schedule ID", "Trek", "Date", "Guests", 
                          "Total Price", "Status", "Guide");
        System.out.println(separator);
        for (Map<String, Object> row : result) {
            System.out.printf(format,
                              row.get("b_id"), row.get("s_id"), row.get("trek_name"), 
                              row.get("schedule_date"), row.get("b_guests"), 
                              row.get("b_total_price"), row.get("b_status"), 
                              row.get("assigned_guide"));
        }
        System.out.println(separator);
    }
    
    // ------------------------------------
    // --- HANDLER METHODS ----------------
    // ------------------------------------
    
    /**
     * Handles the trek booking process, including collecting guest names and 
     * performing necessary database transactions (insert booking, insert guests, update capacity).
     */
    private void handleBookTrek() {
        List<Map<String, Object>> availableSchedules = viewAvailableSchedules(); 
        if (availableSchedules.isEmpty()) {
            return;
        }

        System.out.print("Enter Schedule ID to Book: ");
        int scheduleId;

        // 1. Validate Schedule ID format
        if (sc.hasNextInt()) {
            scheduleId = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Schedule ID format. Must be a number.");
            sc.nextLine();
            return;
        }
        
        // 2. Retrieve Schedule and Trek Price/Capacity details
        String scheduleDetailsQuery = "SELECT s.capacity, t.t_price FROM table_schedules s JOIN tables_treks t ON s.t_id = t.t_id WHERE s.s_id = ?";
        List<Map<String, Object>> detailsResult = con.fetchRecords(scheduleDetailsQuery, scheduleId);

        if (detailsResult.isEmpty()) {
            System.out.println("❌ Schedule ID " + scheduleId + " not found or is no longer available.");
            return;
        }

        int capacity = (int) detailsResult.get(0).get("capacity");
        double pricePerPerson = ((Number) detailsResult.get(0).get("t_price")).doubleValue(); 

        // 3. Get Quantity (Number of Guests)
        int numGuests;
        do {
            System.out.print("Enter total number of people including yourself (1 - " + capacity + "): ");
            if (sc.hasNextInt()) {
                numGuests = sc.nextInt();
                sc.nextLine();
            } else {
                System.out.println("❌ Invalid input. Please enter a number.");
                sc.nextLine();
                return;
            }
            
            if (numGuests <= 0) {
                System.out.println("❌ You must book for at least one person.");
            } else if (numGuests > capacity) {
                System.out.println("❌ Not enough slots available! Maximum allowed is " + capacity + ".");
            }
        } while (numGuests <= 0 || numGuests > capacity);

        // 4. ⭐ COLLECT GUEST NAMES ⭐
        String[] guestNames = new String[numGuests];
        System.out.println("\nPlease enter the full names for your group:");
        for (int i = 0; i < numGuests; i++) {
            System.out.printf("  Name for Person %d: ", i + 1);
            guestNames[i] = sc.nextLine().trim();
        }

        // 5. Calculate Total Price
        double totalPrice = pricePerPerson * numGuests;

        // 6. Perform Insertion & Capacity Update
        
        // --- Transaction Step 1: Insert Booking Record and GET ID ---
        String bookSql = "INSERT INTO table_bookings (u_id, s_id, b_date, b_status, b_guests, b_total_price) " +
                          "VALUES (?, ?, (SELECT schedule_date FROM table_schedules WHERE s_id = ?), ?, ?, ?)";
        
        int bookingId;
        try {
            // Uses the implemented addRecordAndGetId method to insert and get the auto-generated ID
            bookingId = con.addRecordAndGetId(bookSql, customerId, scheduleId, scheduleId, "Pending", numGuests, totalPrice);
        } catch (Exception e) {
            System.out.println("❌ Failed to create main booking record. Transaction aborted. Error: " + e.getMessage());
            return;
        }
        
        // --- Transaction Step 2: ⭐ INSERT GUEST RECORDS ⭐ ---
        // Assumes 'table_guests' with columns (b_id, guest_name)
        String guestSql = "INSERT INTO table_guests (b_id, guest_name) VALUES (?, ?)";
        for (String guestName : guestNames) {
            con.addRecord(guestSql, bookingId, guestName);
        }

        // --- Transaction Step 3: Update Schedule Capacity ---
        int newCapacity = capacity - numGuests;
        String updateCapacitySql = "UPDATE table_schedules SET capacity = ? WHERE s_id = ?";
        con.updateRecord(updateCapacitySql, newCapacity, scheduleId);

        System.out.println("\n=============================================");
        System.out.println("✅ Booking created successfully! (Booking ID: " + bookingId + " | Status: Pending)");
        System.out.printf("   Guests: %d | Total Price: PHP %.2f%n", numGuests, totalPrice);
        System.out.println("   All guest names have been recorded.");
        System.out.println("   Remaining Slots for Schedule ID " + scheduleId + ": " + newCapacity);
        System.out.println("=============================================");
    }

    // HANDLER METHOD: Cancel Booking
    private void handleCancelBooking() {
        viewMyBookings(); // Show customer's current bookings
        
        System.out.print("Enter Booking ID to Cancel: ");
        int cancelBid;

        // 1. Validate Booking ID format
        if (sc.hasNextInt()) {
            cancelBid = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Booking ID format. Must be a number.");
            sc.nextLine();
            return;
        }
        
        // 2. Retrieve details: status, s_id, and guests for capacity refund
        String checkBookingQuery = "SELECT b_status, s_id, b_guests FROM table_bookings WHERE b_id = ? AND u_id = ?";
        List<Map<String, Object>> bookingResult = con.fetchRecords(checkBookingQuery, cancelBid, customerId);

        if (bookingResult.isEmpty()) {
            System.out.println("❌ Booking ID " + cancelBid + " not found or does not belong to your account.");
            return;
        }
        
        String currentStatus = (String) bookingResult.get(0).get("b_status");
        int scheduleId = (int) bookingResult.get(0).get("s_id");
        int cancelledGuests = (int) bookingResult.get(0).get("b_guests");
        
        if ("Cancelled".equalsIgnoreCase(currentStatus) || "Completed".equalsIgnoreCase(currentStatus)) {
            System.out.println("⚠️ Cannot cancel Booking ID " + cancelBid + ". Current status is: " + currentStatus + ".");
            return;
        }
        
        // 3. Update the booking status to 'Cancelled'
        String cancelSql = "UPDATE table_bookings SET b_status = ? WHERE b_id = ? AND u_id = ?";
        con.updateRecord(cancelSql, "Cancelled", cancelBid, customerId);
        
        // 4. Refund Capacity to the schedule
        String refundCapacitySql = "UPDATE table_schedules SET capacity = capacity + ? WHERE s_id = ?";
        con.updateRecord(refundCapacitySql, cancelledGuests, scheduleId);
        
        System.out.println("✅ Booking ID " + cancelBid + " has been marked as 'Cancelled'.");
        System.out.println("   Capacity for Schedule ID " + scheduleId + " refunded: " + cancelledGuests + " slots.");
        
        // Show updated list
        viewMyBookings();
    }


    // ------------------------------------
    // --- MAIN EXECUTION METHOD ----------
    // ------------------------------------

    public void start() {
        int custOpt;
        do {
            System.out.println("\n===== 🚶 CUSTOMER DASHBOARD 🚶 =====");
            System.out.println("1. View Available Treks\n 2. Book a Trek \n 3. View My Bookings\n 4. Cancel Booking \n 5. Log Out");
            System.out.print("Enter choice: ");

            if (sc.hasNextInt()) {
                custOpt = sc.nextInt();
                sc.nextLine();  
            } else {
                System.out.println("❌ Invalid input. Please enter a number (1-5).");
                sc.nextLine();  
                custOpt = 0;
                continue;
            }

            switch (custOpt) {
                case 1:
                    viewAvailableSchedules(); 
                    break;
                case 2: // Book Trek
                    handleBookTrek();
                    break;
                case 3:
                    viewMyBookings();
                    break;
                case 4: // Cancellation
                    handleCancelBooking();
                    break;
                case 5: // Log Out
                    System.out.println("Logging out from Customer Dashboard...");
                    break;
                default:
                    System.out.println("Invalid customer option.");
            }
        } while (custOpt != 5);
    }
}