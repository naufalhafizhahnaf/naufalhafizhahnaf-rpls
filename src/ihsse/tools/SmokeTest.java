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
