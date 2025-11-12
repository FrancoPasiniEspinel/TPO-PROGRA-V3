package clases;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Portafolio {

    // 1. Los activos que componen la solución
    private List<Activo> activosSeleccionados;

    // 2. Los cálculos finales de esta combinación
    private double retornoTotalEstimado;
    private double riesgoTotalAjustado;
    private double costoTotal;

    public Portafolio(List<Activo> activosSeleccionados, double retornoTotalEstimado,
                      double riesgoTotalAjustado, double costoTotal) {
        this.activosSeleccionados = activosSeleccionados;
        this.retornoTotalEstimado = retornoTotalEstimado;
        this.riesgoTotalAjustado = riesgoTotalAjustado;
        this.costoTotal = costoTotal;
    }

    // Getters para poder mostrar los resultados en el Main

    public List<Activo> getActivosSeleccionados() {
        return activosSeleccionados;
    }

    public double getRetornoTotalEstimado() {
        return retornoTotalEstimado;
    }

    public double getRiesgoTotalAjustado() {
        return riesgoTotalAjustado;
    }

    public double getCostoTotal() {
        return costoTotal;
    }

    public Map<String, Double> porcentajePorSector() {
        double total = getCostoTotal();
        Map<String, Double> acumulado = new HashMap<>();
        for (Activo a : getActivosSeleccionados()) {
            acumulado.merge(a.getSector(), a.getMontoMinimo(), Double::sum);
        }
        // pasar a %
        Map<String, Double> pct = new HashMap<>();
        if (total > 0) {
            for (Map.Entry<String, Double> e : acumulado.entrySet()) {
                pct.put(e.getKey(), (e.getValue() / total) * 100.0);
            }
        }
        return pct;
    }

    public Map<String, Double> porcentajePorTipo() {
        double total = getCostoTotal();
        Map<String, Double> acumulado = new HashMap<>();
        for (Activo a : getActivosSeleccionados()) {
            acumulado.merge(a.getTipo(), a.getMontoMinimo(), Double::sum);
        }
        Map<String, Double> pct = new HashMap<>();
        if (total > 0) {
            for (Map.Entry<String, Double> e : acumulado.entrySet()) {
                pct.put(e.getKey(), (e.getValue() / total) * 100.0);
            }
        }
        return pct;
    }
    public static String formatearPorcentajes(Map<String, Double> mapa) {
        return mapa.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> String.format("%s: %.1f%%", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" | "));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- PORTAFOLIO ÓPTIMO ENCONTRADO --- \n");
        sb.append(String.format("  Retorno Total: %.2f%% \n", retornoTotalEstimado));
        sb.append(String.format("  Riesgo Total:  %.2f \n", riesgoTotalAjustado));
        sb.append(String.format("  Costo Total:   $%.2f \n", costoTotal));
        sb.append("  Activos Incluidos (" + activosSeleccionados.size() + "): \n");

        for (Activo a : activosSeleccionados) {
            sb.append(String.format("    - %s (Sector: %s, Tipo: %s, Costo: $%.2f)\n",
                    a.getNombre(), a.getSector(), a.getTipo(), a.getMontoMinimo()));
        }
        sb.append("----------------------------------------");
        return sb.toString();
    }
}
