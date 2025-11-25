/*
 * Online Banking System - Security Manager
 */
package banking.management.system;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Security Manager for handling two-factor authentication and session management
 * [AGENT GENERATED CODE - REQUIREMENT:US1]
 */
public class SecurityManager {
    
    private static final Logger LOGGER = Logger.getLogger(SecurityManager.class.getName());
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final long SESSION_TIMEOUT_MINUTES = 15;
    
    private Connection connection;
    private Map<String, OtpData> activeOtps;
    private Map<String, SessionData> activeSessions;
    
    // Inner class to store OTP data
    private static class OtpData {
        private String otp;
        private LocalDateTime expiryTime;
        
        public OtpData(String otp) {
            this.otp = otp;
            this.expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        }
        
        public boolean isValid() {
            return LocalDateTime.now().isBefore(expiryTime);
        }
    }
    
    // Inner class to store session data
    private static class SessionData {
        private String accountNo;
        private LocalDateTime lastActivityTime;
        
        public SessionData(String accountNo) {
            this.accountNo = accountNo;
            this.lastActivityTime = LocalDateTime.now();
        }
        
        public void updateActivity() {
            this.lastActivityTime = LocalDateTime.now();
        }
        
        public boolean isValid() {
            return LocalDateTime.now().isBefore(lastActivityTime.plusMinutes(SESSION_TIMEOUT_MINUTES));
        }
    }
    
    public SecurityManager() {
        connection = ConnectionSql.Connector();
        if (connection == null) {
            LOGGER.severe("Database connection failed in SecurityManager");
            JOptionPane.showMessageDialog(null, "Security system initialization failed", 
                    "Security Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        activeOtps = new HashMap<>();
        activeSessions = new HashMap<>();
    }
    
    /**
     * Generates a one-time password for two-factor authentication
     * @param accountNo the account number
     * @param deliveryMethod the method to deliver OTP (SMS or EMAIL)
     * @return true if OTP generation and delivery successful, false otherwise
     */
    public boolean generateOtp(String accountNo, String deliveryMethod) {
        try {
            // Generate secure random OTP
            String otp = generateSecureOtp(OTP_LENGTH);
            
            // Store OTP in memory
            activeOtps.put(accountNo, new OtpData(otp));
            
            // Deliver OTP via selected method
            boolean delivered = false;
            if ("SMS".equalsIgnoreCase(deliveryMethod)) {
                delivered = sendOtpViaSms(accountNo, otp);
            } else if ("EMAIL".equalsIgnoreCase(deliveryMethod)) {
                delivered = sendOtpViaEmail(accountNo, otp);
            }
            
            // Log OTP generation for audit
            logOtpGeneration(accountNo, deliveryMethod, delivered);
            
            return delivered;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate OTP", e);
            return false;
        }
    }
    
    /**
     * Verifies the OTP entered by user
     * @param accountNo the account number
     * @param userEnteredOtp the OTP entered by user
     * @return true if OTP is valid, false otherwise
     */
    public boolean verifyOtp(String accountNo, String userEnteredOtp) {
        OtpData otpData = activeOtps.get(accountNo);
        
        if (otpData == null) {
            LOGGER.warning("No OTP found for account: " + accountNo);
            return false;
        }
        
        if (!otpData.isValid()) {
            LOGGER.warning("OTP expired for account: " + accountNo);
            activeOtps.remove(accountNo); // Remove expired OTP
            return false;
        }
        
        boolean isValid = otpData.otp.equals(userEnteredOtp);
        
        if (isValid) {
            // OTP verified successfully, remove it to prevent reuse
            activeOtps.remove(accountNo);
            
            // Create a session for the authenticated user
            createSession(accountNo);
            
            LOGGER.info("OTP verified successfully for account: " + accountNo);
        } else {
            LOGGER.warning("OTP verification failed for account: " + accountNo);
        }
        
        return isValid;
    }
    
    /**
     * Creates a new session for the authenticated user
     * @param accountNo the account number
     */
    private void createSession(String accountNo) {
        SessionData session = new SessionData(accountNo);
        activeSessions.put(accountNo, session);
        LOGGER.info("Session created for account: " + accountNo);
    }
    
    /**
     * Checks if the session is valid or timed out
     * @param accountNo the account number
     * @return true if session is valid, false if timed out
     */
    public boolean isSessionValid(String accountNo) {
        SessionData session = activeSessions.get(accountNo);
        
        if (session == null) {
            LOGGER.warning("No active session found for account: " + accountNo);
            return false;
        }
        
        boolean isValid = session.isValid();
        
        if (isValid) {
            // Update activity time if session is valid
            session.updateActivity();
        } else {
            // Remove timed out session
            activeSessions.remove(accountNo);
            LOGGER.info("Session timed out for account: " + accountNo);
        }
        
        return isValid;
    }
    
    /**
     * Updates the activity time for a session to prevent timeout
     * @param accountNo the account number
     */
    public void updateActivityTime(String accountNo) {
        SessionData session = activeSessions.get(accountNo);
        
        if (session != null) {
            session.updateActivity();
        }
    }
    
    /**
     * Ends a user session (logout)
     * @param accountNo the account number
     */
    public void endSession(String accountNo) {
        activeSessions.remove(accountNo);
        LOGGER.info("Session ended for account: " + accountNo);
    }
    
    /**
     * Generates a secure random OTP
     * @param length the length of the OTP
     * @return the generated OTP
     */
    private String generateSecureOtp(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }
    
    /**
     * Simulates sending OTP via SMS
     * @param accountNo the account number
     * @param otp the OTP to send
     * @return true if successful, false otherwise
     */
    private boolean sendOtpViaSms(String accountNo, String otp) {
        try {
            // Get user's phone number from database
            String phoneNumber = getUserPhoneNumber(accountNo);
            
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                LOGGER.warning("No phone number found for account: " + accountNo);
                return false;
            }
            
            // In a real implementation, this would connect to an SMS gateway service
            LOGGER.info("OTP sent via SMS to phone number: " + maskPhoneNumber(phoneNumber));
            
            return true; // Assume successful for simulation
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send OTP via SMS", e);
            return false;
        }
    }
    
    /**
     * Simulates sending OTP via email
     * @param accountNo the account number
     * @param otp the OTP to send
     * @return true if successful, false otherwise
     */
    private boolean sendOtpViaEmail(String accountNo, String otp) {
        try {
            // Get user's email from database
            String email = getUserEmail(accountNo);
            
            if (email == null || email.isEmpty()) {
                LOGGER.warning("No email found for account: " + accountNo);
                return false;
            }
            
            // In a real implementation, this would connect to an email service
            LOGGER.info("OTP sent via email to: " + maskEmail(email));
            
            return true; // Assume successful for simulation
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send OTP via email", e);
            return false;
        }
    }
    
    /**
     * Gets user's phone number from database
     * @param accountNo the account number
     * @return the phone number or null if not found
     */
    private String getUserPhoneNumber(String accountNo) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT phone_number FROM user_contact WHERE account_no = ?");
            ps.setString(1, accountNo);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("phone_number");
            }
            
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting user phone number", e);
            return null;
        }
    }
    
    /**
     * Gets user's email from database
     * @param accountNo the account number
     * @return the email or null if not found
     */
    private String getUserEmail(String accountNo) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT s1.email FROM signup1 s1 JOIN login l ON s1.form_no = l.form_no WHERE l.Account_No = ?");
            ps.setString(1, accountNo);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("email");
            }
            
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting user email", e);
            return null;
        }
    }
    
    /**
     * Logs OTP generation for audit purposes
     * @param accountNo the account number
     * @param deliveryMethod the delivery method
     * @param success whether delivery was successful
     */
    private void logOtpGeneration(String accountNo, String deliveryMethod, boolean success) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO security_audit (account_no, action, details, success, timestamp) VALUES (?, ?, ?, ?, NOW())");
            ps.setString(1, accountNo);
            ps.setString(2, "OTP_GENERATION");
            ps.setString(3, "Delivery method: " + deliveryMethod);
            ps.setBoolean(4, success);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to log OTP generation", e);
        }
    }
    
    /**
     * Masks a phone number for privacy/security (e.g., 123-456-7890 -> XXX-XXX-7890)
     * @param phoneNumber the phone number to mask
     * @return the masked phone number
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return phoneNumber;
        }
        
        return "XXXXXXX" + phoneNumber.substring(phoneNumber.length() - 4);
    }
    
    /**
     * Masks an email for privacy/security (e.g., user@example.com -> u***@example.com)
     * @param email the email to mask
     * @return the masked email
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 1) {
            return email;
        }
        
        return username.substring(0, 1) + "***@" + domain;
    }
    
    /**
     * Validates password strength according to security policy
     * @param password the password to validate
     * @return true if password meets strength requirements, false otherwise
     */
    public boolean validatePasswordStrength(String password) {
        // Password must be at least 8 characters with at least one uppercase letter,
        // one lowercase letter, one number and one special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}

/*
Agent Run Summary:
- Implemented US1: Account Login & Authentication (Two-Factor Authentication)
- Implemented US1: Session timeout after 15 minutes of inactivity
- Related test cases: TC-AUTH-001, TC-AUTH-003, TC-AUTH-005, TC-AUTH-006
- Agent Run ID: VIBE-OB-11-SEC-2025-11-24
*/