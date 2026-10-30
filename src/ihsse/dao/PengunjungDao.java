package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.Pengunjung;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk tabel pengunjung.
 */
public class PengunjungDao {

    private static final String KOLOM =
            "id, nama, nomor_identitas, no_telepon, instansi, tujuan_kunjungan, "
            + "tanggal_daftar, sudah_induksi";

    public void insert(Pengunjung pengunjung) {
        try (Connection connection = Database.getConnection()) {
            insert(connection, pengunjung);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan pengunjung: " + e.getMessage(), e);
        }
    }

    public void insert(Connection connection, Pengunjung pengunjung) throws SQLException {
        String sql = "INSERT INTO pengunjung (" + KOLOM + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pengunjung.getId());
            statement.setString(2, pengunjung.getNama());
            statement.setString(3, pengunjung.getNomorIdentitas());
            statement.setString(4, pengunjung.getNoTelepon());
            statement.setString(5, pengunjung.getInstansi());
            statement.setString(6, pengunjung.getTujuanKunjungan());
            statement.setDate(7, Date.valueOf(pengunjung.getTanggalDaftar()));
            statement.setBoolean(8, pengunjung.isSudahInduksi());
            statement.executeUpdate();
        }
    }

    public Pengunjung findById(String id) {
        try (Connection connection = Database.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mencari pengunjung: " + e.getMessage(), e);
        }
    }

    public Pengunjung findById(Connection connection, String id) throws SQLException {
        String sql = "SELECT " + KOLOM + " FROM pengunjung WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM pengunjung WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal memeriksa pengunjung: " + e.getMessage(), e);
        }
    }

    public List<Pengunjung> findAll() {
        String sql = "SELECT " + KOLOM + " FROM pengunjung "
                + "ORDER BY tanggal_daftar DESC, id DESC";
        List<Pengunjung> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                daftar.add(map(resultSet));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil daftar pengunjung: "
                    + e.getMessage(), e);
        }
    }

    public List<Pengunjung> findBelumInduksi() {
        String sql = "SELECT " + KOLOM + " FROM pengunjung WHERE sudah_induksi = 0 "
                + "ORDER BY tanggal_daftar ASC, id ASC";
        List<Pengunjung> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                daftar.add(map(resultSet));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil pengunjung belum induksi: "
                    + e.getMessage(), e);
        }
    }

    public void markInduksi(String id) {
        try (Connection connection = Database.getConnection()) {
            markInduksi(connection, id);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menandai induksi: " + e.getMessage(), e);
        }
    }

    public void markInduksi(Connection connection, String id) throws SQLException {
        String sql = "UPDATE pengunjung SET sudah_induksi = 1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM pengunjung");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung pengunjung: " + e.getMessage(), e);
        }
    }

    public static Pengunjung map(ResultSet resultSet) throws SQLException {
        return new Pengunjung(
                resultSet.getString("id"),
                resultSet.getString("nama"),
                resultSet.getString("nomor_identitas"),
                resultSet.getString("no_telepon"),
                resultSet.getString("instansi"),
                resultSet.getString("tujuan_kunjungan"),
                resultSet.getDate("tanggal_daftar").toLocalDate(),
                resultSet.getBoolean("sudah_induksi"));
    }
}
