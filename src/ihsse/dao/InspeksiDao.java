package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.InformasiHSSE;
import ihsse.model.KondisiFasilitas;
import ihsse.model.LaporanInspeksi;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk laporan inspeksi fasilitas.
 */
public class InspeksiDao {

    private final InformasiDao informasiDao = new InformasiDao();

    public void insert(LaporanInspeksi laporan) {
        try (Connection connection = Database.getConnection()) {
            insert(connection, laporan);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan laporan inspeksi: "
                    + e.getMessage(), e);
        }
    }

    public void insert(Connection connection, LaporanInspeksi laporan)
            throws SQLException {
        String sql = "INSERT INTO laporan_inspeksi (nomor_dokumen, tanggal_terbit, "
                + "diterbitkan_oleh, fasilitas_kode, tanggal_cek, kondisi, "
                + "masa_kedaluwarsa, nama_petugas, tindakan, catatan) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, laporan.getNomorDokumen());
            statement.setDate(2, Date.valueOf(laporan.getTanggalTerbit()));
            statement.setString(3, laporan.getDiterbitkanOleh());
            statement.setString(4, laporan.getFasilitas().getKode());
            statement.setDate(5, Date.valueOf(laporan.getTanggalCek()));
            statement.setString(6, laporan.getKondisi().name());
            if (laporan.getMasaKedaluwarsa() == null) {
                statement.setNull(7, Types.DATE);
            } else {
                statement.setDate(7, Date.valueOf(laporan.getMasaKedaluwarsa()));
            }
            statement.setString(8, laporan.getNamaPetugas());
            statement.setString(9, laporan.getTindakan());
            statement.setString(10, laporan.getCatatan());
            statement.executeUpdate();
        }
    }

    public List<LaporanInspeksi> findAll() {
        String sql = "SELECT nomor_dokumen, tanggal_terbit, diterbitkan_oleh, "
                + "fasilitas_kode, tanggal_cek, kondisi, masa_kedaluwarsa, "
                + "nama_petugas, tindakan, catatan FROM laporan_inspeksi "
                + "ORDER BY tanggal_terbit DESC, nomor_dokumen DESC";
        List<LaporanInspeksi> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String kodeFasilitas = resultSet.getString("fasilitas_kode");
                InformasiHSSE informasi = informasiDao.findByKode(
                        connection, kodeFasilitas);
                if (!(informasi instanceof FasilitasKeselamatan)) {
                    continue;
                }
                daftar.add(map(resultSet, (FasilitasKeselamatan) informasi));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil laporan inspeksi: "
                    + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM laporan_inspeksi");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung laporan inspeksi: "
                    + e.getMessage(), e);
        }
    }

    private LaporanInspeksi map(ResultSet resultSet, FasilitasKeselamatan fasilitas)
            throws SQLException {
        Date kedaluwarsa = resultSet.getDate("masa_kedaluwarsa");
        return new LaporanInspeksi(
                resultSet.getString("nomor_dokumen"),
                resultSet.getDate("tanggal_terbit").toLocalDate(),
                resultSet.getString("diterbitkan_oleh"),
                fasilitas,
                resultSet.getDate("tanggal_cek").toLocalDate(),
                KondisiFasilitas.valueOf(resultSet.getString("kondisi")),
                kedaluwarsa == null ? null : kedaluwarsa.toLocalDate(),
                resultSet.getString("nama_petugas"),
                resultSet.getString("tindakan"),
                resultSet.getString("catatan"));
    }
}
