package Main;

import CONFIG.dbConnect;
import java.util.Scanner;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern; // Added for date validation

public class AdminDashboard {

    private final Scanner sc;
    private final dbConnect con;
    
    // Basic date pattern YYYY-MM-DD for validation
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    // Constructor to receive the necessary objects (Scanner and DB connection)
    public AdminDashboard(Scanner sc, dbConnect con) {
        this.sc = sc;
        this.con = con;
    }

    // --- UTILITY VIEW METHODS ---

    // Helper method to create separator lines for the tables (for manual printing if needed)
    private static String createSeparator(int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append("-");
        }
        return sb.toString();
    }
    
    private void viewUsers() {
        String query = "SELECT u_id, u_name, u_email, u_contact, u_type, u_status FROM table_user";
        String[] headers = {"ID", "Name", "Email", "Contact", "Type", "Status"};
        String[] columns = {"u_id", "u_name", "u_email", "u_contact", "u_type", "u_status"};
        System.out.println("\n--- All System Users ---");
        // Alignment MUST be handled within con.viewRecords()
        con.viewRecords(query, headers, columns); 
        System.out.println("------------------------");
    }

    // View Treks (unfiltered)
    private void viewTreks() {
        String query = "SELECT * FROM tables_treks";
        String[] headers = {"Trek ID", "Code", "Description", "Difficulty", "Price"};
        String[] columns = {"t_id", "t_code", "t_desc", "t_difficulty", "t_price"};
        System.out.println("\n--- All Available Treks ---");
        // Alignment MUST be handled within con.viewRecords()
        con.viewRecords(query, headers, columns);
        System.out.println("---------------------------");
    }

    // View All Bookings (unfiltered join) - REVISED to use s_id and show guests/price
    private void viewBookings() {
        String query = "SELECT b.b_id, b.u_id, s.s_id, b.b_guests, b.b_total_price, b.b_status, u.u_name AS customer_name, t.t_desc AS trek_name, gu.u_name AS guide_name " +
                        "FROM table_bookings b " +
                        "JOIN table_user u ON b.u_id = u.u_id " +
                        "JOIN table_schedules s ON b.s_id = s.s_id " +
                        "JOIN tables_treks t ON s.t_id = t.t_id " +
                        "LEFT JOIN table_user gu ON s.guide_id = gu.u_id";
                        
        String[] headers = {"Bkg ID", "Cust ID", "Sched ID", "Guests", "Total Price", "Status", "Customer", "Trek", "Guide"};
        String[] columns = {"b_id", "u_id", "s_id", "b_guests", "b_total_price", "b_status", "customer_name", "trek_name", "guide_name"};
        System.out.println("\n--- All System Bookings ---");
        // Alignment MUST be handled within con.viewRecords()
        con.viewRecords(query, headers, columns);
        System.out.println("---------------------------");
    }
    
    // View Scheduled Treks
    private void viewSchedules() {
        // Assuming table_schedules exists (s_id, t_id, schedule_date, capacity, guide_id)
        String query = "SELECT s_id, t.t_desc AS trek_name, s.schedule_date, s.capacity, u.u_name AS guide_name " +
                       "FROM table_schedules s " +
                       "JOIN tables_treks t ON s.t_id = t.t_id " +
                       "JOIN table_user u ON s.guide_id = u.u_id " +
                       "ORDER BY s.schedule_date";
        String[] headers = {"Schedule ID", "Trek Name", "Date", "Capacity", "Guide"};
        String[] columns = {"s_id", "trek_name", "schedule_date", "capacity", "guide_name"};
        System.out.println("\n--- All Scheduled Treks ---");
        con.viewRecords(query, headers, columns);
        System.out.println("---------------------------");
    }

    // --- PRIVATE HANDLER METHODS ---

    private void handleAccountApproval() {
        viewUsers(); // Show current users before prompting for ID
        System.out.print("Enter ID to Approve: ");
        
        // Input Validation
        if (sc.hasNextInt()) {
            int ids = sc.nextInt();
            sc.nextLine();
            String sql = "UPDATE table_user SET u_status = ? WHERE u_id = ?";
            con.updateRecord(sql, "Approved", ids);
            System.out.println("✅ User ID " + ids + " has been Approved.");
            
            // ⭐ Show the updated table immediately
            viewUsers(); 
            
        } else {
            System.out.println("❌ Invalid ID format. Please enter a number.");
            sc.nextLine();
        }
    }

    private void handleTrekManagement() {
        System.out.println("\n--- Trek Management ---");
        System.out.println(" 1. Add\n 2. View\n 3. Update\n 4. Delete");
        System.out.print("Enter option: ");
        int trekOpt; 
        
        // Input Validation
        if (sc.hasNextInt()) {
            trekOpt = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid option. Please enter a number.");
            sc.nextLine();
            return;
        }
        
        switch (trekOpt) {
            case 1: { // Add Trek
                String code, desc, diff;
                double price;
                
                // Input validation for string fields
                do {
                    System.out.print("Enter Trek Code: ");
                    code = sc.nextLine().trim();
                    if (code.isEmpty()) System.out.println("❌ Trek Code cannot be empty.");
                } while (code.isEmpty());
                
                do {
                    System.out.print("Enter Description: ");
                    desc = sc.nextLine().trim();
                    if (desc.isEmpty()) System.out.println("❌ Description cannot be empty.");
                } while (desc.isEmpty());
                
                do {
                    System.out.print("Enter Difficulty: ");
                    diff = sc.nextLine().trim();
                    if (diff.isEmpty()) System.out.println("❌ Difficulty cannot be empty.");
                } while (diff.isEmpty());
                
                // Input validation for price
                System.out.print("Enter Price: ");
                if (sc.hasNextDouble()) {
                    price = sc.nextDouble();
                    sc.nextLine();
                } else {
                    System.out.println("❌ Invalid price format. Must be a number.");
                    sc.nextLine();
                    break;
                }
                
                if (price <= 0) {
                    System.out.println("❌ Validation failed: Price must be greater than zero.");
                    break;
                }
                
                String addSql = "INSERT INTO tables_treks (t_code, t_desc, t_difficulty, t_price) VALUES (?, ?, ?, ?)";
                con.addRecord(addSql, code, desc, diff, price);
                System.out.println("✅ Trek added successfully!");
                viewTreks(); // Show updated list
                break;
            }
            case 2: // View Treks
                viewTreks();
                break;
            case 3: { // Update Trek
                viewTreks();
                System.out.print("Enter Trek ID to Update: ");
                int tid;
                if (sc.hasNextInt()) {
                    tid = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Description: ");
                    String newDesc = sc.nextLine().trim();
                    if (newDesc.isEmpty()) {
                        System.out.println("❌ Description cannot be empty.");
                        break;
                    }
                    String upSql = "UPDATE tables_treks SET t_desc = ? WHERE t_id = ?";
                    con.updateRecord(upSql, newDesc, tid);
                    System.out.println("✅ Trek updated successfully!");
                    viewTreks(); // Show updated list
                } else {
                    System.out.println("❌ Invalid Trek ID format.");
                    sc.nextLine();
                }
                break;
            }
            case 4: { // Delete Trek
                viewTreks();
                System.out.print("Enter Trek ID to Delete: ");
                if (sc.hasNextInt()) {
                    int delTid = sc.nextInt();
                    sc.nextLine();
                    String delSql = "DELETE FROM tables_treks WHERE t_id = ?";
                    con.deleteRecord(delSql, delTid);
                    System.out.println("✅ Trek deleted successfully!");
                    viewTreks(); // Show updated list
                } else {
                    System.out.println("❌ Invalid Trek ID format.");
                    sc.nextLine();
                }
                break;
            }
            default:
                System.out.println("Invalid trek option.");
        }
    }
    
    // REVISED: Booking Assignment now focuses on managing a booking status through a schedule ID
    private void handleBookingAssignment() {
        viewBookings(); // Show current bookings before prompting for ID
        
        System.out.print("Enter Booking ID to update (e.g., set to Approved/Confirmed): ");
        int bid;
        if (sc.hasNextInt()) {
            bid = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Booking ID format.");
            sc.nextLine();
            return;
        }
        
        // 1. Check if booking exists and get its schedule ID
        String checkBookingQuery = "SELECT s_id FROM table_bookings WHERE b_id = ?";
        List<Map<String, Object>> bookingResult = con.fetchRecords(checkBookingQuery, bid);
        
        if (bookingResult.isEmpty()) {
            System.out.println("❌ Booking ID " + bid + " not found.");
            return;
        }
        
        int scheduleId = (int) bookingResult.get(0).get("s_id");
        
        // 2. Determine Guide Assignment Status (Is a guide already assigned to the schedule?)
        String getGuideQuery = "SELECT guide_id, u_name FROM table_schedules s LEFT JOIN table_user u ON s.guide_id = u.u_id WHERE s_id = ?";
        List<Map<String, Object>> guideCheck = con.fetchRecords(getGuideQuery, scheduleId);
        
        int currentGuideId = guideCheck.get(0).get("guide_id") != null ? (int) guideCheck.get(0).get("guide_id") : 0;
        String currentGuideName = (String) guideCheck.get(0).get("u_name");
        
        if (currentGuideId == 0) {
            System.out.println("\n⚠️ **NO GUIDE ASSIGNED** to Schedule ID " + scheduleId + ".");
            
            // Show available Guides for assignment
            String guideQuery = "SELECT u_id, u_name FROM table_user WHERE u_type = 'Guide' AND u_status = 'Approved'";
            String[] guideHeaders = {"ID", "Name"};
            String[] guideColumns = {"u_id", "u_name"};
            System.out.println("\n--- Available Guides ---");
            con.viewRecords(guideQuery, guideHeaders, guideColumns); 
            System.out.println("------------------------");
            
            System.out.print("Enter Guide ID to assign to SCHEDULE " + scheduleId + " (or 0 to skip guide assignment): ");
            int assignGuideId;
            if (sc.hasNextInt()) {
                assignGuideId = sc.nextInt(); 
                sc.nextLine();
            } else {
                System.out.println("❌ Invalid Guide ID format.");
                sc.nextLine();
                return;
            }
            
            if (assignGuideId > 0) {
                // Assign guide to the SCHEDULE, not the booking directly
                String assignGuideSql = "UPDATE table_schedules SET guide_id = ? WHERE s_id = ?";
                con.updateRecord(assignGuideSql, assignGuideId, scheduleId);
                System.out.println("✅ Guide ID " + assignGuideId + " assigned to Schedule " + scheduleId + ".");
            }
        } else {
            System.out.println("✅ Guide already assigned to Schedule ID " + scheduleId + ": " + currentGuideName);
        }

        // 3. Update booking status
        String bookSql = "UPDATE table_bookings SET b_status = ? WHERE b_id = ?";
        con.updateRecord(bookSql, "Approved", bid); 
        
        System.out.println("✅ Booking ID " + bid + " status updated to 'Approved'.");
        
        // ⭐ Show the updated table immediately
        viewBookings();
    }
    
    // Manage Trek Schedules
    private void handleScheduleTrek() {
        System.out.println("\n--- Schedule Trek Management ---");
        
        // Menu for Schedule Management
        System.out.println(" 1. Add New Schedule\n 2. View All Schedules");
        System.out.print("Enter option: ");
        
        int scheduleOpt;
        if (sc.hasNextInt()) {
            scheduleOpt = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid option. Please enter a number.");
            sc.nextLine();
            return;
        }

        switch (scheduleOpt) {
            case 1: { // Add New Schedule
                viewTreks(); // Show available treks first
                
                System.out.print("Enter Trek ID to schedule: ");
                int trekId;
                if (sc.hasNextInt()) {
                    trekId = sc.nextInt();
                    sc.nextLine();
                } else {
                    System.out.println("❌ Invalid Trek ID format.");
                    sc.nextLine();
                    return;
                }

                // 1. Get Date
                String hikeDate;
                do {
                    System.out.print("Enter Hike Date (YYYY-MM-DD): ");
                    hikeDate = sc.nextLine().trim();
                    
                    if (hikeDate.isEmpty() || !DATE_PATTERN.matcher(hikeDate).matches()) {
                        System.out.println("❌ Invalid date format. Please use YYYY-MM-DD.");
                    } else {
                        break;
                    }
                } while(true);
                
                // 2. Get Capacity (Limit 15)
                int capacity;
                do {
                    System.out.print("Enter Capacity (max 15): ");
                    if (sc.hasNextInt()) {
                        capacity = sc.nextInt();
                        sc.nextLine();
                    } else {
                        System.out.println("❌ Invalid capacity format. Must be a number.");
                        sc.nextLine();
                        return;
                    }
                    if (capacity <= 0 || capacity > 15) {
                        System.out.println("❌ Capacity must be between 1 and 15.");
                    }
                } while (capacity <= 0 || capacity > 15);

                // 3. Get Guide ID (Optional on creation, can be assigned later)
                String guideQuery = "SELECT u_id, u_name FROM table_user WHERE u_type = 'Guide' AND u_status = 'Approved'";
                String[] guideHeaders = {"ID", "Name"};
                String[] guideColumns = {"u_id", "u_name"};
                System.out.println("\n--- Available Guides (Optional Assignment) ---");
                con.viewRecords(guideQuery, guideHeaders, guideColumns);
                System.out.println("----------------------------------------------");
                
                System.out.print("Enter Guide ID to assign now (or 0 for unassigned): ");
                int guideId;
                if (sc.hasNextInt()) {
                    guideId = sc.nextInt();
                    sc.nextLine();
                } else {
                    System.out.println("❌ Invalid Guide ID format. Setting to unassigned (0).");
                    sc.nextLine();
                    guideId = 0;
                }

                // 4. Insert the new schedule record
                String addScheduleSql = "INSERT INTO table_schedules (t_id, schedule_date, capacity, guide_id) VALUES (?, ?, ?, ?)";
                con.addRecord(addScheduleSql, trekId, hikeDate, capacity, guideId > 0 ? guideId : null); // Use null if guideId is 0
                
                System.out.println("✅ Trek scheduled successfully!");
                viewSchedules();
                break;
            }

            case 2: // View All Schedules
                viewSchedules();
                break;
                
            default:
                System.out.println("Invalid schedule option.");
        }
    }


    // --- MAIN EXECUTION METHOD ---

    public void start() {
        int adminOpt;
        do {
            System.out.println("\n===== 👑 ADMIN DASHBOARD 👑 =====");
            System.out.println("1. Approve Account \n2. Manage Treks \n3. View All Bookings \n4. Approve Booking (Assign Guide to Schedule) \n5. Schedule New Trek \n6. Log Out");
            System.out.print("Enter choice: ");

            // Input Validation for Menu Choice
            if (sc.hasNextInt()) {
                adminOpt = sc.nextInt();
                sc.nextLine();
            } else {
                System.out.println("❌ Invalid input. Please enter a number.");
                sc.nextLine();  
                adminOpt = 0; 
                continue;
            }

            switch (adminOpt) {
                case 1:
                    handleAccountApproval();
                    break;
                case 2:
                    handleTrekManagement();
                    break;
                case 3:
                    viewBookings();
                    break;
                case 4:
                    handleBookingAssignment();
                    break;
                case 5: // Case for scheduling
                    handleScheduleTrek();
                    break;
                case 6: // Log Out updated to case 6
                    System.out.println("Logging out from Admin Dashboard...");
                    break;
                default:
                    System.out.println("Invalid admin option.");
            }
        } while (adminOpt != 6); // Loop condition updated to 6
    }
}