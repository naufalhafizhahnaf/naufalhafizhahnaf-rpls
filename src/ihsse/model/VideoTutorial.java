package ihsse.model;

import ihsse.util.Validasi;

/**
 * Video tutorial keselamatan. Berdiri sendiri karena bukan bagian
 * dari hierarki InformasiHSSE, tetapi tetap dikelola oleh admin.
 */
public class VideoTutorial {

    private String id;
    private String judul;
    private String topik;
    private int durasiDetik;
    private String url;

    public VideoTutorial(String id, String judul, String topik, int durasiDetik, String url) {
        this.id = Validasi.wajibIsi(id, "ID video");
        this.judul = Validasi.wajibIsi(judul, "Judul video");
        this.topik = Validasi.wajibIsi(topik, "Topik video");
        this.durasiDetik = Validasi.minimal(durasiDetik, 1, "Durasi video");
        this.url = Validasi.wajibIsi(url, "URL video");
    }

    public String getDurasiFormat() {
        int menit = durasiDetik / 60;
        int detik = durasiDetik % 60;
        return menit + " menit " + detik + " detik";
    }

    public String tampilRingkas() {
        return String.format("[%s] %-12s | %s | %s | %s",
                id, topik, judul, getDurasiFormat(), url);
    }

    public String getId() {
        return id;
    }

    public String getJudul() {
        return judul;
    }

    public String getTopik() {
        return topik;
    }

    public int getDurasiDetik() {
        return durasiDetik;
    }

    public String getUrl() {
        return url;
    }
}
