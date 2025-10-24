package Main;

import CONFIG.dbConnect;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

public class AdminDashboard {

    private final Scanner sc;
    private final dbConnect con;

    // Constructor to receive the necessary objects (Scanner and DB connection)
    public AdminDashboard(Scanner sc, dbConnect con) {
        this.sc = sc;
        this.con = con;
    }


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
        // Alignment MUST be handled within con.viewRecords()
        con.viewRecords(query, headers, columns);
    }

    // View All Bookings (unfiltered join)
    private void viewBookings() {
        String query = "SELECT b.b_id, b.u_id, b.t_id, b.guide_id, b.b_date, b.b_status, u.u_name AS customer_name, t.t_desc AS trek_name " +
                        "FROM table_bookings b " +
                        "JOIN table_user u ON b.u_id = u.u_id " +
                        "JOIN tables_treks t ON b.t_id = t.t_id";
        String[] headers = {"Booking ID", "Customer ID", "Trek ID", "Guide ID", "Date", "Status", "Customer", "Trek"};
        String[] columns = {"b_id", "u_id", "t_id", "guide_id", "b_date", "b_status", "customer_name", "trek_name"};
        System.out.println("\n--- All System Bookings ---");
        // Alignment MUST be handled within con.viewRecords()
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
        System.out.println("Trek Management: 1. Add | 2. View | 3. Update | 4. Delete");
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
            case 1: // Add Trek
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
    
    private void handleBookingAssignment() {
        viewBookings(); // Show current bookings before prompting for ID
        
        System.out.print("Enter Booking ID to Approve: ");
        int bid;
        if (sc.hasNextInt()) {
            bid = sc.nextInt();
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Booking ID format.");
            sc.nextLine();
            return;
        }
        
        // 1. Show available Guides for assignment
        String guideQuery = "SELECT u_id, u_name FROM table_user WHERE u_type = 'Guide' AND u_status = 'Approved'";
        String[] guideHeaders = {"ID", "Name"};
        String[] guideColumns = {"u_id", "u_name"};
        System.out.println("\n--- Available Guides ---");
        // Alignment MUST be handled within con.viewRecords()
        con.viewRecords(guideQuery, guideHeaders, guideColumns); 
        System.out.println("------------------------");
        
        System.out.print("Enter Guide ID to assign: ");
        int assignGuideId;
        if (sc.hasNextInt()) {
            assignGuideId = sc.nextInt(); 
            sc.nextLine();
        } else {
            System.out.println("❌ Invalid Guide ID format.");
            sc.nextLine();
            return;
        }
        
        // 2. Update booking status AND guide_id
        String bookSql = "UPDATE table_bookings SET b_status = ?, guide_id = ? WHERE b_id = ?";
        con.updateRecord(bookSql, "Approved", assignGuideId, bid); 
        
        System.out.println("✅ Booking ID " + bid + " has been Approved and Guide ID " + assignGuideId + " assigned.");
        
        // ⭐ Show the updated table immediately
        viewBookings();
    }

    // --- MAIN EXECUTION METHOD ---

    public void start() {
        int adminOpt;
        do {
            System.out.println("\n===== 👑 ADMIN DASHBOARD 👑 =====");
            System.out.println("1. Approve Account | 2. Manage Treks | 3. View All Bookings | 4. Approve Booking (Assign Guide) | 5. Log Out");
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
                case 5:
                    System.out.println("Logging out from Admin Dashboard...");
                    break;
                default:
                    System.out.println("Invalid admin option.");
            }
        } while (adminOpt != 5);
    }
}