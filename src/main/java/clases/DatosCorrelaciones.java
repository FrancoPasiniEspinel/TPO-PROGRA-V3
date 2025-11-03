package clases;

import java.util.List;
import java.util.Map;

public class DatosCorrelaciones {

    private static List<String> nombresActivos;
    private static List<List<Double>> matrizCorrelaciones;

    public DatosCorrelaciones(List<String> nombresActivos, List<List<Double>> matrizCorrelaciones) {
        this.nombresActivos = nombresActivos;
        this.matrizCorrelaciones = matrizCorrelaciones;
    }

    public List<String> getNombresActivos() {
        return nombresActivos;
    }


    public void setNombresActivos(List<String> nombresActivos) {
        this.nombresActivos = nombresActivos;
    }

    public List<List<Double>> getMatrizCorrelaciones() {
        return matrizCorrelaciones;
    }

    public void setMatrizCorrelaciones(List<List<Double>> matrizCorrelaciones) {
        this.matrizCorrelaciones = matrizCorrelaciones;
    }

    public static double correlacionEntreActivos(String nombre1, String nombre2) {
        int indice_activo1 = nombresActivos.indexOf(nombre1);
        int indice_activo2 = nombresActivos.indexOf(nombre2);
        if (indice_activo1 == -1 || indice_activo2 == -1) {
            throw new IllegalArgumentException("Uno o ambos activos no fueron encontrados en la lista.");
        }

        return matrizCorrelaciones.get(indice_activo1).get(indice_activo2);
    }

    @Override
    public String toString() {
        return "DatosCorrelaciones{" +
                "nombresActivos=" + nombresActivos +
                ", matrizCorrelaciones=" + matrizCorrelaciones +
                '}';
    }



}
