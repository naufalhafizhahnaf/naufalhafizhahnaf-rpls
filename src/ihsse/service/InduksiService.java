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
