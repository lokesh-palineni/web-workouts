/*
 * Online Banking System - Fund Transfer
 */
package banking.management.system;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fund Transfer functionality for transferring between accounts
 * [AGENT GENERATED CODE - REQUIREMENT:US3]
 */
public class FundTransfer extends JFrame implements ActionListener {

    private JLabel titleLabel, fromAccountLabel, toAccountLabel, amountLabel, descriptionLabel, confirmationLabel;
    private JTextField amountField, descriptionField;
    private JComboBox<String> fromAccountCombo, toAccountCombo;
    private JButton transferButton, backButton, clearButton;
    private JPanel confirmationPanel;
    
    private String pin;
    private String accountNo;
    private double availableBalance;
    private static final Logger LOGGER = Logger.getLogger(FundTransfer.class.getName());
    private Connection connection;

    /**
     * Constructor for Fund Transfer screen
     * @param pin User PIN for authentication
     * @param accountNo Current user's account number
     */
    public FundTransfer(String pin, String accountNo) {
        this.pin = pin;
        this.accountNo = accountNo;
        connection = ConnectionSql.Connector();
        
        setLayout(null);
        getContentPane().setBackground(new Color(204, 229, 255));
        
        // Load account balance
        loadAvailableBalance();
        
        // Initialize UI components
        initializeUI();
        
        setSize(1600, 1200);
        setVisible(true);
    }
    
    /**
     * Initializes all UI components for the fund transfer screen
     */
    private void initializeUI() {
        // Logo
        ImageIcon logoIcon = new ImageIcon(ClassLoader.getSystemResource("icons/logo.jpg"));
        Image logoImage = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_DEFAULT);
        ImageIcon scaledLogoIcon = new ImageIcon(logoImage);  
        JLabel logoLabel = new JLabel(scaledLogoIcon);
        logoLabel.setBounds(70, 10, 100, 100);
        add(logoLabel);
        
        // Title
        titleLabel = new JLabel("FUND TRANSFER");
        titleLabel.setFont(new Font("Osward", Font.BOLD, 32));
        titleLabel.setBounds(200, 40, 450, 40);
        titleLabel.setForeground(Color.black);
        add(titleLabel);
        
        // From Account Section
        fromAccountLabel = new JLabel("Transfer From:");
        fromAccountLabel.setFont(new Font("Raleway", Font.BOLD, 20));
        fromAccountLabel.setBounds(120, 150, 200, 30);
        add(fromAccountLabel);
        
        fromAccountCombo = new JComboBox<>();
        fromAccountCombo.setBounds(120, 190, 400, 30);
        fromAccountCombo.setFont(new Font("Raleway", Font.PLAIN, 16));
        add(fromAccountCombo);
        loadUserAccounts(fromAccountCombo);
        
        // To Account Section
        toAccountLabel = new JLabel("Transfer To:");
        toAccountLabel.setFont(new Font("Raleway", Font.BOLD, 20));
        toAccountLabel.setBounds(120, 240, 200, 30);
        add(toAccountLabel);
        
        toAccountCombo = new JComboBox<>();
        toAccountCombo.setBounds(120, 280, 400, 30);
        toAccountCombo.setFont(new Font("Raleway", Font.PLAIN, 16));
        add(toAccountCombo);
        loadAllAccounts(toAccountCombo);
        
        // Amount Section
        amountLabel = new JLabel("Amount:");
        amountLabel.setFont(new Font("Raleway", Font.BOLD, 20));
        amountLabel.setBounds(120, 330, 200, 30);
        add(amountLabel);
        
        amountField = new JTextField();
        amountField.setBounds(120, 370, 400, 30);
        amountField.setFont(new Font("Raleway", Font.PLAIN, 16));
        add(amountField);
        
        // Description Section
        descriptionLabel = new JLabel("Description (Optional):");
        descriptionLabel.setFont(new Font("Raleway", Font.BOLD, 20));
        descriptionLabel.setBounds(120, 420, 300, 30);
        add(descriptionLabel);
        
        descriptionField = new JTextField();
        descriptionField.setBounds(120, 460, 400, 30);
        descriptionField.setFont(new Font("Raleway", Font.PLAIN, 16));
        add(descriptionField);
        
        // Buttons
        transferButton = new JButton("TRANSFER");
        transferButton.setBounds(120, 520, 150, 40);
        transferButton.setBackground(Color.BLACK);
        transferButton.setForeground(Color.WHITE);
        transferButton.setFont(new Font("Raleway", Font.BOLD, 15));
        transferButton.addActionListener(this);
        add(transferButton);
        
        clearButton = new JButton("CLEAR");
        clearButton.setBounds(290, 520, 150, 40);
        clearButton.setBackground(Color.DARK_GRAY);
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("Raleway", Font.BOLD, 15));
        clearButton.addActionListener(this);
        add(clearButton);
        
        backButton = new JButton("BACK");
        backButton.setBounds(460, 520, 150, 40);
        backButton.setBackground(Color.DARK_GRAY);
        backButton.setForeground(Color.WHITE);
        backButton.setFont(new Font("Raleway", Font.BOLD, 15));
        backButton.addActionListener(this);
        add(backButton);
        
        // Confirmation Panel (initially hidden)
        confirmationPanel = new JPanel();
        confirmationPanel.setBounds(120, 580, 500, 200);
        confirmationPanel.setLayout(null);
        confirmationPanel.setBackground(new Color(240, 255, 240));
        confirmationPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        confirmationPanel.setVisible(false);
        add(confirmationPanel);
        
        confirmationLabel = new JLabel("Confirm Transfer Details");
        confirmationLabel.setFont(new Font("Raleway", Font.BOLD, 18));
        confirmationLabel.setBounds(20, 10, 300, 30);
        confirmationPanel.add(confirmationLabel);
        
        // Add transfer image
        ImageIcon transferIcon = new ImageIcon(ClassLoader.getSystemResource("icons/deposit1.jpg"));
        if (transferIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            Image transferImage = transferIcon.getImage().getScaledInstance(800, 800, Image.SCALE_DEFAULT);
            ImageIcon scaledTransferIcon = new ImageIcon(transferImage);
            JLabel transferImageLabel = new JLabel(scaledTransferIcon);
            transferImageLabel.setBounds(800, 0, 800, 800);
            add(transferImageLabel);
        }
    }
    
    /**
     * Load user's available balance
     */
    private void loadAvailableBalance() {
        try {
            ConnectionSql c = new ConnectionSql();
            int balance = 0;
            ResultSet rs = c.s.executeQuery("SELECT * FROM bank WHERE Login_Password = '" + pin + 
                                          "' AND Account_No = '" + accountNo + "'");
            
            while (rs.next()) {
                if (rs.getString("type").equals("Deposit")) {
                    balance += Integer.parseInt(rs.getString("amount"));
                } else {
                    balance -= Integer.parseInt(rs.getString("amount"));
                }
            }
            
            this.availableBalance = balance;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading available balance", e);
        }
    }
    
    /**
     * Load user's accounts into the combo box
     * @param comboBox The combo box to populate with accounts
     */
    private void loadUserAccounts(JComboBox<String> comboBox) {
        try {
            comboBox.addItem(accountNo + " (Balance: $" + availableBalance + ")");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading user accounts", e);
        }
    }
    
    /**
     * Load all accounts into the destination combo box
     * @param comboBox The combo box to populate with accounts
     */
    private void loadAllAccounts(JComboBox<String> comboBox) {
        try {
            ConnectionSql c = new ConnectionSql();
            ResultSet rs = c.s.executeQuery("SELECT DISTINCT Account_No FROM login WHERE Account_No != '" + accountNo + "'");
            
            while (rs.next()) {
                comboBox.addItem(rs.getString("Account_No"));
            }
            
            // Add option for external accounts
            comboBox.addItem("Enter External Account...");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading destination accounts", e);
        }
    }
    
    /**
     * Display confirmation dialog with transfer details
     * @param sourceAccount Source account number
     * @param destAccount Destination account number
     * @param amount Transfer amount
     * @param description Transfer description
     * @return true if user confirms, false otherwise
     */
    private boolean showConfirmationDialog(String sourceAccount, String destAccount, double amount, String description) {
        // Clear confirmation panel
        confirmationPanel.removeAll();
        
        // Set up confirmation details
        JLabel titleLabel = new JLabel("Please Confirm Transfer Details");
        titleLabel.setFont(new Font("Raleway", Font.BOLD, 16));
        titleLabel.setBounds(20, 10, 300, 30);
        confirmationPanel.add(titleLabel);
        
        JLabel fromLabel = new JLabel("From: " + sourceAccount);
        fromLabel.setBounds(20, 50, 450, 20);
        confirmationPanel.add(fromLabel);
        
        JLabel toLabel = new JLabel("To: " + destAccount);
        toLabel.setBounds(20, 80, 450, 20);
        confirmationPanel.add(toLabel);
        
        JLabel amtLabel = new JLabel("Amount: $" + amount);
        amtLabel.setBounds(20, 110, 450, 20);
        confirmationPanel.add(amtLabel);
        
        if (description != null && !description.isEmpty()) {
            JLabel descLabel = new JLabel("Description: " + description);
            descLabel.setBounds(20, 140, 450, 20);
            confirmationPanel.add(descLabel);
        }
        
        JButton confirmButton = new JButton("Confirm");
        confirmButton.setBounds(100, 160, 120, 30);
        confirmButton.setBackground(Color.BLACK);
        confirmButton.setForeground(Color.WHITE);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBounds(250, 160, 120, 30);
        cancelButton.setBackground(Color.DARK_GRAY);
        cancelButton.setForeground(Color.WHITE);
        
        confirmationPanel.add(confirmButton);
        confirmationPanel.add(cancelButton);
        confirmationPanel.setVisible(true);
        
        // Custom confirmation dialog with action listeners
        final boolean[] result = {false}; // Use array to make it effectively final
        
        confirmButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result[0] = true;
                confirmationPanel.setVisible(false);
                processTransfer();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmationPanel.setVisible(false);
            }
        });
        
        return result[0];
    }
    
    /**
     * Process the fund transfer transaction
     */
    private void processTransfer() {
        try {
            String fromAccount = accountNo;
            String toAccount = toAccountCombo.getSelectedItem().toString();
            
            // Check if it's an "Enter External Account" option
            if (toAccount.equals("Enter External Account...")) {
                toAccount = JOptionPane.showInputDialog("Enter external account number:");
                if (toAccount == null || toAccount.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Invalid account number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (toAccount.contains(" ")) {
                // Extract account number if it has additional text
                toAccount = toAccount.split(" ")[0];
            }
            
            double amount = Double.parseDouble(amountField.getText());
            String description = descriptionField.getText();
            
            // Validate amount
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check sufficient funds
            if (amount > availableBalance) {
                JOptionPane.showMessageDialog(this, "Insufficient funds.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Generate transaction reference
            String transactionRef = generateTransactionReference();
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = formatter.format(date);
            
            ConnectionSql c = new ConnectionSql();
            
            // Debit source account
            String debitQuery = "INSERT INTO bank VALUES(?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(debitQuery);
            pstmt.setString(1, pin);
            pstmt.setString(2, fromAccount);
            pstmt.setString(3, timestamp);
            pstmt.setString(4, "Transfer-Out");
            pstmt.setString(5, String.valueOf(amount));
            pstmt.setString(6, description);
            pstmt.setString(7, transactionRef);
            pstmt.executeUpdate();
            
            // Credit destination account
            String creditQuery = "INSERT INTO bank VALUES(?, ?, ?, ?, ?, ?, ?)";
            pstmt = connection.prepareStatement(creditQuery);
            pstmt.setString(1, ""); // No PIN for recipient
            pstmt.setString(2, toAccount);
            pstmt.setString(3, timestamp);
            pstmt.setString(4, "Transfer-In");
            pstmt.setString(5, String.valueOf(amount));
            pstmt.setString(6, description);
            pstmt.setString(7, transactionRef);
            pstmt.executeUpdate();
            
            // Generate receipt
            String receiptMessage = 
                "Transaction Successful!\n\n" +
                "Transaction Reference: " + transactionRef + "\n" +
                "Date: " + timestamp + "\n" +
                "From Account: " + maskAccountNumber(fromAccount) + "\n" +
                "To Account: " + maskAccountNumber(toAccount) + "\n" +
                "Amount: $" + amount + "\n" +
                "Description: " + (description.isEmpty() ? "N/A" : description) + "\n\n" +
                "Thank you for using our banking services.";
            
            JOptionPane.showMessageDialog(this, receiptMessage, "Transaction Receipt", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear fields
            amountField.setText("");
            descriptionField.setText("");
            
            // Update available balance
            loadAvailableBalance();
            
            // Trigger notification if amount is large (> $1000)
            if (amount > 1000) {
                // This would typically call the NotificationService
                LOGGER.info("Large transfer notification triggered for amount: $" + amount);
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transfer", e);
            JOptionPane.showMessageDialog(this, "Transfer failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Generate a unique transaction reference
     * @return Unique transaction reference string
     */
    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Mask account number for privacy in receipts
     * @param accountNumber Account number to mask
     * @return Masked account number
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "XXXX" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Handle button click events
     * @param ae Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == transferButton) {
            try {
                String fromAccount = accountNo;
                String toAccount = toAccountCombo.getSelectedItem().toString();
                
                if (toAccount.equals("Enter External Account...")) {
                    toAccount = JOptionPane.showInputDialog("Enter external account number:");
                    if (toAccount == null || toAccount.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Invalid account number.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else if (toAccount.contains(" ")) {
                    toAccount = toAccount.split(" ")[0];
                }
                
                double amount = 0;
                try {
                    amount = Double.parseDouble(amountField.getText());
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String description = descriptionField.getText();
                
                // Validate inputs
                if (fromAccount.equals(toAccount)) {
                    JOptionPane.showMessageDialog(this, "Source and destination accounts cannot be the same.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (amount > availableBalance) {
                    JOptionPane.showMessageDialog(this, "Insufficient funds.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Show confirmation dialog
                showConfirmationDialog(fromAccount, toAccount, amount, description);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in transfer button action", e);
                JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } else if (ae.getSource() == clearButton) {
            // Clear all fields
            amountField.setText("");
            descriptionField.setText("");
            
        } else if (ae.getSource() == backButton) {
            // Return to transactions screen
            setVisible(false);
            new Transactions(pin, accountNo).setVisible(true);
        }
    }

    /**
     * Main method for testing
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        new FundTransfer("", "");
    }
}

/*
Agent Run Summary:
- Implemented US3: Fund Transfer Between Accounts
- Related test cases: TC-TRF-001, TC-TRF-002, TC-TRF-003
- Agent Run ID: VIBE-OB-11-TRF-2025-11-24
*/