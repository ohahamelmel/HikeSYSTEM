package Main;

import CONFIG.dbConnect;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GuideDashboard {

    private final Scanner sc;
    private final dbConnect con;
    private final int guideId;

    // Constructor...
    public GuideDashboard(Scanner sc, dbConnect con, int guideId) {
        this.sc = sc;
        this.con = con;
        this.guideId = guideId;
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

    // ⭐ View Assigned Bookings (Filters by guideId and includes guest count)
    private void viewAssignedBookings() {
        // Query joins bookings with customer (u), schedule (s), and trek (t) tables, filtered by the current guide's ID
        String Query = "SELECT b.b_id, u.u_name AS customer_name, t.t_desc AS trek_name, s.schedule_date, b.b_status, b.b_guests " +
                        "FROM table_bookings b " +
                        "JOIN table_user u ON b.u_id = u.u_id " +
                        "JOIN table_schedules s ON b.s_id = s.s_id " + // Join to schedule
                        "JOIN  tables_treks t ON s.t_id = t.t_id " +
                        "WHERE s.guide_id = ? AND b.b_status NOT IN ('Cancelled', 'Completed') AND b.b_status IN ('Approved', 'Confirmed')"; // Filter for active treks

        // Fetch the records using the guideId parameter
        List<Map<String, Object>> result = con.fetchRecords(Query, guideId);

        if (result.isEmpty()) {
            System.out.println("✅ No active assigned bookings found.");
            return;
        }

        // Manual printing for filtered results (adjust width as needed)
        int totalWidth = 120;
        String separator = createSeparator(totalWidth);

        System.out.println("\n===== YOUR ACTIVE ASSIGNED BOOKINGS =====");
        System.out.println(separator);
        System.out.printf("| %-12s | %-20s | %-30s | %-12s | %-8s | %-15s |\n", 
                             "Booking ID", "Customer Name", "Trek Name", "Date", "Guests", "Status");
        System.out.println(separator);
        for (Map<String, Object> row : result) {
            System.out.printf("| %-12d | %-20s | %-30s | %-12s | %-8d | %-15s |\n",
                             (Integer) row.get("b_id"), // Cast to Integer for safe printing with %d
                             row.get("customer_name"), 
                             row.get("trek_name"), 
                             row.get("schedule_date"), 
                             (Integer) row.get("b_guests"), // Cast to Integer for safe printing with %d
                             row.get("b_status"));
        }
        System.out.println(separator);
    }

    // ------------------------------------
    // --- HANDLER METHOD (FIXED FOR OPTION 3) ---
    // ------------------------------------

    private void handleViewTrekMembers() {
        viewAssignedBookings(); // Show the guide their active bookings first

        System.out.print("Enter Booking ID to view members: ");
        int bookingId;

        // 1. Validation for Booking ID (must be an integer)
        if (sc.hasNextInt()) {
            bookingId = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Booking ID format. Must be a number.");
            sc.nextLine();
            return;
        }

        // ⭐ Query: Get Lead Booker Details AND Guest Count
        String memberQuery = "SELECT u.u_name, u.u_contact, t.t_desc AS trek_name, b.b_guests " +
                             "FROM table_bookings b " +
                             "JOIN table_user u ON b.u_id = u.u_id " +
                             "JOIN table_schedules s ON b.s_id = s.s_id " +
                             "JOIN  tables_treks t ON s.t_id = t.t_id " +
                             "WHERE b.b_id = ? AND s.guide_id = ? AND b.b_status IN ('Approved', 'Confirmed')";

        List<Map<String, Object>> memberResult = con.fetchRecords(memberQuery, bookingId, guideId);

        if (memberResult.isEmpty()) {
            System.out.println("❌ Booking ID " + bookingId + " not found, is not assigned to you, or is not yet Approved/Confirmed.");
            return;
        }

        // 3. Display Member Details
        Map<String, Object> bookingDetails = memberResult.get(0);
        // FIX: Use String.valueOf() to safely convert to String, avoiding ClassCastException if the DB returns Integer/Long for contact
        String leadName = String.valueOf(bookingDetails.get("u_name"));
        String leadContact = String.valueOf(bookingDetails.get("u_contact"));
        String trekName = String.valueOf(bookingDetails.get("trek_name"));
        
        // Safely cast the result of Map.get() to Integer, then unbox to int.
        Object guestsObj = bookingDetails.get("b_guests");
        int totalGuests = (guestsObj instanceof Integer) ? (Integer) guestsObj : 0;
        
        int totalPeople = 1 + totalGuests; // Lead Booker + Guests

        System.out.println("\n===== 🥾 TREK MANIFEST FOR BOOKING ID: " + bookingId + " (" + trekName + ") =====");
        System.out.println("👥 **TOTAL PEOPLE:** " + totalPeople + " (Lead Booker + " + totalGuests + " Guest(s))");
        int totalWidth = 65;
        String separator = createSeparator(totalWidth);
        String format = "| %-30s | %-25s |\n";

        System.out.println(separator);
        System.out.printf(format, "ROLE / NAME", "CONTACT NUMBER");
        System.out.println(separator);

        // Display the Lead Customer
        System.out.printf(format, "**LEAD CUSTOMER:** " + leadName, leadContact);
        System.out.println(separator);

        // Fetch actual guest names from table_guests
        String guestQuery = "SELECT guest_name FROM table_guests WHERE b_id = ?";
        List<Map<String, Object>> guestResult = con.fetchRecords(guestQuery, bookingId);

        // Always display guests based on totalGuests, using real names if available, otherwise placeholders
        if (!guestResult.isEmpty() && guestResult.size() == totalGuests) {
            // Use actual guest names if the count matches
            for (Map<String, Object> guestRow : guestResult) {
                String guestName = String.valueOf(guestRow.get("guest_name"));
                System.out.printf(format, "Guest: " + guestName, "N/A (Details not tracked)");
            }
        } else {
            // Fallback: Display generic placeholders if no guest names are found or count doesn't match
            for (int i = 1; i <= totalGuests; i++) {
                System.out.printf(format, "Guest #" + i, "N/A (Details not tracked)");
            }
        }
        System.out.println(separator);
        
        System.out.println("⚠️ **NOTE:** Contact details are only available for the Lead Booker. Guest names are recorded for identification (if available).");
    }


    // --- HANDLER METHOD (FIXED SQL INJECTION VULNERABILITY) ---

    private void handleUpdateBookingStatus() {
        // 1. Show bookings first
        viewAssignedBookings(); 

        System.out.print("Enter Booking ID to update: ");
        int bookingIdToUpdate;

        // 2. Validation for Booking ID (must be an integer)
        if (sc.hasNextInt()) {
            bookingIdToUpdate = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Booking ID format. Must be a number.");
            sc.nextLine(); // Clear the bad input
            return;
        }

        // 3. Validation: Check if the booking exists AND is assigned to this guide (via schedule)
        String checkQry = "SELECT b.b_status FROM table_bookings b JOIN table_schedules s ON b.s_id = s.s_id WHERE b.b_id = ? AND s.guide_id = ?";
        List<Map<String, Object>> bookingCheck = con.fetchRecords(checkQry, bookingIdToUpdate, guideId);

        if (bookingCheck.isEmpty()) {
            System.out.println("❌ Error: Booking ID " + bookingIdToUpdate + " not found or not assigned to you.");
            return;
        }

        String newStatus;
        boolean validStatus = false;

        // 4. Validation: Prompt for new Status with constrained options
        do {
            System.out.println("Available statuses: [Confirmed, Completed, Cancelled]");
            System.out.print("Enter new status: ");
            newStatus = sc.nextLine().trim();

            if (newStatus.isEmpty()) {
                System.out.println("❌ Status cannot be empty.");
            } else if (newStatus.equalsIgnoreCase("Confirmed") || 
                       newStatus.equalsIgnoreCase("Completed") || 
                       newStatus.equalsIgnoreCase("Cancelled")) {
                validStatus = true;
                // Normalize status to start with a capital letter
                newStatus = newStatus.substring(0, 1).toUpperCase() + newStatus.substring(1).toLowerCase();
            } else {
                System.out.println("❌ Invalid status. Please choose from: Confirmed, Completed, or Cancelled.");
            }
        } while (!validStatus);


        // ✅ FIX: Use a secure JOIN update statement to prevent SQL Injection
        String updateSql = "UPDATE table_bookings b "
                + "JOIN table_schedules s ON b.s_id = s.s_id "
                + "SET b.b_status = ? "
                + "WHERE b.b_id = ? AND s.guide_id = ?";

        con.updateRecord(updateSql, newStatus, bookingIdToUpdate, guideId);

        System.out.println("✅ Booking ID " + bookingIdToUpdate + " status successfully updated to " + newStatus + ".");
    }

    // --- MAIN EXECUTION METHOD (Unchanged menu structure) ---

    public void start() {
        int guideOpt;
        do {
            System.out.println("\n===== 🧑‍💼 GUIDE DASHBOARD 🧑‍💼 =====");
            System.out.println("1. View Assigned Bookings \n2. Update Booking Status \n3. View Trek Members \n4. Log Out");
            System.out.print("Enter your choice: ");

            // 1. Input Validation for Menu Choice
            if (sc.hasNextInt()) {
                guideOpt = sc.nextInt();
                sc.nextLine();  
            } else {
                System.out.println("❌ Invalid input. Please enter a number (1-4).");
                sc.nextLine(); // Clear the bad input
                guideOpt = 0; // Reset to loop again
                continue;
            }

            switch (guideOpt) {
                case 1:
                    viewAssignedBookings(); 
                    break;
                case 2:
                    handleUpdateBookingStatus();
                    break;
                case 3: 
                    handleViewTrekMembers();
                    break;
                case 4: 
                    System.out.println("Logging out from Guide Dashboard...");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        } while (guideOpt != 4); 
    }
}
