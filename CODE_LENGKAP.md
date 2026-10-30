# Kode Lengkap I-HSSE Safety Induction

Dokumen ini dihasilkan dari source terbaru aplikasi GUI + MySQL.

Kompilasi dari root project:

```bat
dir /s /b src\*.java > sources.txt
javac --release 11 -encoding UTF-8 -cp lib\mysql-connector-j-8.3.0.jar -d build\classes @sources.txt
java -cp "build\classes;lib\mysql-connector-j-8.3.0.jar" ihsse.gui.GuiMain
```

## src/ihsse/dao/AdminDao.java

```java
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
```

## src/ihsse/dao/DenahDao.java

```java
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
```

## src/ihsse/dao/InformasiDao.java

```java
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
```

## src/ihsse/dao/InsidenDao.java

```java
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
```

## src/ihsse/dao/InspeksiDao.java

```java
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
```

## src/ihsse/dao/NomorDao.java

```java
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
```

## src/ihsse/dao/PengunjungDao.java

```java
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
```

## src/ihsse/dao/SertifikatDao.java

```java
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
```

## src/ihsse/dao/StatistikDao.java

```java
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
```

## src/ihsse/dao/VideoDao.java

```java
package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.VideoTutorial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk tabel video_tutorial.
 */
public class VideoDao {

    public List<VideoTutorial> findAll() {
        String sql = "SELECT id, judul, topik, durasi_detik, url "
                + "FROM video_tutorial ORDER BY id";
        List<VideoTutorial> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                daftar.add(new VideoTutorial(
                        resultSet.getString("id"),
                        resultSet.getString("judul"),
                        resultSet.getString("topik"),
                        resultSet.getInt("durasi_detik"),
                        resultSet.getString("url")));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil video tutorial: "
                    + e.getMessage(), e);
        }
    }

    public void insert(VideoTutorial video) {
        String sql = "INSERT INTO video_tutorial (id, judul, topik, durasi_detik, url) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, video.getId());
            statement.setString(2, video.getJudul());
            statement.setString(3, video.getTopik());
            statement.setInt(4, video.getDurasiDetik());
            statement.setString(5, video.getUrl());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan video tutorial: "
                    + e.getMessage(), e);
        }
    }

    public boolean delete(String id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM video_tutorial WHERE id = ?")) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghapus video tutorial: "
                    + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM video_tutorial");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung video tutorial: "
                    + e.getMessage(), e);
        }
    }
}
```

## src/ihsse/dao/VisitorPassDao.java

```java
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
```

## src/ihsse/db/DataAccessException.java

```java
package ihsse.db;

/**
 * Exception runtime untuk kegagalan akses database.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## src/ihsse/db/Database.java

```java
package ihsse.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Pengelola koneksi JDBC ke MySQL/MariaDB XAMPP.
 *
 * Konfigurasi dibaca dari file db.properties di folder project. Jika file
 * tidak ditemukan, dipakai nilai default XAMPP (root tanpa password).
 */
public final class Database {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/ihsse_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties properties = new Properties();
        properties.setProperty("db.url", DEFAULT_URL);
        properties.setProperty("db.user", DEFAULT_USER);
        properties.setProperty("db.password", DEFAULT_PASSWORD);

        File file = new File("db.properties");
        if (file.isFile()) {
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                System.err.println("Gagal membaca db.properties: " + e.getMessage());
            }
        } else {
            try (InputStream input = Database.class.getResourceAsStream("/db.properties")) {
                if (input != null) {
                    try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        properties.load(reader);
                    }
                }
            } catch (IOException e) {
                System.err.println("Gagal membaca db.properties dari classpath: "
                        + e.getMessage());
            }
        }

        URL = System.getProperty("ihsse.db.url", properties.getProperty("db.url"));
        USER = System.getProperty("ihsse.db.user", properties.getProperty("db.user"));
        PASSWORD = System.getProperty("ihsse.db.password",
                properties.getProperty("db.password"));

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "Driver MySQL belum ada. Pastikan lib/mysql-connector-j-8.3.0.jar "
                    + "masuk ke classpath project.");
        }
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Menguji koneksi dan mengembalikan versi server database.
     */
    public static String testConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            return connection.getMetaData().getDatabaseProductName() + " "
                    + connection.getMetaData().getDatabaseProductVersion();
        }
    }

    public static String getUrl() {
        return URL;
    }

    public static String getUser() {
        return USER;
    }

    /**
     * Menjalankan beberapa operasi DAO dalam satu transaksi.
     */
    public static <T> T inTransaction(TransactionWork<T> work) {
        try (Connection connection = getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    // koneksi akan ditutup oleh try-with-resources
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaksi database gagal: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
```

## src/ihsse/gui/DashboardPanel.java

```java
package ihsse.gui;

import ihsse.model.FasilitasKeselamatan;
import ihsse.model.InformasiHSSE;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.StatistikService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard ringkasan angka dan aktivitas terbaru.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private final StatistikService statistikService;
    private final InsidenService insidenService;
    private final InformasiService informasiService;

    private final Map<String, StatCard> cards = new LinkedHashMap<>();
    private final DefaultTableModel insidenModel;
    private final DefaultTableModel fasilitasModel;
    private final JLabel statusLabel = new JLabel(" ");

    public DashboardPanel(StatistikService statistikService,
                          InsidenService insidenService,
                          InformasiService informasiService) {
        this.statistikService = statistikService;
        this.insidenService = insidenService;
        this.informasiService = informasiService;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(2, 3, 14, 14));
        stats.setOpaque(false);
        cards.put("pengunjung", new StatCard("Pengunjung",
                "Total pengunjung terdaftar", new Color(52, 120, 196)));
        cards.put("pass", new StatCard("Visitor Pass",
                "Pass yang sudah diterbitkan", new Color(14, 159, 110)));
        cards.put("sertifikat", new StatCard("Sertifikat Induksi",
                "Peserta yang sudah selesai", new Color(120, 92, 190)));
        cards.put("inspeksi", new StatCard("Laporan Inspeksi",
                "Pemeriksaan fasilitas", new Color(220, 140, 35)));
        cards.put("insiden_baru", new StatCard("Insiden Baru",
                "Menunggu tindak lanjut", new Color(214, 69, 69)));
        cards.put("insiden_selesai", new StatCard("Insiden Selesai",
                "Sudah ditangani admin", new Color(35, 148, 96)));
        for (StatCard card : cards.values()) {
            stats.add(card);
        }
        add(stats, BorderLayout.NORTH);

        JPanel bawah = new JPanel(new GridLayout(1, 2, 16, 0));
        bawah.setOpaque(false);

        insidenModel = new DefaultTableModel(
                new Object[]{"Nomor", "Waktu", "Pelapor", "Lokasi", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bawah.add(buatTabelCard("Laporan Insiden Terbaru",
                "5 laporan terakhir dari pengguna", insidenModel, 4));

        fasilitasModel = new DefaultTableModel(
                new Object[]{"Kode", "Fasilitas", "Kondisi", "Kedaluwarsa"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bawah.add(buatTabelCard("Kondisi Fasilitas Keselamatan",
                "APAR, hydrant, tangga darurat, titik kumpul", fasilitasModel, 2));

        add(bawah, BorderLayout.CENTER);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buatTabelCard(String judul, String keterangan,
                                 DefaultTableModel model, int statusColumn) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = Theme.bold(judul);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel sub = Theme.muted(keterangan);
        sub.setFont(Theme.FONT_SMALL);
        header.add(title);
        header.add(javax.swing.Box.createVerticalStrut(2));
        header.add(sub);
        card.add(header, BorderLayout.NORTH);

        JTable table = Theme.table();
        table.setModel(model);
        table.setRowHeight(28);
        if (statusColumn >= 0 && statusColumn < table.getColumnCount()) {
            table.getColumnModel().getColumn(statusColumn)
                    .setCellRenderer(Theme.statusRenderer());
        }
        Theme.styleTableColumns(table, 150, 140, 130, 160, 90);
        JScrollPane scrollPane = Theme.scroll(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refreshData() {
        cards.get("pengunjung").setValue(statistikService.jumlahPengunjung());
        cards.get("pass").setValue(statistikService.jumlahVisitorPass());
        cards.get("sertifikat").setValue(statistikService.jumlahSertifikat());
        cards.get("inspeksi").setValue(statistikService.jumlahInspeksi());
        cards.get("insiden_baru").setValue(
                statistikService.jumlahInsiden(StatusInsiden.BARU));
        cards.get("insiden_selesai").setValue(
                statistikService.jumlahInsiden(StatusInsiden.SELESAI));

        insidenModel.setRowCount(0);
        for (LaporanInsiden laporan : statistikService.insidenTerbaru(5)) {
            insidenModel.addRow(new Object[]{
                    laporan.getNomorDokumen(),
                    FormatUtil.waktu(laporan.getWaktuKejadian()),
                    laporan.getNamaPelapor(),
                    laporan.getLokasi(),
                    laporan.getStatus().getLabel()
            });
        }

        fasilitasModel.setRowCount(0);
        for (InformasiHSSE informasi
                : informasiService.informasiBerdasarkanKategori("Safety")) {
            if (informasi instanceof FasilitasKeselamatan) {
                FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) informasi;
                fasilitasModel.addRow(new Object[]{
                        fasilitas.getKode(),
                        fasilitas.getNama(),
                        fasilitas.getKondisi().getLabel(),
                        FormatUtil.tanggal(fasilitas.getMasaKedaluwarsa())
                });
            }
        }

        statusLabel.setText("Terakhir diperbarui: "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private static final class StatCard extends JPanel {

        private final JLabel valueLabel = new JLabel("0");

        private StatCard(String judul, String keterangan, Color accent) {
            setLayout(new BorderLayout(0, 6));
            setBackground(Theme.CARD);
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 5, 0, 0, accent),
                    new CompoundBorder(
                            new LineBorder(Theme.BORDER, 1, true),
                            new EmptyBorder(13, 16, 13, 16))));

            valueLabel.setFont(Theme.BIG_NUMBER);
            valueLabel.setForeground(accent);
            JLabel title = new JLabel(judul);
            title.setFont(Theme.FONT_BOLD);
            title.setForeground(Theme.TEXT);
            JLabel hint = new JLabel(keterangan);
            hint.setFont(Theme.FONT_SMALL);
            hint.setForeground(Theme.MUTED);

            JPanel labels = new JPanel();
            labels.setOpaque(false);
            labels.setLayout(new javax.swing.BoxLayout(
                    labels, javax.swing.BoxLayout.Y_AXIS));
            labels.add(title);
            labels.add(hint);

            add(valueLabel, BorderLayout.NORTH);
            add(labels, BorderLayout.CENTER);
        }

        private void setValue(int nilai) {
            valueLabel.setText(String.valueOf(nilai));
        }
    }
}
```

## src/ihsse/gui/DokumenPreviewDialog.java

```java
package ihsse.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Window;
import java.awt.print.PrinterException;

/**
 * Dialog pratinjau dokumen (visitor pass, sertifikat, inspeksi, insiden)
 * dengan tombol cetak dan salin.
 */
public final class DokumenPreviewDialog extends JDialog {

    private final JTextArea textArea;

    private DokumenPreviewDialog(Window owner, String judul, String isi) {
        super(owner, judul, Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 12));
        getRootPane().setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));

        add(Theme.title(judul), BorderLayout.NORTH);

        textArea = new JTextArea(isi);
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        textArea.setLineWrap(false);
        textArea.setBackground(Theme.CARD);
        textArea.setForeground(Theme.TEXT);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = Theme.scroll(textArea);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        scrollPane.setPreferredSize(new Dimension(
                Math.min(820, layar.width - 120),
                Math.min(540, layar.height - 180)));
        add(scrollPane, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton salin = Theme.secondaryButton("Salin Teks");
        salin.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(textArea.getText()), null);
            Theme.showInfo(this, "Dokumen disalin ke clipboard.");
        });
        JButton cetak = Theme.primaryButton("Cetak");
        cetak.addActionListener(e -> {
            try {
                textArea.print();
            } catch (PrinterException ex) {
                Theme.showError(this, "Gagal mencetak: " + ex.getMessage());
            }
        });
        JButton tutup = Theme.secondaryButton("Tutup");
        tutup.addActionListener(e -> dispose());
        tombol.add(salin);
        tombol.add(cetak);
        tombol.add(tutup);
        add(tombol, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Component parent, String judul, String isi) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        DokumenPreviewDialog dialog = new DokumenPreviewDialog(owner, judul, isi);
        dialog.setVisible(true);
    }
}
```

## src/ihsse/gui/FormatUtil.java

```java
package ihsse.gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Format tanggal/jam untuk form dan tabel.
 */
public final class FormatUtil {

    public static final DateTimeFormatter TANGGAL_INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TANGGAL_VIEW =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter WAKTU_INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter WAKTU_VIEW =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    public static final DateTimeFormatter JAM_INPUT =
            DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter JAM_VIEW =
            DateTimeFormatter.ofPattern("HH:mm");

    private FormatUtil() {
    }

    public static LocalDate parseTanggal(String teks) {
        try {
            return LocalDate.parse(teks.trim(), TANGGAL_INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd, contoh 2026-10-30.");
        }
    }

    public static LocalDate parseTanggalOpsional(String teks) {
        if (teks == null || teks.trim().isEmpty()) {
            return null;
        }
        return parseTanggal(teks);
    }

    public static LocalDateTime parseWaktu(String teks) {
        try {
            return LocalDateTime.parse(teks.trim(), WAKTU_INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Format waktu harus yyyy-MM-dd HH:mm, contoh 2026-10-30 09:15.");
        }
    }

    public static LocalTime parseJam(String teks) {
        try {
            return LocalTime.parse(teks.trim(), JAM_INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Format jam harus HH:mm, contoh 09:15.");
        }
    }

    public static String tanggal(LocalDate tanggal) {
        return tanggal == null ? "-" : tanggal.format(TANGGAL_VIEW);
    }

    public static String tanggalInput(LocalDate tanggal) {
        return tanggal == null ? "" : tanggal.format(TANGGAL_INPUT);
    }

    public static String waktu(LocalDateTime waktu) {
        return waktu == null ? "-" : waktu.format(WAKTU_VIEW);
    }

    public static String waktuInput(LocalDateTime waktu) {
        return waktu == null ? "" : waktu.format(WAKTU_INPUT);
    }

    public static String jam(LocalTime jam) {
        return jam == null ? "-" : jam.format(JAM_VIEW);
    }
}
```

## src/ihsse/gui/GuiMain.java

```java
package ihsse.gui;

import ihsse.db.Database;

import javax.swing.SwingUtilities;

/**
 * Entry point aplikasi GUI I-HSSE.
 */
public final class GuiMain {

    private GuiMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                Database.testConnection();
            } catch (Exception | LinkageError ex) {
                Theme.showError(null,
                        "Database belum bisa dihubungi.\n\n"
                        + "1. Pastikan MySQL XAMPP sudah Start.\n"
                        + "2. Jalankan setup_database.bat satu kali.\n"
                        + "3. Periksa db.properties (user/password/port).\n\n"
                        + "Detail: " + ex.getMessage());
            }
            new LoginFrame().setVisible(true);
        });
    }
}
```

## src/ihsse/gui/InduksiPanel.java

```java
package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.SertifikatInduksi;
import ihsse.service.InduksiService;
import ihsse.service.PengunjungService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel safety induction dan sertifikat.
 */
public class InduksiPanel extends JPanel implements Refreshable {

    private final PengunjungService pengunjungService;
    private final InduksiService induksiService;
    private final Admin admin;
    private final Runnable onChange;

    private final DefaultTableModel pengunjungModel;
    private final DefaultTableModel sertifikatModel;
    private final JTable pengunjungTable;
    private final JTable sertifikatTable;
    private final List<Pengunjung> daftarPengunjung = new ArrayList<>();
    private final List<SertifikatInduksi> daftarSertifikat = new ArrayList<>();

    public InduksiPanel(PengunjungService pengunjungService,
                        InduksiService induksiService,
                        Admin admin, Runnable onChange) {
        this.pengunjungService = pengunjungService;
        this.induksiService = induksiService;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel info = Theme.card();
        info.setLayout(new BorderLayout(12, 0));
        JLabel judul = Theme.bold("Materi Wajib Safety Induction");
        judul.setPreferredSize(new Dimension(230, 0));
        info.add(judul, BorderLayout.WEST);
        JTextArea materi = Theme.textArea(4);
        materi.setEditable(false);
        materi.setOpaque(false);
        materi.setRows(4);
        StringBuilder teks = new StringBuilder();
        int nomor = 1;
        for (String item : InduksiService.MATERI_WAJIB) {
            teks.append(nomor++).append(". ").append(item).append('\n');
        }
        materi.setText(teks.toString());
        info.add(materi, BorderLayout.CENTER);
        add(info, BorderLayout.NORTH);

        pengunjungModel = new DefaultTableModel(
                new Object[]{"ID", "Nama", "Instansi", "Tgl Daftar", "Status Induksi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pengunjungTable = Theme.table();
        pengunjungTable.setModel(pengunjungModel);
        pengunjungTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(pengunjungTable, 80, 190, 200, 110, 130);
        pengunjungTable.getColumnModel().getColumn(4)
                .setCellRenderer(Theme.statusRenderer());

        sertifikatModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Peserta", "Fasilitator",
                        "Tgl Selesai"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sertifikatTable = Theme.table();
        sertifikatTable.setModel(sertifikatModel);
        sertifikatTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(sertifikatTable, 190, 100, 200, 190, 110);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buatCardPengunjung(), buatCardSertifikat());
        split.setResizeWeight(0.52);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buatCardPengunjung() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel judul = new JPanel();
        judul.setOpaque(false);
        judul.setLayout(new javax.swing.BoxLayout(judul, javax.swing.BoxLayout.Y_AXIS));
        judul.add(Theme.bold("Peserta Induksi"));
        JLabel ket = Theme.muted("Pilih pengunjung yang sudah menyelesaikan materi.");
        ket.setFont(Theme.FONT_SMALL);
        judul.add(ket);
        header.add(judul, BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton selesaikan = Theme.primaryButton("Selesaikan Induksi");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        selesaikan.addActionListener(e -> selesaikanInduksi());
        muat.addActionListener(e -> refreshData());
        tombol.add(selesaikan);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(pengunjungTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buatCardSertifikat() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.bold("Sertifikat Induksi"), BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        lihat.addActionListener(e -> lihatSertifikat());
        tombol.add(lihat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(sertifikatTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refreshData() {
        daftarPengunjung.clear();
        pengunjungModel.setRowCount(0);
        for (Pengunjung pengunjung : pengunjungService.semuaPengunjung()) {
            daftarPengunjung.add(pengunjung);
            pengunjungModel.addRow(new Object[]{
                    pengunjung.getId(),
                    pengunjung.getNama(),
                    pengunjung.getInstansi(),
                    FormatUtil.tanggal(pengunjung.getTanggalDaftar()),
                    pengunjung.isSudahInduksi() ? "Selesai" : "Belum"
            });
        }

        daftarSertifikat.clear();
        sertifikatModel.setRowCount(0);
        for (SertifikatInduksi sertifikat : induksiService.semuaSertifikat()) {
            daftarSertifikat.add(sertifikat);
            sertifikatModel.addRow(new Object[]{
                    sertifikat.getNomorDokumen(),
                    FormatUtil.tanggal(sertifikat.getTanggalTerbit()),
                    sertifikat.getPengunjung().getNama(),
                    sertifikat.getNamaFasilitator(),
                    FormatUtil.tanggal(sertifikat.getTanggalSelesai())
            });
        }
    }

    private void selesaikanInduksi() {
        int row = pengunjungTable.getSelectedRow();
        if (row < 0 || row >= daftarPengunjung.size()) {
            Theme.showInfo(this, "Pilih pengunjung pada tabel terlebih dahulu.");
            return;
        }
        Pengunjung pengunjung = daftarPengunjung.get(row);
        if (pengunjung.isSudahInduksi()) {
            Theme.showInfo(this, "Pengunjung " + pengunjung.getNama()
                    + " sudah memiliki sertifikat induksi.");
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JTextArea info = Theme.textArea(5);
        info.setEditable(false);
        info.setOpaque(false);
        info.setText("Peserta: " + pengunjung.getNama() + "\n"
                + "Instansi: " + pengunjung.getInstansi() + "\n\n"
                + "Materi yang dinyatakan selesai:\n"
                + "1. " + InduksiService.MATERI_WAJIB.get(0) + "\n"
                + "2. " + InduksiService.MATERI_WAJIB.get(1) + "\n"
                + "3. " + InduksiService.MATERI_WAJIB.get(2) + "\n"
                + "4. " + InduksiService.MATERI_WAJIB.get(3));
        panel.add(info, BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        JTextField fasilitator = Theme.textField();
        fasilitator.setText(admin.getNama() + ", S.K.M.");
        javax.swing.JPanel formWrap = new javax.swing.JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.add(Theme.bold("Nama Fasilitator"), BorderLayout.NORTH);
        formWrap.add(fasilitator, BorderLayout.CENTER);
        panel.add(formWrap, BorderLayout.CENTER);

        int pilihan = JOptionPane.showConfirmDialog(this, panel,
                "Selesaikan Induksi - " + pengunjung.getNama(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (pilihan != JOptionPane.OK_OPTION) {
            return;
        }
        String namaFasilitator = fasilitator.getText();
        if (namaFasilitator == null || namaFasilitator.trim().isEmpty()) {
            Theme.showError(this, "Nama fasilitator wajib diisi.");
            return;
        }

        try {
            SertifikatInduksi sertifikat = induksiService.selesaikanInduksi(
                    pengunjung, admin, namaFasilitator.trim());
            refreshData();
            onChange.run();
            DokumenPreviewDialog.show(this,
                    "Sertifikat - " + sertifikat.getNomorDokumen(), sertifikat.cetak());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal menyelesaikan induksi: " + ex.getMessage());
        }
    }

    private void lihatSertifikat() {
        int row = sertifikatTable.getSelectedRow();
        if (row < 0 || row >= daftarSertifikat.size()) {
            Theme.showInfo(this, "Pilih sertifikat pada tabel terlebih dahulu.");
            return;
        }
        SertifikatInduksi sertifikat = daftarSertifikat.get(row);
        DokumenPreviewDialog.show(this,
                "Sertifikat - " + sertifikat.getNomorDokumen(), sertifikat.cetak());
    }
}
```

## src/ihsse/gui/InformasiDialog.java

```java
package ihsse.gui;

import ihsse.model.AreaMerokok;
import ihsse.model.FasilitasKeamanan;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.FasilitasLingkungan;
import ihsse.model.InformasiHSSE;
import ihsse.model.KondisiFasilitas;
import ihsse.model.TingkatRisiko;
import ihsse.model.ZonaRisiko;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Window;
import java.time.LocalDate;

/**
 * Dialog tambah/ubah informasi HSSE dengan field dinamis sesuai jenis konten.
 */
public class InformasiDialog extends JDialog {

    private static final String[] TIPE_LABEL = {
            "Area Merokok (Health)",
            "Fasilitas Keamanan (Security)",
            "Fasilitas Keselamatan (Safety)",
            "Zona Risiko (Safety)",
            "Fasilitas Lingkungan (Environment)"
    };
    private static final String[] TIPE_KEY = {
            "AreaMerokok", "FasilitasKeamanan", "FasilitasKeselamatan",
            "ZonaRisiko", "FasilitasLingkungan"
    };
    private static final String[] KONDISI_LABEL = {
            "Baik", "Rusak Ringan", "Rusak Berat", "Kedaluwarsa"
    };
    private static final KondisiFasilitas[] KONDISI = {
            KondisiFasilitas.BAIK,
            KondisiFasilitas.RUSAK_RINGAN,
            KondisiFasilitas.RUSAK_BERAT,
            KondisiFasilitas.KEDALUWARSA
    };

    private final JComboBox<String> tipeCombo = Theme.comboBox(TIPE_LABEL);
    private final JTextField kodeField = Theme.textField();
    private final JTextField namaField = Theme.textField();
    private final JTextField lokasiField = Theme.textField();
    private final JTextArea deskripsiArea = Theme.textArea(4);

    private final JTextField luasField = Theme.textField();
    private final JTextField aturanField = Theme.textField();
    private final JTextField sanksiField = Theme.textField();

    private final JTextField jenisKeamananField = Theme.textField();
    private final JTextField jumlahKeamananField = Theme.textField();
    private final JComboBox<String> kondisiKeamananCombo = Theme.comboBox(KONDISI_LABEL);

    private final JTextField jenisKeselamatanField = Theme.textField();
    private final JTextField jumlahKeselamatanField = Theme.textField();
    private final JComboBox<String> kondisiKeselamatanCombo = Theme.comboBox(KONDISI_LABEL);
    private final JTextField kedaluwarsaField = Theme.textField();

    private final JComboBox<String> risikoCombo = Theme.comboBox(
            "Rendah", "Sedang", "Tinggi");
    private final JTextArea prosedurArea = Theme.textArea(3);
    private final JTextField titikKumpulField = Theme.textField();

    private final JTextField jenisLingkunganField = Theme.textField();
    private final JTextField jamField = Theme.textField();
    private final JTextField penanggungJawabField = Theme.textField();

    private final CardLayout detailLayout = new CardLayout();
    private final JPanel detailPanel = new JPanel(detailLayout);
    private InformasiHSSE hasil;

    private InformasiDialog(Frame owner, InformasiHSSE existing) {
        super(owner, existing == null ? "Tambah Informasi HSSE" : "Ubah Informasi HSSE",
                true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(700, layar.width - 60), Math.min(760, layar.height - 80));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(18, 22, 16, 22));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(Theme.title(existing == null ? "Tambah Informasi HSSE"
                : "Ubah Informasi HSSE"));
        header.add(javax.swing.Box.createVerticalStrut(3));
        header.add(Theme.muted("Isi data sesuai jenis konten. Format tanggal: yyyy-MM-dd."));
        root.add(header, BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Jenis Konten", tipeCombo);
        Theme.addFormRow(form, gbc, 1, "Kode", kodeField);
        Theme.addFormRow(form, gbc, 2, "Nama", namaField);
        Theme.addFormRow(form, gbc, 3, "Lokasi", lokasiField);
        JScrollPane deskripsiScroll = Theme.scroll(deskripsiArea);
        deskripsiScroll.setPreferredSize(new Dimension(0, 82));
        Theme.addFormRow(form, gbc, 4, "Deskripsi", deskripsiScroll);
        Theme.addFormRow(form, gbc, 5, "Detail", detailPanel);

        detailPanel.setOpaque(false);
        detailPanel.add(buatPanelAreaMerokok(), TIPE_KEY[0]);
        detailPanel.add(buatPanelKeamanan(), TIPE_KEY[1]);
        detailPanel.add(buatPanelKeselamatan(), TIPE_KEY[2]);
        detailPanel.add(buatPanelRisiko(), TIPE_KEY[3]);
        detailPanel.add(buatPanelLingkungan(), TIPE_KEY[4]);

        tipeCombo.addActionListener(e -> detailLayout.show(detailPanel,
                TIPE_KEY[tipeCombo.getSelectedIndex()]));
        detailLayout.show(detailPanel, TIPE_KEY[0]);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(Theme.CARD);
        root.add(formScroll, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton batal = Theme.secondaryButton("Batal");
        JButton simpan = Theme.primaryButton("Simpan");
        batal.addActionListener(e -> dispose());
        simpan.addActionListener(e -> simpan());
        tombol.add(batal);
        tombol.add(simpan);
        root.add(tombol, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(simpan);

        if (existing != null) {
            isiForm(existing);
        }
        kodeField.requestFocusInWindow();
    }

    private JPanel buatPanelAreaMerokok() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Luas Area (m2)", luasField);
        Theme.addFormRow(panel, gbc, 1, "Aturan", aturanField);
        Theme.addFormRow(panel, gbc, 2, "Sanksi", sanksiField);
        return panel;
    }

    private JPanel buatPanelKeamanan() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Jenis Fasilitas", jenisKeamananField);
        Theme.addFormRow(panel, gbc, 1, "Jumlah", jumlahKeamananField);
        Theme.addFormRow(panel, gbc, 2, "Kondisi", kondisiKeamananCombo);
        return panel;
    }

    private JPanel buatPanelKeselamatan() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Jenis Fasilitas", jenisKeselamatanField);
        Theme.addFormRow(panel, gbc, 1, "Jumlah", jumlahKeselamatanField);
        Theme.addFormRow(panel, gbc, 2, "Kondisi", kondisiKeselamatanCombo);
        Theme.addFormRow(panel, gbc, 3, "Masa Kedaluwarsa", kedaluwarsaField);
        return panel;
    }

    private JPanel buatPanelRisiko() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Tingkat Risiko", risikoCombo);
        JScrollPane prosedurScroll = Theme.scroll(prosedurArea);
        prosedurScroll.setPreferredSize(new Dimension(0, 65));
        Theme.addFormRow(panel, gbc, 1, "Prosedur Evakuasi", prosedurScroll);
        Theme.addFormRow(panel, gbc, 2, "Titik Kumpul", titikKumpulField);
        return panel;
    }

    private JPanel buatPanelLingkungan() {
        JPanel panel = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(panel, gbc, 0, "Jenis Fasilitas", jenisLingkunganField);
        Theme.addFormRow(panel, gbc, 1, "Jam Operasional", jamField);
        Theme.addFormRow(panel, gbc, 2, "Penanggung Jawab", penanggungJawabField);
        return panel;
    }

    private void isiForm(InformasiHSSE existing) {
        tipeCombo.setSelectedIndex(indexTipe(existing));
        tipeCombo.setEnabled(false);
        kodeField.setText(existing.getKode());
        kodeField.setEnabled(false);
        namaField.setText(existing.getNama());
        lokasiField.setText(existing.getLokasi());
        deskripsiArea.setText(existing.getDeskripsi());

        if (existing instanceof AreaMerokok) {
            AreaMerokok area = (AreaMerokok) existing;
            luasField.setText(String.valueOf(area.getLuasArea()));
            aturanField.setText(area.getAturan());
            sanksiField.setText(area.getSanksi());
        } else if (existing instanceof FasilitasKeamanan) {
            FasilitasKeamanan fasilitas = (FasilitasKeamanan) existing;
            jenisKeamananField.setText(fasilitas.getJenis());
            jumlahKeamananField.setText(String.valueOf(fasilitas.getJumlah()));
            kondisiKeamananCombo.setSelectedItem(fasilitas.getKondisi().getLabel());
        } else if (existing instanceof FasilitasKeselamatan) {
            FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) existing;
            jenisKeselamatanField.setText(fasilitas.getJenis());
            jumlahKeselamatanField.setText(String.valueOf(fasilitas.getJumlah()));
            kondisiKeselamatanCombo.setSelectedItem(fasilitas.getKondisi().getLabel());
            kedaluwarsaField.setText(FormatUtil.tanggalInput(
                    fasilitas.getMasaKedaluwarsa()));
        } else if (existing instanceof ZonaRisiko) {
            ZonaRisiko zona = (ZonaRisiko) existing;
            risikoCombo.setSelectedItem(zona.getTingkatRisiko().getLabel());
            prosedurArea.setText(zona.getProsedurEvakuasi());
            titikKumpulField.setText(zona.getTitikKumpul());
        } else if (existing instanceof FasilitasLingkungan) {
            FasilitasLingkungan fasilitas = (FasilitasLingkungan) existing;
            jenisLingkunganField.setText(fasilitas.getJenis());
            jamField.setText(fasilitas.getJamOperasional());
            penanggungJawabField.setText(fasilitas.getPenanggungJawab());
        }
    }

    private void simpan() {
        try {
            String kode = required(kodeField, "Kode");
            String nama = required(namaField, "Nama");
            String lokasi = required(lokasiField, "Lokasi");
            String deskripsi = required(deskripsiArea, "Deskripsi");
            String tipe = TIPE_KEY[tipeCombo.getSelectedIndex()];

            if ("AreaMerokok".equals(tipe)) {
                hasil = new AreaMerokok(kode, nama, lokasi, deskripsi,
                        parseDouble(luasField, "Luas area"),
                        required(aturanField, "Aturan"),
                        required(sanksiField, "Sanksi"));
            } else if ("FasilitasKeamanan".equals(tipe)) {
                hasil = new FasilitasKeamanan(kode, nama, lokasi, deskripsi,
                        required(jenisKeamananField, "Jenis fasilitas"),
                        parseInt(jumlahKeamananField, "Jumlah"),
                        kondisiDari(String.valueOf(kondisiKeamananCombo.getSelectedItem())));
            } else if ("FasilitasKeselamatan".equals(tipe)) {
                LocalDate kedaluwarsa = FormatUtil.parseTanggalOpsional(
                        kedaluwarsaField.getText());
                hasil = new FasilitasKeselamatan(kode, nama, lokasi, deskripsi,
                        required(jenisKeselamatanField, "Jenis fasilitas"),
                        parseInt(jumlahKeselamatanField, "Jumlah"),
                        kondisiDari(String.valueOf(kondisiKeselamatanCombo.getSelectedItem())),
                        kedaluwarsa);
            } else if ("ZonaRisiko".equals(tipe)) {
                hasil = new ZonaRisiko(kode, nama, lokasi, deskripsi,
                        risikoDari(String.valueOf(risikoCombo.getSelectedItem())),
                        required(prosedurArea, "Prosedur evakuasi"),
                        required(titikKumpulField, "Titik kumpul"));
            } else {
                hasil = new FasilitasLingkungan(kode, nama, lokasi, deskripsi,
                        required(jenisLingkunganField, "Jenis fasilitas"),
                        required(jamField, "Jam operasional"),
                        required(penanggungJawabField, "Penanggung jawab"));
            }
            dispose();
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private int indexTipe(InformasiHSSE informasi) {
        if (informasi instanceof AreaMerokok) {
            return 0;
        }
        if (informasi instanceof FasilitasKeamanan) {
            return 1;
        }
        if (informasi instanceof FasilitasKeselamatan) {
            return 2;
        }
        if (informasi instanceof ZonaRisiko) {
            return 3;
        }
        return 4;
    }

    private String required(JTextField field, String label) {
        String nilai = field.getText();
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return nilai.trim();
    }

    private String required(JTextArea area, String label) {
        String nilai = area.getText();
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return nilai.trim();
    }

    private int parseInt(JTextField field, String label) {
        try {
            int nilai = Integer.parseInt(field.getText().trim());
            if (nilai < 1) {
                throw new NumberFormatException();
            }
            return nilai;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " harus angka bulat minimal 1.");
        }
    }

    private double parseDouble(JTextField field, String label) {
        try {
            double nilai = Double.parseDouble(field.getText().trim());
            if (nilai <= 0) {
                throw new NumberFormatException();
            }
            return nilai;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " harus angka lebih dari 0.");
        }
    }

    private KondisiFasilitas kondisiDari(String label) {
        for (int i = 0; i < KONDISI_LABEL.length; i++) {
            if (KONDISI_LABEL[i].equals(label)) {
                return KONDISI[i];
            }
        }
        return KondisiFasilitas.BAIK;
    }

    private TingkatRisiko risikoDari(String label) {
        if ("Rendah".equals(label)) {
            return TingkatRisiko.RENDAH;
        }
        if ("Sedang".equals(label)) {
            return TingkatRisiko.SEDANG;
        }
        return TingkatRisiko.TINGGI;
    }

    public static InformasiHSSE show(Component parent, InformasiHSSE existing) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        Frame frame = owner instanceof Frame ? (Frame) owner : null;
        InformasiDialog dialog = new InformasiDialog(frame, existing);
        dialog.setVisible(true);
        return dialog.hasil;
    }
}
```

## src/ihsse/gui/InformasiPanel.java

```java
package ihsse.gui;

import ihsse.model.DenahGedung;
import ihsse.model.InformasiHSSE;
import ihsse.model.VideoTutorial;
import ihsse.service.InformasiService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel pengelolaan informasi HSSE, video tutorial, dan denah gedung.
 */
public class InformasiPanel extends JPanel implements Refreshable {

    private final InformasiService service;
    private final Runnable onChange;

    private final JTextField searchField = Theme.textField();
    private final JComboBox<String> kategoriCombo = Theme.comboBox(
            "Semua", "Health", "Security", "Safety", "Environment");

    private final DefaultTableModel infoModel;
    private final DefaultTableModel videoModel;
    private final DefaultTableModel denahModel;
    private final JTable infoTable;
    private final List<InformasiHSSE> data = new ArrayList<>();

    public InformasiPanel(InformasiService service, Runnable onChange) {
        this.service = service;
        this.onChange = onChange;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel kiri = Theme.toolbar();
        kiri.add(Theme.bold("Cari"));
        searchField.setPreferredSize(new Dimension(210, 34));
        kiri.add(searchField);
        kiri.add(Theme.bold("Kategori"));
        kategoriCombo.setPreferredSize(new Dimension(135, 34));
        kiri.add(kategoriCombo);

        JPanel kanan = Theme.rightToolbar();
        JButton tambah = Theme.primaryButton("+ Tambah");
        JButton ubah = Theme.secondaryButton("Ubah");
        JButton hapus = Theme.dangerButton("Hapus");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        kanan.add(tambah);
        kanan.add(ubah);
        kanan.add(hapus);
        kanan.add(muat);

        toolbar.add(kiri, BorderLayout.WEST);
        toolbar.add(kanan, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        infoModel = new DefaultTableModel(
                new Object[]{"Kode", "Kategori", "Jenis", "Nama", "Lokasi", "Deskripsi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        infoTable = Theme.table();
        infoTable.setModel(infoModel);
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(infoTable, 70, 90, 135, 230, 220, 330);
        infoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ubah();
                }
            }
        });

        JPanel tabInfo = new JPanel(new BorderLayout());
        tabInfo.setOpaque(false);
        tabInfo.add(Theme.scroll(infoTable), BorderLayout.CENTER);

        videoModel = new DefaultTableModel(
                new Object[]{"ID", "Topik", "Judul", "Durasi", "URL"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable videoTable = Theme.table();
        videoTable.setModel(videoModel);
        videoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(videoTable, 70, 100, 260, 130, 300);
        JPanel tabVideo = new JPanel(new BorderLayout());
        tabVideo.setOpaque(false);
        tabVideo.add(Theme.scroll(videoTable), BorderLayout.CENTER);

        denahModel = new DefaultTableModel(
                new Object[]{"Lantai", "Nama File", "Deskripsi", "Fasilitas Penting"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable denahTable = Theme.table();
        denahTable.setModel(denahModel);
        denahTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(denahTable, 70, 170, 380, 360);
        JPanel tabDenah = new JPanel(new BorderLayout());
        tabDenah.setOpaque(false);
        tabDenah.add(Theme.scroll(denahTable), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT);
        tabs.addTab("Data HSSE", tabInfo);
        tabs.addTab("Video Tutorial", tabVideo);
        tabs.addTab("Denah Gedung", tabDenah);
        add(tabs, BorderLayout.CENTER);

        tambah.addActionListener(e -> tambah());
        ubah.addActionListener(e -> ubah());
        hapus.addActionListener(e -> hapus());
        muat.addActionListener(e -> refreshData());
        kategoriCombo.addActionListener(e -> loadInfo());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadInfo();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadInfo();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadInfo();
            }
        });
    }

    @Override
    public void refreshData() {
        loadInfo();
        loadVideo();
        loadDenah();
    }

    private void loadInfo() {
        String kategori = String.valueOf(kategoriCombo.getSelectedItem());
        String cari = searchField.getText().trim().toLowerCase();

        data.clear();
        infoModel.setRowCount(0);
        for (InformasiHSSE informasi : service.semuaInformasi()) {
            if (!"Semua".equals(kategori)
                    && !informasi.getKategori().equalsIgnoreCase(kategori)) {
                continue;
            }
            String gabungan = (informasi.getKode() + " " + informasi.getNama() + " "
                    + informasi.getLokasi() + " " + informasi.getDeskripsi() + " "
                    + informasi.getJenis()).toLowerCase();
            if (!cari.isEmpty() && !gabungan.contains(cari)) {
                continue;
            }
            data.add(informasi);
            infoModel.addRow(new Object[]{
                    informasi.getKode(),
                    informasi.getKategori(),
                    informasi.getJenis(),
                    informasi.getNama(),
                    informasi.getLokasi(),
                    informasi.getDeskripsi()
            });
        }
    }

    private void loadVideo() {
        videoModel.setRowCount(0);
        for (VideoTutorial video : service.semuaVideo()) {
            videoModel.addRow(new Object[]{
                    video.getId(),
                    video.getTopik(),
                    video.getJudul(),
                    video.getDurasiFormat(),
                    video.getUrl()
            });
        }
    }

    private void loadDenah() {
        denahModel.setRowCount(0);
        for (DenahGedung denah : service.semuaDenah()) {
            denahModel.addRow(new Object[]{
                    "Lantai " + denah.getLantai(),
                    denah.getNamaFile(),
                    denah.getDeskripsi(),
                    String.join(", ", denah.getFasilitasPenting())
            });
        }
    }

    private InformasiHSSE terpilih() {
        int row = infoTable.getSelectedRow();
        if (row < 0 || row >= data.size()) {
            Theme.showInfo(this, "Pilih salah satu data informasi terlebih dahulu.");
            return null;
        }
        return data.get(row);
    }

    private void tambah() {
        InformasiHSSE informasi = InformasiDialog.show(this, null);
        if (informasi == null) {
            return;
        }
        try {
            service.tambahInformasi(informasi);
            onChange.run();
            refreshData();
            Theme.showInfo(this, "Informasi " + informasi.getKode() + " berhasil disimpan.");
        } catch (RuntimeException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private void ubah() {
        InformasiHSSE lama = terpilih();
        if (lama == null) {
            return;
        }
        InformasiHSSE baru = InformasiDialog.show(this, lama);
        if (baru == null) {
            return;
        }
        try {
            service.ubahInformasi(baru);
            onChange.run();
            refreshData();
            Theme.showInfo(this, "Informasi " + baru.getKode() + " berhasil diubah.");
        } catch (RuntimeException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private void hapus() {
        InformasiHSSE informasi = terpilih();
        if (informasi == null) {
            return;
        }
        if (!Theme.confirm(this, "Hapus informasi " + informasi.getKode()
                + " - " + informasi.getNama() + "?")) {
            return;
        }
        try {
            service.hapusInformasi(informasi.getKode());
            onChange.run();
            refreshData();
            Theme.showInfo(this, "Informasi " + informasi.getKode() + " berhasil dihapus.");
        } catch (RuntimeException ex) {
            Theme.showError(this, ex.getMessage());
        }
    }
}
```

## src/ihsse/gui/InsidenPanel.java

```java
package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;
import ihsse.service.InsidenService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel pelaporan insiden dan proses tindak lanjut oleh admin.
 */
public class InsidenPanel extends JPanel implements Refreshable {

    private final InsidenService service;
    private final Admin admin;
    private final Runnable onChange;

    private final JTextField namaPelaporField = Theme.textField();
    private final JTextField kontakField = Theme.textField();
    private final JTextField lokasiField = Theme.textField();
    private final JTextField waktuField = Theme.textField();
    private final JTextArea deskripsiArea = Theme.textArea(4);

    private final JComboBox<String> filterCombo = Theme.comboBox(
            "Semua", "Baru", "Diproses", "Selesai");
    private final DefaultTableModel insidenModel;
    private final JTable insidenTable;
    private final List<LaporanInsiden> daftarInsiden = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();

    public InsidenPanel(InsidenService service, Admin admin, Runnable onChange) {
        this.service = service;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout());
        setOpaque(false);

        insidenModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Pelapor", "Lokasi",
                        "Waktu Kejadian", "Status", "Ditangani Oleh", "Catatan"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        insidenTable = Theme.table();
        insidenTable.setModel(insidenModel);
        insidenTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(insidenTable, 170, 90, 150, 190, 125, 90, 150, 230);
        insidenTable.getColumnModel().getColumn(5)
                .setCellRenderer(Theme.statusRenderer());

        tabs.setFont(Theme.FONT);
        tabs.addTab("Laporkan Insiden", buatTabLapor());
        tabs.addTab("Daftar & Proses", buatTabDaftar());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buatTabLapor() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(Theme.bold("Form Laporan Insiden"));
        JLabel ket = Theme.muted("Laporan akan masuk dengan status Baru dan diverifikasi admin.");
        ket.setFont(Theme.FONT_SMALL);
        header.add(ket);
        card.add(header, BorderLayout.NORTH);

        waktuField.setText(FormatUtil.waktuInput(
                LocalDateTime.now().withSecond(0).withNano(0)));

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Nama Pelapor", namaPelaporField);
        Theme.addFormRow(form, gbc, 1, "Kontak", kontakField);
        Theme.addFormRow(form, gbc, 2, "Lokasi", lokasiField);
        Theme.addFormRow(form, gbc, 3, "Waktu Kejadian", waktuField);
        JScrollPane deskripsiScroll = Theme.scroll(deskripsiArea);
        deskripsiScroll.setPreferredSize(new Dimension(0, 95));
        Theme.addFormRow(form, gbc, 4, "Deskripsi", deskripsiScroll);

        JPanel aksi = Theme.rightToolbar();
        JButton bersih = Theme.secondaryButton("Bersihkan");
        JButton kirim = Theme.primaryButton("Kirim Laporan");
        bersih.addActionListener(e -> bersihkanForm());
        kirim.addActionListener(e -> kirimLaporan());
        aksi.add(bersih);
        aksi.add(kirim);
        Theme.addFormRow(form, gbc, 5, "", aksi);

        card.add(form, BorderLayout.CENTER);
        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buatTabDaftar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel kiri = Theme.toolbar();
        kiri.add(Theme.bold("Filter Status"));
        kiri.add(filterCombo);
        header.add(kiri, BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton proses = Theme.primaryButton("Proses");
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        proses.addActionListener(e -> proses());
        lihat.addActionListener(e -> lihat());
        muat.addActionListener(e -> refreshData());
        tombol.add(proses);
        tombol.add(lihat);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        filterCombo.addActionListener(e -> loadInsiden());
        JScrollPane scroll = Theme.scroll(insidenTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        loadInsiden();
    }

    private void loadInsiden() {
        String filter = String.valueOf(filterCombo.getSelectedItem());
        List<LaporanInsiden> data;
        if ("Semua".equals(filter)) {
            data = service.semuaInsiden();
        } else {
            data = service.berdasarkanStatus(statusDariLabel(filter));
        }

        daftarInsiden.clear();
        insidenModel.setRowCount(0);
        for (LaporanInsiden laporan : data) {
            daftarInsiden.add(laporan);
            insidenModel.addRow(new Object[]{
                    laporan.getNomorDokumen(),
                    FormatUtil.tanggal(laporan.getTanggalTerbit()),
                    laporan.getNamaPelapor(),
                    laporan.getLokasi(),
                    FormatUtil.waktu(laporan.getWaktuKejadian()),
                    laporan.getStatus().getLabel(),
                    laporan.getDitanganiOleh() == null ? "-"
                            : laporan.getDitanganiOleh().getNama(),
                    laporan.getCatatanAdmin()
            });
        }
    }

    private void kirimLaporan() {
        try {
            String nama = required(namaPelaporField, "Nama pelapor");
            String kontak = required(kontakField, "Kontak");
            String lokasi = required(lokasiField, "Lokasi");
            String deskripsi = required(deskripsiArea, "Deskripsi");
            LocalDateTime waktu = FormatUtil.parseWaktu(waktuField.getText());

            LaporanInsiden laporan = service.laporInsiden(
                    nama, kontak, lokasi, deskripsi, waktu);
            bersihkanForm();
            loadInsiden();
            onChange.run();
            Theme.showInfo(this, "Laporan insiden berhasil dikirim.\n\nNomor: "
                    + laporan.getNomorDokumen() + "\nStatus: "
                    + laporan.getStatus().getLabel());
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal mengirim laporan: " + ex.getMessage());
        }
    }

    private void bersihkanForm() {
        namaPelaporField.setText("");
        kontakField.setText("");
        lokasiField.setText("");
        waktuField.setText(FormatUtil.waktuInput(
                LocalDateTime.now().withSecond(0).withNano(0)));
        deskripsiArea.setText("");
        namaPelaporField.requestFocusInWindow();
    }

    private void proses() {
        int row = insidenTable.getSelectedRow();
        if (row < 0 || row >= daftarInsiden.size()) {
            Theme.showInfo(this, "Pilih laporan insiden pada tabel terlebih dahulu.");
            return;
        }
        LaporanInsiden laporan = daftarInsiden.get(row);
        if (laporan.getStatus() == StatusInsiden.SELESAI) {
            Theme.showInfo(this, "Laporan " + laporan.getNomorDokumen()
                    + " sudah SELESAI dan tidak bisa diproses lagi.");
            return;
        }

        List<StatusInsiden> berikutnya = new ArrayList<>();
        for (StatusInsiden status : StatusInsiden.values()) {
            if (laporan.getStatus().bolehBerubahKe(status)) {
                berikutnya.add(status);
            }
        }
        if (berikutnya.isEmpty()) {
            Theme.showInfo(this, "Tidak ada status berikutnya untuk laporan ini.");
            return;
        }

        JComboBox<String> statusCombo = Theme.comboBox();
        for (StatusInsiden status : berikutnya) {
            statusCombo.addItem(status.getLabel());
        }

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JLabel info = Theme.bold("Status saat ini: " + laporan.getStatus().getLabel());
        panel.add(info, BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Status Berikutnya", statusCombo);
        JTextArea catatan = Theme.textArea(3);
        catatan.setText("-".equals(laporan.getCatatanAdmin())
                ? "" : laporan.getCatatanAdmin());
        JScrollPane catatanScroll = Theme.scroll(catatan);
        catatanScroll.setPreferredSize(new Dimension(0, 70));
        Theme.addFormRow(form, gbc, 1, "Catatan Admin", catatanScroll);
        panel.add(form, BorderLayout.CENTER);

        int pilihan = JOptionPane.showConfirmDialog(this, panel,
                "Proses Insiden - " + laporan.getNomorDokumen(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (pilihan != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            StatusInsiden statusBaru = berikutnya.get(statusCombo.getSelectedIndex());
            LaporanInsiden hasil = service.prosesInsiden(
                    laporan.getNomorDokumen(), statusBaru, catatan.getText(), admin);
            loadInsiden();
            onChange.run();
            DokumenPreviewDialog.show(this,
                    "Laporan Insiden - " + hasil.getNomorDokumen(), hasil.cetak());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal memproses insiden: " + ex.getMessage());
        }
    }

    private void lihat() {
        int row = insidenTable.getSelectedRow();
        if (row < 0 || row >= daftarInsiden.size()) {
            Theme.showInfo(this, "Pilih laporan insiden pada tabel terlebih dahulu.");
            return;
        }
        LaporanInsiden laporan = daftarInsiden.get(row);
        DokumenPreviewDialog.show(this,
                "Laporan Insiden - " + laporan.getNomorDokumen(), laporan.cetak());
    }

    private StatusInsiden statusDariLabel(String label) {
        for (StatusInsiden status : StatusInsiden.values()) {
            if (status.getLabel().equalsIgnoreCase(label)) {
                return status;
            }
        }
        return StatusInsiden.BARU;
    }

    private String required(JTextField field, String label) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return field.getText().trim();
    }

    private String required(JTextArea area, String label) {
        if (area.getText() == null || area.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return area.getText().trim();
    }
}
```

## src/ihsse/gui/InspeksiPanel.java

```java
package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.InformasiHSSE;
import ihsse.model.KondisiFasilitas;
import ihsse.model.LaporanInspeksi;
import ihsse.service.InformasiService;
import ihsse.service.InspeksiService;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel inspeksi fasilitas keselamatan.
 */
public class InspeksiPanel extends JPanel implements Refreshable {

    private static final String[] KONDISI_LABEL = {
            "Baik", "Rusak Ringan", "Rusak Berat", "Kedaluwarsa"
    };
    private static final KondisiFasilitas[] KONDISI = {
            KondisiFasilitas.BAIK,
            KondisiFasilitas.RUSAK_RINGAN,
            KondisiFasilitas.RUSAK_BERAT,
            KondisiFasilitas.KEDALUWARSA
    };

    private final InformasiService informasiService;
    private final InspeksiService inspeksiService;
    private final Admin admin;
    private final Runnable onChange;

    private final JComboBox<FasilitasKeselamatan> fasilitasCombo = new JComboBox<>();
    private final JTextField tanggalCekField = Theme.textField();
    private final JComboBox<String> kondisiCombo = Theme.comboBox(KONDISI_LABEL);
    private final JTextField kedaluwarsaField = Theme.textField();
    private final JTextField tindakanField = Theme.textField();
    private final JTextArea catatanArea = Theme.textArea(3);

    private final DefaultTableModel laporanModel;
    private final JTable laporanTable;
    private final List<FasilitasKeselamatan> daftarFasilitas = new ArrayList<>();
    private final List<LaporanInspeksi> daftarLaporan = new ArrayList<>();
    private boolean sedangMemuatFasilitas;

    public InspeksiPanel(InformasiService informasiService,
                         InspeksiService inspeksiService,
                         Admin admin, Runnable onChange) {
        this.informasiService = informasiService;
        this.inspeksiService = inspeksiService;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel formCard = Theme.card();
        formCard.setLayout(new BorderLayout(0, 10));
        formCard.add(Theme.bold("Form Inspeksi Fasilitas"), BorderLayout.NORTH);

        fasilitasCombo.setFont(Theme.FONT);
        fasilitasCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                           int index, boolean isSelected,
                                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof FasilitasKeselamatan) {
                    FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) value;
                    setText(fasilitas.getKode() + " - " + fasilitas.getNama()
                            + " (" + fasilitas.getLokasi() + ")");
                }
                return this;
            }
        });
        fasilitasCombo.addActionListener(e -> {
            if (!sedangMemuatFasilitas) {
                isiDariFasilitas();
            }
        });

        tanggalCekField.setText(LocalDate.now().toString());
        kedaluwarsaField.setText("");

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Fasilitas", fasilitasCombo);
        Theme.addFormRow(form, gbc, 1, "Tanggal Cek", tanggalCekField);
        Theme.addFormRow(form, gbc, 2, "Kondisi", kondisiCombo);
        Theme.addFormRow(form, gbc, 3, "Masa Kedaluwarsa", kedaluwarsaField);
        Theme.addFormRow(form, gbc, 4, "Tindakan", tindakanField);
        JScrollPane catatanScroll = Theme.scroll(catatanArea);
        catatanScroll.setPreferredSize(new Dimension(0, 66));
        Theme.addFormRow(form, gbc, 5, "Catatan", catatanScroll);

        JPanel aksi = Theme.rightToolbar();
        JButton bersih = Theme.secondaryButton("Bersihkan");
        JButton simpan = Theme.primaryButton("Simpan Inspeksi");
        bersih.addActionListener(e -> bersihkanForm());
        simpan.addActionListener(e -> simpan());
        aksi.add(bersih);
        aksi.add(simpan);
        Theme.addFormRow(form, gbc, 6, "", aksi);
        formCard.add(form, BorderLayout.CENTER);
        add(formCard, BorderLayout.NORTH);

        laporanModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Fasilitas", "Tgl Cek", "Kondisi",
                        "Kedaluwarsa", "Petugas", "Tindakan"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        laporanTable = Theme.table();
        laporanTable.setModel(laporanModel);
        laporanTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(laporanTable, 175, 95, 190, 95, 105, 105, 150, 280);
        laporanTable.getColumnModel().getColumn(4)
                .setCellRenderer(Theme.statusRenderer());

        JPanel tableCard = Theme.card();
        tableCard.setLayout(new BorderLayout(0, 10));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.bold("Riwayat Inspeksi"), BorderLayout.WEST);
        JPanel tombol = Theme.rightToolbar();
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        lihat.addActionListener(e -> lihatLaporan());
        muat.addActionListener(e -> refreshData());
        tombol.add(lihat);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        tableCard.add(header, BorderLayout.NORTH);
        JScrollPane scroll = Theme.scroll(laporanTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        String kodeTerpilih = null;
        if (fasilitasCombo.getSelectedItem() instanceof FasilitasKeselamatan) {
            kodeTerpilih = ((FasilitasKeselamatan) fasilitasCombo.getSelectedItem()).getKode();
        }

        sedangMemuatFasilitas = true;
        daftarFasilitas.clear();
        fasilitasCombo.removeAllItems();
        for (InformasiHSSE informasi
                : informasiService.informasiBerdasarkanKategori("Safety")) {
            if (informasi instanceof FasilitasKeselamatan) {
                FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) informasi;
                daftarFasilitas.add(fasilitas);
                fasilitasCombo.addItem(fasilitas);
                if (kodeTerpilih != null && kodeTerpilih.equals(fasilitas.getKode())) {
                    fasilitasCombo.setSelectedItem(fasilitas);
                }
            }
        }
        sedangMemuatFasilitas = false;
        if (fasilitasCombo.getSelectedItem() != null) {
            isiDariFasilitas();
        }

        daftarLaporan.clear();
        laporanModel.setRowCount(0);
        for (LaporanInspeksi laporan : inspeksiService.semuaLaporan()) {
            daftarLaporan.add(laporan);
            laporanModel.addRow(new Object[]{
                    laporan.getNomorDokumen(),
                    FormatUtil.tanggal(laporan.getTanggalTerbit()),
                    laporan.getFasilitas().getKode() + " - "
                            + laporan.getFasilitas().getNama(),
                    FormatUtil.tanggal(laporan.getTanggalCek()),
                    laporan.getKondisi().getLabel(),
                    FormatUtil.tanggal(laporan.getMasaKedaluwarsa()),
                    laporan.getNamaPetugas(),
                    laporan.getTindakan()
            });
        }
    }

    private void isiDariFasilitas() {
        Object value = fasilitasCombo.getSelectedItem();
        if (!(value instanceof FasilitasKeselamatan)) {
            return;
        }
        FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) value;
        kondisiCombo.setSelectedItem(fasilitas.getKondisi().getLabel());
        kedaluwarsaField.setText(FormatUtil.tanggalInput(
                fasilitas.getMasaKedaluwarsa()));
    }

    private void bersihkanForm() {
        tanggalCekField.setText(LocalDate.now().toString());
        kedaluwarsaField.setText("");
        tindakanField.setText("");
        catatanArea.setText("");
        kondisiCombo.setSelectedIndex(0);
        if (fasilitasCombo.getItemCount() > 0) {
            fasilitasCombo.setSelectedIndex(0);
        }
    }

    private void simpan() {
        try {
            Object value = fasilitasCombo.getSelectedItem();
            if (!(value instanceof FasilitasKeselamatan)) {
                throw new IllegalArgumentException("Fasilitas keselamatan wajib dipilih.");
            }
            FasilitasKeselamatan fasilitas = (FasilitasKeselamatan) value;
            LocalDate tanggalCek = FormatUtil.parseTanggal(tanggalCekField.getText());
            KondisiFasilitas kondisi = kondisiDari(
                    String.valueOf(kondisiCombo.getSelectedItem()));
            LocalDate kedaluwarsa = FormatUtil.parseTanggalOpsional(
                    kedaluwarsaField.getText());
            String tindakan = tindakanField.getText();
            if (tindakan == null || tindakan.trim().isEmpty()) {
                throw new IllegalArgumentException("Tindakan wajib diisi.");
            }
            String catatan = catatanArea.getText();
            if (catatan == null || catatan.trim().isEmpty()) {
                catatan = "-";
            }

            LaporanInspeksi laporan = inspeksiService.catatInspeksi(
                    fasilitas, tanggalCek, kondisi, kedaluwarsa, admin,
                    tindakan.trim(), catatan.trim());
            refreshData();
            onChange.run();
            DokumenPreviewDialog.show(this,
                    "Laporan Inspeksi - " + laporan.getNomorDokumen(), laporan.cetak());
            tindakanField.setText("");
            catatanArea.setText("");
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal menyimpan inspeksi: " + ex.getMessage());
        }
    }

    private void lihatLaporan() {
        int row = laporanTable.getSelectedRow();
        if (row < 0 || row >= daftarLaporan.size()) {
            Theme.showInfo(this, "Pilih laporan inspeksi pada tabel terlebih dahulu.");
            return;
        }
        LaporanInspeksi laporan = daftarLaporan.get(row);
        DokumenPreviewDialog.show(this,
                "Laporan Inspeksi - " + laporan.getNomorDokumen(), laporan.cetak());
    }

    private KondisiFasilitas kondisiDari(String label) {
        for (int i = 0; i < KONDISI_LABEL.length; i++) {
            if (KONDISI_LABEL[i].equals(label)) {
                return KONDISI[i];
            }
        }
        return KondisiFasilitas.BAIK;
    }
}
```

## src/ihsse/gui/LoginFrame.java

```java
package ihsse.gui;

import ihsse.dao.AdminDao;
import ihsse.db.Database;
import ihsse.model.Admin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Arrays;

/**
 * Halaman login admin.
 */
public class LoginFrame extends JFrame {

    private final AdminDao adminDao = new AdminDao();
    private final JTextField usernameField = Theme.textField();
    private final JPasswordField passwordField = Theme.passwordField();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    public LoginFrame() {
        setTitle("I-HSSE Safety Induction - Login Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(480, layar.width - 60), Math.min(610, layar.height - 80));
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CARD);
        setContentPane(root);

        root.add(buatHeader(), BorderLayout.NORTH);
        root.add(buatForm(), BorderLayout.CENTER);

        cekDatabase();
    }

    private JPanel buatHeader() {
        JPanel header = new GradientPanel();
        header.setPreferredSize(new Dimension(0, 205));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(38, 40, 28, 40));

        JLabel logo = new JLabel("I-HSSE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 42));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel judul = new JLabel("Safety Induction");
        judul.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        judul.setForeground(new Color(214, 235, 228));
        judul.setAlignmentX(LEFT_ALIGNMENT);

        JLabel kampus = new JLabel("Universitas Gunadarma Kampus J6");
        kampus.setFont(Theme.FONT);
        kampus.setForeground(new Color(180, 205, 220));
        kampus.setAlignmentX(LEFT_ALIGNMENT);

        header.add(logo);
        header.add(Box.createVerticalStrut(4));
        header.add(judul);
        header.add(Box.createVerticalStrut(10));
        header.add(kampus);
        return header;
    }

    private JPanel buatForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CARD);
        wrapper.setBorder(new EmptyBorder(24, 42, 28, 42));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);

        JLabel heading = new JLabel("Masuk sebagai Admin");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading.setForeground(Theme.TEXT);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 18, 0);
        form.add(heading, gbc);

        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = 1;
        form.add(Theme.bold("Username"), gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(usernameField, gbc);

        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.gridy = 3;
        form.add(Theme.bold("Password"), gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 8, 0);
        form.add(passwordField, gbc);

        JCheckBox tampilPassword = new JCheckBox("Tampilkan password");
        tampilPassword.setOpaque(false);
        tampilPassword.setFont(Theme.FONT);
        tampilPassword.setForeground(Theme.MUTED);
        tampilPassword.setFocusPainted(false);
        tampilPassword.addActionListener(e -> passwordField.setEchoChar(
                tampilPassword.isSelected() ? (char) 0 : '\u2022'));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 16, 0);
        form.add(tampilPassword, gbc);

        javax.swing.JButton login = Theme.primaryButton("Masuk");
        login.setPreferredSize(new Dimension(0, 42));
        login.addActionListener(e -> login());
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(login, gbc);
        getRootPane().setDefaultButton(login);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.MUTED);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 10, 0);
        form.add(statusLabel, gbc);

        JLabel hint = new JLabel("Login default: admin / admin123", SwingConstants.CENTER);
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.MUTED);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(hint, gbc);

        wrapper.add(form);
        return wrapper;
    }

    private void cekDatabase() {
        try {
            String versi = Database.testConnection();
            statusLabel.setText("Database terhubung - ihsse_db");
            statusLabel.setToolTipText(versi);
            statusLabel.setForeground(Theme.ACCENT_DARK);
        } catch (Exception | LinkageError ex) {
            statusLabel.setText("Database belum terhubung. Jalankan MySQL XAMPP.");
            statusLabel.setForeground(Theme.DANGER);
        }
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username dan password wajib diisi.");
            statusLabel.setForeground(Theme.DANGER);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Admin admin = adminDao.login(username, password);
            if (admin == null) {
                statusLabel.setText("Username atau password salah.");
                statusLabel.setForeground(Theme.DANGER);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
                return;
            }
            Arrays.fill(passwordField.getPassword(), '\0');
            MainFrame mainFrame = new MainFrame(admin);
            mainFrame.setVisible(true);
            dispose();
        } catch (RuntimeException | LinkageError ex) {
            statusLabel.setText("Login gagal: " + ex.getMessage());
            statusLabel.setForeground(Theme.DANGER);
            Theme.showError(this, "Login gagal.\n\n" + ex.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private static final class GradientPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(
                    0, 0, new Color(10, 31, 51),
                    getWidth(), getHeight(), new Color(16, 91, 94));
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
```

## src/ihsse/gui/MainFrame.java

```java
package ihsse.gui;

import ihsse.model.Admin;
import ihsse.service.InduksiService;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.InspeksiService;
import ihsse.service.PengunjungService;
import ihsse.service.StatistikService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jendela utama aplikasi GUI setelah login.
 */
public class MainFrame extends JFrame {

    private final Admin admin;
    private final InformasiService informasiService = new InformasiService();
    private final PengunjungService pengunjungService = new PengunjungService();
    private final InduksiService induksiService = new InduksiService();
    private final InspeksiService inspeksiService = new InspeksiService();
    private final InsidenService insidenService = new InsidenService();
    private final StatistikService statistikService = new StatistikService();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel pageTitleLabel = new JLabel();
    private final JLabel pageSubtitleLabel = new JLabel();
    private final JPanel navPanel = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> refreshables = new LinkedHashMap<>();
    private final Map<String, String> pageTitles = new LinkedHashMap<>();
    private final Map<String, String> pageSubtitles = new LinkedHashMap<>();
    private String currentKey = "dashboard";

    public MainFrame(Admin admin) {
        this.admin = admin;
        setTitle("I-HSSE Safety Induction - " + admin.getNama());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        int lebar = Math.min(1280, Math.max(760, layar.width - 80));
        int tinggi = Math.min(800, Math.max(560, layar.height - 80));
        setSize(lebar, tinggi);
        setMinimumSize(new Dimension(Math.min(1100, lebar), Math.min(700, tinggi)));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        setContentPane(root);

        root.add(buatSidebar(), BorderLayout.WEST);
        root.add(buatKonten(), BorderLayout.CENTER);

        daftarkanHalaman();
        showPage("dashboard");
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(238, 0));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(26, 24, 24, 24));

        JLabel logo = new JLabel("I-HSSE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Safety Induction");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tagline.setForeground(Theme.SIDEBAR_TEXT);
        tagline.setAlignmentX(LEFT_ALIGNMENT);

        JLabel kampus = new JLabel("Universitas Gunadarma J6");
        kampus.setFont(Theme.FONT_SMALL);
        kampus.setForeground(new Color(145, 170, 190));
        kampus.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalStrut(2));
        brand.add(tagline);
        brand.add(Box.createVerticalStrut(8));
        brand.add(kampus);
        sidebar.add(brand, BorderLayout.NORTH);

        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        sidebar.add(navPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(16, 18, 20, 18));

        JPanel adminBox = new JPanel(new BorderLayout(10, 0));
        adminBox.setOpaque(false);

        JLabel avatar = new JLabel(initial(admin.getNama()), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(35, 78, 116));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel adminInfo = new JPanel();
        adminInfo.setOpaque(false);
        adminInfo.setLayout(new BoxLayout(adminInfo, BoxLayout.Y_AXIS));
        JLabel nama = new JLabel(admin.getNama());
        nama.setFont(Theme.FONT_BOLD);
        nama.setForeground(Color.WHITE);
        JLabel jabatan = new JLabel(admin.getJabatan());
        jabatan.setFont(Theme.FONT_SMALL);
        jabatan.setForeground(new Color(160, 184, 202));
        adminInfo.add(nama);
        adminInfo.add(Box.createVerticalStrut(2));
        adminInfo.add(jabatan);

        adminBox.add(avatar, BorderLayout.WEST);
        adminBox.add(adminInfo, BorderLayout.CENTER);

        JButton logout = Theme.navButton("Keluar");
        logout.setForeground(new Color(255, 185, 185));
        logout.setBorder(new EmptyBorder(8, 8, 8, 8));
        logout.addActionListener(e -> logout());

        footer.add(adminBox, BorderLayout.CENTER);
        footer.add(logout, BorderLayout.SOUTH);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buatKonten() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 28, 8, 28));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        pageTitleLabel.setFont(Theme.TITLE);
        pageTitleLabel.setForeground(Theme.TEXT);
        pageSubtitleLabel.setFont(Theme.SUBTITLE);
        pageSubtitleLabel.setForeground(Theme.MUTED);
        titles.add(pageTitleLabel);
        titles.add(Box.createVerticalStrut(3));
        titles.add(pageSubtitleLabel);

        JLabel tanggal = Theme.muted(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy",
                        java.util.Locale.forLanguageTag("id-ID"))));

        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        kanan.setOpaque(false);
        kanan.add(tanggal);

        header.add(titles, BorderLayout.WEST);
        header.add(kanan, BorderLayout.EAST);

        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(8, 28, 24, 28));

        main.add(header, BorderLayout.NORTH);
        main.add(contentPanel, BorderLayout.CENTER);
        return main;
    }

    private void daftarkanHalaman() {
        DashboardPanel dashboard = new DashboardPanel(
                statistikService, insidenService, informasiService);
        tambahHalaman("dashboard", "Dashboard", "Dashboard",
                "Ringkasan aktivitas HSSE kampus J6.", dashboard);

        InformasiPanel informasi = new InformasiPanel(informasiService, this::dataChanged);
        tambahHalaman("informasi", "Informasi HSSE", "Informasi HSSE",
                "Kelola konten Health, Security, Safety, dan Environment.", informasi);

        PengunjungPanel pengunjung = new PengunjungPanel(
                pengunjungService, admin, this::dataChanged);
        tambahHalaman("pengunjung", "Pengunjung & Pass", "Pengunjung & Visitor Pass",
                "Registrasi pengunjung dan penerbitan visitor pass.", pengunjung);

        InduksiPanel induksi = new InduksiPanel(
                pengunjungService, induksiService, admin, this::dataChanged);
        tambahHalaman("induksi", "Induksi & Sertifikat", "Induksi & Sertifikat",
                "Selesaikan safety induction dan terbitkan sertifikat.", induksi);

        InspeksiPanel inspeksi = new InspeksiPanel(
                informasiService, inspeksiService, admin, this::dataChanged);
        tambahHalaman("inspeksi", "Inspeksi Fasilitas", "Inspeksi Fasilitas",
                "Catat pemeriksaan APAR, hydrant, dan fasilitas keselamatan.", inspeksi);

        InsidenPanel insiden = new InsidenPanel(
                insidenService, admin, this::dataChanged);
        tambahHalaman("insiden", "Laporan Insiden", "Laporan Insiden",
                "Terima laporan publik dan proses tindak lanjutnya.", insiden);
    }

    private void tambahHalaman(String key, String navLabel, String pageTitle,
                               String subtitle, JPanel panel) {
        contentPanel.add(panel, key);
        pageTitles.put(key, pageTitle);
        pageSubtitles.put(key, subtitle);
        if (panel instanceof Refreshable) {
            refreshables.put(key, (Refreshable) panel);
        }

        JButton nav = Theme.navButton(navLabel);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nav.setAlignmentX(LEFT_ALIGNMENT);
        nav.addActionListener(e -> showPage(key));
        navButtons.put(key, nav);
        navPanel.add(nav);
        navPanel.add(Box.createVerticalStrut(4));
    }

    private void showPage(String key) {
        currentKey = key;
        cardLayout.show(contentPanel, key);
        pageTitleLabel.setText(pageTitles.getOrDefault(key, ""));
        pageSubtitleLabel.setText(pageSubtitles.getOrDefault(key, ""));

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean aktif = entry.getKey().equals(key);
            JButton button = entry.getValue();
            button.setBackground(aktif ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR);
            button.setForeground(aktif ? Color.WHITE : Theme.SIDEBAR_TEXT);
            button.setFont(aktif ? Theme.FONT_BOLD
                    : new Font("Segoe UI", Font.PLAIN, 14));
        }

        Refreshable refreshable = refreshables.get(key);
        if (refreshable != null) {
            try {
                refreshable.refreshData();
            } catch (RuntimeException ex) {
                Theme.showError(this, "Gagal memuat halaman " + key + ".\n\n"
                        + ex.getMessage());
            }
        }
    }

    private void dataChanged() {
        Refreshable current = refreshables.get(currentKey);
        if (current != null) {
            try {
                current.refreshData();
            } catch (RuntimeException ex) {
                Theme.showError(this, ex.getMessage());
            }
        }
        if (!"dashboard".equals(currentKey)) {
            Refreshable dashboard = refreshables.get("dashboard");
            if (dashboard != null) {
                dashboard.refreshData();
            }
        }
    }

    private void logout() {
        if (!Theme.confirm(this, "Keluar dari aplikasi?")) {
            return;
        }
        dispose();
        new LoginFrame().setVisible(true);
    }

    private String initial(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            return "A";
        }
        String[] bagian = nama.trim().split("\\s+");
        if (bagian.length == 1) {
            return bagian[0].substring(0, 1).toUpperCase();
        }
        return (bagian[0].substring(0, 1) + bagian[1].substring(0, 1)).toUpperCase();
    }

    // Referensi service agar mudah dikembangkan tanpa mengubah kontrak MainFrame.
    public InformasiService getInformasiService() {
        return informasiService;
    }
}
```

## src/ihsse/gui/PengunjungPanel.java

```java
package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.VisitorPass;
import ihsse.service.PengunjungService;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel registrasi pengunjung dan visitor pass.
 */
public class PengunjungPanel extends JPanel implements Refreshable {

    private final PengunjungService service;
    private final Admin admin;
    private final Runnable onChange;

    private final JTextField namaField = Theme.textField();
    private final JTextField identitasField = Theme.textField();
    private final JTextField teleponField = Theme.textField();
    private final JTextField instansiField = Theme.textField();
    private final JTextField tujuanField = Theme.textField();

    private final DefaultTableModel pengunjungModel;
    private final DefaultTableModel passModel;
    private final JTable pengunjungTable;
    private final JTable passTable;
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<Pengunjung> daftarPengunjung = new ArrayList<>();
    private final List<VisitorPass> daftarPass = new ArrayList<>();

    public PengunjungPanel(PengunjungService service, Admin admin, Runnable onChange) {
        this.service = service;
        this.admin = admin;
        this.onChange = onChange;

        setLayout(new BorderLayout());
        setOpaque(false);

        pengunjungModel = new DefaultTableModel(
                new Object[]{"ID", "Nama", "No. Identitas", "Telepon", "Instansi",
                        "Tujuan Kunjungan", "Tgl Daftar", "Induksi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pengunjungTable = Theme.table();
        pengunjungTable.setModel(pengunjungModel);
        pengunjungTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(pengunjungTable, 75, 170, 130, 120, 160, 230, 100, 80);

        passModel = new DefaultTableModel(
                new Object[]{"Nomor", "Tgl Terbit", "Pengunjung", "Tgl Kunjungan",
                        "Jam", "Berlaku Sampai", "Area Tujuan", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        passTable = Theme.table();
        passTable.setModel(passModel);
        passTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.styleTableColumns(passTable, 165, 90, 170, 110, 65, 105, 220, 95);
        passTable.getColumnModel().getColumn(7).setCellRenderer(Theme.statusRenderer());

        tabs.setFont(Theme.FONT);
        tabs.addTab("Registrasi Pengunjung", buatTabRegistrasi());
        tabs.addTab("Visitor Pass", buatTabPass());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buatTabRegistrasi() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Theme.bold("Form Registrasi Pengunjung"), BorderLayout.NORTH);

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Nama Lengkap", namaField);
        Theme.addFormRow(form, gbc, 1, "No. Identitas", identitasField);
        Theme.addFormRow(form, gbc, 2, "No. Telepon", teleponField);
        Theme.addFormRow(form, gbc, 3, "Instansi", instansiField);
        Theme.addFormRow(form, gbc, 4, "Tujuan Kunjungan", tujuanField);

        JPanel aksi = Theme.rightToolbar();
        JButton reset = Theme.secondaryButton("Reset");
        JButton simpan = Theme.primaryButton("Daftarkan Pengunjung");
        reset.addActionListener(e -> bersihkanForm());
        simpan.addActionListener(e -> registrasi());
        aksi.add(reset);
        aksi.add(simpan);
        Theme.addFormRow(form, gbc, 5, "", aksi);

        card.add(form, BorderLayout.CENTER);
        panel.add(card, BorderLayout.NORTH);

        JPanel tabelCard = Theme.card();
        tabelCard.setLayout(new BorderLayout(0, 10));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.bold("Daftar Pengunjung"), BorderLayout.WEST);
        JPanel tombol = Theme.rightToolbar();
        JButton pass = Theme.primaryButton("Terbitkan Pass");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        pass.addActionListener(e -> {
            Pengunjung pengunjung = terpilih();
            if (pengunjung != null) {
                terbitkanPass(pengunjung);
            }
        });
        muat.addActionListener(e -> refreshData());
        tombol.add(pass);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        tabelCard.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(pengunjungTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        tabelCard.add(scroll, BorderLayout.CENTER);
        panel.add(tabelCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buatTabPass() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel judul = new JPanel();
        judul.setOpaque(false);
        judul.setLayout(new javax.swing.BoxLayout(judul, javax.swing.BoxLayout.Y_AXIS));
        judul.add(Theme.bold("Daftar Visitor Pass"));
        JLabelKeterangan.tambah(judul, "Pass berlaku sesuai masa berlaku yang dipilih.");
        header.add(judul, BorderLayout.WEST);

        JPanel tombol = Theme.rightToolbar();
        JButton terbit = Theme.primaryButton("Terbitkan Pass");
        JButton lihat = Theme.secondaryButton("Lihat / Cetak");
        JButton muat = Theme.secondaryButton("Muat Ulang");
        terbit.addActionListener(e -> terbitkanPass(null));
        lihat.addActionListener(e -> lihatPass());
        muat.addActionListener(e -> refreshData());
        tombol.add(terbit);
        tombol.add(lihat);
        tombol.add(muat);
        header.add(tombol, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = Theme.scroll(passTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        daftarPengunjung.clear();
        pengunjungModel.setRowCount(0);
        for (Pengunjung pengunjung : service.semuaPengunjung()) {
            daftarPengunjung.add(pengunjung);
            pengunjungModel.addRow(new Object[]{
                    pengunjung.getId(),
                    pengunjung.getNama(),
                    pengunjung.getNomorIdentitas(),
                    pengunjung.getNoTelepon(),
                    pengunjung.getInstansi(),
                    pengunjung.getTujuanKunjungan(),
                    FormatUtil.tanggal(pengunjung.getTanggalDaftar()),
                    pengunjung.isSudahInduksi() ? "Sudah" : "Belum"
            });
        }

        daftarPass.clear();
        passModel.setRowCount(0);
        for (VisitorPass pass : service.semuaPass()) {
            daftarPass.add(pass);
            passModel.addRow(new Object[]{
                    pass.getNomorDokumen(),
                    FormatUtil.tanggal(pass.getTanggalTerbit()),
                    pass.getPengunjung().getNama(),
                    FormatUtil.tanggal(pass.getTanggalKunjungan()),
                    FormatUtil.jam(pass.getJamMasuk()),
                    FormatUtil.tanggal(pass.getBerlakuSampai()),
                    pass.getAreaTujuan(),
                    pass.isMasihBerlaku(LocalDate.now()) ? "Aktif" : "Kedaluwarsa"
            });
        }
    }

    private void registrasi() {
        try {
            String nama = required(namaField, "Nama lengkap");
            String identitas = required(identitasField, "No. identitas");
            String telepon = required(teleponField, "No. telepon");
            String instansi = required(instansiField, "Instansi");
            String tujuan = required(tujuanField, "Tujuan kunjungan");

            Pengunjung pengunjung = service.registrasi(
                    nama, identitas, telepon, instansi, tujuan);
            bersihkanForm();
            refreshData();
            onChange.run();
            Theme.showInfo(this, "Pengunjung berhasil diregistrasi.\n\nID: "
                    + pengunjung.getId() + "\nNama: " + pengunjung.getNama());
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Registrasi gagal: " + ex.getMessage());
        }
    }

    private void bersihkanForm() {
        namaField.setText("");
        identitasField.setText("");
        teleponField.setText("");
        instansiField.setText("");
        tujuanField.setText("");
        namaField.requestFocusInWindow();
    }

    private Pengunjung terpilih() {
        int row = pengunjungTable.getSelectedRow();
        if (row < 0 || row >= daftarPengunjung.size()) {
            Theme.showInfo(this, "Pilih pengunjung pada tabel terlebih dahulu.");
            return null;
        }
        return daftarPengunjung.get(row);
    }

    private void terbitkanPass(Pengunjung pengunjung) {
        if (daftarPengunjung.isEmpty()) {
            Theme.showInfo(this, "Belum ada pengunjung. Daftarkan pengunjung terlebih dahulu.");
            return;
        }
        VisitorPass pass = VisitorPassDialog.show(this, service, admin, pengunjung);
        if (pass != null) {
            refreshData();
            onChange.run();
            tabs.setSelectedIndex(1);
            DokumenPreviewDialog.show(this, "Visitor Pass - " + pass.getNomorDokumen(),
                    pass.cetak());
        }
    }

    private void lihatPass() {
        int row = passTable.getSelectedRow();
        if (row < 0 || row >= daftarPass.size()) {
            Theme.showInfo(this, "Pilih visitor pass pada tabel terlebih dahulu.");
            return;
        }
        VisitorPass pass = daftarPass.get(row);
        DokumenPreviewDialog.show(this, "Visitor Pass - " + pass.getNomorDokumen(),
                pass.cetak());
    }

    private String required(JTextField field, String label) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return field.getText().trim();
    }

    /**
     * Helper kecil agar tidak bergantung pada import JLabel di banyak tempat.
     */
    private static final class JLabelKeterangan {
        private static void tambah(JPanel panel, String teks) {
            javax.swing.JLabel label = Theme.muted(teks);
            label.setFont(Theme.FONT_SMALL);
            panel.add(label);
        }
    }
}
```

## src/ihsse/gui/Refreshable.java

```java
package ihsse.gui;

/**
 * Kontrak untuk panel yang bisa memuat ulang data dari database.
 */
public interface Refreshable {

    void refreshData();
}
```

## src/ihsse/gui/Theme.java

```java
package ihsse.gui;

import ihsse.model.StatusInsiden;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tema warna, font, dan komponen standar aplikasi GUI.
 */
public final class Theme {

    public static final Color SIDEBAR = new Color(15, 42, 68);
    public static final Color SIDEBAR_ACTIVE = new Color(24, 61, 96);
    public static final Color SIDEBAR_TEXT = new Color(206, 218, 230);
    public static final Color ACCENT = new Color(14, 159, 110);
    public static final Color ACCENT_DARK = new Color(11, 128, 88);
    public static final Color ACCENT_SOFT = new Color(225, 247, 238);
    public static final Color DANGER = new Color(214, 69, 69);
    public static final Color WARNING = new Color(191, 120, 0);
    public static final Color BACKGROUND = new Color(244, 246, 248);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(33, 37, 41);
    public static final Color MUTED = new Color(108, 117, 125);
    public static final Color BORDER = new Color(222, 226, 230);
    public static final Color TABLE_HEADER = new Color(248, 249, 250);

    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BIG_NUMBER = new Font("Segoe UI", Font.BOLD, 28);

    private Theme() {
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fallback ke look and feel default
        }
        UIManager.put("control", BACKGROUND);
        UIManager.put("info", CARD);
        UIManager.put("nimbusBase", SIDEBAR);
        UIManager.put("nimbusBlueGrey", new Color(210, 216, 222));
        UIManager.put("nimbusLightBackground", CARD);
        UIManager.put("text", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(250, 251, 252));
        UIManager.put("Table.showGrid", false);
        UIManager.put("defaultFont", FONT);
    }

    public static JButton primaryButton(String text) {
        return new RoundedButton(text, ACCENT, Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        return new RoundedButton(text, new Color(233, 236, 239), TEXT);
    }

    public static JButton dangerButton(String text) {
        return new RoundedButton(text, DANGER, Color.WHITE);
    }

    public static JButton navButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(SIDEBAR_TEXT);
        button.setBackground(SIDEBAR);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!SIDEBAR_ACTIVE.equals(button.getBackground())) {
                    button.setBackground(new Color(20, 51, 80));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!SIDEBAR_ACTIVE.equals(button.getBackground())) {
                    button.setBackground(SIDEBAR);
                }
            }
        });
        return button;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SUBTITLE);
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel bold(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT);
        label.setForeground(MUTED);
        return label;
    }

    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setFont(FONT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 9, 7, 9)));
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 9, 7, 9)));
        return field;
    }

    public static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setFont(FONT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        area.setBackground(CARD);
        area.setForeground(TEXT);
        return area;
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(new LineBorder(BORDER, 1, true));
        scrollPane.getViewport().setBackground(CARD);
        return scrollPane;
    }

    public static JTable table() {
        JTable table = new JTable();
        table.setFont(FONT);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(237, 240, 243));
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        return table;
    }

    public static JComboBox<String> comboBox(String... items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(FONT);
        comboBox.setBackground(CARD);
        return comboBox;
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Terjadi Kesalahan",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Informasi",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    public static Color statusColor(String status) {
        if (status == null) {
            return TEXT;
        }
        String value = status.trim();
        if (value.equalsIgnoreCase(StatusInsiden.SELESAI.getLabel())
                || value.equalsIgnoreCase("Aktif")
                || value.equalsIgnoreCase("Baik")
                || value.equalsIgnoreCase("Sudah")) {
            return ACCENT_DARK;
        }
        if (value.equalsIgnoreCase(StatusInsiden.DIPROSES.getLabel())
                || value.equalsIgnoreCase("Rusak Ringan")
                || value.equalsIgnoreCase("Sedang")) {
            return WARNING;
        }
        if (value.equalsIgnoreCase(StatusInsiden.BARU.getLabel())
                || value.equalsIgnoreCase("Kedaluwarsa")
                || value.equalsIgnoreCase("Rusak Berat")
                || value.equalsIgnoreCase("Tinggi")) {
            return DANGER;
        }
        return TEXT;
    }

    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus, int row,
                                                           int column) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                component.setFont(FONT_BOLD);
                if (!isSelected) {
                    component.setForeground(statusColor(String.valueOf(value)));
                }
                return component;
            }
        };
    }

    public static JPanel formPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new java.awt.GridBagLayout());
        return panel;
    }

    public static void addFormRow(JPanel panel, java.awt.GridBagConstraints gbc,
                                  int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(6, 0, 6, 12);
        panel.add(bold(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 0, 6, 0);
        panel.add(field, gbc);
    }

    public static void fillForm(JPanel panel) {
        java.awt.GridBagConstraints spacer = new java.awt.GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = 999;
        spacer.weighty = 1;
        panel.add(new JLabel(""), spacer);
    }

    public static JPanel labelValue(String label, JComponent value) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(bold(label), java.awt.BorderLayout.NORTH);
        panel.add(value, java.awt.BorderLayout.CENTER);
        return panel;
    }

    public static JPanel toolbar() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel rightToolbar() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static void styleTableColumns(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static JLabel badge(String text, Color color) {
        JLabel label = new JLabel("  " + text + "  ");
        label.setFont(FONT_BOLD);
        label.setForeground(color);
        label.setOpaque(true);
        label.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 24));
        label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        return label;
    }

    private static final class RoundedButton extends JButton {

        private final Color baseColor;

        private RoundedButton(String text, Color background, Color foreground) {
            super(text);
            this.baseColor = background;
            setFont(FONT_BOLD);
            setForeground(foreground);
            setBackground(background);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 16, 9, 16));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        setBackground(baseColor.brighter());
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(baseColor);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? getBackground() : new Color(205, 210, 216));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
```

## src/ihsse/gui/VisitorPassDialog.java

```java
package ihsse.gui;

import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.VisitorPass;
import ihsse.service.PengunjungService;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Dialog penerbitan visitor pass.
 */
public class VisitorPassDialog extends JDialog {

    private final PengunjungService service;
    private final Admin admin;
    private final JComboBox<Pengunjung> pengunjungCombo;
    private final JTextField tanggalField = Theme.textField();
    private final JTextField jamField = Theme.textField();
    private final JSpinner masaSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 30, 1));
    private final JTextField areaField = Theme.textField();
    private VisitorPass hasil;

    private VisitorPassDialog(Frame owner, PengunjungService service, Admin admin,
                              Pengunjung selected) {
        super(owner, "Terbitkan Visitor Pass", true);
        this.service = service;
        this.admin = admin;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Dimension layar = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(590, layar.width - 60), Math.min(500, layar.height - 80));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(18, 22, 16, 22));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(Theme.title("Terbitkan Visitor Pass"));
        header.add(javax.swing.Box.createVerticalStrut(3));
        header.add(Theme.muted("Tanggal: yyyy-MM-dd, jam: HH:mm."));
        root.add(header, BorderLayout.NORTH);

        List<Pengunjung> daftar = service.semuaPengunjung();
        pengunjungCombo = new JComboBox<>(daftar.toArray(new Pengunjung[0]));
        pengunjungCombo.setFont(Theme.FONT);
        pengunjungCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                           int index, boolean isSelected,
                                                           boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof Pengunjung) {
                    Pengunjung pengunjung = (Pengunjung) value;
                    setText(pengunjung.getId() + " - " + pengunjung.getNama()
                            + " (" + pengunjung.getInstansi() + ")");
                }
                return this;
            }
        });

        if (selected != null) {
            for (int i = 0; i < pengunjungCombo.getItemCount(); i++) {
                Pengunjung item = pengunjungCombo.getItemAt(i);
                if (item.getId().equals(selected.getId())) {
                    pengunjungCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        tanggalField.setText(LocalDate.now().toString());
        jamField.setText(LocalTime.now().withSecond(0).withNano(0).toString());
        areaField.setText("Lantai 6 - Ruang Seminar");

        JPanel form = Theme.formPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        Theme.addFormRow(form, gbc, 0, "Pengunjung", pengunjungCombo);
        Theme.addFormRow(form, gbc, 1, "Tanggal Kunjungan", tanggalField);
        Theme.addFormRow(form, gbc, 2, "Jam Masuk", jamField);
        Theme.addFormRow(form, gbc, 3, "Masa Berlaku (hari)", masaSpinner);
        Theme.addFormRow(form, gbc, 4, "Area Tujuan", areaField);
        root.add(form, BorderLayout.CENTER);

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton batal = Theme.secondaryButton("Batal");
        JButton simpan = Theme.primaryButton("Terbitkan");
        batal.addActionListener(e -> dispose());
        simpan.addActionListener(e -> simpan());
        tombol.add(batal);
        tombol.add(simpan);
        root.add(tombol, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(simpan);
    }

    private void simpan() {
        try {
            Pengunjung pengunjung = (Pengunjung) pengunjungCombo.getSelectedItem();
            if (pengunjung == null) {
                throw new IllegalArgumentException("Pengunjung wajib dipilih.");
            }
            LocalDate tanggal = FormatUtil.parseTanggal(tanggalField.getText());
            LocalTime jam = FormatUtil.parseJam(jamField.getText());
            int masa = (Integer) masaSpinner.getValue();
            String area = areaField.getText();
            if (area == null || area.trim().isEmpty()) {
                throw new IllegalArgumentException("Area tujuan wajib diisi.");
            }

            hasil = service.terbitkanVisitorPass(
                    pengunjung, admin, tanggal, jam, masa, area.trim());
            dispose();
        } catch (IllegalArgumentException ex) {
            Theme.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Theme.showError(this, "Gagal menerbitkan pass: " + ex.getMessage());
        }
    }

    public static VisitorPass show(Component parent, PengunjungService service,
                                   Admin admin, Pengunjung selected) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        Frame frame = owner instanceof Frame ? (Frame) owner : null;
        VisitorPassDialog dialog = new VisitorPassDialog(frame, service, admin, selected);
        dialog.setVisible(true);
        return dialog.hasil;
    }
}
```

## src/ihsse/main/Main.java

```java
package ihsse.main;

import ihsse.gui.GuiMain;

/**
 * Launcher kompatibilitas.
 * Aplikasi sekarang berbasis GUI Swing; entry point utama ada di
 * {@link GuiMain}.
 */
public class Main {

    public static void main(String[] args) {
        GuiMain.main(args);
    }
}
```

## src/ihsse/model/Admin.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * INHERITANCE: Admin juga merupakan Pengguna, tetapi memiliki data kepegawaian.
 */
public class Admin extends Pengguna {

    private String username;
    private String jabatan;

    public Admin(String id, String nama, String nomorIdentitas, String noTelepon,
                 String username, String jabatan) {
        super(id, nama, nomorIdentitas, noTelepon);
        this.username = Validasi.wajibIsi(username, "Username");
        this.jabatan = Validasi.wajibIsi(jabatan, "Jabatan");
    }

    @Override
    public String getPeran() {
        return "Admin";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = Validasi.wajibIsi(username, "Username");
    }

    public String getJabatan() {
        return jabatan;
    }

    public void setJabatan(String jabatan) {
        this.jabatan = Validasi.wajibIsi(jabatan, "Jabatan");
    }
}
```

## src/ihsse/model/AreaMerokok.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk area dilarang merokok (Health).
 */
public class AreaMerokok extends InformasiHSSE {

    private double luasArea;
    private String aturan;
    private String sanksi;

    public AreaMerokok(String kode, String nama, String lokasi, String deskripsi,
                       double luasArea, String aturan, String sanksi) {
        super(kode, nama, lokasi, deskripsi);
        if (luasArea <= 0) {
            throw new IllegalArgumentException("Luas area harus lebih dari 0.");
        }
        this.luasArea = luasArea;
        this.aturan = Validasi.wajibIsi(aturan, "Aturan");
        this.sanksi = Validasi.wajibIsi(sanksi, "Sanksi");
    }

    @Override
    public String getKategori() {
        return "Health";
    }

    @Override
    public String getJenis() {
        return "Area Merokok";
    }

    /**
     * POLYMORPHISM: detail khusus AreaMerokok ditambahkan di sini.
     */
    @Override
    public String tampilRingkas() {
        return super.tampilRingkas() + String.format(" | Luas: %.0f m2", luasArea);
    }

    public double getLuasArea() {
        return luasArea;
    }

    public void setLuasArea(double luasArea) {
        if (luasArea <= 0) {
            throw new IllegalArgumentException("Luas area harus lebih dari 0.");
        }
        this.luasArea = luasArea;
    }

    public String getAturan() {
        return aturan;
    }

    public String getSanksi() {
        return sanksi;
    }
}
```

## src/ihsse/model/DenahGedung.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Denah gedung per lantai (lantai 1 sampai 11).
 * Daftar fasilitas penting disimpan sebagai List dan dibungkus unmodifiable
 * agar tidak bisa diubah dari luar (encapsulation).
 */
public class DenahGedung {

    private int lantai;
    private String namaFile;
    private String deskripsi;
    private List<String> fasilitasPenting;

    public DenahGedung(int lantai, String namaFile, String deskripsi,
                       List<String> fasilitasPenting) {
        if (lantai < 1) {
            throw new IllegalArgumentException("Lantai minimal 1.");
        }
        this.lantai = lantai;
        this.namaFile = Validasi.wajibIsi(namaFile, "Nama file denah");
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi denah");
        this.fasilitasPenting = new ArrayList<>(Validasi.wajibAda(fasilitasPenting, "Fasilitas penting"));
    }

    public String tampilRingkas() {
        return String.format("Lantai %-2d | %-18s | %s | Fasilitas: %s",
                lantai, namaFile, deskripsi, String.join(", ", fasilitasPenting));
    }

    public int getLantai() {
        return lantai;
    }

    public String getNamaFile() {
        return namaFile;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public List<String> getFasilitasPenting() {
        return Collections.unmodifiableList(fasilitasPenting);
    }
}
```

## src/ihsse/model/Dicetak.java

```java
package ihsse.model;

/**
 * Kontrak untuk semua dokumen yang bisa dicetak.
 * Interface ini membuat proses cetak seragam, walaupun isi tiap dokumen berbeda.
 */
public interface Dicetak {

    String cetak();

    String getNomorDokumen();

    /**
     * Default method: semua kelas yang mengimplementasikan Dicetak
     * otomatis memiliki garis pemisah tanpa harus menuliskannya ulang.
     */
    default String garisPemisah() {
        return "================================================================";
    }
}
```

## src/ihsse/model/DokumenHSSE.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * ABSTRACT CLASS untuk semua dokumen HSSE.
 *
 * Cetak memakai pola Template Method: alur cetak (header, isi, footer)
 * sudah ditentukan di sini, sedangkan isi detail diserahkan ke subclass.
 */
public abstract class DokumenHSSE implements Dicetak {

    private String nomorDokumen;
    private LocalDate tanggalTerbit;
    private String diterbitkanOleh;

    protected DokumenHSSE(String nomorDokumen, LocalDate tanggalTerbit, String diterbitkanOleh) {
        this.nomorDokumen = Validasi.wajibIsi(nomorDokumen, "Nomor dokumen");
        this.tanggalTerbit = Validasi.wajibAda(tanggalTerbit, "Tanggal terbit");
        this.diterbitkanOleh = Validasi.wajibIsi(diterbitkanOleh, "Penerbit dokumen");
    }

    /**
     * FINAL: alur cetak tidak boleh diubah subclass.
     * Subclass hanya menyuplai judul, isi, dan catatan kaki.
     */
    @Override
    public final String cetak() {
        StringBuilder hasil = new StringBuilder();
        hasil.append(garisPemisah()).append(System.lineSeparator());
        hasil.append("        ").append(getJudulDokumen().toUpperCase(Locale.ROOT))
                .append(System.lineSeparator());
        hasil.append(garisPemisah()).append(System.lineSeparator());
        hasil.append(baris("Nomor Dokumen", nomorDokumen));
        hasil.append(baris("Tanggal Terbit", tanggalTerbit.format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        hasil.append(baris("Diterbitkan Oleh", diterbitkanOleh));
        hasil.append("-".repeat(64)).append(System.lineSeparator());
        hasil.append(getIsiDokumen());
        hasil.append("-".repeat(64)).append(System.lineSeparator());
        hasil.append(getCatatanKaki()).append(System.lineSeparator());
        hasil.append(garisPemisah());
        return hasil.toString();
    }

    public abstract String getJudulDokumen();

    /**
     * ABSTRACT METHOD: setiap jenis dokumen mengisi bagian isi dengan caranya sendiri.
     */
    protected abstract String getIsiDokumen();

    protected String getCatatanKaki() {
        return "Dokumen ini dihasilkan oleh sistem I-HSSE Universitas Gunadarma Kampus J6.";
    }

    protected String baris(String label, Object nilai) {
        return String.format("  %-20s: %s%n", label, nilai == null ? "-" : nilai);
    }

    @Override
    public String getNomorDokumen() {
        return nomorDokumen;
    }

    public LocalDate getTanggalTerbit() {
        return tanggalTerbit;
    }

    public String getDiterbitkanOleh() {
        return diterbitkanOleh;
    }
}
```

## src/ihsse/model/FasilitasKeamanan.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk fasilitas keamanan (Security).
 * Contoh: parkiran motor, CCTV, smart lock door, pos satpam.
 */
public class FasilitasKeamanan extends InformasiHSSE {

    private String jenis;
    private int jumlah;
    private KondisiFasilitas kondisi;

    public FasilitasKeamanan(String kode, String nama, String lokasi, String deskripsi,
                           String jenis, int jumlah, KondisiFasilitas kondisi) {
        super(kode, nama, lokasi, deskripsi);
        this.jenis = Validasi.wajibIsi(jenis, "Jenis fasilitas");
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
    }

    @Override
    public String getKategori() {
        return "Security";
    }

    @Override
    public String getJenis() {
        return jenis;
    }

    @Override
    public String tampilRingkas() {
        return super.tampilRingkas()
                + " | Jumlah: " + jumlah
                + " | Kondisi: " + kondisi.getLabel();
    }

    public int getJumlah() {
        return jumlah;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
    }

    public KondisiFasilitas getKondisi() {
        return kondisi;
    }

    public void setKondisi(KondisiFasilitas kondisi) {
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
    }
}
```

## src/ihsse/model/FasilitasKeselamatan.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Turunan InformasiHSSE untuk fasilitas keselamatan (Safety).
 * Contoh: APAR, hydrant, tangga darurat, titik kumpul.
 */
public class FasilitasKeselamatan extends InformasiHSSE {

    private String jenis;
    private int jumlah;
    private KondisiFasilitas kondisi;
    private LocalDate masaKedaluwarsa;

    public FasilitasKeselamatan(String kode, String nama, String lokasi, String deskripsi,
                               String jenis, int jumlah, KondisiFasilitas kondisi,
                               LocalDate masaKedaluwarsa) {
        super(kode, nama, lokasi, deskripsi);
        this.jenis = Validasi.wajibIsi(jenis, "Jenis fasilitas");
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
        // Boleh null untuk fasilitas yang tidak memiliki masa kedaluwarsa,
        // misalnya tangga darurat atau titik kumpul.
        this.masaKedaluwarsa = masaKedaluwarsa;
    }

    @Override
    public String getKategori() {
        return "Safety";
    }

    @Override
    public String getJenis() {
        return jenis;
    }

    @Override
    public String tampilRingkas() {
        String infoKedaluwarsa = masaKedaluwarsa == null
                ? "-"
                : masaKedaluwarsa.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return super.tampilRingkas()
                + " | Jumlah: " + jumlah
                + " | Kondisi: " + kondisi.getLabel()
                + " | Kedaluwarsa: " + infoKedaluwarsa;
    }

    public boolean isKedaluwarsa(LocalDate tanggalAcuan) {
        return masaKedaluwarsa != null && masaKedaluwarsa.isBefore(tanggalAcuan);
    }

    public int getJumlah() {
        return jumlah;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
    }

    public KondisiFasilitas getKondisi() {
        return kondisi;
    }

    public void setKondisi(KondisiFasilitas kondisi) {
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
    }

    public LocalDate getMasaKedaluwarsa() {
        return masaKedaluwarsa;
    }

    public void setMasaKedaluwarsa(LocalDate masaKedaluwarsa) {
        this.masaKedaluwarsa = masaKedaluwarsa;
    }
}
```

## src/ihsse/model/FasilitasLingkungan.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk menu Environment.
 * Contoh: pom bensin kampus dan toko drum/plastik.
 *
 * Catatan: kelas ini ditambahkan agar menu Environment pada aplikasi
 * tetap terwakili. Tidak ada harga, pembayaran, atau stok sesuai batasan tugas.
 */
public class FasilitasLingkungan extends InformasiHSSE {

    private String jenis;
    private String jamOperasional;
    private String penanggungJawab;

    public FasilitasLingkungan(String kode, String nama, String lokasi, String deskripsi,
                               String jenis, String jamOperasional, String penanggungJawab) {
        super(kode, nama, lokasi, deskripsi);
        this.jenis = Validasi.wajibIsi(jenis, "Jenis fasilitas lingkungan");
        this.jamOperasional = Validasi.wajibIsi(jamOperasional, "Jam operasional");
        this.penanggungJawab = Validasi.wajibIsi(penanggungJawab, "Penanggung jawab");
    }

    @Override
    public String getKategori() {
        return "Environment";
    }

    @Override
    public String getJenis() {
        return jenis;
    }

    @Override
    public String tampilRingkas() {
        return super.tampilRingkas()
                + " | Jam: " + jamOperasional
                + " | PJ: " + penanggungJawab;
    }

    public String getJamOperasional() {
        return jamOperasional;
    }

    public String getPenanggungJawab() {
        return penanggungJawab;
    }
}
```

## src/ihsse/model/InformasiHSSE.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * ABSTRACT CLASS untuk seluruh konten informasi HSSE.
 * Turunannya: AreaMerokok, FasilitasKeamanan, FasilitasKeselamatan, ZonaRisiko.
 */
public abstract class InformasiHSSE {

    private String kode;
    private String nama;
    private String lokasi;
    private String deskripsi;

    protected InformasiHSSE(String kode, String nama, String lokasi, String deskripsi) {
        this.kode = Validasi.wajibIsi(kode, "Kode informasi");
        this.nama = Validasi.wajibIsi(nama, "Nama informasi");
        this.lokasi = Validasi.wajibIsi(lokasi, "Lokasi");
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi");
    }

    /**
     * ABSTRACT METHOD: kategori ditentukan oleh masing-masing turunan.
     */
    public abstract String getKategori();

    /**
     * ABSTRACT METHOD: jenis konten juga ditentukan oleh turunan.
     */
    public abstract String getJenis();

    /**
     * Method konkret yang bisa diwarisi. Turunan boleh menambah detail
     * dengan cara meng-override method ini (polymorphism).
     */
    public String tampilRingkas() {
        return String.format("[%s] %-8s | %-22s | %s | %s",
                kode, getKategori(), getJenis(), nama, lokasi);
    }

    public String getKode() {
        return kode;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = Validasi.wajibIsi(nama, "Nama informasi");
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = Validasi.wajibIsi(lokasi, "Lokasi");
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi");
    }
}
```

## src/ihsse/model/KondisiFasilitas.java

```java
package ihsse.model;

/**
 * Kondisi fasilitas hasil inspeksi.
 */
public enum KondisiFasilitas {

    BAIK("Baik", false),
    RUSAK_RINGAN("Rusak Ringan", true),
    RUSAK_BERAT("Rusak Berat", true),
    KEDALUWARSA("Kedaluwarsa", true);

    private final String label;
    private final boolean perluTindakLanjut;

    KondisiFasilitas(String label, boolean perluTindakLanjut) {
        this.label = label;
        this.perluTindakLanjut = perluTindakLanjut;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPerluTindakLanjut() {
        return perluTindakLanjut;
    }
}
```

## src/ihsse/model/LaporanInsiden.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dokumen Laporan Insiden (fitur 4).
 * Status awal selalu BARU dan hanya bisa diproses oleh admin.
 */
public class LaporanInsiden extends DokumenHSSE {

    private static final DateTimeFormatter FORMAT_WAKTU =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private String namaPelapor;
    private String kontakPelapor;
    private String lokasi;
    private String deskripsi;
    private LocalDateTime waktuKejadian;
    private StatusInsiden status;
    private String catatanAdmin;
    private Admin ditanganiOleh;

    public LaporanInsiden(String nomorDokumen, LocalDate tanggalTerbit,
                          String diterbitkanOleh, String namaPelapor,
                          String kontakPelapor, String lokasi, String deskripsi,
                          LocalDateTime waktuKejadian) {
        this(nomorDokumen, tanggalTerbit, diterbitkanOleh, namaPelapor,
                kontakPelapor, lokasi, deskripsi, waktuKejadian,
                StatusInsiden.BARU, "-", null);
    }

    /**
     * Constructor untuk membaca laporan yang sudah tersimpan di database.
     */
    public LaporanInsiden(String nomorDokumen, LocalDate tanggalTerbit,
                          String diterbitkanOleh, String namaPelapor,
                          String kontakPelapor, String lokasi, String deskripsi,
                          LocalDateTime waktuKejadian, StatusInsiden status,
                          String catatanAdmin, Admin ditanganiOleh) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.namaPelapor = Validasi.wajibIsi(namaPelapor, "Nama pelapor");
        this.kontakPelapor = Validasi.wajibIsi(kontakPelapor, "Kontak pelapor");
        this.lokasi = Validasi.wajibIsi(lokasi, "Lokasi insiden");
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi insiden");
        this.waktuKejadian = Validasi.wajibAda(waktuKejadian, "Waktu kejadian");
        this.status = Validasi.wajibAda(status, "Status insiden");
        this.catatanAdmin = catatanAdmin == null || catatanAdmin.trim().isEmpty()
                ? "-" : catatanAdmin.trim();
        this.ditanganiOleh = ditanganiOleh;
    }

    /**
     * Perubahan status mengikuti aturan pada enum StatusInsiden.
     * Jika tidak valid, method melempar exception (dicegah di layer service/Main).
     */
    public void proses(StatusInsiden statusBaru, String catatan, Admin admin) {
        if (statusBaru == null) {
            throw new IllegalArgumentException("Status baru wajib diisi.");
        }
        if (!status.bolehBerubahKe(statusBaru)) {
            throw new IllegalStateException("Status tidak dapat berubah dari "
                    + status.getLabel() + " ke " + statusBaru.getLabel() + ".");
        }
        this.status = statusBaru;
        this.ditanganiOleh = Validasi.wajibAda(admin, "Admin yang memproses");
        if (catatan != null && !catatan.trim().isEmpty()) {
            this.catatanAdmin = catatan.trim();
        }
    }

    @Override
    public String getJudulDokumen() {
        return "Laporan Insiden";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Nama Pelapor", namaPelapor));
        isi.append(baris("Kontak", kontakPelapor));
        isi.append(baris("Waktu Kejadian", waktuKejadian.format(FORMAT_WAKTU)));
        isi.append(baris("Lokasi", lokasi));
        isi.append(baris("Deskripsi", deskripsi));
        isi.append(baris("Status", status.getLabel()));
        isi.append(baris("Ditangani Oleh", ditanganiOleh == null
                ? "-" : ditanganiOleh.getNama() + " (" + ditanganiOleh.getJabatan() + ")"));
        isi.append(baris("Catatan Admin", catatanAdmin));
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        if (status == StatusInsiden.SELESAI) {
            return "Laporan dinyatakan SELESAI. Tindak lanjut sudah dicatat oleh admin.";
        }
        if (status == StatusInsiden.DIPROSES) {
            return "Laporan sedang DIPROSES oleh petugas terkait.";
        }
        return "Laporan BARU diterima dan menunggu verifikasi admin.";
    }

    public String getNamaPelapor() {
        return namaPelapor;
    }

    public String getKontakPelapor() {
        return kontakPelapor;
    }

    public String getLokasi() {
        return lokasi;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public LocalDateTime getWaktuKejadian() {
        return waktuKejadian;
    }

    public StatusInsiden getStatus() {
        return status;
    }

    public String getCatatanAdmin() {
        return catatanAdmin;
    }

    public Admin getDitanganiOleh() {
        return ditanganiOleh;
    }
}
```

## src/ihsse/model/LaporanInspeksi.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dokumen Laporan Inspeksi APAR/hydrant (fitur 3).
 */
public class LaporanInspeksi extends DokumenHSSE {

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private FasilitasKeselamatan fasilitas;
    private LocalDate tanggalCek;
    private KondisiFasilitas kondisi;
    private LocalDate masaKedaluwarsa;
    private String namaPetugas;
    private String tindakan;
    private String catatan;

    public LaporanInspeksi(String nomorDokumen, LocalDate tanggalTerbit,
                           String diterbitkanOleh, FasilitasKeselamatan fasilitas,
                           LocalDate tanggalCek, KondisiFasilitas kondisi,
                           LocalDate masaKedaluwarsa, String namaPetugas,
                           String tindakan, String catatan) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.fasilitas = Validasi.wajibAda(fasilitas, "Fasilitas");
        this.tanggalCek = Validasi.wajibAda(tanggalCek, "Tanggal cek");
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi");
        this.masaKedaluwarsa = masaKedaluwarsa;
        this.namaPetugas = Validasi.wajibIsi(namaPetugas, "Nama petugas");
        this.tindakan = Validasi.wajibIsi(tindakan, "Tindakan");
        this.catatan = catatan == null ? "-" : catatan.trim();
    }

    @Override
    public String getJudulDokumen() {
        return "Laporan Inspeksi APAR/Hydrant";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Kode Fasilitas", fasilitas.getKode()));
        isi.append(baris("Nama Fasilitas", fasilitas.getNama()));
        isi.append(baris("Jenis", fasilitas.getJenis()));
        isi.append(baris("Lokasi", fasilitas.getLokasi()));
        isi.append(baris("Tanggal Pemeriksaan", tanggalCek.format(FORMAT_TANGGAL)));
        isi.append(baris("Kondisi", kondisi.getLabel()));
        isi.append(baris("Masa Kedaluwarsa", masaKedaluwarsa == null
                ? "-" : masaKedaluwarsa.format(FORMAT_TANGGAL)));
        isi.append(baris("Petugas", namaPetugas));
        isi.append(baris("Tindakan", tindakan));
        isi.append(baris("Catatan", catatan));
        isi.append(baris("Rekomendasi", kondisi.isPerluTindakLanjut()
                ? "Perlu tindak lanjut dan pengecekan ulang."
                : "Fasilitas layak dipakai, lanjutkan pemantauan berkala."));
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        return "Laporan ini menjadi dasar pemeliharaan fasilitas keselamatan kampus J6.";
    }

    public FasilitasKeselamatan getFasilitas() {
        return fasilitas;
    }

    public LocalDate getTanggalCek() {
        return tanggalCek;
    }

    public KondisiFasilitas getKondisi() {
        return kondisi;
    }

    public LocalDate getMasaKedaluwarsa() {
        return masaKedaluwarsa;
    }

    public String getNamaPetugas() {
        return namaPetugas;
    }

    public String getTindakan() {
        return tindakan;
    }

    public String getCatatan() {
        return catatan;
    }
}
```

## src/ihsse/model/Pengguna.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * ABSTRACT CLASS: induk dari Pengunjung dan Admin.
 * Semua atribut private (encapsulation), akses hanya lewat getter/setter.
 */
public abstract class Pengguna {

    private String id;
    private String nama;
    private String nomorIdentitas;
    private String noTelepon;

    protected Pengguna(String id, String nama, String nomorIdentitas, String noTelepon) {
        this.id = Validasi.wajibIsi(id, "ID pengguna");
        setNama(nama);
        setNomorIdentitas(nomorIdentitas);
        setNoTelepon(noTelepon);
    }

    /**
     * ABSTRACT METHOD: setiap turunan wajib menjelaskan perannya sendiri.
     * Inilah abstraction yang membuat Pengguna tidak bisa dibuat langsung.
     */
    public abstract String getPeran();

    public String tampilInfo() {
        return String.format("%s | %s | %s", id, nama, getPeran());
    }

    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public final void setNama(String nama) {
        this.nama = Validasi.wajibIsi(nama, "Nama");
    }

    public String getNomorIdentitas() {
        return nomorIdentitas;
    }

    public final void setNomorIdentitas(String nomorIdentitas) {
        this.nomorIdentitas = Validasi.wajibIsi(nomorIdentitas, "Nomor identitas");
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public final void setNoTelepon(String noTelepon) {
        this.noTelepon = Validasi.wajibIsi(noTelepon, "Nomor telepon");
    }
}
```

## src/ihsse/model/Pengunjung.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;

/**
 * INHERITANCE: Pengunjung mewarisi atribut dan method dasar dari Pengguna.
 */
public class Pengunjung extends Pengguna {

    private String instansi;
    private String tujuanKunjungan;
    private LocalDate tanggalDaftar;
    private boolean sudahInduksi;

    public Pengunjung(String id, String nama, String nomorIdentitas, String noTelepon,
                      String instansi, String tujuanKunjungan) {
        this(id, nama, nomorIdentitas, noTelepon, instansi, tujuanKunjungan,
                LocalDate.now(), false);
    }

    /**
     * Constructor untuk membaca data yang sudah tersimpan di database.
     */
    public Pengunjung(String id, String nama, String nomorIdentitas, String noTelepon,
                      String instansi, String tujuanKunjungan,
                      LocalDate tanggalDaftar, boolean sudahInduksi) {
        super(id, nama, nomorIdentitas, noTelepon);
        this.instansi = Validasi.wajibIsi(instansi, "Instansi");
        this.tujuanKunjungan = Validasi.wajibIsi(tujuanKunjungan, "Tujuan kunjungan");
        this.tanggalDaftar = Validasi.wajibAda(tanggalDaftar, "Tanggal daftar");
        this.sudahInduksi = sudahInduksi;
    }

    /**
     * POLYMORPHISM: method yang sama dengan Admin, tetapi hasilnya berbeda.
     */
    @Override
    public String getPeran() {
        return "Pengunjung";
    }

    public void tandaiSudahInduksi() {
        this.sudahInduksi = true;
    }

    public String getInstansi() {
        return instansi;
    }

    public void setInstansi(String instansi) {
        this.instansi = Validasi.wajibIsi(instansi, "Instansi");
    }

    public String getTujuanKunjungan() {
        return tujuanKunjungan;
    }

    public void setTujuanKunjungan(String tujuanKunjungan) {
        this.tujuanKunjungan = Validasi.wajibIsi(tujuanKunjungan, "Tujuan kunjungan");
    }

    public LocalDate getTanggalDaftar() {
        return tanggalDaftar;
    }

    public boolean isSudahInduksi() {
        return sudahInduksi;
    }
}
```

## src/ihsse/model/SertifikatInduksi.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dokumen Sertifikat Induksi (fitur 2).
 */
public class SertifikatInduksi extends DokumenHSSE {

    private Pengunjung pengunjung;
    private List<String> materi;
    private LocalDate tanggalSelesai;
    private String namaFasilitator;

    public SertifikatInduksi(String nomorDokumen, LocalDate tanggalTerbit,
                            String diterbitkanOleh, Pengunjung pengunjung,
                            List<String> materi, LocalDate tanggalSelesai,
                            String namaFasilitator) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.pengunjung = Validasi.wajibAda(pengunjung, "Pengunjung");
        if (materi == null || materi.isEmpty()) {
            throw new IllegalArgumentException("Materi induksi tidak boleh kosong.");
        }
        this.materi = new ArrayList<>(materi);
        this.tanggalSelesai = Validasi.wajibAda(tanggalSelesai, "Tanggal selesai");
        this.namaFasilitator = Validasi.wajibIsi(namaFasilitator, "Nama fasilitator");
    }

    @Override
    public String getJudulDokumen() {
        return "Sertifikat Induksi Keselamatan";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Nama Peserta", pengunjung.getNama()));
        isi.append(baris("Nomor Identitas", pengunjung.getNomorIdentitas()));
        isi.append(baris("Instansi", pengunjung.getInstansi()));
        isi.append(baris("Tanggal Selesai", tanggalSelesai.format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        isi.append(baris("Fasilitator", namaFasilitator));
        isi.append(baris("Status", "SELESAI"));
        isi.append("  Materi yang diselesaikan:").append(System.lineSeparator());
        for (int i = 0; i < materi.size(); i++) {
            isi.append(String.format("    %d. %s%n", i + 1, materi.get(i)));
        }
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        return "Sertifikat ini menyatakan peserta telah menyelesaikan safety induction "
                + "dan berhak mengikuti kegiatan di lingkungan kampus J6.";
    }

    public Pengunjung getPengunjung() {
        return pengunjung;
    }

    public List<String> getMateri() {
        return Collections.unmodifiableList(materi);
    }

    public LocalDate getTanggalSelesai() {
        return tanggalSelesai;
    }

    public String getNamaFasilitator() {
        return namaFasilitator;
    }
}
```

## src/ihsse/model/StatusInsiden.java

```java
package ihsse.model;

/**
 * Status alur penanganan laporan insiden.
 * Enum membatasi nilai yang boleh dipakai sehingga status tidak bisa diisi sembarang teks.
 */
public enum StatusInsiden {

    BARU("Baru"),
    DIPROSES("Diproses"),
    SELESAI("Selesai");

    private final String label;

    StatusInsiden(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Aturan bisnis: BARU -> DIPROSES -> SELESAI.
     * Perubahan lain, termasuk mundur atau langsung selesai, ditolak.
     */
    public boolean bolehBerubahKe(StatusInsiden tujuan) {
        if (tujuan == null) {
            return false;
        }
        if (this == BARU && tujuan == DIPROSES) {
            return true;
        }
        return this == DIPROSES && tujuan == SELESAI;
    }
}
```

## src/ihsse/model/TingkatRisiko.java

```java
package ihsse.model;

/**
 * Tingkat risiko pada zona risiko internal kampus.
 */
public enum TingkatRisiko {

    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi");

    private final String label;

    TingkatRisiko(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

## src/ihsse/model/VideoTutorial.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * Video tutorial keselamatan. Berdiri sendiri karena bukan bagian
 * dari hierarki InformasiHSSE, tetapi tetap dikelola oleh admin.
 */
public class VideoTutorial {

    private String id;
    private String judul;
    private String topik;
    private int durasiDetik;
    private String url;

    public VideoTutorial(String id, String judul, String topik, int durasiDetik, String url) {
        this.id = Validasi.wajibIsi(id, "ID video");
        this.judul = Validasi.wajibIsi(judul, "Judul video");
        this.topik = Validasi.wajibIsi(topik, "Topik video");
        this.durasiDetik = Validasi.minimal(durasiDetik, 1, "Durasi video");
        this.url = Validasi.wajibIsi(url, "URL video");
    }

    public String getDurasiFormat() {
        int menit = durasiDetik / 60;
        int detik = durasiDetik % 60;
        return menit + " menit " + detik + " detik";
    }

    public String tampilRingkas() {
        return String.format("[%s] %-12s | %s | %s | %s",
                id, topik, judul, getDurasiFormat(), url);
    }

    public String getId() {
        return id;
    }

    public String getJudul() {
        return judul;
    }

    public String getTopik() {
        return topik;
    }

    public int getDurasiDetik() {
        return durasiDetik;
    }

    public String getUrl() {
        return url;
    }
}
```

## src/ihsse/model/VisitorPass.java

```java
package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Dokumen Visitor Pass (fitur 1).
 */
public class VisitorPass extends DokumenHSSE {

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMAT_JAM =
            DateTimeFormatter.ofPattern("HH:mm");

    private Pengunjung pengunjung;
    private LocalDate tanggalKunjungan;
    private LocalTime jamMasuk;
    private int masaBerlakuHari;
    private String areaTujuan;

    public VisitorPass(String nomorDokumen, LocalDate tanggalTerbit, String diterbitkanOleh,
                       Pengunjung pengunjung, LocalDate tanggalKunjungan, LocalTime jamMasuk,
                       int masaBerlakuHari, String areaTujuan) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.pengunjung = Validasi.wajibAda(pengunjung, "Pengunjung");
        this.tanggalKunjungan = Validasi.wajibAda(tanggalKunjungan, "Tanggal kunjungan");
        this.jamMasuk = Validasi.wajibAda(jamMasuk, "Jam masuk");
        this.masaBerlakuHari = Validasi.minimal(masaBerlakuHari, 1, "Masa berlaku");
        this.areaTujuan = Validasi.wajibIsi(areaTujuan, "Area tujuan");
    }

    public LocalDate getBerlakuSampai() {
        return tanggalKunjungan.plusDays(masaBerlakuHari);
    }

    public boolean isMasihBerlaku(LocalDate tanggal) {
        return !tanggal.isAfter(getBerlakuSampai());
    }

    @Override
    public String getJudulDokumen() {
        return "Visitor Pass";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Nama Pengunjung", pengunjung.getNama()));
        isi.append(baris("Nomor Identitas", pengunjung.getNomorIdentitas()));
        isi.append(baris("Instansi", pengunjung.getInstansi()));
        isi.append(baris("Tujuan Kunjungan", pengunjung.getTujuanKunjungan()));
        isi.append(baris("Area Tujuan", areaTujuan));
        isi.append(baris("Tanggal Kunjungan", tanggalKunjungan.format(FORMAT_TANGGAL)));
        isi.append(baris("Jam Masuk", jamMasuk.format(FORMAT_JAM)));
        isi.append(baris("Berlaku Sampai", getBerlakuSampai().format(FORMAT_TANGGAL)
                + " (" + masaBerlakuHari + " hari)"));
        isi.append(baris("Status Pass", isMasihBerlaku(LocalDate.now())
                ? "AKTIF" : "KEDALUWARSA"));
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        return "Pass wajib ditunjukkan kepada petugas keamanan selama berada di kampus J6.";
    }

    public Pengunjung getPengunjung() {
        return pengunjung;
    }

    public LocalDate getTanggalKunjungan() {
        return tanggalKunjungan;
    }

    public LocalTime getJamMasuk() {
        return jamMasuk;
    }

    public int getMasaBerlakuHari() {
        return masaBerlakuHari;
    }

    public String getAreaTujuan() {
        return areaTujuan;
    }
}
```

## src/ihsse/model/ZonaRisiko.java

```java
package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk zona risiko internal kampus.
 */
public class ZonaRisiko extends InformasiHSSE {

    private TingkatRisiko tingkatRisiko;
    private String prosedurEvakuasi;
    private String titikKumpul;

    public ZonaRisiko(String kode, String nama, String lokasi, String deskripsi,
                      TingkatRisiko tingkatRisiko, String prosedurEvakuasi,
                      String titikKumpul) {
        super(kode, nama, lokasi, deskripsi);
        this.tingkatRisiko = Validasi.wajibAda(tingkatRisiko, "Tingkat risiko");
        this.prosedurEvakuasi = Validasi.wajibIsi(prosedurEvakuasi, "Prosedur evakuasi");
        this.titikKumpul = Validasi.wajibIsi(titikKumpul, "Titik kumpul");
    }

    @Override
    public String getKategori() {
        return "Safety";
    }

    @Override
    public String getJenis() {
        return "Zona Risiko";
    }

    @Override
    public String tampilRingkas() {
        return super.tampilRingkas()
                + " | Risiko: " + tingkatRisiko.getLabel()
                + " | Titik kumpul: " + titikKumpul;
    }

    public TingkatRisiko getTingkatRisiko() {
        return tingkatRisiko;
    }

    public void setTingkatRisiko(TingkatRisiko tingkatRisiko) {
        this.tingkatRisiko = Validasi.wajibAda(tingkatRisiko, "Tingkat risiko");
    }

    public String getProsedurEvakuasi() {
        return prosedurEvakuasi;
    }

    public String getTitikKumpul() {
        return titikKumpul;
    }
}
```

## src/ihsse/service/InduksiService.java

```java
package ihsse.service;

import ihsse.dao.PengunjungDao;
import ihsse.dao.SertifikatDao;
import ihsse.db.Database;
import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.SertifikatInduksi;
import ihsse.util.Validasi;

import java.time.LocalDate;
import java.util.List;

/**
 * Service safety induction dan penerbitan sertifikat.
 * Sertifikat dan status pengunjung disimpan dalam satu transaksi database.
 */
public class InduksiService {

    /**
     * STATIC FINAL: materi wajib sama untuk semua pengunjung.
     */
    public static final List<String> MATERI_WAJIB = List.of(
            "Penggunaan APAR (Alat Pemadam Api Ringan)",
            "Prosedur Evakuasi Kebakaran",
            "Mitigasi dan Evakuasi Gempa Bumi",
            "Tindakan Saat Terjebak di Lift");

    private final SertifikatDao sertifikatDao = new SertifikatDao();
    private final PengunjungDao pengunjungDao = new PengunjungDao();
    private final NomorService nomorService = new NomorService();

    public SertifikatInduksi selesaikanInduksi(Pengunjung pengunjung, Admin admin,
                                               String namaFasilitator) {
        Validasi.wajibAda(pengunjung, "Pengunjung");
        Validasi.wajibAda(admin, "Admin");
        if (pengunjung.isSudahInduksi()
                || sertifikatDao.existsByPengunjung(pengunjung.getId())) {
            throw new IllegalStateException(
                    "Pengunjung " + pengunjung.getNama()
                    + " sudah memiliki sertifikat induksi.");
        }

        LocalDate tanggalSelesai = LocalDate.now();
        SertifikatInduksi sertifikat = new SertifikatInduksi(
                nomorService.nomorSertifikat(tanggalSelesai),
                tanggalSelesai,
                admin.getNama(),
                pengunjung,
                MATERI_WAJIB,
                tanggalSelesai,
                namaFasilitator);

        Database.inTransaction(connection -> {
            sertifikatDao.insert(connection, sertifikat);
            pengunjungDao.markInduksi(connection, pengunjung.getId());
            return null;
        });
        pengunjung.tandaiSudahInduksi();
        return sertifikat;
    }

    public List<SertifikatInduksi> semuaSertifikat() {
        return sertifikatDao.findAll();
    }
}
```

## src/ihsse/service/InformasiService.java

```java
package ihsse.service;

import ihsse.dao.DenahDao;
import ihsse.dao.InformasiDao;
import ihsse.dao.VideoDao;
import ihsse.model.DenahGedung;
import ihsse.model.InformasiHSSE;
import ihsse.model.VideoTutorial;
import ihsse.util.Validasi;

import java.util.List;

/**
 * Service pengelolaan konten informasi HSSE.
 * Semua data disimpan di database MySQL.
 */
public class InformasiService {

    private final InformasiDao informasiDao = new InformasiDao();
    private final VideoDao videoDao = new VideoDao();
    private final DenahDao denahDao = new DenahDao();

    public void tambahInformasi(InformasiHSSE informasi) {
        Validasi.wajibAda(informasi, "Informasi HSSE");
        if (informasiDao.findByKode(informasi.getKode()) != null) {
            throw new IllegalArgumentException("Kode informasi sudah dipakai: "
                    + informasi.getKode());
        }
        informasiDao.insert(informasi);
    }

    public void ubahInformasi(InformasiHSSE informasi) {
        Validasi.wajibAda(informasi, "Informasi HSSE");
        if (informasiDao.findByKode(informasi.getKode()) == null) {
            throw new IllegalArgumentException("Informasi tidak ditemukan: "
                    + informasi.getKode());
        }
        informasiDao.update(informasi);
    }

    public boolean hapusInformasi(String kode) {
        return informasiDao.delete(kode);
    }

    public InformasiHSSE cariInformasi(String kode) {
        return informasiDao.findByKode(kode);
    }

    public List<InformasiHSSE> semuaInformasi() {
        return informasiDao.findAll();
    }

    public List<InformasiHSSE> informasiBerdasarkanKategori(String kategori) {
        return informasiDao.findByKategori(kategori);
    }

    public void tambahVideo(VideoTutorial video) {
        Validasi.wajibAda(video, "Video tutorial");
        videoDao.insert(video);
    }

    public List<VideoTutorial> semuaVideo() {
        return videoDao.findAll();
    }

    public void tambahDenah(DenahGedung denah) {
        Validasi.wajibAda(denah, "Denah gedung");
        denahDao.insert(denah);
    }

    public List<DenahGedung> semuaDenah() {
        return denahDao.findAll();
    }
}
```

## src/ihsse/service/InsidenService.java

```java
package ihsse.service;

import ihsse.dao.InsidenDao;
import ihsse.model.Admin;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;
import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pelaporan insiden dan proses perubahan status oleh admin.
 */
public class InsidenService {

    private final InsidenDao insidenDao = new InsidenDao();
    private final NomorService nomorService = new NomorService();

    public LaporanInsiden laporInsiden(String namaPelapor, String kontakPelapor,
                                       String lokasi, String deskripsi,
                                       LocalDateTime waktuKejadian) {
        LocalDate tanggalLaporan = LocalDate.now();
        LaporanInsiden laporan = new LaporanInsiden(
                nomorService.nomorInsiden(tanggalLaporan),
                tanggalLaporan,
                "Sistem I-HSSE (pelaporan publik)",
                namaPelapor,
                kontakPelapor,
                lokasi,
                deskripsi,
                waktuKejadian);
        insidenDao.insert(laporan);
        return laporan;
    }

    public LaporanInsiden prosesInsiden(String nomorDokumen, StatusInsiden statusBaru,
                                        String catatan, Admin admin) {
        Validasi.wajibAda(admin, "Admin yang memproses");
        LaporanInsiden laporan = cari(nomorDokumen);
        laporan.proses(statusBaru, catatan, admin);
        insidenDao.update(laporan);
        return laporan;
    }

    public LaporanInsiden cari(String nomorDokumen) {
        LaporanInsiden laporan = insidenDao.findByNomor(nomorDokumen);
        if (laporan == null) {
            throw new IllegalArgumentException("Laporan insiden tidak ditemukan: "
                    + nomorDokumen);
        }
        return laporan;
    }

    public List<LaporanInsiden> semuaInsiden() {
        return insidenDao.findAll();
    }

    public List<LaporanInsiden> berdasarkanStatus(StatusInsiden status) {
        return insidenDao.findByStatus(status);
    }
}
```

## src/ihsse/service/InspeksiService.java

```java
package ihsse.service;

import ihsse.dao.InformasiDao;
import ihsse.dao.InspeksiDao;
import ihsse.db.Database;
import ihsse.model.Admin;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.KondisiFasilitas;
import ihsse.model.LaporanInspeksi;
import ihsse.util.Validasi;

import java.time.LocalDate;
import java.util.List;

/**
 * Service pencatatan inspeksi APAR/hydrant.
 * Laporan inspeksi dan kondisi master fasilitas diubah dalam satu transaksi.
 */
public class InspeksiService {

    private final InspeksiDao inspeksiDao = new InspeksiDao();
    private final InformasiDao informasiDao = new InformasiDao();
    private final NomorService nomorService = new NomorService();

    public LaporanInspeksi catatInspeksi(FasilitasKeselamatan fasilitas,
                                         LocalDate tanggalCek,
                                         KondisiFasilitas kondisi,
                                         LocalDate masaKedaluwarsa,
                                         Admin petugas,
                                         String tindakan,
                                         String catatan) {
        Validasi.wajibAda(fasilitas, "Fasilitas");
        Validasi.wajibAda(tanggalCek, "Tanggal cek");
        Validasi.wajibAda(kondisi, "Kondisi");
        Validasi.wajibAda(petugas, "Petugas");

        fasilitas.setKondisi(kondisi);
        if (masaKedaluwarsa != null) {
            fasilitas.setMasaKedaluwarsa(masaKedaluwarsa);
        }

        LocalDate tanggalTerbit = LocalDate.now();
        LaporanInspeksi laporan = new LaporanInspeksi(
                nomorService.nomorInspeksi(tanggalTerbit),
                tanggalTerbit,
                petugas.getNama(),
                fasilitas,
                tanggalCek,
                kondisi,
                masaKedaluwarsa,
                petugas.getNama(),
                tindakan,
                catatan);

        Database.inTransaction(connection -> {
            inspeksiDao.insert(connection, laporan);
            informasiDao.updateKondisi(connection, fasilitas.getKode(),
                    kondisi, fasilitas.getMasaKedaluwarsa());
            return null;
        });
        return laporan;
    }

    public List<LaporanInspeksi> semuaLaporan() {
        return inspeksiDao.findAll();
    }
}
```

## src/ihsse/service/NomorService.java

```java
package ihsse.service;

import ihsse.dao.NomorDao;
import ihsse.util.Penomoran;

import java.time.LocalDate;

/**
 * Service pembuat nomor dokumen berbasis sequence database.
 */
public class NomorService {

    private final NomorDao nomorDao = new NomorDao();

    public String idPengunjung() {
        return Penomoran.idPengunjung(nomorDao.next("pengunjung"));
    }

    public String nomorVisitorPass(LocalDate tanggal) {
        return Penomoran.nomorVisitorPass(tanggal, nomorDao.next("visitor_pass"));
    }

    public String nomorSertifikat(LocalDate tanggal) {
        return Penomoran.nomorSertifikat(tanggal, nomorDao.next("sertifikat"));
    }

    public String nomorInspeksi(LocalDate tanggal) {
        return Penomoran.nomorInspeksi(tanggal, nomorDao.next("inspeksi"));
    }

    public String nomorInsiden(LocalDate tanggal) {
        return Penomoran.nomorInsiden(tanggal, nomorDao.next("insiden"));
    }
}
```

## src/ihsse/service/PengunjungService.java

```java
package ihsse.service;

import ihsse.dao.PengunjungDao;
import ihsse.dao.VisitorPassDao;
import ihsse.model.Admin;
import ihsse.model.Pengunjung;
import ihsse.model.VisitorPass;
import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service registrasi pengunjung dan penerbitan Visitor Pass.
 * Data disimpan di database MySQL.
 */
public class PengunjungService {

    private final PengunjungDao pengunjungDao = new PengunjungDao();
    private final VisitorPassDao visitorPassDao = new VisitorPassDao();
    private final NomorService nomorService = new NomorService();

    public Pengunjung registrasi(String nama, String nomorIdentitas, String noTelepon,
                                 String instansi, String tujuanKunjungan) {
        Pengunjung pengunjung = new Pengunjung(
                nomorService.idPengunjung(), nama, nomorIdentitas, noTelepon,
                instansi, tujuanKunjungan);
        pengunjungDao.insert(pengunjung);
        return pengunjung;
    }

    public VisitorPass terbitkanVisitorPass(Pengunjung pengunjung, Admin admin,
                                            LocalDate tanggalKunjungan, LocalTime jamMasuk,
                                            int masaBerlakuHari, String areaTujuan) {
        Validasi.wajibAda(admin, "Admin penerbit");
        if (pengunjung == null || !pengunjungDao.existsById(pengunjung.getId())) {
            throw new IllegalArgumentException(
                    "Pengunjung harus terdaftar sebelum visitor pass diterbitkan.");
        }
        LocalDate tanggalTerbit = LocalDate.now();
        VisitorPass pass = new VisitorPass(
                nomorService.nomorVisitorPass(tanggalTerbit),
                tanggalTerbit,
                admin.getNama(),
                pengunjung,
                tanggalKunjungan,
                jamMasuk,
                masaBerlakuHari,
                areaTujuan);
        visitorPassDao.insert(pass);
        return pass;
    }

    public Pengunjung cari(String id) {
        return pengunjungDao.findById(id);
    }

    public List<Pengunjung> semuaPengunjung() {
        return pengunjungDao.findAll();
    }

    public List<Pengunjung> pengunjungBelumInduksi() {
        return pengunjungDao.findBelumInduksi();
    }

    public List<VisitorPass> semuaPass() {
        return visitorPassDao.findAll();
    }
}
```

## src/ihsse/service/StatistikService.java

```java
package ihsse.service;

import ihsse.dao.StatistikDao;
import ihsse.model.LaporanInsiden;
import ihsse.model.StatusInsiden;

import java.util.List;

/**
 * Service ringkasan angka untuk dashboard.
 */
public class StatistikService {

    private final StatistikDao statistikDao = new StatistikDao();

    public int jumlahPengunjung() {
        return statistikDao.jumlahPengunjung();
    }

    public int jumlahVisitorPass() {
        return statistikDao.jumlahVisitorPass();
    }

    public int jumlahSertifikat() {
        return statistikDao.jumlahSertifikat();
    }

    public int jumlahInspeksi() {
        return statistikDao.jumlahInspeksi();
    }

    public int jumlahInsiden() {
        return statistikDao.jumlahInsiden();
    }

    public int jumlahInsiden(StatusInsiden status) {
        return statistikDao.jumlahInsiden(status);
    }

    public int jumlahInformasi() {
        return statistikDao.jumlahInformasi();
    }

    public List<LaporanInsiden> insidenTerbaru(int batas) {
        return statistikDao.insidenTerbaru(batas);
    }
}
```

## src/ihsse/tools/GuiSmokeTest.java

```java
package ihsse.tools;

import ihsse.dao.AdminDao;
import ihsse.gui.InduksiPanel;
import ihsse.gui.InformasiPanel;
import ihsse.gui.InsidenPanel;
import ihsse.gui.InspeksiPanel;
import ihsse.gui.LoginFrame;
import ihsse.gui.MainFrame;
import ihsse.gui.PengunjungPanel;
import ihsse.gui.Theme;
import ihsse.model.Admin;
import ihsse.service.InduksiService;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.InspeksiService;
import ihsse.service.PengunjungService;

import javax.swing.SwingUtilities;

/**
 * Menguji inisialisasi seluruh panel GUI dan pembacaan data dari database.
 */
public final class GuiSmokeTest {

    private GuiSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Theme.install();
        Admin admin = new AdminDao().login("admin", "admin123");
        if (admin == null) {
            throw new IllegalStateException("Login admin gagal.");
        }

        SwingUtilities.invokeAndWait(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.dispose();

            MainFrame mainFrame = new MainFrame(admin);
            mainFrame.dispose();

            InformasiPanel informasi = new InformasiPanel(new InformasiService(), () -> {
            });
            informasi.refreshData();

            PengunjungPanel pengunjung = new PengunjungPanel(
                    new PengunjungService(), admin, () -> {
            });
            pengunjung.refreshData();

            InduksiPanel induksi = new InduksiPanel(
                    new PengunjungService(), new InduksiService(), admin, () -> {
            });
            induksi.refreshData();

            InspeksiPanel inspeksi = new InspeksiPanel(
                    new InformasiService(), new InspeksiService(), admin, () -> {
            });
            inspeksi.refreshData();

            InsidenPanel insiden = new InsidenPanel(
                    new InsidenService(), admin, () -> {
            });
            insiden.refreshData();
        });

        System.out.println("GUI SMOKE TEST LULUS.");
    }
}
```

## src/ihsse/tools/SmokeTest.java

```java
package ihsse.tools;

import ihsse.dao.AdminDao;
import ihsse.db.Database;
import ihsse.model.Admin;
import ihsse.model.FasilitasKeselamatan;
import ihsse.model.KondisiFasilitas;
import ihsse.model.LaporanInsiden;
import ihsse.model.LaporanInspeksi;
import ihsse.model.Pengunjung;
import ihsse.model.SertifikatInduksi;
import ihsse.model.StatusInsiden;
import ihsse.model.VisitorPass;
import ihsse.service.InduksiService;
import ihsse.service.InformasiService;
import ihsse.service.InsidenService;
import ihsse.service.InspeksiService;
import ihsse.service.PengunjungService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Smoke test database + service. Jalankan dengan:
 * java -cp "build\classes;lib\mysql-connector-j-8.3.0.jar" ihsse.tools.SmokeTest
 */
public final class SmokeTest {

    private SmokeTest() {
    }

    public static void main(String[] args) {
        String visitorId = null;
        String passNo = null;
        String sertifikatNo = null;
        String inspeksiNo = null;
        String insidenNo = null;
        String fasilitasKode = null;
        boolean gagal = false;

        try {
            System.out.println("Database : " + Database.testConnection());

            AdminDao adminDao = new AdminDao();
            Admin admin = adminDao.login("admin", "admin123");
            cek(admin != null, "login admin / admin123");

            InformasiService informasiService = new InformasiService();
            cek(!informasiService.semuaInformasi().isEmpty(), "data informasi HSSE terbaca");
            cek(informasiService.semuaVideo().size() >= 2, "video tutorial terbaca");
            cek(informasiService.semuaDenah().size() == 11, "denah 11 lantai terbaca");

            String suffix = String.valueOf(System.currentTimeMillis());
            suffix = suffix.substring(Math.max(0, suffix.length() - 8));
            fasilitasKode = "TST-SAF-" + suffix;

            FasilitasKeselamatan fasilitas = new FasilitasKeselamatan(
                    fasilitasKode, "APAR Uji Smoke", "Ruang Uji", "Fasilitas uji",
                    "APAR", 1, KondisiFasilitas.BAIK, LocalDate.now().plusMonths(3));
            informasiService.tambahInformasi(fasilitas);
            cek(informasiService.cariInformasi(fasilitasKode) != null,
                    "CREATE informasi HSSE");

            FasilitasKeselamatan fasilitasUbah = new FasilitasKeselamatan(
                    fasilitasKode, "APAR Uji Smoke Updated", "Ruang Uji",
                    "Fasilitas uji setelah update", "APAR", 2,
                    KondisiFasilitas.BAIK, LocalDate.now().plusMonths(4));
            informasiService.ubahInformasi(fasilitasUbah);
            cek("APAR Uji Smoke Updated".equals(
                    informasiService.cariInformasi(fasilitasKode).getNama()),
                    "UPDATE informasi HSSE");

            PengunjungService pengunjungService = new PengunjungService();
            Pengunjung pengunjung = pengunjungService.registrasi(
                    "Pengunjung Smoke Test", "TEST-" + suffix, "08129999" + suffix,
                    "Laboratorium Uji", "Verifikasi integrasi database");
            visitorId = pengunjung.getId();
            cek(visitorId != null, "registrasi pengunjung");

            VisitorPass pass = pengunjungService.terbitkanVisitorPass(
                    pengunjung, admin, LocalDate.now(), LocalTime.of(10, 15), 1,
                    "Ruang Uji");
            passNo = pass.getNomorDokumen();
            cek(passNo != null, "penerbitan visitor pass");

            InduksiService induksiService = new InduksiService();
            SertifikatInduksi sertifikat = induksiService.selesaikanInduksi(
                    pengunjung, admin, "Fasilitator Uji");
            sertifikatNo = sertifikat.getNomorDokumen();
            cek(sertifikat.getMateri().size() == InduksiService.MATERI_WAJIB.size(),
                    "sertifikat induksi + materi");

            InspeksiService inspeksiService = new InspeksiService();
            LaporanInspeksi inspeksi = inspeksiService.catatInspeksi(
                    fasilitasUbah, LocalDate.now(), KondisiFasilitas.RUSAK_RINGAN,
                    LocalDate.now().plusMonths(6), admin,
                    "Uji tekanan dan segel", "Catatan smoke test");
            inspeksiNo = inspeksi.getNomorDokumen();
            cek(inspeksiNo != null, "laporan inspeksi");
            cek(informasiService.cariInformasi(fasilitasKode) instanceof FasilitasKeselamatan,
                    "fasilitas keselamatan tetap terbaca");
            cek(((FasilitasKeselamatan) informasiService.cariInformasi(fasilitasKode))
                            .getKondisi() == KondisiFasilitas.RUSAK_RINGAN,
                    "kondisi fasilitas tersinkron setelah inspeksi");

            InsidenService insidenService = new InsidenService();
            LaporanInsiden insiden = insidenService.laporInsiden(
                    "Pelapor Smoke Test", "08127777" + suffix,
                    "Ruang Uji", "Deskripsi insiden smoke test",
                    LocalDateTime.now().minusMinutes(5));
            insidenNo = insiden.getNomorDokumen();
            cek(insiden.getStatus() == StatusInsiden.BARU, "laporan insiden status BARU");

            insidenService.prosesInsiden(insidenNo, StatusInsiden.DIPROSES,
                    "Sedang diperiksa", admin);
            LaporanInsiden diproses = insidenService.cari(insidenNo);
            cek(diproses.getStatus() == StatusInsiden.DIPROSES,
                    "perubahan status BARU -> DIPROSES");

            insidenService.prosesInsiden(insidenNo, StatusInsiden.SELESAI,
                    "Sudah ditangani", admin);
            LaporanInsiden selesai = insidenService.cari(insidenNo);
            cek(selesai.getStatus() == StatusInsiden.SELESAI,
                    "perubahan status DIPROSES -> SELESAI");

            boolean deleteDitolak = false;
            try {
                informasiService.hapusInformasi(fasilitasKode);
            } catch (ihsse.db.DataAccessException ex) {
                deleteDitolak = true;
            }
            cek(deleteDitolak,
                    "delete fasilitas ditolak saat masih dipakai laporan inspeksi");

            System.out.println();
            System.out.println("SEMUA SMOKE TEST LULUS.");
        } catch (Throwable ex) {
            System.out.println();
            System.out.println("SMOKE TEST GAGAL: " + ex.getMessage());
            ex.printStackTrace(System.out);
            gagal = true;
        } finally {
            bersihkan(visitorId, passNo, sertifikatNo, inspeksiNo, insidenNo,
                    fasilitasKode);
        }
        if (gagal) {
            System.exit(1);
        }
    }

    private static void cek(boolean kondisi, String pesan) {
        if (!kondisi) {
            throw new IllegalStateException("Gagal: " + pesan);
        }
        System.out.println("[OK] " + pesan);
    }

    private static void bersihkan(String visitorId, String passNo, String sertifikatNo,
                                  String inspeksiNo, String insidenNo,
                                  String fasilitasKode) {
        System.out.println();
        System.out.println("Membersihkan data uji...");
        try (Connection connection = Database.getConnection()) {
            if (visitorId != null) {
                jalankan(connection,
                        "DELETE FROM sertifikat_materi WHERE nomor_sertifikat IN "
                        + "(SELECT nomor_dokumen FROM sertifikat_induksi "
                        + "WHERE pengunjung_id = ?)", visitorId);
                jalankan(connection,
                        "DELETE FROM sertifikat_induksi WHERE pengunjung_id = ?",
                        visitorId);
                jalankan(connection,
                        "DELETE FROM visitor_pass WHERE pengunjung_id = ?", visitorId);
            }
            if (passNo != null) {
                jalankan(connection,
                        "DELETE FROM visitor_pass WHERE nomor_dokumen = ?", passNo);
            }
            if (sertifikatNo != null) {
                jalankan(connection,
                        "DELETE FROM sertifikat_materi WHERE nomor_sertifikat = ?",
                        sertifikatNo);
                jalankan(connection,
                        "DELETE FROM sertifikat_induksi WHERE nomor_dokumen = ?",
                        sertifikatNo);
            }
            if (inspeksiNo != null) {
                jalankan(connection,
                        "DELETE FROM laporan_inspeksi WHERE nomor_dokumen = ?",
                        inspeksiNo);
            }
            if (insidenNo != null) {
                jalankan(connection,
                        "DELETE FROM laporan_insiden WHERE nomor_dokumen = ?",
                        insidenNo);
            }
            if (fasilitasKode != null) {
                jalankan(connection,
                        "DELETE FROM informasi_hsse WHERE kode = ?", fasilitasKode);
            }
            if (visitorId != null) {
                jalankan(connection, "DELETE FROM pengunjung WHERE id = ?", visitorId);
            }
            System.out.println("Data uji sudah dibersihkan.");
        } catch (SQLException ex) {
            System.out.println("Peringatan cleanup: " + ex.getMessage());
        }
    }

    private static void jalankan(Connection connection, String sql, String parameter)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            statement.executeUpdate();
        }
    }
}
```

## src/ihsse/util/PasswordUtil.java

```java
package ihsse.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitas hash password SHA-256.
 * Password asli tidak pernah disimpan di database.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String sha256(String teks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(teks.getBytes(StandardCharsets.UTF_8));
            StringBuilder hasil = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hasil.append(String.format("%02x", b));
            }
            return hasil.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritma SHA-256 tidak tersedia.", e);
        }
    }

    public static boolean cocok(String passwordAsli, String hashTersimpan) {
        if (passwordAsli == null || hashTersimpan == null) {
            return false;
        }
        String hashInput = sha256(passwordAsli);
        return MessageDigest.isEqual(
                hashInput.getBytes(StandardCharsets.UTF_8),
                hashTersimpan.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
```

## src/ihsse/util/Penomoran.java

```java
package ihsse.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Formatter nomor dokumen.
 * Nilai urutan berasal dari tabel nomor_urut di database agar nomor tetap
 * unik walaupun aplikasi ditutup dan dibuka kembali.
 */
public final class Penomoran {

    private static final DateTimeFormatter TANGGAL =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private Penomoran() {
    }

    public static String idPengunjung(int urut) {
        return String.format("PGN-%03d", urut);
    }

    public static String nomorVisitorPass(LocalDate tanggal, int urut) {
        return "VP-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }

    public static String nomorSertifikat(LocalDate tanggal, int urut) {
        return "SERT-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }

    public static String nomorInspeksi(LocalDate tanggal, int urut) {
        return "INS-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }

    public static String nomorInsiden(LocalDate tanggal, int urut) {
        return "INC-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }
}
```

## src/ihsse/util/Validasi.java

```java
package ihsse.util;

/**
 * Kelas utilitas untuk validasi sederhana.
 * Semua method bersifat static sehingga bisa dipakai tanpa membuat objek.
 */
public final class Validasi {

    // Constructor private: kelas utilitas tidak perlu diinstansiasi.
    private Validasi() {
    }

    public static String wajibIsi(String nilai, String namaField) {
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        return nilai.trim();
    }

    public static <T> T wajibAda(T nilai, String namaField) {
        if (nilai == null) {
            throw new IllegalArgumentException(namaField + " tidak boleh null.");
        }
        return nilai;
    }

    public static int minimal(int nilai, int batas, String namaField) {
        if (nilai < batas) {
            throw new IllegalArgumentException(namaField + " minimal " + batas + ".");
        }
        return nilai;
    }
}
```
