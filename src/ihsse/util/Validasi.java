package ihsse.util;

/**
 * Kelas utilitas untuk validasi sederhana.
 * Semua method bersifat static sehingga bisa dipakai tanpa membuat objek.
 */
public final class Validasi {

    // Constructor private: kelas utilitas tidak perlu diinstansiasi.
    private Validasi() {
    }

    public static String wajibIsi(String nilai, String namaField) {
        if (nilai == null || nilai.trim().isEmpty()) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        return nilai.trim();
    }

    public static <T> T wajibAda(T nilai, String namaField) {
        if (nilai == null) {
            throw new IllegalArgumentException(namaField + " tidak boleh null.");
        }
        return nilai;
    }

    public static int minimal(int nilai, int batas, String namaField) {
        if (nilai < batas) {
            throw new IllegalArgumentException(namaField + " minimal " + batas + ".");
        }
        return nilai;
    }
}
