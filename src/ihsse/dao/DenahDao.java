package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.DenahGedung;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk denah gedung dan daftar fasilitas penting per lantai.
 */
public class DenahDao {

    public List<DenahGedung> findAll() {
        String sql = "SELECT lantai, nama_file, deskripsi FROM denah_gedung ORDER BY lantai";
        List<DenahGedung> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int lantai = resultSet.getInt("lantai");
                daftar.add(new DenahGedung(
                        lantai,
                        resultSet.getString("nama_file"),
                        resultSet.getString("deskripsi"),
                        findFasilitas(connection, lantai)));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil denah gedung: "
                    + e.getMessage(), e);
        }
    }

    public void insert(DenahGedung denah) {
        Database.inTransaction(connection -> {
            String sqlDenah = "INSERT INTO denah_gedung (lantai, nama_file, deskripsi) "
                    + "VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sqlDenah)) {
                statement.setInt(1, denah.getLantai());
                statement.setString(2, denah.getNamaFile());
                statement.setString(3, denah.getDeskripsi());
                statement.executeUpdate();
            }

            String sqlFasilitas = "INSERT INTO denah_fasilitas "
                    + "(lantai, urutan, nama_fasilitas) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sqlFasilitas)) {
                int urutan = 1;
                for (String fasilitas : denah.getFasilitasPenting()) {
                    statement.setInt(1, denah.getLantai());
                    statement.setInt(2, urutan++);
                    statement.setString(3, fasilitas);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }

    private List<String> findFasilitas(Connection connection, int lantai)
            throws SQLException {
        String sql = "SELECT nama_fasilitas FROM denah_fasilitas "
                + "WHERE lantai = ? ORDER BY urutan";
        List<String> fasilitas = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, lantai);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    fasilitas.add(resultSet.getString("nama_fasilitas"));
                }
            }
        }
        return fasilitas;
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM denah_gedung");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung denah gedung: "
                    + e.getMessage(), e);
        }
    }
}
