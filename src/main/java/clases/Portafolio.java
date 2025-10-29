package clases;

import java.util.List;

public class Portafolio {

    // 1. Los activos que componen la solución
    private List<Activo> activosSeleccionados;

    // 2. Los cálculos finales de esta combinación
    private double retornoTotalEstimado;
    private double riesgoTotalAjustado;
    private double costoTotal;

    /**
     * Este es el constructor que faltaba.
     * Recibe los datos de una solución encontrada y los guarda.
     */
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

    /**
     * Un método toString() es súper útil para imprimir el resultado
     * de forma prolija en la consola.
     */
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
