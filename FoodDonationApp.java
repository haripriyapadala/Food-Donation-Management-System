import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// 1. CUSTOM EXCEPTION (Advanced Java)
class InvalidQuantityException extends Exception {
    public InvalidQuantityException(String message) {
        super(message);
    }
}

// 2. INTERFACE (OOP: Polymorphism & Contract)
interface Manageable {
    String getStatus();
    void setStatus(String status);
}
// 3. ABSTRACT CLASS (OOP: Abstraction)
abstract class User {
    private String name;
    private String contact;

    public User(String name, String contact) {
        this.name = name;
        this.contact = contact;
    }

    public String getName() { return name; }
    public String getContact() { return contact; }
    public abstract String getUserType();
}


// 4. INHERITANCE (OOP: Specialization)
class Donor extends User {
    private String address;

    public Donor(String name, String contact, String address) {
        super(name, contact);
        this.address = address;
    }

    public String getAddress() { return address; }

    @Override
    public String getUserType() {
        return "Food Donor";
    }
}


// 5. ENCAPSULATION & DATA MODEL
class FoodDonation implements Manageable {
    private int id;
    private String donorName;
    private String foodItem;
    private double quantityKg;
    private String category;
    private String status;

    public FoodDonation(int id, String donorName, String foodItem, double quantityKg, 
                        String category, String status) throws InvalidQuantityException {
        if (quantityKg <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than 0 kg!");
        }
        this.id = id;
        this.donorName = donorName;
        this.foodItem = foodItem;
        this.quantityKg = quantityKg;
        this.category = category;
        this.status = status;
    }

    public int getId() { return id; }
    public String getDonorName() { return donorName; }
    public String getFoodItem() { return foodItem; }
    public double getQuantityKg() { return quantityKg; }
    public String getCategory() { return category; }

    @Override
    public String getStatus() { return status; }

    @Override
    public void setStatus(String status) { this.status = status; }
}


// 6. PERSISTENCE & DATA ACCESS LAYER (JDBC + Collections Fallback)

class DatabaseHandler {
    private static final String DB_URL = "jdbc:sqlite:food_donation.db";
    private boolean isDbAvailable = false;
    private final List<FoodDonation> memoryFallbackList = new ArrayList<>();
    private int idCounter = 1;

    public DatabaseHandler() {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS donations (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "donor_name TEXT, food_item TEXT, " +
                             "quantity REAL, category TEXT, status TEXT)";
                stmt.execute(sql);
                isDbAvailable = true;
            }
        } catch (Exception e) {
            isDbAvailable = false;
        }
    }

    public boolean addDonation(FoodDonation donation) {
        if (isDbAvailable) {
            String sql = "INSERT INTO donations (donor_name, food_item, quantity, category, status) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, donation.getDonorName());
                pstmt.setString(2, donation.getFoodItem());
                pstmt.setDouble(3, donation.getQuantityKg());
                pstmt.setString(4, donation.getCategory());
                pstmt.setString(5, donation.getStatus());
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                return false;
            }
        } else {
            memoryFallbackList.add(donation);
            return true;
        }
    }

    public List<FoodDonation> getAllDonations() {
        List<FoodDonation> list = new ArrayList<>();
        if (isDbAvailable) {
            String sql = "SELECT * FROM donations";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new FoodDonation(
                        rs.getInt("id"), rs.getString("donor_name"),
                        rs.getString("food_item"), rs.getDouble("quantity"),
                        rs.getString("category"), rs.getString("status")
                    ));
                }
            } catch (Exception e) {
                list.addAll(memoryFallbackList);
            }
        } else {
            list.addAll(memoryFallbackList);
        }
        return list;
    }

    public boolean updateStatus(int id, String newStatus) {
        if (isDbAvailable) {
            String sql = "UPDATE donations SET status = ? WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, id);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                return false;
            }
        } else {
            for (FoodDonation d : memoryFallbackList) {
                if (d.getId() == id) {
                    d.setStatus(newStatus);
                    return true;
                }
            }
            return false;
        }
    }

    public int getNextId() { return idCounter++; }
}


// 7. PRESENTATION LAYER (Java Swing GUI)

public class FoodDonationApp extends JFrame {
    private JTextField txtDonorName, txtContact, txtFoodItem, txtQuantity;
    private JComboBox<String> cmbCategory;
    private JTable donationTable;
    private DefaultTableModel tableModel;
    private DatabaseHandler dbHandler;

    public FoodDonationApp() {
        dbHandler = new DatabaseHandler();
        setTitle("Food Donation & Relief Management System");
        setSize(850, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add Food Donation"));
        formPanel.add(new JLabel("Donor Name:"));
        txtDonorName = new JTextField(); formPanel.add(txtDonorName);
        formPanel.add(new JLabel("Contact:"));
        txtContact = new JTextField(); formPanel.add(txtContact);
        formPanel.add(new JLabel("Food Item:"));
        txtFoodItem = new JTextField(); formPanel.add(txtFoodItem);
        formPanel.add(new JLabel("Quantity (Kg):"));
        txtQuantity = new JTextField(); formPanel.add(txtQuantity);
        formPanel.add(new JLabel("Category:"));
        cmbCategory = new JComboBox<>(new String[]{"Cooked Meals", "Packaged Food", "Raw Groceries", "Bakery/Fruits"});
        formPanel.add(cmbCategory);

        JButton btnSubmit = new JButton("Donate Food");
        btnSubmit.addActionListener(e -> handleAddDonation());
        formPanel.add(btnSubmit);

        JButton btnClear = new JButton("Clear");
        btnClear.addActionListener(e -> clearForm());
        formPanel.add(btnClear);

        add(formPanel, BorderLayout.WEST);

        // Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Donor", "Food Item", "Qty (Kg)", "Category", "Status"}, 0);
        donationTable = new JTable(tableModel);
        add(new JScrollPane(donationTable), BorderLayout.CENTER);

        // Actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnDistribute = new JButton("Mark Distributed");
        btnDistribute.addActionListener(e -> handleDistribute());
        bottomPanel.add(btnDistribute);
        add(bottomPanel, BorderLayout.SOUTH);

        loadDonationsToTable();
    }

    private void handleAddDonation() {
        try {
            double qty = Double.parseDouble(txtQuantity.getText().trim());
            Donor donor = new Donor(txtDonorName.getText().trim(), txtContact.getText().trim(), "City Center");
            FoodDonation donation = new FoodDonation(dbHandler.getNextId(), donor.getName(), 
                                                     txtFoodItem.getText().trim(), qty, 
                                                     (String)cmbCategory.getSelectedItem(), "Available");
            if (dbHandler.addDonation(donation)) {
                JOptionPane.showMessageDialog(this, "Donation added successfully!");
                clearForm();
                loadDonationsToTable();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be numeric!");
        } catch (InvalidQuantityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void handleDistribute() {
        int row = donationTable.getSelectedRow();
        if (row != -1) {
            int id = (int) tableModel.getValueAt(row, 0);
            dbHandler.updateStatus(id, "Distributed");
            loadDonationsToTable();
        }
    }

    private void loadDonationsToTable() {
        tableModel.setRowCount(0);
        for (FoodDonation d : dbHandler.getAllDonations()) {
            tableModel.addRow(new Object[]{d.getId(), d.getDonorName(), d.getFoodItem(), d.getQuantityKg(), d.getCategory(), d.getStatus()});
        }
    }

    private void clearForm() {
        txtDonorName.setText(""); txtContact.setText("");
        txtFoodItem.setText(""); txtQuantity.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoodDonationApp().setVisible(true));
    }
}
