package ihsse.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Formatter nomor dokumen.
 * Nilai urutan berasal dari tabel nomor_urut di database agar nomor tetap
 * unik walaupun aplikasi ditutup dan dibuka kembali.
 */
public final class Penomoran {

    private static final DateTimeFormatter TANGGAL =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private Penomoran() {
    }

    public static String idPengunjung(int urut) {
        return String.format("PGN-%03d", urut);
    }

    public static String nomorVisitorPass(LocalDate tanggal, int urut) {
        return "VP-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }

    public static String nomorSertifikat(LocalDate tanggal, int urut) {
        return "SERT-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }

    public static String nomorInspeksi(LocalDate tanggal, int urut) {
        return "INS-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }

    public static String nomorInsiden(LocalDate tanggal, int urut) {
        return "INC-" + tanggal.format(TANGGAL) + "-" + String.format("%03d", urut);
    }
}
