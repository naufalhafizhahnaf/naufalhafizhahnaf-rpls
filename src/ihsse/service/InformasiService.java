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
