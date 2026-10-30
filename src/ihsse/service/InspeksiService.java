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
