package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Dokumen Visitor Pass (fitur 1).
 */
public class VisitorPass extends DokumenHSSE {

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMAT_JAM =
            DateTimeFormatter.ofPattern("HH:mm");

    private Pengunjung pengunjung;
    private LocalDate tanggalKunjungan;
    private LocalTime jamMasuk;
    private int masaBerlakuHari;
    private String areaTujuan;

    public VisitorPass(String nomorDokumen, LocalDate tanggalTerbit, String diterbitkanOleh,
                       Pengunjung pengunjung, LocalDate tanggalKunjungan, LocalTime jamMasuk,
                       int masaBerlakuHari, String areaTujuan) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.pengunjung = Validasi.wajibAda(pengunjung, "Pengunjung");
        this.tanggalKunjungan = Validasi.wajibAda(tanggalKunjungan, "Tanggal kunjungan");
        this.jamMasuk = Validasi.wajibAda(jamMasuk, "Jam masuk");
        this.masaBerlakuHari = Validasi.minimal(masaBerlakuHari, 1, "Masa berlaku");
        this.areaTujuan = Validasi.wajibIsi(areaTujuan, "Area tujuan");
    }

    public LocalDate getBerlakuSampai() {
        return tanggalKunjungan.plusDays(masaBerlakuHari);
    }

    public boolean isMasihBerlaku(LocalDate tanggal) {
        return !tanggal.isAfter(getBerlakuSampai());
    }

    @Override
    public String getJudulDokumen() {
        return "Visitor Pass";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Nama Pengunjung", pengunjung.getNama()));
        isi.append(baris("Nomor Identitas", pengunjung.getNomorIdentitas()));
        isi.append(baris("Instansi", pengunjung.getInstansi()));
        isi.append(baris("Tujuan Kunjungan", pengunjung.getTujuanKunjungan()));
        isi.append(baris("Area Tujuan", areaTujuan));
        isi.append(baris("Tanggal Kunjungan", tanggalKunjungan.format(FORMAT_TANGGAL)));
        isi.append(baris("Jam Masuk", jamMasuk.format(FORMAT_JAM)));
        isi.append(baris("Berlaku Sampai", getBerlakuSampai().format(FORMAT_TANGGAL)
                + " (" + masaBerlakuHari + " hari)"));
        isi.append(baris("Status Pass", isMasihBerlaku(LocalDate.now())
                ? "AKTIF" : "KEDALUWARSA"));
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        return "Pass wajib ditunjukkan kepada petugas keamanan selama berada di kampus J6.";
    }

    public Pengunjung getPengunjung() {
        return pengunjung;
    }

    public LocalDate getTanggalKunjungan() {
        return tanggalKunjungan;
    }

    public LocalTime getJamMasuk() {
        return jamMasuk;
    }

    public int getMasaBerlakuHari() {
        return masaBerlakuHari;
    }

    public String getAreaTujuan() {
        return areaTujuan;
    }
}
