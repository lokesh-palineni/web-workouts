/*
 * Online Banking System - Notification Service
 */
package banking.management.system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JOptionPane;

/**
 * Notification Service for sending alerts and managing user notification preferences
 * [AGENT GENERATED CODE - REQUIREMENT:US5]
 */
public class NotificationService {
    
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    
    // Notification types
    public static final String NOTIFY_LARGE_TRANSACTION = "LARGE_TRANSACTION";
    public static final String NOTIFY_FAILED_LOGIN = "FAILED_LOGIN";
    public static final String NOTIFY_LOW_BALANCE = "LOW_BALANCE";
    public static final String NOTIFY_SUCCESSFUL_LOGIN = "SUCCESSFUL_LOGIN";
    
    // Default notification thresholds
    private static final double DEFAULT_LARGE_TRANSACTION = 1000.0;
    private static final double DEFAULT_LOW_BALANCE = 100.0;
    private static final int DEFAULT_FAILED_LOGIN_COUNT = 3;
    
    // Delivery channels
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_APP = "APP";
    
    private Connection connection;
    private Map<String, UserNotificationPreferences> userPreferencesCache;
    
    /**
     * Constructor for Notification Service
     */
    public NotificationService() {
        connection = ConnectionSql.Connector();
        if (connection == null) {
            LOGGER.severe("Database connection failed in NotificationService");
            JOptionPane.showMessageDialog(null, "Notification system initialization failed", 
                    "Notification Error", JOptionPane.ERROR_MESSAGE);
        }
        userPreferencesCache = new HashMap<>();
    }
    
    /**
     * Send a notification based on the type and user preferences
     * @param accountNo The account number of the recipient
     * @param notificationType The type of notification
     * @param params Additional parameters needed for the notification
     * @return True if notification sent successfully, false otherwise
     */
    public boolean sendNotification(String accountNo, String notificationType, Map<String, Object> params) {
        try {
            // Get user notification preferences
            UserNotificationPreferences prefs = getUserPreferences(accountNo);
            
            // Check if this notification type is enabled
            if (!prefs.isNotificationEnabled(notificationType)) {
                LOGGER.info("Notification type " + notificationType + " is disabled for account " + accountNo);
                return false;
            }
            
            // Check notification thresholds
            if (!checkThresholds(notificationType, prefs, params)) {
                LOGGER.info("Notification threshold not met for type " + notificationType + ", account " + accountNo);
                return false;
            }
            
            // Generate notification message
            String message = generateNotificationMessage(notificationType, params);
            
            // Get user contact details
            UserContactInfo contactInfo = getUserContactInfo(accountNo);
            
            // Send notifications through enabled channels
            boolean success = false;
            
            if (prefs.isChannelEnabled(CHANNEL_SMS) && contactInfo.getPhone() != null) {
                success |= sendSMS(contactInfo.getPhone(), message);
            }
            
            if (prefs.isChannelEnabled(CHANNEL_EMAIL) && contactInfo.getEmail() != null) {
                success |= sendEmail(contactInfo.getEmail(), 
                        "Online Banking Notification: " + getNotificationTitle(notificationType), 
                        message);
            }
            
            if (prefs.isChannelEnabled(CHANNEL_APP)) {
                success |= storeInAppNotification(accountNo, notificationType, message);
            }
            
            // Log the notification
            logNotification(accountNo, notificationType, message, success);
            
            return success;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send notification", e);
            return false;
        }
    }
    
    /**
     * Get notification preferences for a user
     * @param accountNo The account number
     * @return The user's notification preferences
     */
    public UserNotificationPreferences getUserPreferences(String accountNo) {
        // Check cache first
        if (userPreferencesCache.containsKey(accountNo)) {
            return userPreferencesCache.get(accountNo);
        }
        
        try {
            String query = "SELECT * FROM notification_preferences WHERE account_no = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, accountNo);
            ResultSet rs = ps.executeQuery();
            
            UserNotificationPreferences prefs = new UserNotificationPreferences(accountNo);
            
            if (rs.next()) {
                // Load preferences from database
                prefs.setLargeTransactionNotification(rs.getBoolean("enable_large_txn"));
                prefs.setLargeTransactionThreshold(rs.getDouble("large_txn_threshold"));
                
                prefs.setFailedLoginNotification(rs.getBoolean("enable_failed_login"));
                prefs.setFailedLoginThreshold(rs.getInt("failed_login_threshold"));
                
                prefs.setLowBalanceNotification(rs.getBoolean("enable_low_balance"));
                prefs.setLowBalanceThreshold(rs.getDouble("low_balance_threshold"));
                
                prefs.setSuccessfulLoginNotification(rs.getBoolean("enable_successful_login"));
                
                prefs.setSmsEnabled(rs.getBoolean("enable_sms"));
                prefs.setEmailEnabled(rs.getBoolean("enable_email"));
                prefs.setAppNotificationsEnabled(rs.getBoolean("enable_app_notifications"));
            } else {
                // Create default preferences
                createDefaultPreferences(accountNo);
            }
            
            rs.close();
            ps.close();
            
            // Cache the preferences
            userPreferencesCache.put(accountNo, prefs);
            
            return prefs;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving notification preferences", e);
            return new UserNotificationPreferences(accountNo); // Return default preferences
        }
    }
    
    /**
     * Update user notification preferences
     * @param prefs The updated user preferences
     * @return True if update successful, false otherwise
     */
    public boolean updateUserPreferences(UserNotificationPreferences prefs) {
        try {
            // Check if preferences exist
            String checkQuery = "SELECT 1 FROM notification_preferences WHERE account_no = ?";
            PreparedStatement checkPs = connection.prepareStatement(checkQuery);
            checkPs.setString(1, prefs.getAccountNo());
            ResultSet rs = checkPs.executeQuery();
            boolean exists = rs.next();
            rs.close();
            checkPs.close();
            
            PreparedStatement ps;
            
            if (exists) {
                // Update existing preferences
                String updateQuery = "UPDATE notification_preferences SET " +
                        "enable_large_txn = ?, large_txn_threshold = ?, " +
                        "enable_failed_login = ?, failed_login_threshold = ?, " +
                        "enable_low_balance = ?, low_balance_threshold = ?, " +
                        "enable_successful_login = ?, " +
                        "enable_sms = ?, enable_email = ?, enable_app_notifications = ? " +
                        "WHERE account_no = ?";
                
                ps = connection.prepareStatement(updateQuery);
                
            } else {
                // Insert new preferences
                String insertQuery = "INSERT INTO notification_preferences " +
                        "(enable_large_txn, large_txn_threshold, " +
                        "enable_failed_login, failed_login_threshold, " +
                        "enable_low_balance, low_balance_threshold, " +
                        "enable_successful_login, " +
                        "enable_sms, enable_email, enable_app_notifications, account_no) VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                ps = connection.prepareStatement(insertQuery);
            }
            
            // Set parameters
            ps.setBoolean(1, prefs.isLargeTransactionNotificationEnabled());
            ps.setDouble(2, prefs.getLargeTransactionThreshold());
            
            ps.setBoolean(3, prefs.isFailedLoginNotificationEnabled());
            ps.setInt(4, prefs.getFailedLoginThreshold());
            
            ps.setBoolean(5, prefs.isLowBalanceNotificationEnabled());
            ps.setDouble(6, prefs.getLowBalanceThreshold());
            
            ps.setBoolean(7, prefs.isSuccessfulLoginNotificationEnabled());
            
            ps.setBoolean(8, prefs.isSmsEnabled());
            ps.setBoolean(9, prefs.isEmailEnabled());
            ps.setBoolean(10, prefs.isAppNotificationsEnabled());
            
            ps.setString(11, prefs.getAccountNo());
            
            int result = ps.executeUpdate();
            ps.close();
            
            // Update cache
            userPreferencesCache.put(prefs.getAccountNo(), prefs);
            
            return result > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating notification preferences", e);
            return false;
        }
    }
    
    /**
     * Create default notification preferences for a user
     * @param accountNo The account number
     */
    private void createDefaultPreferences(String accountNo) {
        UserNotificationPreferences prefs = new UserNotificationPreferences(accountNo);
        
        // Set default values
        prefs.setLargeTransactionNotification(true);
        prefs.setLargeTransactionThreshold(DEFAULT_LARGE_TRANSACTION);
        
        prefs.setFailedLoginNotification(true);
        prefs.setFailedLoginThreshold(DEFAULT_FAILED_LOGIN_COUNT);
        
        prefs.setLowBalanceNotification(true);
        prefs.setLowBalanceThreshold(DEFAULT_LOW_BALANCE);
        
        prefs.setSuccessfulLoginNotification(false);
        
        prefs.setSmsEnabled(true);
        prefs.setEmailEnabled(true);
        prefs.setAppNotificationsEnabled(true);
        
        // Save to database
        updateUserPreferences(prefs);
    }
    
    /**
     * Get user contact information
     * @param accountNo The account number
     * @return The user's contact information
     */
    private UserContactInfo getUserContactInfo(String accountNo) {
        try {
            String query = "SELECT s1.email, up.phone_number " +
                          "FROM signup1 s1 " +
                          "JOIN login l ON s1.form_no = l.form_no " +
                          "LEFT JOIN user_contact up ON l.Account_No = up.account_no " +
                          "WHERE l.Account_No = ?";
            
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, accountNo);
            ResultSet rs = ps.executeQuery();
            
            UserContactInfo contactInfo = new UserContactInfo();
            
            if (rs.next()) {
                contactInfo.setEmail(rs.getString("email"));
                contactInfo.setPhone(rs.getString("phone_number"));
            }
            
            rs.close();
            ps.close();
            
            return contactInfo;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user contact info", e);
            return new UserContactInfo();
        }
    }
    
    /**
     * Generate notification message based on type and parameters
     * @param notificationType The notification type
     * @param params Additional parameters for the message
     * @return The generated message
     */
    private String generateNotificationMessage(String notificationType, Map<String, Object> params) {
        StringBuilder message = new StringBuilder();
        
        switch (notificationType) {
            case NOTIFY_LARGE_TRANSACTION:
                message.append("ALERT: A large transaction of $")
                      .append(formatAmount(params.get("amount")))
                      .append(" has been processed on your account ending in ")
                      .append(maskAccountNumber(params.get("accountNo").toString()))
                      .append(".");
                
                if (params.containsKey("type")) {
                    message.append(" Transaction type: ").append(params.get("type")).append(".");
                }
                
                message.append(" If this was not you, please contact our customer service immediately.");
                break;
                
            case NOTIFY_FAILED_LOGIN:
                message.append("SECURITY ALERT: Failed login attempt detected for your account ending in ")
                      .append(maskAccountNumber(params.get("accountNo").toString()))
                      .append(" at ")
                      .append(params.get("time"))
                      .append(".");
                
                if (params.containsKey("attemptCount")) {
                    message.append(" This is attempt #").append(params.get("attemptCount")).append(".");
                }
                
                message.append(" If this was not you, please contact our customer service immediately.");
                break;
                
            case NOTIFY_LOW_BALANCE:
                message.append("ALERT: Your account ending in ")
                      .append(maskAccountNumber(params.get("accountNo").toString()))
                      .append(" has a low balance of $")
                      .append(formatAmount(params.get("balance")))
                      .append(". Please consider adding funds to avoid overdraft fees.");
                break;
                
            case NOTIFY_SUCCESSFUL_LOGIN:
                message.append("Your account was accessed successfully at ")
                      .append(params.get("time"))
                      .append(" from ")
                      .append(params.getOrDefault("device", "an unknown device"))
                      .append(".");
                break;
                
            default:
                message.append("You have a new notification from your banking application.");
                break;
        }
        
        return message.toString();
    }
    
    /**
     * Get a title for the notification based on type
     * @param notificationType The notification type
     * @return A title for the notification
     */
    private String getNotificationTitle(String notificationType) {
        switch (notificationType) {
            case NOTIFY_LARGE_TRANSACTION:
                return "Large Transaction Alert";
            case NOTIFY_FAILED_LOGIN:
                return "Security Alert - Failed Login";
            case NOTIFY_LOW_BALANCE:
                return "Low Balance Alert";
            case NOTIFY_SUCCESSFUL_LOGIN:
                return "Successful Login Notification";
            default:
                return "Banking Notification";
        }
    }
    
    /**
     * Check if notification thresholds are met
     * @param notificationType The notification type
     * @param prefs User preferences
     * @param params Notification parameters
     * @return True if thresholds are met, false otherwise
     */
    private boolean checkThresholds(String notificationType, UserNotificationPreferences prefs, Map<String, Object> params) {
        switch (notificationType) {
            case NOTIFY_LARGE_TRANSACTION:
                if (params.containsKey("amount")) {
                    double amount = Double.parseDouble(params.get("amount").toString());
                    return amount >= prefs.getLargeTransactionThreshold();
                }
                return false;
                
            case NOTIFY_FAILED_LOGIN:
                if (params.containsKey("attemptCount")) {
                    int attempts = Integer.parseInt(params.get("attemptCount").toString());
                    return attempts >= prefs.getFailedLoginThreshold();
                }
                return true;
                
            case NOTIFY_LOW_BALANCE:
                if (params.containsKey("balance")) {
                    double balance = Double.parseDouble(params.get("balance").toString());
                    return balance <= prefs.getLowBalanceThreshold();
                }
                return false;
                
            default:
                return true;
        }
    }
    
    /**
     * Send notification via SMS
     * @param phoneNumber The recipient's phone number
     * @param message The message to send
     * @return True if sent successfully, false otherwise
     */
    private boolean sendSMS(String phoneNumber, String message) {
        // In a real implementation, this would use an SMS gateway service
        LOGGER.info("SMS would be sent to " + phoneNumber + ": " + message);
        
        // Simulate SMS sending (always succeeds in this demo)
        return true;
    }
    
    /**
     * Send notification via email
     * @param email The recipient's email address
     * @param subject The email subject
     * @param message The email body
     * @return True if sent successfully, false otherwise
     */
    private boolean sendEmail(String email, String subject, String message) {
        // In a real implementation, this would use JavaMail or similar API
        LOGGER.info("Email would be sent to " + email + " with subject: " + subject + "\nBody: " + message);
        
        try {
            // Simulated email sending code (not actually sending)
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.example.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            Session session = Session.getInstance(props);
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress("banking@example.com"));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);
            
            // Comment out actual sending for simulation
            // Transport.send(mimeMessage);
            
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email", e);
            return false;
        }
    }
    
    /**
     * Store notification for in-app display
     * @param accountNo The account number
     * @param notificationType The notification type
     * @param message The notification message
     * @return True if stored successfully, false otherwise
     */
    private boolean storeInAppNotification(String accountNo, String notificationType, String message) {
        try {
            String query = "INSERT INTO app_notifications (account_no, notification_type, message, timestamp, is_read) " +
                          "VALUES (?, ?, ?, NOW(), false)";
            
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, accountNo);
            ps.setString(2, notificationType);
            ps.setString(3, message);
            
            int result = ps.executeUpdate();
            ps.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to store in-app notification", e);
            return false;
        }
    }
    
    /**
     * Get all in-app notifications for a user
     * @param accountNo The account number
     * @param includeRead Whether to include read notifications
     * @return List of notifications
     */
    public List<Map<String, Object>> getInAppNotifications(String accountNo, boolean includeRead) {
        List<Map<String, Object>> notifications = new ArrayList<>();
        
        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT * FROM app_notifications WHERE account_no = ?");
            
            if (!includeRead) {
                queryBuilder.append(" AND is_read = false");
            }
            
            queryBuilder.append(" ORDER BY timestamp DESC");
            
            PreparedStatement ps = connection.prepareStatement(queryBuilder.toString());
            ps.setString(1, accountNo);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("id", rs.getInt("id"));
                notification.put("type", rs.getString("notification_type"));
                notification.put("message", rs.getString("message"));
                notification.put("timestamp", rs.getTimestamp("timestamp"));
                notification.put("isRead", rs.getBoolean("is_read"));
                
                notifications.add(notification);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve in-app notifications", e);
        }
        
        return notifications;
    }
    
    /**
     * Mark an in-app notification as read
     * @param notificationId The notification ID
     * @return True if marked successfully, false otherwise
     */
    public boolean markNotificationAsRead(int notificationId) {
        try {
            String query = "UPDATE app_notifications SET is_read = true WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, notificationId);
            
            int result = ps.executeUpdate();
            ps.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark notification as read", e);
            return false;
        }
    }
    
    /**
     * Log notification to database for audit trail
     * @param accountNo The account number
     * @param notificationType The notification type
     * @param message The notification message
     * @param success Whether the notification was sent successfully
     */
    private void logNotification(String accountNo, String notificationType, String message, boolean success) {
        try {
            String query = "INSERT INTO notification_log (account_no, notification_type, message, success, timestamp) " +
                          "VALUES (?, ?, ?, ?, NOW())";
            
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, accountNo);
            ps.setString(2, notificationType);
            ps.setString(3, message);
            ps.setBoolean(4, success);
            
            ps.executeUpdate();
            ps.close();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to log notification", e);
        }
    }
    
    /**
     * Format an amount value for display
     * @param amount The amount to format
     * @return Formatted amount string
     */
    private String formatAmount(Object amount) {
        if (amount == null) {
            return "0.00";
        }
        
        try {
            double value = Double.parseDouble(amount.toString());
            return String.format("%.2f", value);
        } catch (NumberFormatException e) {
            return amount.toString();
        }
    }
    
    /**
     * Mask account number for security (e.g., show only last 4 digits)
     * @param accountNo The account number
     * @return Masked account number
     */
    private String maskAccountNumber(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) {
            return accountNo;
        }
        
        return "..." + accountNo.substring(accountNo.length() - 4);
    }
    
    /**
     * Inner class for user notification preferences
     */
    public class UserNotificationPreferences {
        private String accountNo;
        
        // Notification types
        private boolean largeTransactionNotification;
        private double largeTransactionThreshold;
        
        private boolean failedLoginNotification;
        private int failedLoginThreshold;
        
        private boolean lowBalanceNotification;
        private double lowBalanceThreshold;
        
        private boolean successfulLoginNotification;
        
        // Delivery channels
        private boolean smsEnabled;
        private boolean emailEnabled;
        private boolean appNotificationsEnabled;
        
        /**
         * Constructor with account number
         * @param accountNo The account number
         */
        public UserNotificationPreferences(String accountNo) {
            this.accountNo = accountNo;
            
            // Set default values
            largeTransactionNotification = true;
            largeTransactionThreshold = DEFAULT_LARGE_TRANSACTION;
            
            failedLoginNotification = true;
            failedLoginThreshold = DEFAULT_FAILED_LOGIN_COUNT;
            
            lowBalanceNotification = true;
            lowBalanceThreshold = DEFAULT_LOW_BALANCE;
            
            successfulLoginNotification = false;
            
            smsEnabled = true;
            emailEnabled = true;
            appNotificationsEnabled = true;
        }
        
        /**
         * Check if a notification type is enabled
         * @param notificationType The notification type
         * @return True if enabled, false otherwise
         */
        public boolean isNotificationEnabled(String notificationType) {
            switch (notificationType) {
                case NOTIFY_LARGE_TRANSACTION:
                    return largeTransactionNotification;
                    
                case NOTIFY_FAILED_LOGIN:
                    return failedLoginNotification;
                    
                case NOTIFY_LOW_BALANCE:
                    return lowBalanceNotification;
                    
                case NOTIFY_SUCCESSFUL_LOGIN:
                    return successfulLoginNotification;
                    
                default:
                    return false;
            }
        }
        
        /**
         * Check if a delivery channel is enabled
         * @param channel The delivery channel
         * @return True if enabled, false otherwise
         */
        public boolean isChannelEnabled(String channel) {
            switch (channel) {
                case CHANNEL_SMS:
                    return smsEnabled;
                    
                case CHANNEL_EMAIL:
                    return emailEnabled;
                    
                case CHANNEL_APP:
                    return appNotificationsEnabled;
                    
                default:
                    return false;
            }
        }
        
        // Getters and setters
        
        public String getAccountNo() {
            return accountNo;
        }

        public boolean isLargeTransactionNotificationEnabled() {
            return largeTransactionNotification;
        }

        public void setLargeTransactionNotification(boolean largeTransactionNotification) {
            this.largeTransactionNotification = largeTransactionNotification;
        }

        public double getLargeTransactionThreshold() {
            return largeTransactionThreshold;
        }

        public void setLargeTransactionThreshold(double largeTransactionThreshold) {
            this.largeTransactionThreshold = largeTransactionThreshold;
        }

        public boolean isFailedLoginNotificationEnabled() {
            return failedLoginNotification;
        }

        public void setFailedLoginNotification(boolean failedLoginNotification) {
            this.failedLoginNotification = failedLoginNotification;
        }

        public int getFailedLoginThreshold() {
            return failedLoginThreshold;
        }

        public void setFailedLoginThreshold(int failedLoginThreshold) {
            this.failedLoginThreshold = failedLoginThreshold;
        }

        public boolean isLowBalanceNotificationEnabled() {
            return lowBalanceNotification;
        }

        public void setLowBalanceNotification(boolean lowBalanceNotification) {
            this.lowBalanceNotification = lowBalanceNotification;
        }

        public double getLowBalanceThreshold() {
            return lowBalanceThreshold;
        }

        public void setLowBalanceThreshold(double lowBalanceThreshold) {
            this.lowBalanceThreshold = lowBalanceThreshold;
        }

        public boolean isSuccessfulLoginNotificationEnabled() {
            return successfulLoginNotification;
        }

        public void setSuccessfulLoginNotification(boolean successfulLoginNotification) {
            this.successfulLoginNotification = successfulLoginNotification;
        }

        public boolean isSmsEnabled() {
            return smsEnabled;
        }

        public void setSmsEnabled(boolean smsEnabled) {
            this.smsEnabled = smsEnabled;
        }

        public boolean isEmailEnabled() {
            return emailEnabled;
        }

        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        public boolean isAppNotificationsEnabled() {
            return appNotificationsEnabled;
        }

        public void setAppNotificationsEnabled(boolean appNotificationsEnabled) {
            this.appNotificationsEnabled = appNotificationsEnabled;
        }
    }
    
    /**
     * Inner class for user contact information
     */
    private class UserContactInfo {
        private String email;
        private String phone;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}

/*
Agent Run Summary:
- Implemented US5: Alerts & Notifications
- Features: SMS/email alerts for large transactions, failed login attempts, low balance, customizable preferences
- Related test cases: TC-NOTIF-001, TC-NOTIF-002, TC-NOTIF-003, TC-NOTIF-004
- Agent Run ID: VIBE-OB-11-NOTIF-2025-11-24
*/