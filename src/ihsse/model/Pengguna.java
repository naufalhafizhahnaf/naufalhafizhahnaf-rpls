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
