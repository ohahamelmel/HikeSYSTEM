package Main;

import CONFIG.dbConnect;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;
import Main.AdminDashboard;
import Main.GuideDashboard;
import Main.CustomerDashboard;

public class main {

    // Basic regex for email validation
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        // Initialize DB Connection and Scanner
        dbConnect con = new dbConnect();
        int choice;
        char cont = 'Y'; 
        Scanner sc = new Scanner(System.in);
        final int MIN_PASS_LENGTH = 6; // Constant for password length validation

        // Label for easy continuation of the main loop (used in registration error handling)
        mainLoop: do {
            System.out.println("\n===== MAIN MENU =====");
            System.out.println("Welcome to Baklay Cebu: Online Hiking & Trekking Bookings");
            System.out.println("1. Login | 2. Register | 3. Exit");
            System.out.print("Enter choice: ");

            // --- Menu Choice Input Validation ---
            if (!sc.hasNextInt()) {
                System.out.println("❌ Invalid input. Please enter a number (1, 2, or 3).");
                sc.nextLine(); // Clear bad input
                continue; // Restart the loop
            }
            choice = sc.nextInt();
            sc.nextLine(); // Consume newline

            switch (choice) {
                case 1: // LOGIN
                    String em, pas;
                    
                    // 1. Validate Email input for LOGIN
                    do {
                        System.out.print("Enter email: ");
                        em = sc.nextLine().trim();
                        if (em.isEmpty()) {
                            System.out.println("❌ Email cannot be empty.");
                        } else if (!EMAIL_PATTERN.matcher(em).matches()) {
                            System.out.println("❌ Invalid email format.");
                        } else {
                            break;
                        }
                    } while (true);
                    
                    // 2. Validate Password input for LOGIN
                    do {
                        System.out.print("Enter Password: ");
                        pas = sc.nextLine().trim(); 
                        if (pas.isEmpty()) {
                            System.out.println("❌ Password cannot be empty.");
                        } else {
                            break;
                        }
                    } while (true);

                    // --- SECURE LOGIN LOGIC ---
                    String qry = "SELECT u_id, u_type, u_status, u_pass FROM table_user WHERE u_email = ?";
                    List<Map<String, Object>> result = con.fetchRecords(qry, em);
                    
                    if (result.isEmpty()) {
                        System.out.println("❌ INVALID CREDENTIALS (Email not found)");
                    } else {
                        Map<String, Object> user = result.get(0);
                        String storedHashedPassword = user.get("u_pass").toString();

                        // HASH the trimmed raw input password
                        String inputHashedPassword = dbConnect.hashPassword(pas); 
                        
                        // VERIFY: Compare the newly generated hash with the stored hash
                        if (inputHashedPassword != null && inputHashedPassword.equals(storedHashedPassword)) {
                            // Password is correct!
                            String stat = user.get("u_status").toString();
                            String type = user.get("u_type").toString();
                            int userId = ((Number) user.get("u_id")).intValue();

                            if (stat.equals("Pending")) {
                                System.out.println("⚠️ Account is Pending, Contact the Admin!");
                            } else {
                                System.out.println("✅ LOGIN SUCCESS! Welcome, " + type + "!");
                                
                                // DELEGATION TO DASHBOARD CLASSES
                                if (type.equals("Admin")) {
                                    new AdminDashboard(sc, con).start();
                                } else if (type.equals("Guide")) {
                                    new GuideDashboard(sc, con, userId).start();
                                } else if (type.equals("Customer")) {
                                    new CustomerDashboard(sc, con, userId).start();
                                }
                            }
                        } else {
                            // Password verification failed
                            System.out.println("❌ INVALID CREDENTIALS (Incorrect password)");
                        }
                    }
                    break;

                case 2: // REGISTRATION
                    // ✅ FIX: Declare all variables at the start of the case block
                    String name, contact, email, pass;
                    
                    // 1. Validate Name
                    do {
                        System.out.print("Enter user name: ");
                        name = sc.nextLine().trim();
                        if (name.isEmpty()) {
                            System.out.println("❌ User name cannot be empty.");
                        }
                    } while (name.isEmpty());

                    // 2. Validate Contact
                    do {
                        System.out.print("Enter user contact: ");
                        contact = sc.nextLine().trim();
                        if (contact.isEmpty()) {
                            System.out.println("❌ Contact number cannot be empty.");
                        } else if (!contact.matches("\\d+")) { // Check if it contains only digits
                            System.out.println("❌ Contact number must contain only digits.");
                        } else {
                            break;
                        }
                    } while (true);
                    
                    // 3. Validate Email (Format & Uniqueness)
                    do {
                        System.out.print("Enter user email: ");
                        email = sc.nextLine().trim(); // Note: 'email' is now in scope from the top of case 2
                        
                        if (email.isEmpty()) {
                            System.out.println("❌ Email cannot be empty.");
                            continue;
                        }

                        // Email Format Check using the provided EMAIL_PATTERN
                        if (!EMAIL_PATTERN.matcher(email).matches()) {
                            System.out.println("❌ Invalid email format. Please try again.");
                            continue;
                        }
                        
                        // Check Email Uniqueness (Database check)
                        String checkQry = "SELECT * FROM table_user WHERE u_email = ?";
                        if (!con.fetchRecords(checkQry, email).isEmpty()) {
                            System.out.println("❌ Email already exists. Enter another email.");
                        } else {
                            break; // Email is valid and unique
                        }
                    } while (true);
                    
                    // 4. Validate User Type Input
                    int typeInt;
                    String tp;
                    while (true) {
                        System.out.print("Enter user Type (1 - Admin/2 - Guide/3 - Customer): ");
                        if (!sc.hasNextInt()) {
                            System.out.println("❌ Invalid type input. Must be a number. Returning to main menu.");
                            sc.nextLine(); // Clear bad input
                            continue mainLoop; // Go back to the main menu
                        }
                        typeInt = sc.nextInt(); 
                        sc.nextLine(); // Consume newline
                        
                        if (typeInt >= 1 && typeInt <= 3) {
                            tp = (typeInt == 1) ? "Admin" : (typeInt == 2) ? "Guide" : "Customer";
                            break;
                        } else {
                            System.out.println("❌ Invalid type, choose between 1 and 3 only.");
                        }
                    }
                    
                    // 5. Validate Password (Minimum Length)
                    do {
                        System.out.print("Enter Password (Min " + MIN_PASS_LENGTH + " chars): ");
                        // 🔑 FIX 2: Trim password input during REGISTRATION
                        pass = sc.nextLine().trim();
                        
                        if (pass.isEmpty()) {
                            System.out.println("❌ Password cannot be empty.");
                        } else if (pass.length() < MIN_PASS_LENGTH) {
                            System.out.println("❌ Password must be at least " + MIN_PASS_LENGTH + " characters long.");
                        } else {
                            break;
                        }
                    } while (true);
                    
                    // 🔑 HASH THE TRIMMED PASSWORD BEFORE STORING 🔑
                    String hashedPasswordToStore = dbConnect.hashPassword(pass);
                    
                    if (hashedPasswordToStore == null) {
                        System.out.println("❌ Registration failed due to password hashing error.");
                        break;
                    }

                    // Insert record using the hashed password
                    String regSql = "INSERT INTO table_user (u_name, u_contact, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?, ?)";
                    con.addRecord(regSql, name, contact, email, tp, "Pending", hashedPasswordToStore);
                    System.out.println("✅ Registration successful! (Status: Pending - Await admin approval)");
                    break;

                case 3:
                    System.out.println("👋 Thank you for using Baklay Cebu. Program ended.");
                    sc.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("❌ Invalid choice.");
            }
            
            // --- Continue Prompt ---
            System.out.print("Do you want to continue? (Y/N): ");
            String contInput = sc.nextLine();
            cont = contInput.isEmpty() ? 'N' : contInput.toUpperCase().charAt(0);

        } while (cont == 'Y');
        
        // Final cleanup if the user exits via the continue prompt
        sc.close();
        System.out.println("👋 Program ended.");
    }
}