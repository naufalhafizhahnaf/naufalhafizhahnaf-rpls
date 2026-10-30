package ihsse.model;

/**
 * Kontrak untuk semua dokumen yang bisa dicetak.
 * Interface ini membuat proses cetak seragam, walaupun isi tiap dokumen berbeda.
 */
public interface Dicetak {

    String cetak();

    String getNomorDokumen();

    /**
     * Default method: semua kelas yang mengimplementasikan Dicetak
     * otomatis memiliki garis pemisah tanpa harus menuliskannya ulang.
     */
    default String garisPemisah() {
        return "================================================================";
    }
}
