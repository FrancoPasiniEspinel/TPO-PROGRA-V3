package clases;

import clases.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // Definiciones estáticas para la validación de entrada
    private static final Set<String> SECTORES_VALIDOS = Set.of(
            "Tecnologia", "Finanzas", "Energia", "Salud", "Agro"
    );

    private static final Set<String> TIPOS_ACTIVO_VALIDOS = Set.of(
            "Accion",
            "Bono Soberano",
            "Obligacion Negociable",
            "ETF",
            "CEDEAR"
    );

    public static void main(String[] args) {

        // --- VARIABLES GLOBALES DE DATOS ---
        List<Activo> todosLosActivos = new ArrayList<>();
        DatosCorrelaciones correlaciones = null;

        // --- 1. Carga de Datos (Usando rutas reales) ---
        try {
            // **AJUSTA ESTAS RUTAS A LA UBICACIÓN CORRECTA DE TUS ARCHIVOS CSV**
            todosLosActivos = CargarDatos.leerArchivoActivos();
            correlaciones = CargarDatos.leerArchivoCorrelaciones();

        } catch (IOException e) {
            System.err.println(" Error fatal al leer los archivos de datos. Asegúrate de que las rutas sean correctas y los archivos existan.");
            System.err.println("Detalle: " + e.getMessage());
            return;
        }

        Scanner teclado = new Scanner(System.in);

        // --- 2. Ingreso de Datos del Cliente (Interacción) ---

        System.out.print("------MENÚ PRINCIPAL------\n");
        System.out.print("Ingrese su nombre: ");
        String nombre = teclado.nextLine();

        // [Lógica para PerfilRiesgo]
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

        // [Lógica para Monto Máximo]
        double montoMaximo = 0;
        while (montoMaximo <= 0) {
            try {
                System.out.print("Ingrese el monto máximo a invertir: $");
                montoMaximo = teclado.nextDouble();
                if (montoMaximo <= 0) {
                    System.out.println("El monto debe ser un número positivo.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número.");
                teclado.next();
            }
        }
        teclado.nextLine();

        // [Lógica para Plazo Inversión]
        int plazoInversion = 0;
        while (plazoInversion <= 0) {
            try {
                System.out.print("Ingrese el plazo de la inversión en meses: ");
                plazoInversion = teclado.nextInt();
                if (plazoInversion <= 0) {
                    System.out.println("El plazo debe ser un número entero positivo.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número entero.");
                teclado.next();
            }
        }
        teclado.nextLine();


        // --- Lógica de Preferencias por Sector ---
        Map<String, Double> preferenciasSector = new HashMap<>();
        double porcentajeRestanteSector = 100.0;

        System.out.println("\n--- Preferencias por Sector ---");
        System.out.println("Sectores disponibles: " + SECTORES_VALIDOS);

        while (porcentajeRestanteSector > 0) {
            System.out.printf("\nPorcentaje restante a asignar a sectores: %.2f%%\n", porcentajeRestanteSector);
            System.out.print("Ingrese Sector (o 'OTROS' para restante, 'FIN' para terminar): ");
            String sector = teclado.nextLine().toUpperCase();

            if (sector.equalsIgnoreCase("FIN")) break;

            if (sector.equalsIgnoreCase("OTROS")) {
                if (porcentajeRestanteSector > 0) {
                    preferenciasSector.put("OTROS", porcentajeRestanteSector);
                    porcentajeRestanteSector = 0;
                }
                break;
            }

            if (!SECTORES_VALIDOS.contains(sector)) {
                System.out.println("⚠️ Sector no válido. Elija de la lista.");
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
        if (porcentajeRestanteSector <= 0) System.out.println(" 100% sectorial asignado.");


        //Lógica de Preferencias por Tipo de Activo

        Map<String, Double> preferenciasTipoActivo = new HashMap<>();
        double porcentajeRestanteTipo = 100.0;

        System.out.println("\n--- Preferencias por Tipo de Activo ---");
        System.out.println("Tipos de activo disponibles: " + TIPOS_ACTIVO_VALIDOS);

        while (porcentajeRestanteTipo > 0) {
            System.out.printf("\nPorcentaje restante a asignar: %.2f%%\n", porcentajeRestanteTipo);
            System.out.print("Ingrese Tipo de activo (o 'OTROS' para restante, 'FIN' para terminar): ");
            String tipoActivo = teclado.nextLine();

            if (tipoActivo.equalsIgnoreCase("FIN")) break;

            if (tipoActivo.equalsIgnoreCase("OTROS")) {
                if (porcentajeRestanteTipo > 0) {
                    preferenciasTipoActivo.put("OTROS", porcentajeRestanteTipo);
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

        if (porcentajeRestanteTipo <= 0) System.out.println("100% tipos asignado.");


        // 3. Creación del Cliente y Llamada al Backtracking

        Cliente cliente = new Cliente(nombre, montoMaximo, plazoInversion, perfilSeleccionado, preferenciasSector, preferenciasTipoActivo);
        System.out.println("\n--- Cliente Creado Exitosamente ---");


        // 4. EJECUCIÓN DEL ALGORITMO


        System.out.println("\n--- Calculando Portafolio Óptimo (Backtracking) ---");
        try {
            // Instanciar el optimizador
            MetodosBackTracking optimizador = new MetodosBackTracking();

            // Llamar a la función con los datos REALES
            List<Portafolio> portafoliosFinales = optimizador.encontrarPortafolioOptimo(
                    cliente,
                    todosLosActivos,
                    correlaciones
            );

            // 5. Mostrar el resultado
            System.out.println("\n--- RECOMENDACIONES DE PORTAFOLIO (TOP 3) ---");
            if (portafoliosFinales.isEmpty()) {
                System.out.println("No se encontró ningún portafolio que cumpla con todas las restricciones del cliente.");
            } else {
                int i = 1;
                for (Portafolio p : portafoliosFinales) {
                    System.out.println("----------------------------------------");
                    System.out.println("Alternativa #" + i++);
                    System.out.printf("  Retorno Esperado: %.4f\n", p.getRetornoTotalEstimado(),"%");
                    System.out.printf("  Riesgo Total (Desv. Est.): %.4f\n", p.getRiesgoTotalAjustado(),"%");
                    System.out.printf("  Costo de Inversión: $%.2f\n", p.getCostoTotal());

                    String activosNombres  = p.getActivosSeleccionados().stream()
                            .map(Activo::getNombre)
                            .collect(Collectors.joining(", "));
                    System.out.println("  Activos Seleccionados: " + activosNombres);
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