package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk fasilitas keamanan (Security).
 * Contoh: parkiran motor, CCTV, smart lock door, pos satpam.
 */
public class FasilitasKeamanan extends InformasiHSSE {

    private String jenis;
    private int jumlah;
    private KondisiFasilitas kondisi;

    public FasilitasKeamanan(String kode, String nama, String lokasi, String deskripsi,
                           String jenis, int jumlah, KondisiFasilitas kondisi) {
        super(kode, nama, lokasi, deskripsi);
        this.jenis = Validasi.wajibIsi(jenis, "Jenis fasilitas");
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
    }

    @Override
    public String getKategori() {
        return "Security";
    }

    @Override
    public String getJenis() {
        return jenis;
    }

    @Override
    public String tampilRingkas() {
        return super.tampilRingkas()
                + " | Jumlah: " + jumlah
                + " | Kondisi: " + kondisi.getLabel();
    }

    public int getJumlah() {
        return jumlah;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
    }

    public KondisiFasilitas getKondisi() {
        return kondisi;
    }

    public void setKondisi(KondisiFasilitas kondisi) {
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
    }
}
