package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * ABSTRACT CLASS untuk semua dokumen HSSE.
 *
 * Cetak memakai pola Template Method: alur cetak (header, isi, footer)
 * sudah ditentukan di sini, sedangkan isi detail diserahkan ke subclass.
 */
public abstract class DokumenHSSE implements Dicetak {

    private String nomorDokumen;
    private LocalDate tanggalTerbit;
    private String diterbitkanOleh;

    protected DokumenHSSE(String nomorDokumen, LocalDate tanggalTerbit, String diterbitkanOleh) {
        this.nomorDokumen = Validasi.wajibIsi(nomorDokumen, "Nomor dokumen");
        this.tanggalTerbit = Validasi.wajibAda(tanggalTerbit, "Tanggal terbit");
        this.diterbitkanOleh = Validasi.wajibIsi(diterbitkanOleh, "Penerbit dokumen");
    }

    /**
     * FINAL: alur cetak tidak boleh diubah subclass.
     * Subclass hanya menyuplai judul, isi, dan catatan kaki.
     */
    @Override
    public final String cetak() {
        StringBuilder hasil = new StringBuilder();
        hasil.append(garisPemisah()).append(System.lineSeparator());
        hasil.append("        ").append(getJudulDokumen().toUpperCase(Locale.ROOT))
                .append(System.lineSeparator());
        hasil.append(garisPemisah()).append(System.lineSeparator());
        hasil.append(baris("Nomor Dokumen", nomorDokumen));
        hasil.append(baris("Tanggal Terbit", tanggalTerbit.format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        hasil.append(baris("Diterbitkan Oleh", diterbitkanOleh));
        hasil.append("-".repeat(64)).append(System.lineSeparator());
        hasil.append(getIsiDokumen());
        hasil.append("-".repeat(64)).append(System.lineSeparator());
        hasil.append(getCatatanKaki()).append(System.lineSeparator());
        hasil.append(garisPemisah());
        return hasil.toString();
    }

    public abstract String getJudulDokumen();

    /**
     * ABSTRACT METHOD: setiap jenis dokumen mengisi bagian isi dengan caranya sendiri.
     */
    protected abstract String getIsiDokumen();

    protected String getCatatanKaki() {
        return "Dokumen ini dihasilkan oleh sistem I-HSSE Universitas Gunadarma Kampus J6.";
    }

    protected String baris(String label, Object nilai) {
        return String.format("  %-20s: %s%n", label, nilai == null ? "-" : nilai);
    }

    @Override
    public String getNomorDokumen() {
        return nomorDokumen;
    }

    public LocalDate getTanggalTerbit() {
        return tanggalTerbit;
    }

    public String getDiterbitkanOleh() {
        return diterbitkanOleh;
    }
}
