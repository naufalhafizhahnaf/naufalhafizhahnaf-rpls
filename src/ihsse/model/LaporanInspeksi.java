package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dokumen Laporan Inspeksi APAR/hydrant (fitur 3).
 */
public class LaporanInspeksi extends DokumenHSSE {

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private FasilitasKeselamatan fasilitas;
    private LocalDate tanggalCek;
    private KondisiFasilitas kondisi;
    private LocalDate masaKedaluwarsa;
    private String namaPetugas;
    private String tindakan;
    private String catatan;

    public LaporanInspeksi(String nomorDokumen, LocalDate tanggalTerbit,
                           String diterbitkanOleh, FasilitasKeselamatan fasilitas,
                           LocalDate tanggalCek, KondisiFasilitas kondisi,
                           LocalDate masaKedaluwarsa, String namaPetugas,
                           String tindakan, String catatan) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.fasilitas = Validasi.wajibAda(fasilitas, "Fasilitas");
        this.tanggalCek = Validasi.wajibAda(tanggalCek, "Tanggal cek");
        this.kondisi = Validasi.wajibAda(kondisi, "Kondisi");
        this.masaKedaluwarsa = masaKedaluwarsa;
        this.namaPetugas = Validasi.wajibIsi(namaPetugas, "Nama petugas");
        this.tindakan = Validasi.wajibIsi(tindakan, "Tindakan");
        this.catatan = catatan == null ? "-" : catatan.trim();
    }

    @Override
    public String getJudulDokumen() {
        return "Laporan Inspeksi APAR/Hydrant";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Kode Fasilitas", fasilitas.getKode()));
        isi.append(baris("Nama Fasilitas", fasilitas.getNama()));
        isi.append(baris("Jenis", fasilitas.getJenis()));
        isi.append(baris("Lokasi", fasilitas.getLokasi()));
        isi.append(baris("Tanggal Pemeriksaan", tanggalCek.format(FORMAT_TANGGAL)));
        isi.append(baris("Kondisi", kondisi.getLabel()));
        isi.append(baris("Masa Kedaluwarsa", masaKedaluwarsa == null
                ? "-" : masaKedaluwarsa.format(FORMAT_TANGGAL)));
        isi.append(baris("Petugas", namaPetugas));
        isi.append(baris("Tindakan", tindakan));
        isi.append(baris("Catatan", catatan));
        isi.append(baris("Rekomendasi", kondisi.isPerluTindakLanjut()
                ? "Perlu tindak lanjut dan pengecekan ulang."
                : "Fasilitas layak dipakai, lanjutkan pemantauan berkala."));
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        return "Laporan ini menjadi dasar pemeliharaan fasilitas keselamatan kampus J6.";
    }

    public FasilitasKeselamatan getFasilitas() {
        return fasilitas;
    }

    public LocalDate getTanggalCek() {
        return tanggalCek;
    }

    public KondisiFasilitas getKondisi() {
        return kondisi;
    }

    public LocalDate getMasaKedaluwarsa() {
        return masaKedaluwarsa;
    }

    public String getNamaPetugas() {
        return namaPetugas;
    }

    public String getTindakan() {
        return tindakan;
    }

    public String getCatatan() {
        return catatan;
    }
}
