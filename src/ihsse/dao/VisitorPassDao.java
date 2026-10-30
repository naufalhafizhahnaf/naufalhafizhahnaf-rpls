package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.Pengunjung;
import ihsse.model.VisitorPass;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk tabel visitor_pass.
 */
public class VisitorPassDao {

    private final PengunjungDao pengunjungDao = new PengunjungDao();

    public void insert(VisitorPass pass) {
        String sql = "INSERT INTO visitor_pass (nomor_dokumen, tanggal_terbit, "
                + "diterbitkan_oleh, pengunjung_id, tanggal_kunjungan, jam_masuk, "
                + "masa_berlaku_hari, area_tujuan) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pass.getNomorDokumen());
            statement.setDate(2, Date.valueOf(pass.getTanggalTerbit()));
            statement.setString(3, pass.getDiterbitkanOleh());
            statement.setString(4, pass.getPengunjung().getId());
            statement.setDate(5, Date.valueOf(pass.getTanggalKunjungan()));
            statement.setTime(6, Time.valueOf(pass.getJamMasuk()));
            statement.setInt(7, pass.getMasaBerlakuHari());
            statement.setString(8, pass.getAreaTujuan());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan visitor pass: "
                    + e.getMessage(), e);
        }
    }

    public List<VisitorPass> findAll() {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "pengunjung_id, tanggal_kunjungan, jam_masuk, masa_berlaku_hari, "
                + "area_tujuan FROM visitor_pass "
                + "ORDER BY tanggal_terbit DESC, nomor_dokumen DESC";
        List<VisitorPass> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Pengunjung pengunjung = pengunjungDao.findById(
                        connection, resultSet.getString("pengunjung_id"));
                if (pengunjung == null) {
                    continue;
                }
                daftar.add(new VisitorPass(
                        resultSet.getString("nomor_dokumen"),
                        resultSet.getDate("tanggal_terbit").toLocalDate(),
                        resultSet.getString("diterbitkan_oleh"),
                        pengunjung,
                        resultSet.getDate("tanggal_kunjungan").toLocalDate(),
                        resultSet.getTime("jam_masuk").toLocalTime(),
                        resultSet.getInt("masa_berlaku_hari"),
                        resultSet.getString("area_tujuan")));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil daftar visitor pass: "
                    + e.getMessage(), e);
        }
    }

    public List<VisitorPass> findByPengunjung(String pengunjungId) {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "pengunjung_id, tanggal_kunjungan, jam_masuk, masa_berlaku_hari, "
                + "area_tujuan FROM visitor_pass WHERE pengunjung_id = ? "
                + "ORDER BY tanggal_terbit DESC, nomor_dokumen DESC";
        List<VisitorPass> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pengunjungId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Pengunjung pengunjung = pengunjungDao.findById(
                            connection, resultSet.getString("pengunjung_id"));
                    if (pengunjung == null) {
                        continue;
                    }
                    daftar.add(new VisitorPass(
                            resultSet.getString("nomor_dokumen"),
                            resultSet.getDate("tanggal_terbit").toLocalDate(),
                            resultSet.getString("diterbitkan_oleh"),
                            pengunjung,
                            resultSet.getDate("tanggal_kunjungan").toLocalDate(),
                            resultSet.getTime("jam_masuk").toLocalTime(),
                            resultSet.getInt("masa_berlaku_hari"),
                            resultSet.getString("area_tujuan")));
                }
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil visitor pass pengunjung: "
                    + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM visitor_pass");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung visitor pass: "
                    + e.getMessage(), e);
        }
    }
}
