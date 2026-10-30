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
