package ihsse.model;

/**
 * Kondisi fasilitas hasil inspeksi.
 */
public enum KondisiFasilitas {

    BAIK("Baik", false),
    RUSAK_RINGAN("Rusak Ringan", true),
    RUSAK_BERAT("Rusak Berat", true),
    KEDALUWARSA("Kedaluwarsa", true);

    private final String label;
    private final boolean perluTindakLanjut;

    KondisiFasilitas(String label, boolean perluTindakLanjut) {
        this.label = label;
        this.perluTindakLanjut = perluTindakLanjut;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPerluTindakLanjut() {
        return perluTindakLanjut;
    }
}
