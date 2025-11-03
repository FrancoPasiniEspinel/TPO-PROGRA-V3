package clases;
import java.io.IOException;
import java.util.*;




    public class TestOptimizacionPortafolio {
        public static void main(String[] args) {
            try {
                // 1. Rutas a los archivos CSV
                String rutaActivos = "C:/Users/franc/Desktop/PROGRA 3/CSV/activos_financieros_60.csv";
                String rutaCorrelaciones = "C:/Users/franc/Desktop/correlaciones_60 (1).csv";

                // 2. Cargar los datos
                List<Activo> listaActivos = CargarDatos.leerArchivoActivos(rutaActivos);
                DatosCorrelaciones correlaciones = CargarDatos.leerArchivoCorrelaciones(rutaCorrelaciones);

                // 3. Crear un portafolio de prueba (repetimos algún activo a propósito)
                List<Activo> portafolioDePrueba = new ArrayList<>();
                Map<String, Activo> mapaActivos = crearMapaActivos(listaActivos);

                portafolioDePrueba.add(mapaActivos.get("AAPL")); // Supone que existe en el CSV
                portafolioDePrueba.add(mapaActivos.get("GOOG")); // Supone que existe
                portafolioDePrueba.add(mapaActivos.get("AAPL")); // Repetido a propósito

                // 4. Calcular riesgo total
                List<List<Double>> matriz = correlaciones.getMatrizCorrelaciones();
                List<String> nombresOrdenados = correlaciones.getNombresActivos();

                double riesgo = MetodosBackTracking.calcularRiesgoTotal(
                        matriz,
                        portafolioDePrueba
                );

                // 5. Mostrar resultado
                System.out.println("Riesgo total del portafolio de prueba: " + riesgo);

            } catch (IOException e) {
                System.err.println("Error leyendo archivos: " + e.getMessage());
            } catch (NullPointerException e) {
                System.err.println("Un activo usado en el portafolio no existe en el archivo CSV.");
            }
        }

        // Método auxiliar para crear un mapa {nombre → Activo}
        private static Map<String, Activo> crearMapaActivos(List<Activo> activos) {
            Map<String, Activo> mapa = new HashMap<>();
            for (Activo a : activos) {
                mapa.put(a.getNombre(), a);
            }
            return mapa;
        }
    }


