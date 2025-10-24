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
    
    // View Assigned Bookings (Filters by guideId)
    private void viewAssignedBookings() {
        // Query joins bookings with customer (u) and trek (t) tables, filtered by the current guide's ID
        String Query = "SELECT b.b_id, u.u_name AS customer_name, t.t_desc AS trek_name, b.b_date, b.b_status " +
                        "FROM table_bookings b " +
                        "JOIN table_user u ON b.u_id = u.u_id " +
                        "JOIN tables_treks t ON b.t_id = t.t_id " +
                        "WHERE b.guide_id = ?"; 
        
        // Fetch the records using the guideId parameter
        List<Map<String, Object>> result = con.fetchRecords(Query, guideId);
        
        if (result.isEmpty()) {
            System.out.println("✅ No assigned bookings found.");
            return;
        }
        
        // Manual printing for filtered results (adjust width as needed)
        int totalWidth = 100;
        String separator = createSeparator(totalWidth);
        
        System.out.println(separator);
        System.out.printf("| %-12s | %-20s | %-20s | %-12s | %-15s |\n", 
                            "Booking ID", "Customer Name", "Trek Name", "Date", "Status");
        System.out.println(separator);
        for (Map<String, Object> row : result) {
            System.out.printf("| %-12s | %-20s | %-20s | %-12s | %-15s |\n",
                              row.get("b_id"), row.get("customer_name"), row.get("trek_name"), 
                              row.get("b_date"), row.get("b_status"));
        }
        System.out.println(separator);
    }

    // --- HANDLER METHOD WITH ENHANCED VALIDATION ---

    private void handleUpdateBookingStatus() {
        // 1. Show bookings first
        viewAssignedBookings(); 
        
        System.out.print("Enter Booking ID to update: ");
        insssst bookingIdToUpdate;
        
        // 2. Validation for Booking ID (must be an integer)
        if (sc.hasNextInt()) {
            bookingIdToUpdate = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Booking ID format. Must be a number.");
            sc.nextLine();
            return;
        }
        
        // 3. Validation: Check if the booking exists AND is assigned to this guide
        String checkQry = "SELECT b_status FROM table_bookings WHERE b_id = ? AND guide_id = ?";
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


        // CRITICAL: Filtered by guideId for security
        String updateSql = "UPDATE table_bookings SET b_status = ? WHERE b_id = ? AND guide_id = ?";
        
        // 💥 FIX APPLIED: Removed the assignment to 'int rowsUpdated' 
        //                 to fix the compilation error.
        con.updateRecord(updateSql, newStatus, bookingIdToUpdate, guideId);
        
        // Rely on the prior existence check and the database call succeeding
        System.out.println("✅ Booking ID " + bookingIdToUpdate + " status successfully updated to " + newStatus + ".");
    }

    // --- MAIN EXECUTION METHOD ---

    public void start() {
        int guideOpt;
        do {
            System.out.println("\n===== 🧑‍💼 GUIDE DASHBOARD 🧑‍💼 =====");
            System.out.println("1. View Assigned Bookings | 2. Update Booking Status | 3. Log Out");
            System.out.print("Enter your choice: ");

            // 1. Input Validation for Menu Choice
            if (sc.hasNextInt()) {
                guideOpt = sc.nextInt();
                sc.nextLine();  
            } else {
                System.out.println("❌ Invalid input. Please enter a number (1-3).");
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
                    System.out.println("Logging out from Guide Dashboard...");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        } while (guideOpt != 3);
    }
}