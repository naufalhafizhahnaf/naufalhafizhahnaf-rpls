package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk menu Environment.
 * Contoh: pom bensin kampus dan toko drum/plastik.
 *
 * Catatan: kelas ini ditambahkan agar menu Environment pada aplikasi
 * tetap terwakili. Tidak ada harga, pembayaran, atau stok sesuai batasan tugas.
 */
public class FasilitasLingkungan extends InformasiHSSE {

    private String jenis;
    private String jamOperasional;
    private String penanggungJawab;

    public FasilitasLingkungan(String kode, String nama, String lokasi, String deskripsi,
                               String jenis, String jamOperasional, String penanggungJawab) {
        super(kode, nama, lokasi, deskripsi);
        this.jenis = Validasi.wajibIsi(jenis, "Jenis fasilitas lingkungan");
        this.jamOperasional = Validasi.wajibIsi(jamOperasional, "Jam operasional");
        this.penanggungJawab = Validasi.wajibIsi(penanggungJawab, "Penanggung jawab");
    }

    @Override
    public String getKategori() {
        return "Environment";
    }

    @Override
    public String getJenis() {
        return jenis;
    }

    @Override
    public String tampilRingkas() {
        return super.tampilRingkas()
                + " | Jam: " + jamOperasional
                + " | PJ: " + penanggungJawab;
    }

    public String getJamOperasional() {
        return jamOperasional;
    }

    public String getPenanggungJawab() {
        return penanggungJawab;
    }
}
