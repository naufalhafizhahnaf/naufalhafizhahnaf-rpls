package ihsse.model;

import ihsse.util.Validasi;

/**
 * INHERITANCE: Admin juga merupakan Pengguna, tetapi memiliki data kepegawaian.
 */
public class Admin extends Pengguna {

    private String username;
    private String jabatan;

    public Admin(String id, String nama, String nomorIdentitas, String noTelepon,
                 String username, String jabatan) {
        super(id, nama, nomorIdentitas, noTelepon);
        this.username = Validasi.wajibIsi(username, "Username");
        this.jabatan = Validasi.wajibIsi(jabatan, "Jabatan");
    }

    @Override
    public String getPeran() {
        return "Admin";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = Validasi.wajibIsi(username, "Username");
    }

    public String getJabatan() {
        return jabatan;
    }

    public void setJabatan(String jabatan) {
        this.jabatan = Validasi.wajibIsi(jabatan, "Jabatan");
    }
}
