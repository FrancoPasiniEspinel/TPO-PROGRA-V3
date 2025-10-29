package clases;

import Interfaces.ICargarDatos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public  class CargarDatos implements ICargarDatos {


    //chequear si este metodo esta bien

    public static List<Activo> leerArchivoActivos(String ruta) throws FileNotFoundException {
        var elArchivo = new File(ruta);
        List<Activo> listaActivos = new ArrayList<>();
        try {
            var entrada = new BufferedReader(new FileReader(elArchivo));
            entrada.readLine();
            String  linea = entrada.readLine();
            while (linea != null){
                String[] atributos= linea.split(",");
                List<Double> porcentajes = new ArrayList<>();
                completarAtributos(atributos, porcentajes, listaActivos);
                linea = entrada.readLine();
            }
            entrada.close();
        }catch ( FileNotFoundException e) {
            throw new FileNotFoundException();

        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        return listaActivos;

    }


    public static List<Activo> completarAtributos(String[] atributos, List<Double> porcentajes, List<Activo> listaActivos ) {
        String nombre= atributos[0];
        double retornoEsperado = Double.parseDouble(atributos[1]);
        double riesgo = Double.parseDouble(atributos[2]);
        double montoMinimo = Double.parseDouble(atributos[3]);
        String tipo=atributos[4];
        String sector=atributos[5];
        for (int i = 6; i < atributos.length; i++) {
            double porcentaje = Double.parseDouble(atributos[i]);
            porcentajes.add(porcentaje);

        }

        Activo nuevo_activo= new Activo(nombre, retornoEsperado, riesgo, montoMinimo, tipo, sector, porcentajes);
        listaActivos.add(nuevo_activo);
        return listaActivos;//preguntar si hay que cargar todos los archivos en memoria
    }

    public static DatosCorrelaciones leerArchivoCorrelaciones(String ruta) throws IOException {
        var elArchivo = new File(ruta);
        List<String> nombresActivos = new ArrayList<>();
        List<List<Double>> matrizCorrelaciones = new  ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(elArchivo))) {
            String linea;

            // 1. Leer la primera fila (encabezados) para los nombres
            if ((linea = br.readLine()) != null) {
                // Separa por punto y coma y omite el primer elemento que suele estar vacío
                String[] encabezados = linea.split(",");
                for (int i = 1; i < encabezados.length; i++) {
                    nombresActivos.add(encabezados[i]);
                }
            }

            // 2. Leer el resto de las filas para la matriz
            while ((linea = br.readLine()) != null) {
                String[] valores = linea.split(",");
                // La matriz tendrá una columna menos que el número de valores (se omite el nombre de la fila)
                List<Double> filaNumerica = new ArrayList<>();

                for (int i = 1; i < valores.length; i++) {
                    // Reemplaza la coma decimal por un punto y convierte a double
                    filaNumerica.add(Double.parseDouble(valores[i].replace(',', '.')));
                }
                matrizCorrelaciones.add(filaNumerica);
                DatosCorrelaciones datosCo= new DatosCorrelaciones(nombresActivos, matrizCorrelaciones);
            }
        }catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DatosCorrelaciones datosCo= new DatosCorrelaciones(nombresActivos, matrizCorrelaciones);
        return datosCo;
    }
}







