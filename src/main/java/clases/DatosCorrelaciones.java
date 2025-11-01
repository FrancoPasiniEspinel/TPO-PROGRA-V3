package clases;

import java.util.List;
import java.util.Map;

public class DatosCorrelaciones {

    private List<String> nombresActivos;
    private List<List<Double>> matrizCorrelaciones;

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

    @Override
    public String toString() {
        return "DatosCorrelaciones{" +
                "nombresActivos=" + nombresActivos +
                ", matrizCorrelaciones=" + matrizCorrelaciones +
                '}';
    }



}
