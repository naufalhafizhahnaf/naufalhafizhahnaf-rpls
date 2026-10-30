package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO ringkasan angka untuk dashboard.
 */
public class StatistikDao {

    private final InsidenDao insidenDao = new InsidenDao();

    public int jumlah(String tabel) {
        if (!"pengunjung".equals(tabel) && !"visitor_pass".equals(tabel)
                && !"sertifikat_induksi".equals(tabel)
                && !"laporan_inspeksi".equals(tabel)
                && !"laporan_insiden".equals(tabel)
                && !"informasi_hsse".equals(tabel)) {
            throw new IllegalArgumentException("Tabel statistik tidak dikenal: " + tabel);
        }
        String sql = "SELECT COUNT(*) FROM " + tabel;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung data " + tabel + ": "
                    + e.getMessage(), e);
        }
    }

    public int jumlahPengunjung() {
        return jumlah("pengunjung");
    }

    public int jumlahVisitorPass() {
        return jumlah("visitor_pass");
    }

    public int jumlahSertifikat() {
        return jumlah("sertifikat_induksi");
    }

    public int jumlahInspeksi() {
        return jumlah("laporan_inspeksi");
    }

    public int jumlahInsiden() {
        return jumlah("laporan_insiden");
    }

    public int jumlahInsiden(StatusInsiden status) {
        return insidenDao.countByStatus(status);
    }

    public int jumlahInformasi() {
        return jumlah("informasi_hsse");
    }

    public List<LaporanInsiden> insidenTerbaru(int batas) {
        List<LaporanInsiden> semua = insidenDao.findAll();
        if (semua.size() <= batas) {
            return semua;
        }
        return semua.subList(0, batas);
    }
}
