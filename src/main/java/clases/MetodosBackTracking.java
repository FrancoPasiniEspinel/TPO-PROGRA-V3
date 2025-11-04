package clases;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.sql.DriverManager.println;

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


    private Set<Solucion> solucionesUnicas;
    private List<Activo> activosOrdenadosPorRetorno;

    public List<Portafolio> encontrarPortafolioOptimo(Cliente cliente, List<Activo> todosLosActivos, DatosCorrelaciones correlaciones) {

        // 1. Guardar entradas en variables de instancia
        this.cliente = cliente;
        this.correlaciones = correlaciones;

        // 2. Inicializar estado de la solución
        this.solucionesUnicas = new HashSet<>();// Equivale a -infinito

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
        //System.out.println("DEBUG (INICIAL): Activos elegibles disponibles: " + this.activosElegibles.size());
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
        //System.out.println("DEBUG: Backtracking finalizado. Soluciones únicas encontradas: " + this.solucionesUnicas.size());
//6a. Convertir el Set a una List
        List<Solucion> ranking = new ArrayList<>(this.solucionesUnicas);

// 6b. Ordenar la lista (de mayor a menor retorno, usando compareTo)
        Collections.sort(ranking);

// 6c. Extraer los portafolios del Top 3 (o menos si no hay 3)
        List<Portafolio> portafoliosTop = new ArrayList<>();
        int count = 0;
        for (Solucion s : ranking) {
            /*if (count >= CANTIDAD_ALTERNATIVAS) {
                break; // Ya tenemos el Top 3
            }*/
            portafoliosTop.add(s.getPortafolio());
            //count++;
        }

        return portafoliosTop; }

    // Pon esta clase DENTRO de tu clase principal del optimizador
    private class Solucion implements Comparable<Solucion> {
        Portafolio portafolio;
        double retorno;
        private final Set<String> nombresActivos;

        public Solucion(Portafolio portafolio, double retorno) {
            this.portafolio = portafolio;
            this.retorno = retorno;
            this.nombresActivos = portafolio.getActivosSeleccionados().stream()
                    .map(Activo::getNombre)
                    .collect(Collectors.toSet());
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
        // --- ¡AÑADE EL MÉTODO equals! ---
        // Dos soluciones son "iguales" si tienen el mismo conjunto de activos
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Solucion that = (Solucion) obj;
            // Compara los Sets de nombres de activos
            return this.nombresActivos.equals(that.nombresActivos);
        }

        // --- ¡AÑADE EL MÉTODO hashCode! ---
        // Un hashCode basado en los nombres de los activos
        @Override
        public int hashCode() {
            return Objects.hash(nombresActivos);
        }
    }


    public double calcularRiesgoTotal(List<List<Double>> matrizCorrelacion, List<Activo> activosVivos){
        double riesgoTotal = 0.0;
        double montoTotal = calcularCostoTotal(activosVivos);
        if (montoTotal==0) {
        return 0.0;
        }
        Set<String> activosProcesados = new HashSet<>();
        for (int i = 0; i < activosVivos.size(); i++) {
            double riesgoActivo=0.0;
            String nombreActivoActual = activosVivos.get(i).getNombre();
            if (activosProcesados.add(nombreActivoActual) == false) {
                continue;
            }
            int contadorActivos = cantidadActivos(activosVivos.get(i).getNombre(), activosVivos);
            double participacion = (activosVivos.get(i).getMontoMinimo() * contadorActivos) / montoTotal;
            riesgoActivo = ((activosVivos.get(i).getRiesgo()) * participacion);
            riesgoTotal += riesgoActivo;
            for (int j = i + 1; j < activosVivos.size(); j++) {
                riesgoTotal += ((DatosCorrelaciones.correlacionEntreActivos(activosVivos.get(i).getNombre(), activosVivos.get(j).getNombre())) * (activosVivos.get(i).getRiesgo()) * (activosVivos.get(j).getRiesgo())) / 100;
            }
        }
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
    List<Activo> candidatos = this.activosOrdenadosPorRetorno.subList(idx, this.activosOrdenadosPorRetorno.size());

    // Llama a 'completarPorRetorno' que ahora usará la lista pre-ordenada
    List<Activo> portafolioConGreedy = completarPorRetorno(candidatos,
            portafolioSimulado,
            slotsRestantes
    );

    double retornoEstimado = calcularRetornoTotal(portafolioConGreedy);
    return retornoEstimado;
}

    /**

     Rellena los slots restantes de un portafolio simulado con los mejores
     activos de la lista pre-ordenada 'activosOrdenadosPorRetorno'.
     Esta versión es OPTIMISTA (ignora el presupuesto).*/
    public List<Activo> completarPorRetorno(List<Activo> candidatos, List<Activo> portafolioSimulado, int slotsRestantes) {

        // ¡YA NO ORDENA! Usa el atributo de la clase (this.activosOrdenadosPorRetorno)

        for (Activo activo : candidatos) {
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
        /*System.out.println("--- DEBUG: Iniciando procesarActivos ---");
        System.out.println("Activos iniciales: " + todosLosActivos.size());*/

        // --- 1. Banderas (igual que antes) ---
        boolean prefiereOtrosSector = cliente.getPreferenciasSector().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        boolean prefiereOtrosTipo = cliente.getPreferenciasTipoActivo().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        /*System.out.println("¿Prefiere 'Otros' Sector? -> " + prefiereOtrosSector);
        System.out.println("¿Prefiere 'Otros' Tipo? -> " + prefiereOtrosTipo);*/

        // --- 2. Sets en minúscula (igual que antes) ---
        Set<String> sectoresPermitidos = cliente.getPreferenciasSector().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> tiposPermitidos = cliente.getPreferenciasTipoActivo().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        /*System.out.println("Sectores Permitidos (en minúscula): " + sectoresPermitidos);
        System.out.println("Tipos Permitidos (en minúscula): " + tiposPermitidos);*/

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
        /* --- DEBUG: Ver cuántos quedan después del filtro de sector ---
        List<Activo> postSector = streamFiltrado.collect(Collectors.toList());
        System.out.println("Activos restantes tras filtro Sector: " + postSector.size());
        streamFiltrado = postSector.stream();*/ // Convertir de nuevo a stream

        // Filtro 2: ¿Filtramos por Tipo?
        // Solo filtramos si la opción "Otros" NO está presente.
        if (!prefiereOtrosTipo) {
            streamFiltrado = streamFiltrado.filter(a ->
                    tiposPermitidos.contains(a.getTipo().toLowerCase())
            );
        }

        /* --- DEBUG: Ver cuántos quedan después del filtro de tipo ---
        List<Activo> postTipo = streamFiltrado.collect(Collectors.toList());
        System.out.println("Activos restantes tras filtro Tipo: " + postTipo.size());
        streamFiltrado = postTipo.stream();*/
        // Si prefiereOtrosTipo es 'true', este 'if' se salta.

        // Filtro 3: El presupuesto (este siempre se aplica)
        streamFiltrado = streamFiltrado.filter(a ->
                a.getMontoMinimo() <= cliente.getMontoMaximo()
        );
        /* --- DEBUG: Ver cuántos quedan después del filtro de presupuesto ---
        List<Activo> postPresupuesto = streamFiltrado.collect(Collectors.toList());
        System.out.println("Activos restantes tras filtro Presupuesto: " + postPresupuesto.size());
        System.out.println("(Presupuesto Máx. Cliente: " + cliente.getMontoMaximo() + ")");
        streamFiltrado = postPresupuesto.stream();*/

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
        double gastoOtrosSector = 0.0;

        String claveOtrosSector = null;
        for (String k : this.max_porSector.keySet()) {
            if (k.equalsIgnoreCase("Otros")) {
                claveOtrosSector = k; // En tu caso, encontrará "otros"
                break;
            }
        }

        // --- NUEVO DEBUG ---
        //System.out.println("  [DEBUG PODA 3 - Sector] Clave 'Otros' encontrada: " + claveOtrosSector);
        //System.out.println("  [DEBUG PODA 3 - Sector] Gasto actual: " + gastoSector);
        // --- FIN DEBUG ---

        for (Map.Entry<String, Double> entry : gastoSector.entrySet()) {
            String sector = entry.getKey();
            double gasto = entry.getValue();

            if (this.max_porSector.containsKey(sector)) {
                if (gasto > this.max_porSector.get(sector)) {
                    System.out.println("    -> FALLA: Límite de sector específico " + sector);
                    return false;
                }
            } else if (claveOtrosSector == null || !sector.equalsIgnoreCase(claveOtrosSector)) {
                // (Si claveOtrosSector es null, O si el sector no es la clave "Otros")
                gastoOtrosSector += gasto;
            }
        }

        // --- NUEVO DEBUG ---
        //System.out.println("  [DEBUG PODA 3 - Sector] Gasto 'Otros' acumulado: " + gastoOtrosSector);
        // --- FIN DEBUG ---

        if (claveOtrosSector != null) {
            if (gastoOtrosSector > this.max_porSector.get(claveOtrosSector)) {
                System.out.println("    -> FALLA: Límite de 'Otros' (Sector) excedido.");
                return false;
            }
        } else if (gastoOtrosSector > 0) {
            System.out.println("    -> FALLA: Gasto 'Otros' (Sector) no permitido.");
            return false;
        }

        // --- 2. Revisión de Tipos (con los mismos debugs) ---
        double gastoOtrosTipo = 0.0;

        String claveOtrosTipo = null;
        for (String k : this.max_porTipo.keySet()) {
            if (k.equalsIgnoreCase("Otros")) {
                claveOtrosTipo = k;
                break;
            }
        }

        // --- NUEVO DEBUG ---
        //System.out.println("  [DEBUG PODA 3 - Tipo] Clave 'Otros' encontrada: " + claveOtrosTipo);
        //System.out.println("  [DEBUG PODA 3 - Tipo] Gasto actual: " + gastoTipo);
        // --- FIN DEBUG ---

        for (Map.Entry<String, Double> entry : gastoTipo.entrySet()) {
            String tipo = entry.getKey();
            double gasto = entry.getValue();

            if (this.max_porTipo.containsKey(tipo)) {
                if (gasto > this.max_porTipo.get(tipo)) {
                    System.out.println("    -> FALLA: Límite de tipo específico " + tipo);
                    return false;
                }
            } else if (claveOtrosTipo == null || !tipo.equalsIgnoreCase(claveOtrosTipo)) {
                gastoOtrosTipo += gasto;
            }
        }

        // --- NUEVO DEBUG ---
        //System.out.println("  [DEBUG PODA 3 - Tipo] Gasto 'Otros' acumulado: " + gastoOtrosTipo);
        // --- FIN DEBUG ---

        if (claveOtrosTipo != null) {
            if (gastoOtrosTipo > this.max_porTipo.get(claveOtrosTipo)) {
                //System.out.println("    -> FALLA: Límite de 'Otros' (Tipo) excedido.");
                return false;
            }
        } else if (gastoOtrosTipo > 0) {
            //System.out.println("    -> FALLA: Gasto 'Otros' (Tipo) no permitido.");
            return false;
        }

        // Si pasó todo, imprime esto
        //System.out.println("  [DEBUG PODA 3] -> PASA");
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
            System.out.println("Presupuesto supera lo establecido");
            return; // Se pasó del presupuesto

        }

        // Poda 2: Riesgo
        // si riesgo_total(S) > RIESGO_MAX → retornar
        // Poda 2: Riesgo
// si riesgo_total(S) > RIESGO_MAX → retornarDA ERROR

        double riesgoActual = calcularRiesgoTotal(this.correlaciones.getMatrizCorrelaciones(), portafolioActual);

        if (riesgoActual > this.RIESGO_MAX) {
            //System.out.println("DEBUG: el riesgo acutal es mayor al riesgo maximo");
            return; // Se pasó del riesgo
        }


        // Poda 3: Diversificación (Máximos de dinero)
        // si !cumple_diversificacion_parcial(S, gastoSector, gastoTipo) → retornar
        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            //System.out.println("DEBUG: no comple diversificaion actual");
            return; // Se pasó del % máximo en un sector o tipo
        }

// Poda 4: Cotas (Branch and Bound)
        double cotaSuperior = calcularCotaSuperior(idx, portafolioActual, presupuestoUsado, portafolioActual.size());
        double scoreParaVencer = this.RETORNO_MIN; // ¡Solo podemos usar el mínimo!, ya que lo que queremos es saber si es un portafolio valido
        if (idx == 0 && portafolioActual.size() == 0) {
            System.out.println("DEBUG PODA 4 INICIAL: Cota Máx (Opt) = " + String.format("%.4f", cotaSuperior) + ". Minimo Requerido = " + String.format("%.4f", scoreParaVencer));
        }

        if (cotaSuperior < scoreParaVencer) {
            //System.out.println("DEBUG: Poda 4 (Cota) cortada en índice " + idx + "...");
            return;
        }

// --- 2. EVALUAR Y GUARDAR SOLUCIÓN CANDIDATA ---
// (Esto se ejecuta si la rama NO fue podada)

        int nroActivos = portafolioActual.size();
        double retornoActual = calcularRetornoTotal(portafolioActual);

// si MIN_ACTIVOS ≤ |S| (ya sabemos que es ≤ MAX_ACTIVOS por la poda)
        if (nroActivos >= this.MIN_ACTIVOS) {

            if (retornoActual >= this.RETORNO_MIN &&
                    riesgoActual <= this.RIESGO_MAX &&
                    esDiversificacionFinalValida(gastoSector, gastoTipo)) {

                //System.out.println("DEBUG: ¡SOLUCIÓN VÁLIDA ENCONTRADA! Ret: " + retornoActual + ", Riesgo: " + riesgoActual);

                // 1. Crear el objeto Solucion
                double costoActual = calcularCostoTotal(portafolioActual);
                Solucion nuevaSolucion = new Solucion(
                        // ¡IMPORTANTE! Se crea una COPIA
                        new Portafolio(new ArrayList<>(portafolioActual), retornoActual, riesgoActual, costoActual),
                        retornoActual);

                // 2. Añadir al Set.
                // ¡'equals' y 'hashCode' se encargan de los duplicados!
                this.solucionesUnicas.add(nuevaSolucion);
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
// Lógica de "Undo" más segura
        double gastoActualSector = gastoSector.get(sector);
        if (gastoActualSector - nuevoCosto == 0.0) {
            gastoSector.remove(sector); // Elimina la clave si el gasto es 0
        } else {
            gastoSector.put(sector, gastoActualSector - nuevoCosto);
        }

        double gastoActualTipo = gastoTipo.get(tipo);
        if (gastoActualTipo - nuevoCosto == 0.0) {
            gastoTipo.remove(tipo); // Elimina la clave si el gasto es 0
        } else {
            gastoTipo.put(tipo, gastoActualTipo - nuevoCosto);
        }


            // --- Decisión 2: NO INCLUIR el activo 'idx' ---

            // Backtrack(idx+1, S, gastoSector, gastoTipo, presupuestoUsado)
            backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado);
        }
    }


// <<< BORRÉ EL MÉTODO 'backtrack' VACÍO QUE ESTABA DUPLICADO ACÁ >>>
