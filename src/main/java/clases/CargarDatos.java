package clases;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public  class CargarDatos {

    public static List<Activo> leerArchivoActivos() throws IOException {
        String nombreArchivo = "/activos_financieros_60.csv";
        InputStream inputStream = CargarDatos.class.getResourceAsStream(nombreArchivo);
        if (inputStream == null) {
            throw new IOException("No se pudo encontrar el archivo en resources: " + nombreArchivo);
        }
        List<Activo> listaActivos = new ArrayList<>();
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
            entrada.readLine();
            String linea;
            while ((linea = entrada.readLine()) != null) {
                String[] atributos = linea.split(",");
                Activo activo = completarAtributos(atributos);
                listaActivos.add(activo);
            }

        }

        return listaActivos;
    }

    public static Activo completarAtributos(String[] atributos) {
        String nombre= atributos[0];
        double retornoEsperado = Double.parseDouble(atributos[1]);
        double riesgo = Double.parseDouble(atributos[2]);
        double montoMinimo = Double.parseDouble(atributos[3]);
        String tipo=atributos[4];
        String sector=atributos[5];
        List<Double> porcentajes = new ArrayList<>();
        for (int i = 6; i < atributos.length; i++) {
            double porcentaje = Double.parseDouble(atributos[i]);
            porcentajes.add(porcentaje);

        }

         return new Activo(nombre, retornoEsperado, riesgo, montoMinimo, tipo, sector, porcentajes);

    }

    public static DatosCorrelaciones leerArchivoCorrelaciones() throws IOException {
        String nombreArchivo = "/correlaciones_60 (1).csv";
        List<String> nombresActivos = new ArrayList<>();
        List<List<Double>> matrizCorrelaciones = new ArrayList<>();
        InputStream inputStream = CargarDatos.class.getResourceAsStream(nombreArchivo);
        if (inputStream == null) {
            throw new IOException("No se pudo encontrar el archivo en resources: " + nombreArchivo);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linea;

            if ((linea = br.readLine()) != null) {
                String[] encabezados = linea.split(",");
                for (int i = 1; i < encabezados.length; i++) {
                    nombresActivos.add(encabezados[i]);
                }
            }

            while ((linea = br.readLine()) != null) {
                String[] valores = linea.split(",");
                List<Double> filaNumerica = new ArrayList<>();

                for (int i = 1; i < valores.length; i++) {
                    filaNumerica.add(Double.parseDouble(valores[i].replace(',', '.')));
                }
                matrizCorrelaciones.add(filaNumerica);
            }


        }return new DatosCorrelaciones(nombresActivos, matrizCorrelaciones);
    }
    }







