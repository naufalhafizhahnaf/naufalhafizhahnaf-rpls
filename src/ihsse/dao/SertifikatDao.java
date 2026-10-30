package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.Pengunjung;
import ihsse.model.SertifikatInduksi;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk sertifikat induksi dan materi induksi.
 */
public class SertifikatDao {

    private final PengunjungDao pengunjungDao = new PengunjungDao();

    public void insert(SertifikatInduksi sertifikat) {
        Database.inTransaction(connection -> {
            insert(connection, sertifikat);
            return null;
        });
    }

    public void insert(Connection connection, SertifikatInduksi sertifikat)
            throws SQLException {
        String sql = "INSERT INTO sertifikat_induksi (nomor_dokumen, tanggal_terbit, "
                + "diterbitkan_oleh, pengunjung_id, tanggal_selesai, nama_fasilitator) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sertifikat.getNomorDokumen());
            statement.setDate(2, Date.valueOf(sertifikat.getTanggalTerbit()));
            statement.setString(3, sertifikat.getDiterbitkanOleh());
            statement.setString(4, sertifikat.getPengunjung().getId());
            statement.setDate(5, Date.valueOf(sertifikat.getTanggalSelesai()));
            statement.setString(6, sertifikat.getNamaFasilitator());
            statement.executeUpdate();
        }

        String sqlMateri = "INSERT INTO sertifikat_materi "
                + "(nomor_sertifikat, urutan, materi) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sqlMateri)) {
            int urutan = 1;
            for (String materi : sertifikat.getMateri()) {
                statement.setString(1, sertifikat.getNomorDokumen());
                statement.setInt(2, urutan++);
                statement.setString(3, materi);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<SertifikatInduksi> findAll() {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "pengunjung_id, tanggal_selesai, nama_fasilitator "
                + "FROM sertifikat_induksi "
                + "ORDER BY tanggal_terbit DESC, nomor_dokumen DESC";
        List<SertifikatInduksi> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Pengunjung pengunjung = pengunjungDao.findById(
                        connection, resultSet.getString("pengunjung_id"));
                if (pengunjung == null) {
                    continue;
                }
                daftar.add(new SertifikatInduksi(
                        resultSet.getString("nomor_dokumen"),
                        resultSet.getDate("tanggal_terbit").toLocalDate(),
                        resultSet.getString("diterbitkan_oleh"),
                        pengunjung,
                        materi(connection, resultSet.getString("nomor_dokumen")),
                        resultSet.getDate("tanggal_selesai").toLocalDate(),
                        resultSet.getString("nama_fasilitator")));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil sertifikat induksi: "
                    + e.getMessage(), e);
        }
    }

    public boolean existsByPengunjung(String pengunjungId) {
        String sql = "SELECT 1 FROM sertifikat_induksi WHERE pengunjung_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pengunjungId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal memeriksa sertifikat induksi: "
                    + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sertifikat_induksi");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung sertifikat induksi: "
                    + e.getMessage(), e);
        }
    }

    private List<String> materi(Connection connection, String nomorDokumen)
            throws SQLException {
        String sql = "SELECT materi FROM sertifikat_materi "
                + "WHERE nomor_sertifikat = ? ORDER BY urutan";
        List<String> materi = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nomorDokumen);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    materi.add(resultSet.getString("materi"));
                }
            }
        }
        return materi;
    }
}
