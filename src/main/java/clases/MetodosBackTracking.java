package clases;

import java.util.List;

public class MetodosBackTracking {
    public static double calcularRiesgoTotal(double[][] matrizCorrelacion, List<Activo> activosVivos)/*los activos tenemos que pasarlos como unicos y poner la cantidad*/ {


        double riesgoTotal = 0.0;
        double montoTotal= calcularCostoTotal(activosVivos);
        for (int i = 0; i < activosVivos.size(); i++) {
            double riesgoActivo=0;
            double participacion=activosVivos.get(i).getMontoMinimo()/montoTotal;//aca faltaria multiplicar por la cantidad de veces que esta el activo en el portafolio
            riesgoActivo+=activosVivos.get(i).getRiesgo()*participacion;
            for (int j = i+1; j<activosVivos.size(); j++) {
                //metodo correlacion entre activos
                riesgoActivo+=(DatosCorrelaciones.correlacionEntreActivos(activosVivos.get(i).getNombre(),activosVivos.get(i).getNombre()))*(activosVivos.get(j).getRiesgo())*(activosVivos.get(i).getRiesgo());

            }
            riesgoTotal+=riesgoActivo;
        }
        return riesgoTotal;
    }


}
