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
