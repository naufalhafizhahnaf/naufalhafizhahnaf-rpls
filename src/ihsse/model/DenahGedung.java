package ihsse.model;

import ihsse.util.Validasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Denah gedung per lantai (lantai 1 sampai 11).
 * Daftar fasilitas penting disimpan sebagai List dan dibungkus unmodifiable
 * agar tidak bisa diubah dari luar (encapsulation).
 */
public class DenahGedung {

    private int lantai;
    private String namaFile;
    private String deskripsi;
    private List<String> fasilitasPenting;

    public DenahGedung(int lantai, String namaFile, String deskripsi,
                       List<String> fasilitasPenting) {
        if (lantai < 1) {
            throw new IllegalArgumentException("Lantai minimal 1.");
        }
        this.lantai = lantai;
        this.namaFile = Validasi.wajibIsi(namaFile, "Nama file denah");
        this.deskripsi = Validasi.wajibIsi(deskripsi, "Deskripsi denah");
        this.fasilitasPenting = new ArrayList<>(Validasi.wajibAda(fasilitasPenting, "Fasilitas penting"));
    }

    public String tampilRingkas() {
        return String.format("Lantai %-2d | %-18s | %s | Fasilitas: %s",
                lantai, namaFile, deskripsi, String.join(", ", fasilitasPenting));
    }

    public int getLantai() {
        return lantai;
    }

    public String getNamaFile() {
        return namaFile;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public List<String> getFasilitasPenting() {
        return Collections.unmodifiableList(fasilitasPenting);
    }
}
