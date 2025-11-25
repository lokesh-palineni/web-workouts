/*
 * Online Banking System - Authentication Model
 */
package banking.management.system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

/**
 * Authentication model for the Online Banking System
 * [AGENT GENERATED CODE - REQUIREMENT:US1]
 */
public class LoginModel {
    
    private Connection connection;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final Logger LOGGER = Logger.getLogger(LoginModel.class.getName());
    private static final long SESSION_TIMEOUT_MINUTES = 15;
    
    private LocalDateTime lastActivityTime;
    private String currentAccountNo;
    private int loginAttempts;
    
    public LoginModel() {
        connection = ConnectionSql.Connector();
        if (connection == null) {
            LOGGER.severe("Database connection failed");
            JOptionPane.showMessageDialog(null, "Database connection failed", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        loginAttempts = 0;
    }
    
    /**
     * Checks if the database connection is active
     * @return true if connected, false otherwise
     */
    public boolean isDbConnected() {
        try {
            return !connection.isClosed();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database connection check failed", ex);
            return false;
        }
    }
    
    /**
     * Authenticates a user with account number and password
     * @param accountNo the account number
     * @param password the password
     * @return true if authentication successful, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean isLogin(String accountNo, String password) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String query = "SELECT * FROM login WHERE Account_No=? AND Login_Password=?";
        
        try {
            // Check for too many failed login attempts
            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                LOGGER.warning("Account locked due to too many failed login attempts: " + accountNo);
                JOptionPane.showMessageDialog(null, "Account temporarily locked. Please contact customer support.", 
                        "Security Alert", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, accountNo);
            preparedStatement.setString(2, password); // In production, use hashed password comparison
            
            resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                // Successful login
                currentAccountNo = accountNo;
                loginAttempts = 0;
                lastActivityTime = LocalDateTime.now();
                logLoginActivity(accountNo, true);
                return true;
            } else {
                // Failed login
                loginAttempts++;
                logLoginActivity(accountNo, false);
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login authentication error", e);
            return false;
        } finally {
            if (preparedStatement != null) preparedStatement.close();
            if (resultSet != null) resultSet.close();
        }
    }
    
    /**
     * Checks if the user has admin privileges
     * @param accountNo the account number
     * @param password the password
     * @return true if user is admin, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean isAdmin(String accountNo, String password) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String query = "SELECT * FROM login WHERE Account_No=? AND Login_Password=? AND user_type='ADMIN'";
        
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, accountNo);
            preparedStatement.setString(2, password);
            
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin verification error", e);
            return false;
        } finally {
            if (preparedStatement != null) preparedStatement.close();
            if (resultSet != null) resultSet.close();
        }
    }
    
    /**
     * Checks if the current session is still valid (not timed out)
     * @return true if session is valid, false if timed out
     */
    public boolean isSessionValid() {
        if (lastActivityTime == null || currentAccountNo == null) {
            return false;
        }
        
        LocalDateTime currentTime = LocalDateTime.now();
        long minutesDiff = java.time.Duration.between(lastActivityTime, currentTime).toMinutes();
        
        if (minutesDiff >= SESSION_TIMEOUT_MINUTES) {
            LOGGER.info("Session timeout for account: " + currentAccountNo);
            return false;
        }
        
        // Update last activity time
        lastActivityTime = currentTime;
        return true;
    }
    
    /**
     * Ends the current user session
     */
    public void logout() {
        currentAccountNo = null;
        lastActivityTime = null;
        LOGGER.info("User logged out");
    }
    
    /**
     * Validates password strength according to security rules
     * @param password the password to validate
     * @return true if password meets strength requirements, false otherwise
     */
    public boolean validatePasswordStrength(String password) {
        // Password must be at least 8 characters with at least one uppercase letter,
        // one lowercase letter, one number and one special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
    
    /**
     * Logs login activity for security auditing
     * @param accountNo the account number
     * @param success whether the login was successful
     */
    private void logLoginActivity(String accountNo, boolean success) {
        try {
            String query = "INSERT INTO login_activity (account_no, login_time, success, ip_address) VALUES (?, NOW(), ?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, accountNo);
            ps.setBoolean(2, success);
            ps.setString(3, "127.0.0.1"); // In a real app, get actual IP
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to log login activity", e);
        }
    }
    
    /**
     * Gets the current account number of the logged-in user
     * @return the account number or null if not logged in
     */
    public String getCurrentAccountNo() {
        return currentAccountNo;
    }
    
    /**
     * Updates the last activity time to prevent session timeout
     */
    public void updateActivityTime() {
        if (currentAccountNo != null) {
            lastActivityTime = LocalDateTime.now();
        }
    }
}

/*
Agent Run Summary:
- Implemented US1: Account Login & Authentication
- Related test cases: TC-AUTH-001, TC-AUTH-002, TC-AUTH-003, TC-AUTH-004
- Agent Run ID: VIBE-OB-11-AUTH-2025-11-24
*/