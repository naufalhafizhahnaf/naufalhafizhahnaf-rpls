package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.AreaMerokok;
import ihsse.model.FasilitasKeamanan;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.FasilitasLingkungan;
import ihsse.model.InformasiHSSE;
import ihsse.model.KondisiFasilitas;
import ihsse.model.TingkatRisiko;
import ihsse.model.ZonaRisiko;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk tabel informasi_hsse.
 * Satu tabel dipakai untuk semua turunan InformasiHSSE (single table inheritance).
 */
public class InformasiDao {

    private static final String KOLOM = "kode, kategori, tipe, nama, lokasi, deskripsi, "
            + "jenis, jumlah, kondisi, masa_kedaluwarsa, luas_area, aturan, sanksi, "
            + "tingkat_risiko, prosedur_evakuasi, titik_kumpul, jam_operasional, "
            + "penanggung_jawab";

    public List<InformasiHSSE> findAll() {
        String sql = "SELECT " + KOLOM + " FROM informasi_hsse ORDER BY kode";
        List<InformasiHSSE> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                daftar.add(map(resultSet));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil informasi HSSE: "
                    + e.getMessage(), e);
        }
    }

    public List<InformasiHSSE> findByKategori(String kategori) {
        String sql = "SELECT " + KOLOM + " FROM informasi_hsse "
                + "WHERE kategori = ? ORDER BY kode";
        List<InformasiHSSE> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, kategori);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    daftar.add(map(resultSet));
                }
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil informasi kategori "
                    + kategori + ": " + e.getMessage(), e);
        }
    }

    public InformasiHSSE findByKode(String kode) {
        try (Connection connection = Database.getConnection()) {
            return findByKode(connection, kode);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mencari informasi HSSE: "
                    + e.getMessage(), e);
        }
    }

    public InformasiHSSE findByKode(Connection connection, String kode) throws SQLException {
        String sql = "SELECT " + KOLOM + " FROM informasi_hsse WHERE kode = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, kode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public void insert(InformasiHSSE informasi) {
        String sql = "INSERT INTO informasi_hsse (" + KOLOM + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, informasi.getKode());
            bindDetail(statement, informasi, 2);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan informasi HSSE: "
                    + e.getMessage(), e);
        }
    }

    public void update(InformasiHSSE informasi) {
        String sql = "UPDATE informasi_hsse SET kategori = ?, tipe = ?, nama = ?, "
                + "lokasi = ?, deskripsi = ?, jenis = ?, jumlah = ?, kondisi = ?, "
                + "masa_kedaluwarsa = ?, luas_area = ?, aturan = ?, sanksi = ?, "
                + "tingkat_risiko = ?, prosedur_evakuasi = ?, titik_kumpul = ?, "
                + "jam_operasional = ?, penanggung_jawab = ? WHERE kode = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDetail(statement, informasi, 1);
            statement.setString(18, informasi.getKode());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengubah informasi HSSE: "
                    + e.getMessage(), e);
        }
    }

    public boolean delete(String kode) {
        String cekSql = "SELECT COUNT(*) FROM laporan_inspeksi WHERE fasilitas_kode = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement cek = connection.prepareStatement(cekSql)) {
            cek.setString(1, kode);
            try (ResultSet resultSet = cek.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) > 0) {
                    throw new DataAccessException(
                            "Fasilitas " + kode + " sudah dipakai pada laporan inspeksi "
                            + "sehingga tidak boleh dihapus.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM informasi_hsse WHERE kode = ?")) {
                statement.setString(1, kode);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghapus informasi HSSE: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Dipakai oleh proses inspeksi untuk menyinkronkan kondisi fasilitas.
     */
    public void updateKondisi(Connection connection, String kode,
                              KondisiFasilitas kondisi, LocalDate masaKedaluwarsa)
            throws SQLException {
        String sql = "UPDATE informasi_hsse SET kondisi = ?, masa_kedaluwarsa = ? "
                + "WHERE kode = ? AND tipe = 'FasilitasKeselamatan'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, kondisi.name());
            if (masaKedaluwarsa == null) {
                statement.setNull(2, Types.DATE);
            } else {
                statement.setDate(2, Date.valueOf(masaKedaluwarsa));
            }
            statement.setString(3, kode);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Fasilitas keselamatan tidak ditemukan: " + kode);
            }
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM informasi_hsse");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung informasi HSSE: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Menulis 17 kolom detail mulai dari posisi start.
     * Insert: kode di posisi 1, detail mulai 2.
     * Update: detail mulai 1, kode di posisi 18.
     */
    private void bindDetail(PreparedStatement statement, InformasiHSSE informasi,
                            int start) throws SQLException {
        int i = start;
        statement.setString(i++, informasi.getKategori());
        statement.setString(i++, tipe(informasi));
        statement.setString(i++, informasi.getNama());
        statement.setString(i++, informasi.getLokasi());
        statement.setString(i++, informasi.getDeskripsi());
        statement.setString(i++, informasi.getJenis());
        statement.setNull(i++, Types.INTEGER);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i++, Types.DATE);
        statement.setNull(i++, Types.DOUBLE);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i++, Types.VARCHAR);
        statement.setNull(i, Types.VARCHAR);

        int jumlahIndex = start + 6;
        int kondisiIndex = start + 7;
        int kedaluwarsaIndex = start + 8;
        int luasIndex = start + 9;
        int aturanIndex = start + 10;
        int sanksiIndex = start + 11;
        int risikoIndex = start + 12;
        int prosedurIndex = start + 13;
        int titikKumpulIndex = start + 14;
        int jamIndex = start + 15;
        int penanggungJawabIndex = start + 16;

        if (informasi instanceof AreaMerokok) {
            AreaMerokok area = (AreaMerokok) informasi;
            statement.setDouble(luasIndex, area.getLuasArea());
            statement.setString(aturanIndex, area.getAturan());
            statement.setString(sanksiIndex, area.getSanksi());
        } else if (informasi instanceof FasilitasKeamanan) {
            FasilitasKeamanan fasilitas = (FasilitasKeamanan) informasi;
            statement.setInt(jumlahIndex, fasilitas.getJumlah());
            statement.setString(kondisiIndex, fasilitas.getKondisi().name());
        } else if (informasi instanceof FasilitasKeselamatan) {
            FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) informasi;
            statement.setInt(jumlahIndex, fasilitas.getJumlah());
            statement.setString(kondisiIndex, fasilitas.getKondisi().name());
            if (fasilitas.getMasaKedaluwarsa() != null) {
                statement.setDate(kedaluwarsaIndex,
                        Date.valueOf(fasilitas.getMasaKedaluwarsa()));
            }
        } else if (informasi instanceof ZonaRisiko) {
            ZonaRisiko zona = (ZonaRisiko) informasi;
            statement.setString(risikoIndex, zona.getTingkatRisiko().name());
            statement.setString(prosedurIndex, zona.getProsedurEvakuasi());
            statement.setString(titikKumpulIndex, zona.getTitikKumpul());
        } else if (informasi instanceof FasilitasLingkungan) {
            FasilitasLingkungan fasilitas = (FasilitasLingkungan) informasi;
            statement.setString(jamIndex, fasilitas.getJamOperasional());
            statement.setString(penanggungJawabIndex, fasilitas.getPenanggungJawab());
        }
    }

    private String tipe(InformasiHSSE informasi) {
        if (informasi instanceof AreaMerokok) {
            return "AreaMerokok";
        }
        if (informasi instanceof FasilitasKeamanan) {
            return "FasilitasKeamanan";
        }
        if (informasi instanceof FasilitasKeselamatan) {
            return "FasilitasKeselamatan";
        }
        if (informasi instanceof ZonaRisiko) {
            return "ZonaRisiko";
        }
        if (informasi instanceof FasilitasLingkungan) {
            return "FasilitasLingkungan";
        }
        throw new IllegalArgumentException("Tipe informasi tidak dikenal: "
                + informasi.getClass().getName());
    }

    private InformasiHSSE map(ResultSet resultSet) throws SQLException {
        String kode = resultSet.getString("kode");
        String nama = resultSet.getString("nama");
        String lokasi = resultSet.getString("lokasi");
        String deskripsi = resultSet.getString("deskripsi");
        String tipe = resultSet.getString("tipe");

        if ("AreaMerokok".equals(tipe)) {
            return new AreaMerokok(kode, nama, lokasi, deskripsi,
                    resultSet.getDouble("luas_area"),
                    resultSet.getString("aturan"),
                    resultSet.getString("sanksi"));
        }
        if ("FasilitasKeamanan".equals(tipe)) {
            return new FasilitasKeamanan(kode, nama, lokasi, deskripsi,
                    resultSet.getString("jenis"),
                    resultSet.getInt("jumlah"),
                    kondisi(resultSet.getString("kondisi")));
        }
        if ("FasilitasKeselamatan".equals(tipe)) {
            return new FasilitasKeselamatan(kode, nama, lokasi, deskripsi,
                    resultSet.getString("jenis"),
                    resultSet.getInt("jumlah"),
                    kondisi(resultSet.getString("kondisi")),
                    tanggal(resultSet.getDate("masa_kedaluwarsa")));
        }
        if ("ZonaRisiko".equals(tipe)) {
            String risiko = resultSet.getString("tingkat_risiko");
            return new ZonaRisiko(kode, nama, lokasi, deskripsi,
                    risiko == null ? TingkatRisiko.RENDAH : TingkatRisiko.valueOf(risiko),
                    resultSet.getString("prosedur_evakuasi"),
                    resultSet.getString("titik_kumpul"));
        }
        if ("FasilitasLingkungan".equals(tipe)) {
            return new FasilitasLingkungan(kode, nama, lokasi, deskripsi,
                    resultSet.getString("jenis"),
                    resultSet.getString("jam_operasional"),
                    resultSet.getString("penanggung_jawab"));
        }
        throw new SQLException("Tipe informasi HSSE tidak dikenal: " + tipe);
    }

    private KondisiFasilitas kondisi(String nilai) {
        return nilai == null ? KondisiFasilitas.BAIK : KondisiFasilitas.valueOf(nilai);
    }

    private LocalDate tanggal(Date tanggal) {
        return tanggal == null ? null : tanggal.toLocalDate();
    }
}
