package clases;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public  class CargarDatos {


    //chequear si este metodo esta bien

    public static List<Activo> leerArchivoActivos() throws IOException {
        String nombreArchivo = "/activos_financieros_60.csv"; // El "/" inicial es importante
        InputStream inputStream = CargarDatos.class.getResourceAsStream(nombreArchivo);
        // 3. ¡Importante! Verifica que el archivo exista
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

        //preguntar si hay que cargar todos los archivos en memoria
    }

    public static DatosCorrelaciones leerArchivoCorrelaciones() throws IOException {
// 1. Define el nombre del archivo (como está en la carpeta 'resources')
        String nombreArchivo = "/correlaciones_60 (1).csv"; // <-- ¡Cambia esto si tu archivo se llama diferente!

        List<String> nombresActivos = new ArrayList<>();
        List<List<Double>> matrizCorrelaciones = new ArrayList<>();

        // 2. Obtén el archivo como InputStream
        // ¡RECUERDA CAMBIAR 'LectorDeCorrelaciones.class' por el nombre de tu clase!
        InputStream inputStream = CargarDatos.class.getResourceAsStream(nombreArchivo);

        // 3. Verifica que el archivo se encontró
        if (inputStream == null) {
            throw new IOException("No se pudo encontrar el archivo en resources: " + nombreArchivo);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
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
            }


        }return new DatosCorrelaciones(nombresActivos, matrizCorrelaciones);
    }
    }







