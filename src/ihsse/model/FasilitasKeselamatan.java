package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Turunan InformasiHSSE untuk fasilitas keselamatan (Safety).
 * Contoh: APAR, hydrant, tangga darurat, titik kumpul.
 */
public class FasilitasKeselamatan extends InformasiHSSE {

    private String jenis;
    private int jumlah;
    private KondisiFasilitas kondisi;
    private LocalDate masaKedaluwarsa;

    public FasilitasKeselamatan(String kode, String nama, String lokasi, String deskripsi,
                               String jenis, int jumlah, KondisiFasilitas kondisi,
                               LocalDate masaKedaluwarsa) {
        super(kode, nama, lokasi, deskripsi);
        this.jenis = Validasi.wajibIsi(jenis, "Jenis fasilitas");
        this.jumlah = Validasi.minimal(jumlah, 1, "Jumlah fasilitas");
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi fasilitas");
        // Boleh null untuk fasilitas yang tidak memiliki masa kedaluwarsa,
        // misalnya tangga darurat atau titik kumpul.
        this.masaKedaluwarsa = masaKedaluwarsa;
    }

    @Override
    public String getKategori() {
        return "Safety";
    }

    @Override
    public String getJenis() {
        return jenis;
    }

    @Override
    public String tampilRingkas() {
        String infoKedaluwarsa = masaKedaluwarsa == null
                ? "-"
                : masaKedaluwarsa.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return super.tampilRingkas()
                + " | Jumlah: " + jumlah
                + " | Kondisi: " + kondisi.getLabel()
                + " | Kedaluwarsa: " + infoKedaluwarsa;
    }

    public boolean isKedaluwarsa(LocalDate tanggalAcuan) {
        return masaKedaluwarsa != null && masaKedaluwarsa.isBefore(tanggalAcuan);
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

    public LocalDate getMasaKedaluwarsa() {
        return masaKedaluwarsa;
    }

    public void setMasaKedaluwarsa(LocalDate masaKedaluwarsa) {
        this.masaKedaluwarsa = masaKedaluwarsa;
    }
}
