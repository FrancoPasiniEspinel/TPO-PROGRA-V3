import clases.*;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            String ruta = "C:/Users/franc/Desktop/PROGRA 3/CSV/activos_financieros_60.csv";
            List<Activo> todosLosActivos= CargarDatos.leerArchivoActivos(ruta);
            String ruta2 = "C:/Users/franc/Desktop/correlaciones_60 (1).csv";
            DatosCorrelaciones correlaciones= CargarDatos.leerArchivoCorrelaciones(ruta2);
            System.out.println(CargarDatos.leerArchivoCorrelaciones(ruta2));
        } catch (IOException e) {
            System.out.println("Error al leer los archivos de datos: " + e.getMessage());
            return;
        }

        Scanner teclado = new Scanner(System.in);

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
                if (montoMaximo <= 0) {
                    System.out.println("El monto debe ser un número positivo.");
                }
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
                if (plazoInversion <= 0) {
                    System.out.println("El plazo debe ser un número entero positivo.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número entero.");
                teclado.next();
            }
        }
        teclado.nextLine();


        Map<String, Double> preferenciasSector = new HashMap<>();
        System.out.println("Ingrese sus preferencias por sector (nombre del sector y porcentaje de preferencia). Escriba 'fin' para terminar.");
        while (true) {
            System.out.print("Sector: ");
            String sector = teclado.nextLine();
            if (sector.equalsIgnoreCase("fin")) {
                break;
            }
            try {
                System.out.print("Porcentaje: ");
                double porcentaje = teclado.nextDouble();
                teclado.nextLine();// Limpia el buffer del scanner.
                preferenciasSector.put(sector, porcentaje);
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número para el porcentaje.");
                teclado.next(); // Limpia el buffer del scanner.
            }
        }

        Map<String, Double> preferenciasTipoActivo = new HashMap<>();
        System.out.println("Ingrese sus preferencias por tipo de activo (nombre del tipo y porcentaje de preferencia). Escriba 'fin' para terminar.");
        while (true) {
            System.out.print("Tipo de activo: ");
            String tipoActivo = teclado.nextLine();
            if (tipoActivo.equalsIgnoreCase("fin")) {
                break;
            }
            // Bloque protegido para la entrada de porcentajes de preferencias por tipo de activo.
            try {
                System.out.print("Porcentaje: ");
                double porcentaje = teclado.nextDouble();
                teclado.nextLine(); // Limpia el buffer del scanner.
                preferenciasTipoActivo.put(tipoActivo, porcentaje);
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número para el porcentaje.");
                teclado.next(); // Limpia el buffer del scanner.
            }
        }

        Cliente cliente = new Cliente(nombre, montoMaximo, plazoInversion, perfilSeleccionado, preferenciasSector, preferenciasTipoActivo);
        System.out.println("\nCliente creado exitosamente:");


        teclado.close();
    }
}