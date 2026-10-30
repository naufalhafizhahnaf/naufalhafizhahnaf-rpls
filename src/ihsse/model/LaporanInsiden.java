package ihsse.model;

import ihsse.util.Validasi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dokumen Laporan Insiden (fitur 4).
 * Status awal selalu BARU dan hanya bisa diproses oleh admin.
 */
public class LaporanInsiden extends DokumenHSSE {

    private static final DateTimeFormatter FORMAT_WAKTU =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private String namaPelapor;
    private String kontakPelapor;
    private String lokasi;
    private String deskripsi;
    private LocalDateTime waktuKejadian;
    private StatusInsiden status;
    private String catatanAdmin;
    private Admin ditanganiOleh;

    public LaporanInsiden(String nomorDokumen, LocalDate tanggalTerbit,
                          String diterbitkanOleh, String namaPelapor,
                          String kontakPelapor, String lokasi, String deskripsi,
                          LocalDateTime waktuKejadian) {
        this(nomorDokumen, tanggalTerbit, diterbitkanOleh, namaPelapor,
                kontakPelapor, lokasi, deskripsi, waktuKejadian,
                StatusInsiden.BARU, "-", null);
    }

    /**
     * Constructor untuk membaca laporan yang sudah tersimpan di database.
     */
    public LaporanInsiden(String nomorDokumen, LocalDate tanggalTerbit,
                          String diterbitkanOleh, String namaPelapor,
                          String kontakPelapor, String lokasi, String deskripsi,
                          LocalDateTime waktuKejadian, StatusInsiden status,
                          String catatanAdmin, Admin ditanganiOleh) {
        super(nomorDokumen, tanggalTerbit, diterbitkanOleh);
        this.namaPelapor = Validasi.wajibIsi(namaPelapor, "Nama pelapor");
        this.kontakPelapor = Validasi.wajibIsi(kontakPelapor, "Kontak pelapor");
        this.lokasi = Validasi.wajibIsi(lokasi, "Lokasi insiden");
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi insiden");
        this.waktuKejadian = Validasi.wajibAda(waktuKejadian, "Waktu kejadian");
        this.status = Validasi.wajibAda(status, "Status insiden");
        this.catatanAdmin = catatanAdmin == null || catatanAdmin.trim().isEmpty()
                ? "-" : catatanAdmin.trim();
        this.ditanganiOleh = ditanganiOleh;
    }

    /**
     * Perubahan status mengikuti aturan pada enum StatusInsiden.
     * Jika tidak valid, method melempar exception (dicegah di layer service/Main).
     */
    public void proses(StatusInsiden statusBaru, String catatan, Admin admin) {
        if (statusBaru == null) {
            throw new IllegalArgumentException("Status baru wajib diisi.");
        }
        if (!status.bolehBerubahKe(statusBaru)) {
            throw new IllegalStateException("Status tidak dapat berubah dari "
                    + status.getLabel() + " ke " + statusBaru.getLabel() + ".");
        }
        this.status = statusBaru;
        this.ditanganiOleh = Validasi.wajibAda(admin, "Admin yang memproses");
        if (catatan != null && !catatan.trim().isEmpty()) {
            this.catatanAdmin = catatan.trim();
        }
    }

    @Override
    public String getJudulDokumen() {
        return "Laporan Insiden";
    }

    @Override
    protected String getIsiDokumen() {
        StringBuilder isi = new StringBuilder();
        isi.append(baris("Nama Pelapor", namaPelapor));
        isi.append(baris("Kontak", kontakPelapor));
        isi.append(baris("Waktu Kejadian", waktuKejadian.format(FORMAT_WAKTU)));
        isi.append(baris("Lokasi", lokasi));
        isi.append(baris("Deskripsi", deskripsi));
        isi.append(baris("Status", status.getLabel()));
        isi.append(baris("Ditangani Oleh", ditanganiOleh == null
                ? "-" : ditanganiOleh.getNama() + " (" + ditanganiOleh.getJabatan() + ")"));
        isi.append(baris("Catatan Admin", catatanAdmin));
        return isi.toString();
    }

    @Override
    protected String getCatatanKaki() {
        if (status == StatusInsiden.SELESAI) {
            return "Laporan dinyatakan SELESAI. Tindak lanjut sudah dicatat oleh admin.";
        }
        if (status == StatusInsiden.DIPROSES) {
            return "Laporan sedang DIPROSES oleh petugas terkait.";
        }
        return "Laporan BARU diterima dan menunggu verifikasi admin.";
    }

    public String getNamaPelapor() {
        return namaPelapor;
    }

    public String getKontakPelapor() {
        return kontakPelapor;
    }

    public String getLokasi() {
        return lokasi;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public LocalDateTime getWaktuKejadian() {
        return waktuKejadian;
    }

    public StatusInsiden getStatus() {
        return status;
    }

    public String getCatatanAdmin() {
        return catatanAdmin;
    }

    public Admin getDitanganiOleh() {
        return ditanganiOleh;
    }
}
