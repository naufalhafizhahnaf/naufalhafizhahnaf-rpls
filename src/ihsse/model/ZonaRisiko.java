package ihsse.model;

import ihsse.util.Validasi;

/**
 * Turunan InformasiHSSE untuk zona risiko internal kampus.
 */
public class ZonaRisiko extends InformasiHSSE {

    private TingkatRisiko tingkatRisiko;
    private String prosedurEvakuasi;
    private String titikKumpul;

    public ZonaRisiko(String kode, String nama, String lokasi, String deskripsi,
                      TingkatRisiko tingkatRisiko, String prosedurEvakuasi,
                      String titikKumpul) {
        super(kode, nama, lokasi, deskripsi);
        this.tingkatRisiko = Validasi.wajibAda(tingkatRisiko, "Tingkat risiko");
        this.prosedurEvakuasi = Validasi.wajibIsi(prosedurEvakuasi, "Prosedur evakuasi");
        this.titikKumpul = Validasi.wajibIsi(titikKumpul, "Titik kumpul");
    }

    @Override
    public String getKategori() {
        return "Safety";
    }

    @Override
    public String getJenis() {
        return "Zona Risiko";
    }

    @Override
    public String tampilRingkas() {
        return super.tampilRingkas()
                + " | Risiko: " + tingkatRisiko.getLabel()
                + " | Titik kumpul: " + titikKumpul;
    }

    public TingkatRisiko getTingkatRisiko() {
        return tingkatRisiko;
    }

    public void setTingkatRisiko(TingkatRisiko tingkatRisiko) {
        this.tingkatRisiko = Validasi.wajibAda(tingkatRisiko, "Tingkat risiko");
    }

    public String getProsedurEvakuasi() {
        return prosedurEvakuasi;
    }

    public String getTitikKumpul() {
        return titikKumpul;
    }
}
