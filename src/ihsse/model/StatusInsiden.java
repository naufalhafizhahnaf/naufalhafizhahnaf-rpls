package ihsse.model;

/**
 * Status alur penanganan laporan insiden.
 * Enum membatasi nilai yang boleh dipakai sehingga status tidak bisa diisi sembarang teks.
 */
public enum StatusInsiden {

    BARU("Baru"),
    DIPROSES("Diproses"),
    SELESAI("Selesai");

    private final String label;

    StatusInsiden(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Aturan bisnis: BARU -> DIPROSES -> SELESAI.
     * Perubahan lain, termasuk mundur atau langsung selesai, ditolak.
     */
    public boolean bolehBerubahKe(StatusInsiden tujuan) {
        if (tujuan == null) {
            return false;
        }
        if (this == BARU && tujuan == DIPROSES) {
            return true;
        }
        return this == DIPROSES && tujuan == SELESAI;
    }
}
