package ihsse.model;

import ihsse.util.Validasi;

/**
 * ABSTRACT CLASS untuk seluruh konten informasi HSSE.
 * Turunannya: AreaMerokok, FasilitasKeamanan, FasilitasKeselamatan, ZonaRisiko.
 */
public abstract class InformasiHSSE {

    private String kode;
    private String nama;
    private String lokasi;
    private String deskripsi;

    protected InformasiHSSE(String kode, String nama, String lokasi, String deskripsi) {
        this.kode = Validasi.wajibIsi(kode, "Kode informasi");
        this.nama = Validasi.wajibIsi(nama, "Nama informasi");
        this.lokasi = Validasi.wajibIsi(lokasi, "Lokasi");
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi");
    }

    /**
     * ABSTRACT METHOD: kategori ditentukan oleh masing-masing turunan.
     */
    public abstract String getKategori();

    /**
     * ABSTRACT METHOD: jenis konten juga ditentukan oleh turunan.
     */
    public abstract String getJenis();

    /**
     * Method konkret yang bisa diwarisi. Turunan boleh menambah detail
     * dengan cara meng-override method ini (polymorphism).
     */
    public String tampilRingkas() {
        return String.format("[%s] %-8s | %-22s | %s | %s",
                kode, getKategori(), getJenis(), nama, lokasi);
    }

    public String getKode() {
        return kode;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = Validasi.wajibIsi(nama, "Nama informasi");
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = Validasi.wajibIsi(lokasi, "Lokasi");
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi");
    }
}
