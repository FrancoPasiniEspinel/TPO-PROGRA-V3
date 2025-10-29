package clases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Activo {
    private String nombre;
    private double retornoEsperado;
    private double riesgo;
    private double montoMinimo;
    private String tipo;
    private String sector;
    private List<Double> historial; //hashmap
    // hashmap correlaciones???<---no es atributo<--- tenemos que hacer algun metodo para usar la informacion de la matriz de correlaciones


    public Activo(String nombre, double retornoEsperado, double riesgo, double montoMinimo, String tipo, String sector, List<Double> historial) {
        this.nombre = nombre;
        this.retornoEsperado = retornoEsperado;
        this.riesgo = riesgo;
        this.montoMinimo = montoMinimo;
        this.tipo = tipo;
        this.sector = sector;
        this.historial = historial;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getRetornoEsperado() {
        return retornoEsperado;
    }

    public void setRetornoEsperado(double retornoEsperado) {
        this.retornoEsperado = retornoEsperado;
    }

    public double getRiesgo() {
        return riesgo;
    }

    public void setRiesgo(double riesgo) {
        this.riesgo = riesgo;
    }

    public double getMontoMinimo() {
        return montoMinimo;
    }

    public void setMontoMinimo(double montoMinimo) {
        this.montoMinimo = montoMinimo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public List<Double> getHistorial() {
        return historial;
    }

    public void setHistorial(ArrayList<Double> historial) {
        this.historial = historial;
    }


    @Override
    public String toString() {
        return "Activo{" +
                "nombre='" + nombre + '\'' +
                ", retornoEsperado=" + retornoEsperado +
                ", riesgo=" + riesgo +
                ", montoMinimo=" + montoMinimo +
                ", tipo='" + tipo + '\'' +
                ", sector='" + sector + '\'' +
                ", historial=" + historial +
                '}';
    }

    //Metodos
    /*public void agregarRendimiento(double rendimiento) {
        historial.add(rendimiento);
    }*/

    public static double calcularRiesgoTotal(double[][] matrizCorrelacion, List<Activo> activosVivos) {
        double riesgoTotal = 0.0;
        for (int i = 0; i < activosVivos.size(); i++) {
            riesgoTotal+=activosVivos.get(i).getRiesgo();
            for (int j = 0; j<activosVivos.size(); j++) {
                riesgoTotal+=matrizCorrelacion[i][j];
            }

        }
        return riesgoTotal;
    }





}
