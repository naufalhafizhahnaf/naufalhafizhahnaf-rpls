package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dokumen Sertifikat Induksi (fitur 2).
 */
public class SertifikatInduksi extends DokumenHSSE {

    private Pengunjung pengunjung;
    private List<String> materi;
    private LocalDate tanggalSelesai;
    private String namaFasilitator;

    public SertifikatInduksi(String nomorDokumen, LocalDate tanggalTerbit,
                            String diterbitkanOleh, Pengunjung pengunjung,
                            List<String> materi, LocalDate tanggalSelesai,
                            String namaFasilitator) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.pengunjung = Validasi.wajibAda(pengunjung, "Pengunjung");
        if (materi == null || materi.isEmpty()) {
            throw new IllegalArgumentException("Materi induksi tidak boleh kosong.");
        }
        this.materi = new ArrayList<>(materi);
        this.tanggalSelesai = Validasi.wajibAda(tanggalSelesai, "Tanggal selesai");
        this.namaFasilitator = Validasi.wajibIsi(namaFasilitator, "Nama fasilitator");
    }

    @Override
    public String getJudulDokumen() {
        return "Sertifikat Induksi Keselamatan";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Nama Peserta", pengunjung.getNama()));
        isi.append(baris("Nomor Identitas", pengunjung.getNomorIdentitas()));
        isi.append(baris("Instansi", pengunjung.getInstansi()));
        isi.append(baris("Tanggal Selesai", tanggalSelesai.format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        isi.append(baris("Fasilitator", namaFasilitator));
        isi.append(baris("Status", "SELESAI"));
        isi.append("  Materi yang diselesaikan:").append(System.lineSeparator());
        for (int i = 0; i < materi.size(); i++) {
            isi.append(String.format("    %d. %s%n", i + 1, materi.get(i)));
        }
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        return "Sertifikat ini menyatakan peserta telah menyelesaikan safety induction "
                + "dan berhak mengikuti kegiatan di lingkungan kampus J6.";
    }

    public Pengunjung getPengunjung() {
        return pengunjung;
    }

    public List<String> getMateri() {
        return Collections.unmodifiableList(materi);
    }

    public LocalDate getTanggalSelesai() {
        return tanggalSelesai;
    }

    public String getNamaFasilitator() {
        return namaFasilitator;
    }
}
