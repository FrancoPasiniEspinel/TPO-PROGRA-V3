package clases;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private Map<String, Double> maxUSD_porSector;
    private Map<String, Double> maxUSD_porTipo;

    // NOTA: Tu pseudocódigo menciona minUSD... pero la clase Cliente no los tiene.
    // Si los necesitás para "es_diversificacion_final_valida", hay que agregarlos al Cliente.
    private Map<String, Double> minUSD_porSector;
    private Map<String, Double> minUSD_porTipo;

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

        // Usamos el retorno más alto entre el deseado y el del perfil
        //this.RETORNO_MIN = Math.max(cliente.getRetornoMinimoDeseado(), cliente.getPerfil().getRetornoMinimo()); comentado

        // 3b. Calcular límites absolutos de diversificación (en dólares)
        this.maxUSD_porSector = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasSector().entrySet()) {
            // entry.getValue() es el porcentaje (ej: 0.30)
            maxUSD_porSector.put(entry.getKey(), cliente.getMontoMaximo() * entry.getValue());
        }
        this.maxUSD_porTipo = new HashMap<>();
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasTipoActivo().entrySet()) {
            maxUSD_porTipo.put(entry.getKey(), cliente.getMontoMaximo() * entry.getValue());
        }

        // Inicializo los 'minUSD' vacíos por ahora
        this.minUSD_porSector = new HashMap<>();
        this.minUSD_porTipo = new HashMap<>();

        // 3c. Filtrar y Ordenar la lista de activos (según tu informe estratégico)
        this.activosElegibles = preprocesarActivos(todosLosActivos, cliente);


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

    /**
     * Método privado para el Pre-procesamiento (Fase Cero)
     * Filtra los activos que no interesan al cliente y los ordena
     * según el perfil de riesgo (como dice el informe estratégico ).
     */
    private List<Activo> preprocesarActivos(List<Activo> todosLosActivos, Cliente cliente) {

        // 1. Filtrado
        List<Activo> filtrados = todosLosActivos.stream()
                // Solo activos de sectores que el cliente quiere
                .filter(a -> cliente.getPreferenciasSector().containsKey(a.getSector()))
                // Solo activos de tipos que el cliente quiere
                .filter(a -> cliente.getPreferenciasTipoActivo().containsKey(a.getTipo()))
                // Filtra activos que por sí solos ya superan el presupuesto
                .filter(a -> a.getMontoMinimo() <= cliente.getMontoMaximo())
                .collect(Collectors.toList());

        // 2. Ordenamiento Estratégico (según tu informe)
        PerfilRiesgo perfil = cliente.getPerfil();

        if (perfil == PerfilRiesgo.AGRESIVO || perfil == PerfilRiesgo.MODERADAMENTE_AGRESIVO) {
            // Ordenar por retorno (mayor a menor)
            filtrados.sort(Comparator.comparingDouble(Activo::getRetornoEsperado).reversed());
        }
        else if (perfil == PerfilRiesgo.CONSERVADOR || perfil == PerfilRiesgo.MODERADAMENTE_CONSERVADOR) {
            // Ordenar por riesgo (menor a mayor)
            filtrados.sort(Comparator.comparingDouble(Activo::getRiesgo));
        }
        else { // MODERADO
            // Ordenar por ratio Retorno/Riesgo (mayor a menor)
            filtrados.sort(Comparator.comparingDouble((Activo a) -> {
                if (a.getRiesgo() == 0) return 0.0;
                return a.getRetornoEsperado() / a.getRiesgo();
            }).reversed());
        }

        return filtrados;
    }


    // --- FUNCIONES AUXILIARES DE CÁLCULO ---

    /**
     * Calcula el costo total de un portafolio.
     * Corresponde a 'func costo_total(S)'
     */
    private double calcularCostoTotal(List<Activo> portafolio) {
        double costo = 0.0;
        for (Activo a : portafolio) {
            costo += a.getMontoMinimo(); // Asumo que el costo es el monto mínimo
        }
        return costo;
    }

    /**
     * Calcula el retorno total de un portafolio.
     * Corresponde a 'func retorno_total(S)'
     */
    private double calcularRetornoTotal(List<Activo> portafolio) {
        double retorno = 0.0;
        for (Activo a : portafolio) {
            retorno += a.getRetornoEsperado();
        }
        return retorno;
    }

    /**
     * Calcula el riesgo total de un portafolio.
     * Corresponde a 'func riesgo_total(S)'
     * riesgoTotal(S) = Σ riesgo[i] + Σ riesgoPar(i,j)
     */
    // <<< ESTE MÉTODO FALTABA EN TU COPIA >>>
    private double calcularRiesgoTotal(List<Activo> portafolio) {
        double riesgo = 0.0;

        // 1. Sumar los riesgos individuales (Σ riesgo[i])
        for (Activo a : portafolio) {
            riesgo += a.getRiesgo();
        }

        // 2. Sumar el riesgo de pares (Σ riesgoPar(i,j))
        // Iteramos sobre cada par único (i < j)
        for (int i = 0; i < portafolio.size(); i++) {
            for (int j = i + 1; j < portafolio.size(); j++) {
                Activo a1 = portafolio.get(i);
                Activo a2 = portafolio.get(j);
                // Usamos un helper para buscar el valor en la matriz
                riesgo += getCorrelacionEntre(a1.getNombre(), a2.getNombre());
            }
        }
        return riesgo;
    }


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
    }


    // --- FUNCIONES AUXILIARES DE DIVERSIFICACIÓN ---

    /**
     * Verifica si la diversificación parcial es válida (para podar).
     * Corresponde a 'func cumple_diversificacion_parcial(...)'
     * Chequea que NO se superen los MÁXIMOS de dinero por sector/tipo.
     */
    private boolean cumpleDiversificacionParcial(Map<String, Double> gastoSector, Map<String, Double> gastoTipo) {
        // 1. Chequear gasto por Sector
        for (Map.Entry<String, Double> entry : gastoSector.entrySet()) {
            String sector = entry.getKey();
            double gasto = entry.getValue();
            if (gasto > this.maxUSD_porSector.getOrDefault(sector, Double.MAX_VALUE)) {
                return false; // Se pasó del máximo permitido
            }
        }

        // 2. Chequear gasto por Tipo
        for (Map.Entry<String, Double> entry : gastoTipo.entrySet()) {
            String tipo = entry.getKey();
            double gasto = entry.getValue();
            if (gasto > this.maxUSD_porTipo.getOrDefault(tipo, Double.MAX_VALUE)) {
                return false; // Se pasó del máximo permitido
            }
        }

        return true; // Pasó todas las validaciones de máximos
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
        for (Map.Entry<String, Double> entry : this.minUSD_porSector.entrySet()) {
            String sector = entry.getKey();
            double minimoRequerido = entry.getValue();
            if (gastoSector.getOrDefault(sector, 0.0) < minimoRequerido) {
                return false; // No se alcanzó el mínimo requerido
            }
        }

        // 3. Chequear MÍNIMOS de gasto por Tipo
        for (Map.Entry<String, Double> entry : this.minUSD_porTipo.entrySet()) {
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
    private double calcularCotaSuperior(int idx, List<Activo> portafolioActual,
                                        double presupuestoUsado, int slotsUsados) {

        // r = retorno_total(S)
        double retornoEstimado = calcularRetornoTotal(portafolioActual);

        // slotsRest = MAX_ACTIVOS - slotsUsados
        int slotsRestantes = this.MAX_ACTIVOS - slotsUsados;

        // presRest = PRESUPUESTO_MAX - presupuestoUsado
        double presupuestoRestante = this.PRESUPUESTO_MAX - presupuestoUsado;

        // candidatos = activos con índice >= idx no usados
        // ¡VENTAJA! Ya tenemos la lista 'activosElegibles' ordenada por retorno
        // (si el perfil es Agresivo) o por ratio (si es Moderado), que es
        // lo que pide el pseudocódigo.

        // para cada a en candidatos:
        for (int i = idx; i < this.activosElegibles.size(); i++) {
            if (slotsRestantes == 0) {
                break; // No podemos agregar más activos
            }

            Activo candidato = this.activosElegibles.get(i);

            // si costo[a] <= presRest: (Estimación optimista)
            // Tu pseudocódigo lo hace simple, lo seguimos.
            if (candidato.getMontoMinimo() <= presupuestoRestante) {
                retornoEstimado += candidato.getRetornoEsperado();
                presupuestoRestante -= candidato.getMontoMinimo();
                slotsRestantes--;
            }
        }

        return retornoEstimado;
    }


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
        double riesgoActual = calcularRiesgoTotal(portafolioActual);
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
        double cotaInferior = Math.max(this.mejorRetorno, this.RETORNO_MIN);

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
                // NOTA: Tu pseudocódigo no implementa el desempate por correlación,
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