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
