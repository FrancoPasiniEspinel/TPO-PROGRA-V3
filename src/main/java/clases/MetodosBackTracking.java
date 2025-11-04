package clases;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetodosBackTracking {

    private Cliente cliente;
    private List<Activo> activosElegibles; // La lista pre-filtrada y ordenada
    private DatosCorrelaciones correlaciones;
    private double RIESGO_MAX;
    private double RETORNO_MIN;
    private double PRESUPUESTO_MAX;
    private Map<String, Double> max_porSector;
    private Map<String, Double> max_porTipo;

    private Map<String, Double> min_porSector;
    private Map<String, Double> min_porTipo;

    private final int MIN_ACTIVOS = 3;//cambiado
    private final int MAX_ACTIVOS = 6;
    private final int CANTIDAD_ALTERNATIVAS = 3;

    private List<Solucion> mejoresSoluciones;

    private List<Activo> activosOrdenadosPorRetorno;

    public List<Portafolio> encontrarPortafolioOptimo(Cliente cliente, List<Activo> todosLosActivos, DatosCorrelaciones correlaciones) {

        // 1. Guardar entradas en variables de instancia
        this.cliente = cliente;
        this.correlaciones = correlaciones;

        // 2. Inicializar estado de la solución
        this.mejoresSoluciones = new ArrayList<>(); // Equivale a -infinito

        // 3. Fase Cero: Preparación Inteligente (Pre-procesamiento)

        // 3a. Calcular límites y restricciones del Cliente
        // (Hago esto aquí porque me pediste no modificar las clases originales)
        this.PRESUPUESTO_MAX = cliente.getMontoMaximo();
        this.RIESGO_MAX = cliente.getPerfil().getRiesgoMaximo();

        // Usamos el retorno del perfil
        this.RETORNO_MIN = cliente.getPerfil().getRetornoMinimo();

        // 3b. Calcular límites absolutos de diversificación (en dólares)
        this.max_porSector = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasSector().entrySet()) {
            // entry.getValue() es el porcentaje (ej: 0.30)
            max_porSector.put(entry.getKey(), cliente.getMontoMaximo() * entry.getValue() / 100);
        }
        this.max_porTipo = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasTipoActivo().entrySet()) {
            max_porTipo.put(entry.getKey(), cliente.getMontoMaximo() * entry.getValue() / 100.0);
        }

        // Inicializo los 'minUSD' vacíos por ahora
        this.min_porSector = new HashMap<>();
        this.min_porTipo = new HashMap<>();

        // 3c. Filtrar y Ordenar la lista de activos (según tu informe estratégico)
        this.activosElegibles = procesarActivos(todosLosActivos, cliente);

        //AGREGO ESTA LINEA PARA DEBUG!!!!!
        System.out.println("DEBUG (INICIAL): Activos elegibles disponibles: " + this.activosElegibles.size());
        this.activosOrdenadosPorRetorno = this.activosElegibles.stream()
                .sorted(Comparator.comparingDouble(Activo::getRetornoEsperado).reversed())
                .collect(Collectors.toList());


        // 4. Inicializar mapas para el backtracking
        // (gastoSector y gastoTipo del pseudocódigo)
        Map<String, Double> gastoSectorInicial = new HashMap<>();
        Map<String, Double> gastoTipoInicial = new HashMap<>();

        // Inicializamos los mapas de gasto en 0.0
        for (String sector : cliente.getPreferenciasSector().keySet()) {
            gastoSectorInicial.put(sector, 0.0);
        }
        for (String tipo : cliente.getPreferenciasTipoActivo().keySet()) {
            gastoTipoInicial.put(tipo, 0.0);
        }

        // 5. Iniciar la recursión
        // S = ∅ (una nueva lista vacía)
        // presupuestoUsado = 0
        backtrack(0, new ArrayList<Activo>(), gastoSectorInicial, gastoTipoInicial, 0.0);
        List<Portafolio> portafoliosOrdenados = new ArrayList<>();
        for (Solucion s : this.mejoresSoluciones) {
            portafoliosOrdenados.add(s.getPortafolio());
        }

        return portafoliosOrdenados; // Devuelve la lista Top 3
    }

    // Pon esta clase DENTRO de tu clase principal del optimizador
    private class Solucion implements Comparable<Solucion> {
        Portafolio portafolio;
        double retorno;

        public Solucion(Portafolio portafolio, double retorno) {
            this.portafolio = portafolio;
            this.retorno = retorno;
        }

        public double getRetorno() {
            return retorno;
        }

        public Portafolio getPortafolio() {
            return portafolio;
        }

        // Esto nos permite ordenar de MAYOR a MENOR retorno
        @Override
        public int compareTo(Solucion otra) {
            return Double.compare(otra.retorno, this.retorno);
        }
    }


    public double calcularRiesgoTotal(List<List<Double>> matrizCorrelacion, List<Activo> activosVivos)/*los activos tenemos que pasarlos como unicos y poner la cantidad*/ {
        double riesgoTotal = 0.0;
        double montoTotal = calcularCostoTotal(activosVivos);
        Set<String> activosProcesados = new HashSet<>();
        for (int i = 0; i < activosVivos.size(); i++) {
            String nombreActivoActual = activosVivos.get(i).getNombre();
            if (activosProcesados.add(nombreActivoActual) == false) {
                continue;
            }
            double riesgoActivo = 0;
            int contadorActivos = cantidadActivos(activosVivos.get(i).getNombre(), activosVivos);
            double participacion = (activosVivos.get(i).getMontoMinimo() * contadorActivos) / montoTotal;//aca faltaria multiplicar por la cantidad de veces que esta el activo en el portafolio
            riesgoActivo += activosVivos.get(i).getRiesgo() * participacion;
            for (int j = i + 1; j < activosVivos.size(); j++) {
                //metodo correlacion entre activos
                riesgoActivo += (DatosCorrelaciones.correlacionEntreActivos(activosVivos.get(j).getNombre(), activosVivos.get(i).getNombre())) * (activosVivos.get(j).getRiesgo()) * (activosVivos.get(i).getRiesgo());

            }
            riesgoTotal += riesgoActivo;

        }
        System.out.println("DEBUG RIESGO: " + riesgoTotal);
        return riesgoTotal;
    }

    public int cantidadActivos(String nombre, List<Activo> activosVivos) {
        int contador = 0;
        for (Activo activo : activosVivos) {
            if (activo.getNombre().equals(nombre)) {
                contador += 1;
            }
        }
        return contador;
    }

    public double calcularRetornoTotal(List<Activo> portafolio) {
        if  (portafolio.isEmpty()) {
            return 0.0;
        }
        double retorno = 0.0;
        double montoTotal = calcularCostoTotal(portafolio);
        for (Activo a : portafolio) {
            double participacion = a.getMontoMinimo() / montoTotal;
            retorno += participacion * a.getRetornoEsperado();
        }
        return retorno;
    }

    private double calcularCostoTotal(List<Activo> portafolio) {//esta bien
        double costo = 0.0;
        for (Activo a : portafolio) {
            costo += a.getMontoMinimo();
        }

        return costo;
    }
// --- REEMPLAZA TUS MÉTODOS DE COTA CON ESTO ---

    private double calcularCotaSuperior(int idx, List<Activo> portafolioActual,
                                        double presupuestoUsado, int slotsUsados) {

        // OJO: usa portafolioActual.size() para ser preciso
        int slotsRestantes = this.MAX_ACTIVOS - portafolioActual.size();

        // (Ya no necesitamos 'presupuestoRestante')

        List<Activo> portafolioSimulado = new ArrayList<>(portafolioActual);

        // Llama a 'completarPorRetorno' que ahora usará la lista pre-ordenada
        List<Activo> portafolioConGreedy = completarPorRetorno(
                portafolioSimulado,
                slotsRestantes
        );

        double retornoEstimado = calcularRetornoTotal(portafolioConGreedy);
        return retornoEstimado;
    }

    /**
     * Rellena los slots restantes de un portafolio simulado con los mejores
     * activos de la lista pre-ordenada 'activosOrdenadosPorRetorno'.
     * Esta versión es OPTIMISTA (ignora el presupuesto).
     */
    public List<Activo> completarPorRetorno(List<Activo> portafolioSimulado, int slotsRestantes) {

        // ¡YA NO ORDENA! Usa el atributo de la clase (this.activosOrdenadosPorRetorno)

        for (Activo activo : this.activosOrdenadosPorRetorno) {
            if (slotsRestantes != 0) {
                // Si el portafolio simulado NO contiene ya este activo...
                if (!portafolioSimulado.contains(activo)) {
                    portafolioSimulado.add(activo); // ...lo agrega
                    slotsRestantes--;
                }
            } else {
                break; // Se llenaron los slots
            }
        }
        return portafolioSimulado;
    }


    public List<Activo> procesarActivos(List<Activo> todosLosActivos, Cliente cliente) {
        System.out.println("--- DEBUG: Iniciando procesarActivos ---");
        System.out.println("Activos iniciales: " + todosLosActivos.size());

        // --- 1. Banderas (igual que antes) ---
        boolean prefiereOtrosSector = cliente.getPreferenciasSector().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        boolean prefiereOtrosTipo = cliente.getPreferenciasTipoActivo().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        System.out.println("¿Prefiere 'Otros' Sector? -> " + prefiereOtrosSector);
        System.out.println("¿Prefiere 'Otros' Tipo? -> " + prefiereOtrosTipo);

        // --- 2. Sets en minúscula (igual que antes) ---
        Set<String> sectoresPermitidos = cliente.getPreferenciasSector().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> tiposPermitidos = cliente.getPreferenciasTipoActivo().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        System.out.println("Sectores Permitidos (en minúscula): " + sectoresPermitidos);
        System.out.println("Tipos Permitidos (en minúscula): " + tiposPermitidos);

        // --- 3. Lógica del IF ---

        // Empezamos con el stream de todos los activos
        Stream<Activo> streamFiltrado = todosLosActivos.stream();

        // Filtro 1: ¿Filtramos por Sector?
        // Solo filtramos si la opción "Otros" NO está presente.
        if (!prefiereOtrosSector) {
            streamFiltrado = streamFiltrado.filter(a ->
                    sectoresPermitidos.contains(a.getSector().toLowerCase())
            );
        }
        // Si prefiereOtrosSector es 'true', este 'if' se salta
        // y no se aplica ningún filtro de sector (quedan todos).
        // --- DEBUG: Ver cuántos quedan después del filtro de sector ---
        List<Activo> postSector = streamFiltrado.collect(Collectors.toList());
        System.out.println("Activos restantes tras filtro Sector: " + postSector.size());
        streamFiltrado = postSector.stream(); // Convertir de nuevo a stream

        // Filtro 2: ¿Filtramos por Tipo?
        // Solo filtramos si la opción "Otros" NO está presente.
        if (!prefiereOtrosTipo) {
            streamFiltrado = streamFiltrado.filter(a ->
                    tiposPermitidos.contains(a.getTipo().toLowerCase())
            );
        }

        // --- DEBUG: Ver cuántos quedan después del filtro de tipo ---
        List<Activo> postTipo = streamFiltrado.collect(Collectors.toList());
        System.out.println("Activos restantes tras filtro Tipo: " + postTipo.size());
        streamFiltrado = postTipo.stream();
        // Si prefiereOtrosTipo es 'true', este 'if' se salta.

        // Filtro 3: El presupuesto (este siempre se aplica)
        streamFiltrado = streamFiltrado.filter(a ->
                a.getMontoMinimo() <= cliente.getMontoMaximo()
        );
        // --- DEBUG: Ver cuántos quedan después del filtro de presupuesto ---
        List<Activo> postPresupuesto = streamFiltrado.collect(Collectors.toList());
        System.out.println("Activos restantes tras filtro Presupuesto: " + postPresupuesto.size());
        System.out.println("(Presupuesto Máx. Cliente: " + cliente.getMontoMaximo() + ")");
        streamFiltrado = postPresupuesto.stream();

        // --- 4. Recolectar y Ordenar ---

        // Al final, recolectamos el resultado de todos los filtros que se hayan aplicado
        List<Activo> filtrados = streamFiltrado.collect(Collectors.toList());
        PerfilRiesgo perfil = cliente.getPerfil();

        switch (perfil) {
            case AGRESIVO:
                filtrados.sort(Comparator.comparingDouble(Activo::getRetornoEsperado).reversed());
                break;

            case MODERADAMENTE_AGRESIVO:
                filtrados.sort(Comparator.comparingDouble(Activo::getRetornoEsperado).reversed());
                break;

            case MODERADO:
                filtrados.sort(Comparator.comparingDouble((Activo a) -> {
                    if (a.getRiesgo() == 0) return 0.0;
                    return a.getRetornoEsperado() / a.getRiesgo();
                }).reversed());
                break;

            case MODERADAMENTE_CONSERVADOR:
                filtrados.sort(Comparator.comparingDouble(Activo::getRiesgo));
                break;

            case CONSERVADOR:
                filtrados.sort(Comparator.comparingDouble(Activo::getRiesgo));
                break;
        }

        return filtrados;
    }

    /**
     * Verifica si el gasto actual (parcial) viola los límites MÁXIMOS de diversificación.
     * Esta función SÍ maneja la categoría "Otros".
     */
    private boolean cumpleDiversificacionParcial(Map<String, Double> gastoSector, Map<String, Double> gastoTipo) {

        // --- 1. Revisión de Sectores ---
        double gastoOtrosSector = 0.0; // Acumulador para gasto "Otros"

        for (Map.Entry<String, Double> entry : gastoSector.entrySet()) {
            String sector = entry.getKey();
            double gasto = entry.getValue();

            // Verifica si el sector actual tiene un límite específico (ej: "Tecnologia")
            if (this.max_porSector.containsKey(sector)) {
                // Es un sector específico. Comprueba su límite.
                if (gasto > this.max_porSector.get(sector)) {
                    return false; // Violó el límite de un sector específico
                }
            } else {
                // No es un sector específico, así que suma al gasto de "Otros"
                gastoOtrosSector += gasto;
            }
        }

        // Al final del bucle, comprueba el límite total de "Otros" (si existe)
        if (this.max_porSector.containsKey("Otros")) {
            if (gastoOtrosSector > this.max_porSector.get("Otros")) {
                return false; // Violó el límite de "Otros"
            }
        } else if (gastoOtrosSector > 0) {
            // Hubo gasto "Otro" pero la categoría "Otros" no estaba permitida
            return false;
        }


        // --- 2. Revisión de Tipos (misma lógica) ---
        double gastoOtrosTipo = 0.0; // Acumulador para gasto "Otros"

        for (Map.Entry<String, Double> entry : gastoTipo.entrySet()) {
            String tipo = entry.getKey();
            double gasto = entry.getValue();

            if (this.max_porTipo.containsKey(tipo)) {
                // Es un tipo específico. Comprueba su límite.
                if (gasto > this.max_porTipo.get(tipo)) {
                    return false; // Violó el límite de un tipo específico
                }
            } else {
                // No es un tipo específico, suma al gasto de "Otros"
                gastoOtrosTipo += gasto;
            }
        }

        // Al final, comprueba el límite total de "Otros" para Tipos
        if (this.max_porTipo.containsKey("Otros")) {
            if (gastoOtrosTipo > this.max_porTipo.get("Otros")) {
                return false; // Violó el límite de "Otros"
            }
        } else if (gastoOtrosTipo > 0) {
            return false;
        }

        // Si pasó todas las revisiones, es válido
        return true;
    }

    private boolean esDiversificacionFinalValida(Map<String, Double> gastoSector, Map<String, Double> gastoTipo) {
        // 1. Chequear máximos (reutilizamos la función anterior)
        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return false;
        }

        // 2. Chequear MÍNIMOS de gasto por Sector
        // NOTA: Tu clase Cliente no define mínimos, así que tu pseudocódigo
        // "si minUSD_porSector[s] existe..." siempre será falso.
        // Lo programo por si decidís agregarlos después.
        for (Map.Entry<String, Double> entry : this.min_porSector.entrySet()) {
            String sector = entry.getKey();
            double minimoRequerido = entry.getValue();
            if (gastoSector.getOrDefault(sector, 0.0) < minimoRequerido) {
                return false; // No se alcanzó el mínimo requerido
            }
        }

        // 3. Chequear MÍNIMOS de gasto por Tipo
        for (Map.Entry<String, Double> entry : this.min_porTipo.entrySet()) {
            String tipo = entry.getKey();
            double minimoRequerido = entry.getValue();
            if (gastoTipo.getOrDefault(tipo, 0.0) < minimoRequerido) {
                return false; // No se alcanzó el mínimo requerido
            }
        }

        return true; // Pasó todas las validaciones (máximos y mínimos)
    }


    //-------------------------------------------------BACKTRACKING----------------------------------------------------------//


    private void backtrack(int idx, List<Activo> portafolioActual,
                           Map<String, Double> gastoSector, Map<String, Double> gastoTipo,
                           double presupuestoUsado) {

        // --- 1. PODAS (PRUNING) ---
        // (Las 4 podas que definiste)

        // Poda 1: Presupuesto
        // si presupuestoUsado > PRESUPUESTO_MAX → retornar
        if (presupuestoUsado > this.PRESUPUESTO_MAX) {
            return; // Se pasó del presupuesto
        }

        // Poda 2: Riesgo
        // si riesgo_total(S) > RIESGO_MAX → retornar
        // Poda 2: Riesgo
// si riesgo_total(S) > RIESGO_MAX → retornarDA ERROR

        double riesgoActual = calcularRiesgoTotal(this.correlaciones.getMatrizCorrelaciones(), portafolioActual);

        if (riesgoActual > this.RIESGO_MAX) {
            return; // Se pasó del riesgo
        }


        // Poda 3: Diversificación (Máximos de dinero)
        // si !cumple_diversificacion_parcial(S, gastoSector, gastoTipo) → retornar
        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return; // Se pasó del % máximo en un sector o tipo
        }

// Poda 4: Cotas (Branch and Bound)
        double cotaSuperior = calcularCotaSuperior(idx, portafolioActual, presupuestoUsado, portafolioActual.size());

// 1. Determinar el "puntaje a vencer".
//    Por defecto, es el mínimo que pide el cliente.
        double scoreParaVencer = this.RETORNO_MIN;

// 2. Si ya tenemos un Top 3 completo...
        if (this.mejoresSoluciones.size() == CANTIDAD_ALTERNATIVAS) {

            // ...el "puntaje a vencer" es el retorno de la 3ra mejor solución.
            double tercerMejorRetorno = this.mejoresSoluciones.get(CANTIDAD_ALTERNATIVAS - 1).getRetorno();

            // Nos quedamos con el más alto entre el mínimo del cliente y nuestro 3er lugar
            scoreParaVencer = Math.max(scoreParaVencer, tercerMejorRetorno);
        }


        if (cotaSuperior < scoreParaVencer) {
            System.out.println("DEBUG: Poda 4 (Cota) cortada en índice " + idx + ". Cota (" + String.format("%.4f", cotaSuperior) + ") < Minimo Requerido/3er Lugar (" + String.format("%.4f", scoreParaVencer) + ")");
            return;
        }


// --- 2. EVALUAR Y GUARDAR SOLUCIÓN CANDIDATA ---
// (Esto se ejecuta si la rama NO fue podada)

        int nroActivos = portafolioActual.size();
        double retornoActual = calcularRetornoTotal(portafolioActual);

// si MIN_ACTIVOS ≤ |S| (ya sabemos que es ≤ MAX_ACTIVOS por la poda)
        if (nroActivos >= this.MIN_ACTIVOS) {

            // Comprobamos si cumple las condiciones FINALES
            // (Re-usamos el 'riesgoActual' que calculamos en la Poda 2)
            if (retornoActual >= this.RETORNO_MIN &&
                    riesgoActual <= this.RIESGO_MAX &&
                    esDiversificacionFinalValida(gastoSector, gastoTipo)) {
                System.out.println("DEBUG: ¡SOLUCIÓN ENCONTRADA! Ret: " + retornoActual + ", Riesgo: " + riesgoActual);
                // <-- ¡FUNCIÓN NUEVA!

                // --- ¡LÓGICA DEL TOP 3! ---

                // 1. Crear el objeto Solucion
                double costoActual = calcularCostoTotal(portafolioActual);

                Solucion nuevaSolucion = new Solucion(
                        // ¡IMPORTANTE! Se crea una COPIA del portafolio y sus activos
                        new Portafolio(new ArrayList<>(portafolioActual), retornoActual, riesgoActual, costoActual),
                        retornoActual);

                // 2. Añadir la nueva solución a la lista
                this.mejoresSoluciones.add(nuevaSolucion);

                // 3. Re-ordenar la lista (de mayor a menor retorno)
                //    (La clase Solucion debe tener el método 'compareTo' para esto)
                Collections.sort(this.mejoresSoluciones);

                // 4. Recortar la lista si excede el Top 3
                if (this.mejoresSoluciones.size() > CANTIDAD_ALTERNATIVAS) {
                    // Quita el último elemento (el 4to, que es el peor)
                    this.mejoresSoluciones.remove(CANTIDAD_ALTERNATIVAS);
                }
            } else {
                // --- AÑADE ESTE BLOQUE 'ELSE' PARA VER POR QUÉ FALLA ---
                System.out.println("DEBUG: Solución RECHAZADA (nroActivos=" + nroActivos + ")");
                if (retornoActual < this.RETORNO_MIN) {
                    System.out.println("    -> MOTIVO: Retorno " + String.format("%.2f", retornoActual) + " < Mínimo " + String.format("%.2f", this.RETORNO_MIN));
                }
                if (riesgoActual > this.RIESGO_MAX) {
                    System.out.println("    -> MOTIVO: Riesgo " + String.format("%.2f", riesgoActual) + " > Máximo " + String.format("%.2f", this.RIESGO_MAX));
                }
                if (!esDiversificacionFinalValida(gastoSector, gastoTipo)) {
                    System.out.println("    -> MOTIVO: Falla Diversificación Final");
                }
            }


            // --- 3. CORTE (CASO BASE) ---

            // si idx == n o |S| == MAX_ACTIVOS → retornar
            if (nroActivos == this.MAX_ACTIVOS || // Se llenó el portafolio (máx 6)
                    idx == this.activosElegibles.size()) { // No hay más activos para decidir
                return;
            }


            // --- 4. RECURSIÓN (RAMIFICACIÓN) ---

            // Obtenemos el activo a decidir
            Activo activoActual = this.activosElegibles.get(idx);
            double nuevoCosto = activoActual.getMontoMinimo();
            String sector = activoActual.getSector();
            String tipo = activoActual.getTipo();

            // --- Decisión 1: INCLUIR el activo 'idx' ---

            // a = Activos[idx]
            // S.push(a)
            portafolioActual.add(activoActual);

// ¡ARREGLO! Usa getOrDefault para evitar NullPointerException
            gastoSector.put(sector, gastoSector.getOrDefault(sector, 0.0) + nuevoCosto);
            gastoTipo.put(tipo, gastoTipo.getOrDefault(tipo, 0.0) + nuevoCosto);

            // Backtrack(idx+1, S, gastoSector, gastoTipo, presupuestoUsado + costo[a])
            backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado + nuevoCosto);

            // --- Backtrack (Deshacer la decisión) ---

            // S.pop()
            portafolioActual.remove(nroActivos); // Saca el último

            // gastoSector[sector[a]] -= costo[a]
            gastoSector.put(sector, gastoSector.get(sector) - nuevoCosto);
            // gastoTipo[tipo[a]] -= costo[a]
            // ...
            gastoTipo.put(tipo, gastoTipo.get(tipo) - nuevoCosto);


            // --- Decisión 2: NO INCLUIR el activo 'idx' ---

            // Backtrack(idx+1, S, gastoSector, gastoTipo, presupuestoUsado)
            backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado);
        }
    }
}

// <<< BORRÉ EL MÉTODO 'backtrack' VACÍO QUE ESTABA DUPLICADO ACÁ >>>
