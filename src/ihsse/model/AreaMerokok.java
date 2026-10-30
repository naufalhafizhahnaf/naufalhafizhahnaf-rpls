package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk area dilarang merokok (Health).
 */
public class AreaMerokok extends InformasiHSSE {

    private double luasArea;
    private String aturan;
    private String sanksi;

    public AreaMerokok(String kode, String nama, String lokasi, String deskripsi,
                       double luasArea, String aturan, String sanksi) {
        super(kode, nama, lokasi, deskripsi);
        if (luasArea <= 0) {
            throw new IllegalArgumentException("Luas area harus lebih dari 0.");
        }
        this.luasArea = luasArea;
        this.aturan = Validasi.wajibIsi(aturan, "Aturan");
        this.sanksi = Validasi.wajibIsi(sanksi, "Sanksi");
    }

    @Override
    public String getKategori() {
        return "Health";
    }

    @Override
    public String getJenis() {
        return "Area Merokok";
    }

    /**
     * POLYMORPHISM: detail khusus AreaMerokok ditambahkan di sini.
     */
    @Override
    public String tampilRingkas() {
        return super.tampilRingkas() + String.format(" | Luas: %.0f m2", luasArea);
    }

    public double getLuasArea() {
        return luasArea;
    }

    public void setLuasArea(double luasArea) {
        if (luasArea <= 0) {
            throw new IllegalArgumentException("Luas area harus lebih dari 0.");
        }
        this.luasArea = luasArea;
    }

    public String getAturan() {
        return aturan;
    }

    public String getSanksi() {
        return sanksi;
    }
}
