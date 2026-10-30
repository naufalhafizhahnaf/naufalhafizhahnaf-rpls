package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.Admin;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk laporan insiden.
 */
public class InsidenDao {

    private final AdminDao adminDao = new AdminDao();

    public void insert(LaporanInsiden laporan) {
        String sql = "INSERT INTO laporan_insiden (nomor_dokumen, tanggal_terbit, "
                + "diterbitkan_oleh, nama_pelapor, kontak_pelapor, lokasi, deskripsi, "
                + "waktu_kejadian, status, catatan_admin, ditangani_oleh) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, laporan.getNomorDokumen());
            statement.setDate(2, Date.valueOf(laporan.getTanggalTerbit()));
            statement.setString(3, laporan.getDiterbitkanOleh());
            statement.setString(4, laporan.getNamaPelapor());
            statement.setString(5, laporan.getKontakPelapor());
            statement.setString(6, laporan.getLokasi());
            statement.setString(7, laporan.getDeskripsi());
            statement.setTimestamp(8, Timestamp.valueOf(laporan.getWaktuKejadian()));
            statement.setString(9, laporan.getStatus().name());
            statement.setString(10, laporan.getCatatanAdmin());
            if (laporan.getDitanganiOleh() == null) {
                statement.setNull(11, Types.VARCHAR);
            } else {
                statement.setString(11, laporan.getDitanganiOleh().getId());
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan laporan insiden: "
                    + e.getMessage(), e);
        }
    }

    public void update(LaporanInsiden laporan) {
        String sql = "UPDATE laporan_insiden SET status = ?, catatan_admin = ?, "
                + "ditangani_oleh = ? WHERE nomor_dokumen = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, laporan.getStatus().name());
            statement.setString(2, laporan.getCatatanAdmin());
            if (laporan.getDitanganiOleh() == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, laporan.getDitanganiOleh().getId());
            }
            statement.setString(4, laporan.getNomorDokumen());
            if (statement.executeUpdate() == 0) {
                throw new DataAccessException("Laporan insiden tidak ditemukan: "
                        + laporan.getNomorDokumen());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengubah laporan insiden: "
                    + e.getMessage(), e);
        }
    }

    public List<LaporanInsiden> findAll() {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "nama_pelapor, kontak_pelapor, lokasi, deskripsi, waktu_kejadian, "
                + "status, catatan_admin, ditangani_oleh FROM laporan_insiden "
                + "ORDER BY waktu_kejadian DESC, nomor_dokumen DESC";
        List<LaporanInsiden> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                daftar.add(map(resultSet));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil laporan insiden: "
                    + e.getMessage(), e);
        }
    }

    public LaporanInsiden findByNomor(String nomorDokumen) {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "nama_pelapor, kontak_pelapor, lokasi, deskripsi, waktu_kejadian, "
                + "status, catatan_admin, ditangani_oleh FROM laporan_insiden "
                + "WHERE nomor_dokumen = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nomorDokumen);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mencari laporan insiden: "
                    + e.getMessage(), e);
        }
    }

    public List<LaporanInsiden> findByStatus(StatusInsiden status) {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "nama_pelapor, kontak_pelapor, lokasi, deskripsi, waktu_kejadian, "
                + "status, catatan_admin, ditangani_oleh FROM laporan_insiden "
                + "WHERE status = ? ORDER BY waktu_kejadian DESC, nomor_dokumen DESC";
        List<LaporanInsiden> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    daftar.add(map(resultSet));
                }
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil insiden berstatus "
                    + status.getLabel() + ": " + e.getMessage(), e);
        }
    }

    public int count() {
        return countBySql("SELECT COUNT(*) FROM laporan_insiden");
    }

    public int countByStatus(StatusInsiden status) {
        String sql = "SELECT COUNT(*) FROM laporan_insiden WHERE status = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung insiden: "
                    + e.getMessage(), e);
        }
    }

    private int countBySql(String sql) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung insiden: "
                    + e.getMessage(), e);
        }
    }

    private LaporanInsiden map(ResultSet resultSet) throws SQLException {
        String adminId = resultSet.getString("ditangani_oleh");
        Admin admin = adminId == null ? null : adminDao.findById(adminId);
        return new LaporanInsiden(
                resultSet.getString("nomor_dokumen"),
                resultSet.getDate("tanggal_terbit").toLocalDate(),
                resultSet.getString("diterbitkan_oleh"),
                resultSet.getString("nama_pelapor"),
                resultSet.getString("kontak_pelapor"),
                resultSet.getString("lokasi"),
                resultSet.getString("deskripsi"),
                resultSet.getTimestamp("waktu_kejadian").toLocalDateTime(),
                StatusInsiden.valueOf(resultSet.getString("status")),
                resultSet.getString("catatan_admin"),
                admin);
    }
}
