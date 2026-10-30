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
