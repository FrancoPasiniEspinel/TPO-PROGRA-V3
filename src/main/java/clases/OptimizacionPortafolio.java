package clases;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

public class OptimizacionPortafolio {

    // --- Variables de Instancia (Estado Global y Entradas) ---

    // 1. Entradas (se inicializan en el metodo lanzador)
    private Cliente cliente;
    private List<Activo> activosElegibles; // La lista pre-filtrada y ordenada
    private DatosCorrelaciones correlaciones;

    // 2. Restricciones (se calculan en el metodo lanzador)
    private double RIESGO_MAX;
    private double RETORNO_MIN;
    private double PRESUPUESTO_MAX;
    private Map<String, Double> max_porSector;
    private Map<String, Double> max_porTipo;

    // NOTA: Tu pseudocódigo menciona minUSD... pero la clase Cliente no los tiene.
    // Si los necesitás para "es_diversificacion_final_valida", hay que agregarlos al Cliente.
    private Map<String, Double> min_porSector;
    private Map<String, Double> min_porTipo;

    private final int MIN_ACTIVOS = 3;
    private final int MAX_ACTIVOS = 6;

    // 3. Estado de la Solución (Global al backtracking)
    private Portafolio mejorSolucion; // Usamos tu clase Portafolio
    private double mejorRetorno;


    // --- MÉTODO LANZADOR (Equivalente a "ResolverPortafolio") ---

    /**
     * Método principal que inicia la optimización del portafolio.
     * Corresponde al procedimiento "ResolverPortafolio" del pseudocódigo.
     * Este es el método que llamás desde el Main.
     */
    public Portafolio encontrarPortafolioOptimo(Cliente cliente, List<Activo> todosLosActivos, DatosCorrelaciones correlaciones) {

        // 1. Guardar entradas en variables de instancia
        this.cliente = cliente;
        this.correlaciones = correlaciones;

        // 2. Inicializar estado de la solución
        this.mejorSolucion = null;
        this.mejorRetorno = -Double.MAX_VALUE; // Equivale a -infinito

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
            max_porSector.put(entry.getKey(), cliente.getMontoMaximo() * entry.getValue());
        }
        this.max_porTipo = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasTipoActivo().entrySet()) {
            max_porTipo.put(entry.getKey(), cliente.getMontoMaximo() * entry.getValue());
        }

        // Inicializo los 'minUSD' vacíos por ahora
        this.min_porSector = new HashMap<>();
        this.min_porTipo = new HashMap<>();

        // 3c. Filtrar y Ordenar la lista de activos (según tu informe estratégico)
        this.activosElegibles = procesarActivos(todosLosActivos, cliente);


        // 4. Inicializar mapas para el backtracking
        // (gastoSector y gastoTipo del pseudocódigo)
        Map<String, Double> gastoSectorInicial = new HashMap<>();
        Map<String, Double> gastoTipoInicial = new HashMap<>();

        // Inicializamos los mapas de gasto en 0.0
        for(String sector : cliente.getPreferenciasSector().keySet()) {
            gastoSectorInicial.put(sector, 0.0);
        }
        for(String tipo : cliente.getPreferenciasTipoActivo().keySet()) {
            gastoTipoInicial.put(tipo, 0.0);
        }

        // 5. Iniciar la recursión
        // S = ∅ (una nueva lista vacía)
        // presupuestoUsado = 0
        backtrack(0, new ArrayList<Activo>(), gastoSectorInicial, gastoTipoInicial, 0.0);

        // 6. Devolver el resultado
        // (El Main se encargará de imprimirlo o mostrar "No hay solución")
        return this.mejorSolucion;
    }
    public  double calcularRiesgoTotal(List<List<Double>> matrizCorrelacion, List<Activo> activosVivos)/*los activos tenemos que pasarlos como unicos y poner la cantidad*/ {
        double riesgoTotal = 0.0;
        double montoTotal= calcularCostoTotal(activosVivos);
        Set<String> activosProcesados= new HashSet<>();
        for (int i = 0; i < activosVivos.size(); i++) {
            String nombreActivoActual= activosVivos.get(i).getNombre();
            if (activosProcesados.add(nombreActivoActual) == false) {
                continue;
            }
            double riesgoActivo=0;
            int contadorActivos= cantidadActivos(activosVivos.get(i).getNombre(), activosVivos);
            double participacion=(activosVivos.get(i).getMontoMinimo()*contadorActivos)/montoTotal;//aca faltaria multiplicar por la cantidad de veces que esta el activo en el portafolio
            riesgoActivo+=activosVivos.get(i).getRiesgo()*participacion;
            for (int j = i+1; j<activosVivos.size(); j++) {
                //metodo correlacion entre activos
                riesgoActivo+=(DatosCorrelaciones.correlacionEntreActivos(activosVivos.get(j).getNombre(),activosVivos.get(i).getNombre()))*(activosVivos.get(j).getRiesgo())*(activosVivos.get(i).getRiesgo());

            }
            riesgoTotal+=riesgoActivo;
        }
        return riesgoTotal;
    }

    /**
     * Método privado para el Pre-procesamiento (Fase Cero)
     * Filtra los activos que no interesan al cliente y los ordena
     * según el perfil de riesgo (como dice el informe estratégico ).
     */

    public List<Activo> procesarActivos(List<Activo> todosLosActivos, Cliente cliente) {

        boolean prefiereOtrosSector = cliente.getPreferenciasSector().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        boolean prefiereOtrosTipo = cliente.getPreferenciasTipoActivo().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));

        List<Activo> filtrados = todosLosActivos.stream()
                .filter(a -> prefiereOtrosSector || cliente.getPreferenciasSector().containsKey(a.getSector()))
                .filter(a -> prefiereOtrosTipo || cliente.getPreferenciasTipoActivo().containsKey(a.getTipo()))
                .filter(a -> a.getMontoMinimo() <= cliente.getMontoMaximo())
                .collect(Collectors.toList());

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



    // --- FUNCIONES AUXILIARES DE CÁLCULO ---

    /**
     * Calcula el costo total de un portafolio.
     * Corresponde a 'func costo_total(S)'
     */
    private static double calcularCostoTotal(List<Activo> portafolio) {//esta bien
        double costo = 0.0;
        for (Activo a : portafolio) {
            costo+= a.getMontoMinimo();
        }

        return costo;
    }

    /**
     * Calcula el retorno total de un portafolio.
     * Corresponde a 'func retorno_total(S)'
     */
    public double calcularRetornoTotal(List<Activo> portafolio) {
        double retorno = 0.0;
        double montoTotal= calcularCostoTotal(portafolio);
        for(Activo a : portafolio) {
            double participacion=a.getMontoMinimo()/montoTotal;
            retorno+=participacion*a.getRetornoEsperado();
        }
        return retorno;
    }

    /**
     * Calcula el riesgo total de un portafolio.
     * Corresponde a 'func riesgo_total(S)'
         * riesgoTotal(S) = Σ riesgo[i] + Σ riesgoPar(i,j)
     */
    // <<< ESTE MÉTODO FALTABA EN TU COPIA >>>





    /**
     * Método helper PRIVADO para buscar la correlación entre dos activos
     * en la matriz de correlaciones.
     */
    private double getCorrelacionEntre(String ticker1, String ticker2) {
        // 1. Encontrar los índices de cada activo
        int index1 = this.correlaciones.getNombresActivos().indexOf(ticker1);
        int index2 = this.correlaciones.getNombresActivos().indexOf(ticker2);

        // 2. Manejar error si no se encuentra
        if (index1 == -1 || index2 == -1) {
            // Si esto pasa, tus datos CSV están inconsistentes
            System.err.println("Error: Activo no encontrado en matriz de correlación: "
                    + (index1 == -1 ? ticker1 : ticker2));
            return 0.0; // Devolvemos 0 para no afectar la suma, pero es un error
        }

        // 3. Buscar el valor en la matriz
        return this.correlaciones.getMatrizCorrelaciones().get(index1).get(index2);
    }//no sirve




    // --- FUNCIONES AUXILIARES DE DIVERSIFICACIÓN ---

    /**
     * Verifica si la diversificación parcial es válida (para podar).
     * Corresponde a 'func cumple_diversificacion_parcial(...)'
     * Chequea que NO se superen los MÁXIMOS de dinero por sector/tipo.
     */
    private boolean cumpleDiversificacionParcial(Map<String, Double> gastoSector, Map<String, Double> gastoTipo) {

        boolean prefiereOtrosSector = this.max_porSector.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        boolean prefiereOtrosTipo = this.max_porTipo.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));

        if (!prefiereOtrosSector) {
            for (Map.Entry<String, Double> entry : gastoSector.entrySet()) {
                String sector = entry.getKey();
                double gasto = entry.getValue();
                if (gasto > this.max_porSector.getOrDefault(sector, Double.MAX_VALUE)) {
                    return false;
                }
            }
        }

        if (!prefiereOtrosTipo) {
            for (Map.Entry<String, Double> entry : gastoTipo.entrySet()) {
                String tipo = entry.getKey();
                double gasto = entry.getValue();
                if (gasto > this.max_porTipo.getOrDefault(tipo, Double.MAX_VALUE)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verifica si la diversificación final es válida (para una solución).
     * Corresponde a 'func es_diversificacion_final_valida(...)'
     * Chequea MÁXIMOS (re-usando la parcial) y MÍNIMOS de dinero.
     */
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

    // --- FUNCIÓN AUXILIAR DE PODA (COTAS) ---

    /**
     * Calcula la Cota Superior de retorno (el "retorno soñado").
     * Corresponde a 'func cota_superior_retorno(...)'
     */






    // --- MÉTODO BACKTRACK (COMPLETO) ---

    /**
     * Esta es la función recursiva principal.
     * Corresponde al procedimiento "Backtrack" del pseudocódigo.
     */
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
        ;

        if (riesgoActual > this.RIESGO_MAX) {
            return; // Se pasó del riesgo
        }


        // Poda 3: Diversificación (Máximos de dinero)
        // si !cumple_diversificacion_parcial(S, gastoSector, gastoTipo) → retornar
        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return; // Se pasó del % máximo en un sector o tipo
        }

        // Poda 4: Cotas (Branch and Bound)
        // ub = cota_superior_retorno(idx, S, presupuestoUsado, |S|)
        // si ub < max(mejorRetorno, RETORNO_MIN) → retornar
        double cotaSuperior = calcularCotaSuperior(idx, portafolioActual, presupuestoUsado, portafolioActual.size());
        double cotaInferior = Math.max(this.mejorRetorno, RETORNO_MIN);

        if (cotaSuperior < cotaInferior) {
            return; // Esta rama nunca superará el récord actual ni el mínimo del cliente
        }


        // --- 2. EVALUAR SOLUCIÓN CANDIDATA ---
        // (Como dice tu informe, evaluamos "a mitad de ramificación" )

        int nroActivos = portafolioActual.size();

        // si MIN_ACTIVOS ≤ |S| ≤ MAX_ACTIVOS:
        if (nroActivos >= this.MIN_ACTIVOS) {
            // (Ya sabemos que es <= MAX_ACTIVOS por la poda de corte de abajo)

            // rActual = retorno_total(S)
            double rActual = calcularRetornoTotal(portafolioActual);

            // si rActual ≥ RETORNO_MIN y riesgo_total(S) ≤ RIESGO_MAX у
            // es_diversificacion_final_valida(S, gastoSector, gastoTipo):
            if (rActual >= this.RETORNO_MIN &&
                    riesgoActual <= this.RIESGO_MAX && // Re-usamos el riesgo ya calculado
                    esDiversificacionFinalValida(gastoSector, gastoTipo)) {

                // si rActual > mejorRetorno:
                if (rActual > this.mejorRetorno) {
                    // ¡NUEVO RÉCORD!
                    this.mejorRetorno = rActual;

                    // mejorSolucion = copia(S)
                    double costoActual = calcularCostoTotal(portafolioActual);
                    this.mejorSolucion = new Portafolio(
                            new ArrayList<>(portafolioActual), // ¡IMPORTANTE! Se crea una COPIA
                            rActual,
                            riesgoActual,
                            costoActual
                    );
                }
                // NOTA: Tu pseudocódigo no implementa el desempate por correlación,w
                // pero si quisieras, iría en un 'else if (rActual == this.mejorRetorno)'.
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

        // gastoSector[sector[a]] += costo[a]
        gastoSector.put(sector, gastoSector.get(sector) + nuevoCosto);
        // gastoTipo[tipo[a]] += costo[a]
        gastoTipo.put(tipo, gastoTipo.get(tipo) + nuevoCosto);

        // Backtrack(idx+1, S, gastoSector, gastoTipo, presupuestoUsado + costo[a])
        backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado + nuevoCosto);

        // --- Backtrack (Deshacer la decisión) ---

        // S.pop()
        portafolioActual.remove(nroActivos); // Saca el último

        // gastoSector[sector[a]] -= costo[a]
        gastoSector.put(sector, gastoSector.get(sector) - nuevoCosto);
        // gastoTipo[tipo[a]] -= costo[a]
        gastoTipo.put(tipo, gastoTipo.get(tipo) - nuevoCosto);


        // --- Decisión 2: NO INCLUIR el activo 'idx' ---

        // Backtrack(idx+1, S, gastoSector, gastoTipo, presupuestoUsado)
        backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado);
    }

    // <<< BORRÉ EL MÉTODO 'backtrack' VACÍO QUE ESTABA DUPLICADO ACÁ >>>
}