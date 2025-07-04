package catering.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import catering.util.LogManager;


public class PersistenceManager {

    private static final Logger LOGGER = LogManager.getLogger(PersistenceManager.class);
    private static final String DB_PATH = new File("database", "catering.db").getAbsolutePath();
    private static final String SCRIPT_PATH = new File("database", "catering_init_sqlite.sql").getAbsolutePath();
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private static int lastId;

    // Make constructor private to prevent instantiation
    private PersistenceManager() {
    }

    // Ensure the database file exists
    private static void ensureDbExists() {
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            try {
                // Create the parent directory if it doesn't exist
                dbFile.getParentFile().mkdirs();

                try (Connection conn = DriverManager.getConnection(URL)) {
                    // Connection is automatically closed by try-with-resources
                }

                // After creating the empty database, initialize it with schema
                initializeDatabase(SCRIPT_PATH);

                LOGGER.info("Database created and initialized at: " + dbFile.getAbsolutePath());
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to create database", ex);
            }
        }
    }

    public static boolean initializeDatabase() {
        return initializeDatabase(SCRIPT_PATH);
    }

    /**
     * Initializes the database using an SQL script file
     * 
     * @param scriptFilePath path to the SQL script file
     * @return true if initialization was successful, false otherwise
     */

    public static boolean initializeDatabase(String scriptFilePath) {
        File scriptFile = new File(scriptFilePath);
        if (!scriptFile.exists()) {
            LOGGER.severe("SQL script file not found: " + scriptFile.getAbsolutePath());
            return false;
        }

        try {
            // Read the SQL file content
            String sqlScript = new String(Files.readAllBytes(scriptFile.toPath()), StandardCharsets.UTF_8);

            // Split the script into individual statements using semicolon as delimiter
            String[] statements = sqlScript.split(";");

            // Execute each statement
            try (Connection conn = DriverManager.getConnection(URL);
                 Statement stmt = conn.createStatement()) {

                for (String statement : statements) {
                    String trimmedStmt = statement.trim();
                    if (!trimmedStmt.isEmpty()) {
                        stmt.executeUpdate(trimmedStmt);
                    }
                }

                LOGGER.info("Database initialized successfully from " + scriptFilePath);
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading SQL file: " + scriptFilePath, e);
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing SQL from file: " + scriptFilePath, e);
            return false;
        }
    }

    /**
     * Executes a SQL query with parameters and processes the results with a handler
     * 
     * @param query   SQL query with ? placeholders for parameters
     * @param handler ResultHandler to process each row in the result set
     * @param params  Variable argument list of parameters to bind to the query
     */
    public static void executeQuery(String query, ResultHandler handler, Object... params) {
        ensureDbExists();
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement ps = conn.prepareStatement(query)) {

            // Set parameters if any
            setParameters(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    handler.handle(rs);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error executing query: " + query, ex);
        }
    }

    /**
     * Executes a batch update with a parameterized query
     * 
     * @param parametrizedQuery SQL query with ? placeholders
     * @param itemNumber        Number of items to process in the batch
     * @param handler           BatchUpdateHandler for setting parameters and
     *                          handling generated keys
     * @return Array of row counts for each batch operation
     */
    public static int[] executeBatchUpdate(String parametrizedQuery, int itemNumber, BatchUpdateHandler handler) {
        ensureDbExists();
        int[] result = new int[0];
        try (
                Connection conn = DriverManager.getConnection(URL);
                PreparedStatement ps = conn.prepareStatement(parametrizedQuery, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < itemNumber; i++) {
                handler.handleBatchItem(ps, i);
                ps.addBatch();
            }
            result = ps.executeBatch();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int count = 0;
                while (keys.next()) {
                    handler.handleGeneratedIds(keys, count);
                    count++;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error executing batch update: " + parametrizedQuery, ex);
        }

        return result;
    }

    /**
     * Executes an update (INSERT, UPDATE, DELETE) with parameters
     * 
     * @param update SQL update statement with ? placeholders
     * @param params Variable argument list of parameters to bind to the statement
     * @return Number of rows affected
     */
    public static int executeUpdate(String update, Object... params) {
        ensureDbExists();
        int result = 0;
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement ps = conn.prepareStatement(update, Statement.RETURN_GENERATED_KEYS)) {

            // Set parameters if any
            setParameters(ps, params);

            result = ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    lastId = rs.getInt(1);
                } else {
                    lastId = 0;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL Error executing update: " + update, ex);
        }
        return result;
    }

    /**
     * Helper method to set parameters on a PreparedStatement
     * 
     * @param ps     The PreparedStatement to set parameters on
     * @param params The parameters to set
     * @throws SQLException If there's an error setting parameters
     */
    private static void setParameters(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (params[i] instanceof String) {
                ps.setString(i + 1, (String) params[i]);
            } else if (params[i] instanceof Integer) {
                ps.setInt(i + 1, (Integer) params[i]);
            } else if (params[i] instanceof Boolean) {
                ps.setBoolean(i + 1, (Boolean) params[i]);
            } else if (params[i] instanceof Long) {
                ps.setLong(i + 1, (Long) params[i]);
            } else if (params[i] instanceof Float) {
                ps.setFloat(i + 1, (Float) params[i]);
            } else if (params[i] instanceof Double) {
                ps.setDouble(i + 1, (Double) params[i]);
            } else if (params[i] instanceof java.sql.Date) {
                ps.setDate(i + 1, (java.sql.Date) params[i]);
            } else if (params[i] instanceof java.util.Date) {
                ps.setTimestamp(i + 1, new Timestamp(((java.util.Date) params[i]).getTime()));
            } else if (params[i] instanceof Timestamp) {
                ps.setTimestamp(i + 1, (Timestamp) params[i]);
            } else if (params[i] instanceof Time) {
                ps.setTime(i + 1, (Time) params[i]);
            } else if (params[i] instanceof byte[]) {
                ps.setBytes(i + 1, (byte[]) params[i]);
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * Gets the ID generated by the last executed INSERT statement
     * 
     * @return The generated ID, or 0 if none was generated
     */
    public static int getLastId() {
        return lastId;
    }

    /**
     * Gets a connection to the database
     * 
     * @return A new Connection to the database
     * @throws SQLException If a database error occurs
     */
    public static Connection getConnection() throws SQLException {
        ensureDbExists();
        return DriverManager.getConnection(URL);
    }
}
