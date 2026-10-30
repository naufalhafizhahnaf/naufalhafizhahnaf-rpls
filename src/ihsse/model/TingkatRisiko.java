package ihsse.model;

/**
 * Tingkat risiko pada zona risiko internal kampus.
 */
public enum TingkatRisiko {

    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi");

    private final String label;

    TingkatRisiko(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
