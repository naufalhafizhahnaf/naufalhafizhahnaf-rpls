package ihsse.gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Format tanggal/jam untuk form dan tabel.
 */
public final class FormatUtil {

    public static final DateTimeFormatter TANGGAL_INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TANGGAL_VIEW =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter WAKTU_INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter WAKTU_VIEW =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    public static final DateTimeFormatter JAM_INPUT =
            DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter JAM_VIEW =
            DateTimeFormatter.ofPattern("HH:mm");

    private FormatUtil() {
    }

    public static LocalDate parseTanggal(String teks) {
        try {
            return LocalDate.parse(teks.trim(), TANGGAL_INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Format tanggal harus yyyy-MM-dd, contoh 2026-10-30.");
        }
    }

    public static LocalDate parseTanggalOpsional(String teks) {
        if (teks == null || teks.trim().isEmpty()) {
            return null;
        }
        return parseTanggal(teks);
    }

    public static LocalDateTime parseWaktu(String teks) {
        try {
            return LocalDateTime.parse(teks.trim(), WAKTU_INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Format waktu harus yyyy-MM-dd HH:mm, contoh 2026-10-30 09:15.");
        }
    }

    public static LocalTime parseJam(String teks) {
        try {
            return LocalTime.parse(teks.trim(), JAM_INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Format jam harus HH:mm, contoh 09:15.");
        }
    }

    public static String tanggal(LocalDate tanggal) {
        return tanggal == null ? "-" : tanggal.format(TANGGAL_VIEW);
    }

    public static String tanggalInput(LocalDate tanggal) {
        return tanggal == null ? "" : tanggal.format(TANGGAL_INPUT);
    }

    public static String waktu(LocalDateTime waktu) {
        return waktu == null ? "-" : waktu.format(WAKTU_VIEW);
    }

    public static String waktuInput(LocalDateTime waktu) {
        return waktu == null ? "" : waktu.format(WAKTU_INPUT);
    }

    public static String jam(LocalTime jam) {
        return jam == null ? "-" : jam.format(JAM_VIEW);
    }
}
