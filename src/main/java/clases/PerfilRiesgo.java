package clases;

public enum PerfilRiesgo {
    CONSERVADOR (20,10),
    MODERADAMENTE_CONSERVADOR (30,12),
    MODERADO (40,14),
    MODERADAMENTE_AGRESIVO (50,16),
    AGRESIVO (60,18);

    private final double riesgoMaximo;
    private final double retornoMinimo;

    PerfilRiesgo(double riesgoMaximo, double retornoMinimo) {
        this.riesgoMaximo = riesgoMaximo;
        this.retornoMinimo = retornoMinimo;
    }

    public double getRiesgoMaximo() {
        return riesgoMaximo;
    }
    public double getRetornoMinimo() {
        return retornoMinimo;
    }
}
