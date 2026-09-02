package parking.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton JDBC connection manager for campus_parking MySQL database.
 */
public class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/campus_parking?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "FORZA_FERRARI";   // MySQL passwordySQL password

    private static Connection connection = null;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found. Add mysql-connector-j to /lib folder.\n" + e.getMessage());
            }
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException ignored) {}
    }

    /** Quick connectivity test — returns null on success, error message on failure. */
    public static String testConnection() {
        try {
            Connection c = getConnection();
            if (c != null && !c.isClosed()) return null;
            return "Connection returned null.";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }
}
