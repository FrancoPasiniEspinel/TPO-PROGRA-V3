package clases;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static final Set<String> SECTORES_VALIDOS = Set.of(
            "tecnologia", "finanzas", "energia", "salud", "agro", "consumo"
    );

    private static final Set<String> TIPOS_ACTIVO_VALIDOS = Set.of(
            "accion",
            "bono soberano",
            "obligacion negociable",
            "etf",
            "cedear"
    );

    public static void main(String[] args) {

        List<Activo> todosLosActivos = new ArrayList<>();
        DatosCorrelaciones correlaciones = null;

        // 1. Carga de Datos
        try {
            todosLosActivos = CargarDatos.leerArchivoActivos();
            correlaciones = CargarDatos.leerArchivoCorrelaciones();
        } catch (IOException e) {
            System.err.println(" Error fatal al leer los archivos de datos. Asegúrate de que las rutas sean correctas y los archivos existan.");
            System.err.println("Detalle: " + e.getMessage());
            return;
        }

        Scanner teclado = new Scanner(System.in);

        // 2. Ingreso de datos del cliente
        System.out.print("------MENÚ PRINCIPAL------\n");
        System.out.print("Ingrese su nombre: ");
        String nombre = teclado.nextLine();

        PerfilRiesgo perfilSeleccionado = null;
        while (perfilSeleccionado == null) {
            try {
                System.out.print("Ingrese su perfil de riesgo (CONSERVADOR, MODERADAMENTE_CONSERVADOR, MODERADO, MODERADAMENTE_AGRESIVO, AGRESIVO): ");
                String perfil = teclado.nextLine().toUpperCase();
                perfilSeleccionado = PerfilRiesgo.valueOf(perfil);
                System.out.println("Perfil seleccionado correctamente.");
            } catch (IllegalArgumentException e) {
                System.out.println("Perfil de riesgo no válido. Por favor, intente de nuevo.");
            }
        }

        double montoMaximo = 0;
        while (montoMaximo <= 0) {
            try {
                System.out.print("Ingrese el monto máximo a invertir: $");
                montoMaximo = teclado.nextDouble();
                if (montoMaximo <= 0) System.out.println("El monto debe ser un número positivo.");
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número.");
                teclado.next();
            }
        }
        teclado.nextLine();

        int plazoInversion = 0;
        while (plazoInversion <= 0) {
            try {
                System.out.print("Ingrese el plazo de la inversión en meses: ");
                plazoInversion = teclado.nextInt();
                if (plazoInversion <= 0) System.out.println("El plazo debe ser un número entero positivo.");
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número entero.");
                teclado.next();
            }
        }
        teclado.nextLine();

        //  Preferencias de sectores y tipos (idéntico a tu versión)
        Map<String, Double> preferenciasSector = new HashMap<>();
        double porcentajeRestanteSector = 100.0;
        System.out.println("\n--- Preferencias por Sector ---");
        System.out.println("Sectores disponibles: " + SECTORES_VALIDOS);
        while (porcentajeRestanteSector > 0) {
            System.out.printf("\nPorcentaje restante a asignar a sectores: %.2f%%\n", porcentajeRestanteSector);
            System.out.print("Ingrese Sector (o 'OTROS' para restante, 'FIN' para terminar): ");
            String sector = teclado.nextLine().toLowerCase();

            if (sector.equals("fin")) break;
            if (sector.equals("otros")) {
                if (porcentajeRestanteSector > 0) {
                    preferenciasSector.put("otros", porcentajeRestanteSector);
                    porcentajeRestanteSector = 0;
                }
                break;
            }
            if (!SECTORES_VALIDOS.contains(sector)) {
                System.out.println(" Sector no válido. Elija de la lista.");
                continue;
            }

            try {
                System.out.print("Porcentaje a asignar a " + sector + ": ");
                double porcentaje = teclado.nextDouble();
                teclado.nextLine();

                if (porcentaje <= 0 || porcentaje > porcentajeRestanteSector) {
                    System.out.printf(" Porcentaje no válido. Máximo: %.2f%%.\n", porcentajeRestanteSector);
                    continue;
                }

                preferenciasSector.put(sector, preferenciasSector.getOrDefault(sector, 0.0) + porcentaje);
                porcentajeRestanteSector -= porcentaje;
                System.out.printf("Asignado. (Total %s: %.2f%%).\n", sector, preferenciasSector.get(sector));
            } catch (InputMismatchException e) {
                System.out.println(" Entrada no válida. Ingrese un número.");
                teclado.nextLine();
            }
        }

        Map<String, Double> preferenciasTipoActivo = new HashMap<>();
        double porcentajeRestanteTipo = 100.0;
        System.out.println("\n--- Preferencias por Tipo de Activo ---");
        System.out.println("Tipos de activo disponibles: " + TIPOS_ACTIVO_VALIDOS);

        while (porcentajeRestanteTipo > 0) {
            System.out.printf("\nPorcentaje restante a asignar: %.2f%%\n", porcentajeRestanteTipo);
            System.out.print("Ingrese Tipo de activo (o 'OTROS' para restante, 'FIN' para terminar): ");
            String tipoActivo = teclado.nextLine().toLowerCase();

            if (tipoActivo.equals("fin")) break;
            if (tipoActivo.equals("otros")) {
                if (porcentajeRestanteTipo > 0) {
                    preferenciasTipoActivo.put("otros", porcentajeRestanteTipo);
                    porcentajeRestanteTipo = 0;
                }
                break;
            }
            if (!TIPOS_ACTIVO_VALIDOS.contains(tipoActivo)) {
                System.out.println(" Tipo de activo no válido. Elija de la lista.");
                continue;
            }

            try {
                System.out.print("Porcentaje a asignar a " + tipoActivo + ": ");
                double porcentaje = teclado.nextDouble();
                teclado.nextLine();

                if (porcentaje <= 0 || porcentaje > porcentajeRestanteTipo) {
                    System.out.printf("Porcentaje no válido. Máximo: %.2f%%.\n", porcentajeRestanteTipo);
                    continue;
                }

                preferenciasTipoActivo.put(tipoActivo, preferenciasTipoActivo.getOrDefault(tipoActivo, 0.0) + porcentaje);
                porcentajeRestanteTipo -= porcentaje;
                System.out.printf("Asignado. (Total %s: %.2f%%).\n", tipoActivo, preferenciasTipoActivo.get(tipoActivo));
            } catch (InputMismatchException e) {
                System.out.println(" Entrada no válida. Ingrese un número.");
                teclado.nextLine();
            }
        }

        // 3. Crear cliente y ejecutar algoritmo
        Cliente cliente = new Cliente(nombre, montoMaximo, plazoInversion, perfilSeleccionado, preferenciasSector, preferenciasTipoActivo);
        System.out.println("\n--- Cliente Creado Exitosamente ---");

        System.out.println("\n--- Calculando Portafolio Óptimo (Backtracking) ---");
        try {
            MetodosBackTracking optimizador = new MetodosBackTracking();
            List<Portafolio> portafoliosFinales = optimizador.encontrarPortafolioOptimo(cliente, todosLosActivos, correlaciones);

            // 5. Mostrar resultado
            System.out.println("\n--- RECOMENDACIONES DE PORTAFOLIO (TOP 3) ---");
            if (portafoliosFinales.isEmpty()) {
                System.out.println("No se encontró ningún portafolio que cumpla con todas las restricciones del cliente.");
            } else {
                for (int i = 0; i < portafoliosFinales.size(); i++) {
                    Portafolio p = portafoliosFinales.get(i);
                    System.out.println("----------------------------------------");
                    if (i == 0)
                        System.out.println("PORTAFOLIO GANADOR!");
                    else
                        System.out.println("Alternativa #" + i);

                    System.out.printf("  Retorno Esperado: %.4f%%\n", p.getRetornoTotalEstimado());
                    System.out.printf("  Riesgo Total (Desv. Est.): %.4f%%\n", p.getRiesgoTotalAjustado());
                    System.out.printf("  Costo de Inversión: $%.2f\n", p.getCostoTotal());

                    String activosNombres = p.getActivosSeleccionados().stream()
                            .map(Activo::getNombre)
                            .collect(Collectors.joining(", "));
                    System.out.println("  Activos Seleccionados: " + activosNombres);

                    //porcentajes por sector y tipo
                    Map<String, Double> pctSector = p.porcentajePorSector();
                    Map<String, Double> pctTipo = p.porcentajePorTipo();

                    System.out.println("  Distribución por Sector: " + Portafolio.formatearPorcentajes(pctSector));
                    System.out.println("  Distribución por Tipo:   " + Portafolio.formatearPorcentajes(pctTipo));
                }
                System.out.println("----------------------------------------");
            }

        } catch (Exception e) {
            System.out.println("ERROR INESPERADO durante el cálculo del portafolio: " + e.getMessage());
            e.printStackTrace();
        }

        teclado.close();
    }
}