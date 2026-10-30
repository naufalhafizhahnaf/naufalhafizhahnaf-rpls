package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.Admin;
import ihsse.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO untuk tabel admin dan proses login.
 */
public class AdminDao {

    /**
     * Mengembalikan Admin jika username dan password cocok, atau null jika salah.
     */
    public Admin login(String username, String password) {
        String sql = "SELECT id, nama, nomor_identitas, no_telepon, username, jabatan, "
                + "password_hash FROM admin WHERE username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                String hash = resultSet.getString("password_hash");
                if (!PasswordUtil.cocok(password, hash)) {
                    return null;
                }
                return map(resultSet);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal memeriksa login admin: " + e.getMessage(), e);
        }
    }

    public Admin findById(String id) {
        String sql = "SELECT id, nama, nomor_identitas, no_telepon, username, jabatan, "
                + "password_hash FROM admin WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mencari admin: " + e.getMessage(), e);
        }
    }

    public Admin findByUsername(String username) {
        String sql = "SELECT id, nama, nomor_identitas, no_telepon, username, jabatan, "
                + "password_hash FROM admin WHERE username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mencari admin: " + e.getMessage(), e);
        }
    }

    private Admin map(ResultSet resultSet) throws SQLException {
        return new Admin(
                resultSet.getString("id"),
                resultSet.getString("nama"),
                resultSet.getString("nomor_identitas"),
                resultSet.getString("no_telepon"),
                resultSet.getString("username"),
                resultSet.getString("jabatan"));
    }
}
