package catering.util;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for date handling in the catering application
 */
public class DateUtils {
    private static final Logger LOGGER = LogManager.getLogger(DateUtils.class);

    /**
     * Safely converts a SQLite Unix timestamp to a java.sql.Date
     * 
     * @param rs         ResultSet to read from
     * @param columnName Name of the column containing the timestamp
     * @return A java.sql.Date object or null if conversion fails
     */
    public static Date getDateFromResultSet(ResultSet rs, String columnName) {
        try {
            // First try getting as a long (most efficient)
            long timestamp = rs.getLong(columnName);

            // Check if the field was NULL in the database
            if (rs.wasNull()) {
                LOGGER.fine(columnName + " is NULL in database");
                return null;
            }

            // Return the date if we got a valid timestamp
            if (timestamp > 0) {
                Date date = new Date(timestamp);
                LOGGER.fine("Successfully parsed " + columnName + " timestamp " + timestamp + " to date: " + date);
                return date;
            } else {
                LOGGER.fine(columnName + " timestamp is zero or negative: " + timestamp);
            }

            // Try string parsing as fallback
            String dateStr = rs.getString(columnName);
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    timestamp = Long.parseLong(dateStr);
                    Date date = new Date(timestamp);
                    LOGGER.fine("Successfully parsed " + columnName + " string " + dateStr + " to date: " + date);
                    return date;
                } catch (NumberFormatException ex) {
                    LOGGER.warning("Invalid Unix timestamp in column " + columnName + ": " + dateStr);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error retrieving Unix timestamp from column " + columnName, ex);
        }
        return null;
    }

    /**
     * Safely converts any text representation to a java.sql.Date
     * 
     * @param dateStr The date string to convert
     * @return A java.sql.Date or null if conversion fails
     */
    public static Date safeValueOf(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Try standard SQL date format
            return Date.valueOf(dateStr);
        } catch (IllegalArgumentException ex) {
            try {
                // Try parsing as timestamp
                long timestamp = Long.parseLong(dateStr);
                return new Date(timestamp);
            } catch (NumberFormatException nfe) {
                LOGGER.warning("Failed to parse date string: " + dateStr);
                return null;
            }
        }
    }
}