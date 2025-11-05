package clases;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MetodosBackTracking {
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
        this.correlaciones = correlaciones;

        // 2. Inicializar estado de la solución
        this.solucionesUnicas = new HashSet<>();// Equivale a -infinito

        // 3. Fase Cero: Preparación Inteligente (Pre-procesamiento)

        // 3a. Calcular límites y restricciones del Cliente
        this.PRESUPUESTO_MAX = cliente.getMontoMaximo();
        this.RIESGO_MAX = cliente.getPerfil().getRiesgoMaximo();

        // Usamos el retorno del perfil
        this.RETORNO_MIN = cliente.getPerfil().getRetornoMinimo();

        // Calcular límites absolutos de diversificación
        this.max_porSector = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasSector().entrySet()) {
            String claveNormalizada = entry.getKey().toLowerCase();
            max_porSector.put(claveNormalizada, cliente.getMontoMaximo() * entry.getValue() / 100);
        }
        this.max_porTipo = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasTipoActivo().entrySet()) {
            String claveNormalizada = entry.getKey().toLowerCase();
            max_porTipo.put(claveNormalizada, cliente.getMontoMaximo() * entry.getValue() / 100.0);
        }

        // Inicializamos los HashMap de diversificacion
        this.min_porSector = new HashMap<>();
        this.min_porTipo = new HashMap<>();

        // 3c. Filtrar y Ordenar la lista de activos (según tu informe estratégico)
        this.activosElegibles = procesarActivos(todosLosActivos, cliente);

        this.activosOrdenadosPorRetorno = this.activosElegibles.stream()
                .sorted(Comparator.comparingDouble(Activo::getRetornoEsperado).reversed())
                .collect(Collectors.toList());


        // 4. Inicializar mapas para el backtracking
        Map<String, Double> gastoSectorInicial = new HashMap<>();
        Map<String, Double> gastoTipoInicial = new HashMap<>();

        // Inicializamos los mapas de gasto en 0.0
        for (String sector : cliente.getPreferenciasSector().keySet()) {
            gastoSectorInicial.put(sector.toLowerCase(), 0.0);
        }
        for (String tipo : cliente.getPreferenciasTipoActivo().keySet()) {
            gastoTipoInicial.put(tipo.toLowerCase(), 0.0);
        }

        // 5. Iniciar la recursión
        backtrack(0, new ArrayList<Activo>(), gastoSectorInicial, gastoTipoInicial, 0.0);
//6a. Convertir el Set que devuelve el backtracking
        List<Solucion> ranking = new ArrayList<>(this.solucionesUnicas);

// 6b. Ordenar la lista (de mayor a menor retorno, usando compareTo)
        Collections.sort(ranking);

// 6c. Extraer los portafolios del Top 3 (o menos si no hay 3)
        List<Portafolio> portafoliosTop = new ArrayList<>();
        int count = 0;
        for (Solucion s : ranking) {
            if (count >= CANTIDAD_ALTERNATIVAS) {
                break; // Ya tenemos el Top 3
            }
            portafoliosTop.add(s.getPortafolio());
            count++;
        }

        return portafoliosTop; }

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

        // Dos soluciones son "iguales" si tienen el mismo conjunto de activos
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Solucion that = (Solucion) obj;
            // Compara los Sets de nombres de activos
            return this.nombresActivos.equals(that.nombresActivos);
        }


        // Un hashCode basado en los nombres de los activos
        @Override
        public int hashCode() {
            return Objects.hash(nombresActivos);
        }
    }


    public double calcularRiesgoTotal(List<Activo> activosVivos){
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

private double calcularCotaSuperior(int idx, List<Activo> portafolioActual,
                                    double presupuestoUsado, int slotsUsados) {
    int slotsRestantes = this.MAX_ACTIVOS - portafolioActual.size();

    List<Activo> portafolioSimulado = new ArrayList<>(portafolioActual);
    List<Activo> candidatos = this.activosOrdenadosPorRetorno.subList(idx, this.activosOrdenadosPorRetorno.size());
    List<Activo> portafolioConGreedy = completarPorRetorno(candidatos,
            portafolioSimulado,
            slotsRestantes
    );

    double retornoEstimado = calcularRetornoTotal(portafolioConGreedy);
    return retornoEstimado;
}


    public List<Activo> completarPorRetorno(List<Activo> candidatos, List<Activo> portafolioSimulado, int slotsRestantes) {


        for (Activo activo : candidatos) {
            if (slotsRestantes != 0) {
                if (!portafolioSimulado.contains(activo)) {
                    portafolioSimulado.add(activo);
                    slotsRestantes--;
                }
            } else {
                break;
            }
        }
        return portafolioSimulado;
    }



    public List<Activo> procesarActivos(List<Activo> todosLosActivos, Cliente cliente) {


        // --- 1. Banderas (igual que antes) ---
        boolean prefiereOtrosSector = cliente.getPreferenciasSector().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        boolean prefiereOtrosTipo = cliente.getPreferenciasTipoActivo().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));

        // --- 2. Sets en minúscula (igual que antes) ---
        Set<String> sectoresPermitidos = cliente.getPreferenciasSector().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> tiposPermitidos = cliente.getPreferenciasTipoActivo().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

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


        // Filtro 2: ¿Filtramos por Tipo?
        // Solo filtramos si la opción "Otros" NO está presente.
        if (!prefiereOtrosTipo) {
            streamFiltrado = streamFiltrado.filter(a ->
                    tiposPermitidos.contains(a.getTipo().toLowerCase())
            );
        }

        // Filtro 3: El presupuesto (este siempre se aplica)
        streamFiltrado = streamFiltrado.filter(a ->
                a.getMontoMinimo() <= cliente.getMontoMaximo()
        );

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


        if (claveOtrosTipo != null) {
            if (gastoOtrosTipo > this.max_porTipo.get(claveOtrosTipo)) {
                return false;
            }
        } else if (gastoOtrosTipo > 0) {
            return false;
        }
        return true;
    }
    private boolean esDiversificacionFinalValida(Map<String, Double> gastoSector, Map<String, Double> gastoTipo) {
        // 1. Chequear máximos (reutilizamos la función anterior)
        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return false;
        }

        // 2. Chequear MÍNIMOS de gasto por Sector
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

        // 1. PODAS

        // Poda 1: Presupuesto
        if (presupuestoUsado > this.PRESUPUESTO_MAX) {;
            return; // Se pasó del presupuesto

        }

        // Poda 2: Riesgo
        double riesgoActual = calcularRiesgoTotal(portafolioActual);

        if (riesgoActual > this.RIESGO_MAX) {
            return; // Se pasó del riesgo
        }


        // Poda 3: Diversificación (Máximos de dinero)
        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return; // Se pasó del % máximo en un sector o tipo
        }

        // Poda 4: Cotas
        double cotaSuperior = calcularCotaSuperior(idx, portafolioActual, presupuestoUsado, portafolioActual.size());
        double scoreParaVencer = this.RETORNO_MIN; // Solo podemos usar el mínimo, ya que lo que queremos es saber si es un portafolio valido
        if (idx == 0 && portafolioActual.size() == 0) {
            System.out.println("DEBUG PODA 4 INICIAL: Cota Máx (Opt) = " + String.format("%.4f", cotaSuperior) + ". Minimo Requerido = " + String.format("%.4f", scoreParaVencer));
        }

        if (cotaSuperior < scoreParaVencer) {
            return;
        }

        // 2. EVALUAR Y GUARDAR SOLUCIÓN CANDIDATA

        int nroActivos = portafolioActual.size();
        double retornoActual = calcularRetornoTotal(portafolioActual);


        if (nroActivos >= this.MIN_ACTIVOS) {

            if (retornoActual >= this.RETORNO_MIN &&
                    riesgoActual <= this.RIESGO_MAX &&
                    esDiversificacionFinalValida(gastoSector, gastoTipo)) {

                // 1. Creamos el objeto Solucion
                double costoActual = calcularCostoTotal(portafolioActual);
                Solucion nuevaSolucion = new Solucion(
                        new Portafolio(new ArrayList<>(portafolioActual), retornoActual, riesgoActual, costoActual),
                        retornoActual);

                // 2. Añadir al Set.
                this.solucionesUnicas.add(nuevaSolucion);
            }
        }


            // 3. CORTE (CASO BASE)
            if (nroActivos == this.MAX_ACTIVOS || // Se llenó el portafolio (máximo 6)
                    idx == this.activosElegibles.size()) {
                return;
            }

            // 4. RECURSIÓN (ramificacion)
            Activo activoActual = this.activosElegibles.get(idx);
            double nuevoCosto = activoActual.getMontoMinimo();
            String sector = activoActual.getSector().toLowerCase();
            String tipo = activoActual.getTipo().toLowerCase();

            // Decisión 1: INCLUIR el activo (idx)
            portafolioActual.add(activoActual);
            
            //Actualizamos los valores de los sectores
            gastoSector.put(sector, gastoSector.getOrDefault(sector, 0.0) + nuevoCosto);
            gastoTipo.put(tipo, gastoTipo.getOrDefault(tipo, 0.0) + nuevoCosto);

            //Aca empieza la ramificacion
            backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado + nuevoCosto);

            // --- Backtrack (Deshacer la decisión)
            portafolioActual.remove(nroActivos); // Saca el último

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


            // Decisión 2: NO INCLUIR el activo (idx)

            backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado);
        }
    }

