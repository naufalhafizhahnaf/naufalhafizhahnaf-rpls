package ihsse.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Pengelola koneksi JDBC ke MySQL/MariaDB XAMPP.
 *
 * Konfigurasi dibaca dari file db.properties di folder project. Jika file
 * tidak ditemukan, dipakai nilai default XAMPP (root tanpa password).
 */
public final class Database {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/ihsse_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties properties = new Properties();
        properties.setProperty("db.url", DEFAULT_URL);
        properties.setProperty("db.user", DEFAULT_USER);
        properties.setProperty("db.password", DEFAULT_PASSWORD);

        File file = new File("db.properties");
        if (file.isFile()) {
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                System.err.println("Gagal membaca db.properties: " + e.getMessage());
            }
        } else {
            try (InputStream input = Database.class.getResourceAsStream("/db.properties")) {
                if (input != null) {
                    try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        properties.load(reader);
                    }
                }
            } catch (IOException e) {
                System.err.println("Gagal membaca db.properties dari classpath: "
                        + e.getMessage());
            }
        }

        URL = System.getProperty("ihsse.db.url", properties.getProperty("db.url"));
        USER = System.getProperty("ihsse.db.user", properties.getProperty("db.user"));
        PASSWORD = System.getProperty("ihsse.db.password",
                properties.getProperty("db.password"));

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "Driver MySQL belum ada. Pastikan lib/mysql-connector-j-8.3.0.jar "
                    + "masuk ke classpath project.");
        }
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Menguji koneksi dan mengembalikan versi server database.
     */
    public static String testConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            return connection.getMetaData().getDatabaseProductName() + " "
                    + connection.getMetaData().getDatabaseProductVersion();
        }
    }

    public static String getUrl() {
        return URL;
    }

    public static String getUser() {
        return USER;
    }

    /**
     * Menjalankan beberapa operasi DAO dalam satu transaksi.
     */
    public static <T> T inTransaction(TransactionWork<T> work) {
        try (Connection connection = getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    // koneksi akan ditutup oleh try-with-resources
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaksi database gagal: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
