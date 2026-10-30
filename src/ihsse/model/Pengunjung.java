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
