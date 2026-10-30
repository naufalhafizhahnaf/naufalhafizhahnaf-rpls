package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO untuk sequence nomor dokumen agar nomor tetap unik
 * walaupun aplikasi ditutup dan dibuka kembali.
 */
public class NomorDao {

    public int next(String nama) {
        try {
            return Database.inTransaction(connection -> ambilBerikutnya(connection, nama));
        } catch (DataAccessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DataAccessException("Gagal membuat nomor urut " + nama + ": "
                    + e.getMessage(), e);
        }
    }

    private int ambilBerikutnya(Connection connection, String nama) throws SQLException {
        int nilai = 0;
        boolean ada = false;

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT nilai FROM nomor_urut WHERE nama = ? FOR UPDATE")) {
            statement.setString(1, nama);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    nilai = resultSet.getInt("nilai");
                    ada = true;
                }
            }
        }

        if (!ada) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO nomor_urut (nama, nilai) VALUES (?, 0)")) {
                statement.setString(1, nama);
                statement.executeUpdate();
            }
        }

        nilai++;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE nomor_urut SET nilai = ? WHERE nama = ?")) {
            statement.setInt(1, nilai);
            statement.setString(2, nama);
            statement.executeUpdate();
        }
        return nilai;
    }
}
