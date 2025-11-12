package clases;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetodosBackTracking {
    private List<Activo> activosElegibles;
    private DatosCorrelaciones correlaciones;
    private double RIESGO_MAX;
    private double RETORNO_MIN;
    private double PRESUPUESTO_MAX;
    private Map<String, Double> max_porSector;
    private Map<String, Double> max_porTipo;
    private Map<String, Double> preferenciasSectorCliente;
    private Map<String, Double> preferenciasTipoActivoCliente;
    private final double MARGEN_DIVERSIFICACION_PORCENTUAL = 5.0;
    private boolean prefiereOtrosTipoCliente;
    private boolean prefiereOtrosSectorCliente;
    private final int MIN_ACTIVOS = 3;
    private final int MAX_ACTIVOS = 6;
    private final int CANTIDAD_ALTERNATIVAS = 3;
    private Set<Solucion> solucionesUnicas;
    private List<Activo> activosOrdenadosPorRetorno;


    public List<Portafolio> encontrarPortafolioOptimo(Cliente cliente, List<Activo> todosLosActivos, DatosCorrelaciones correlaciones) {

        this.correlaciones = correlaciones;
        this.solucionesUnicas = new HashSet<>();
        this.PRESUPUESTO_MAX = cliente.getMontoMaximo();
        this.RIESGO_MAX = cliente.getPerfil().getRiesgoMaximo();
        this.RETORNO_MIN = cliente.getPerfil().getRetornoMinimo();
        this.max_porSector = new HashMap<>();
        this.max_porTipo = new HashMap<>();

        for (Map.Entry<String, Double> entry : cliente.getPreferenciasSector().entrySet()) {
            String claveNormalizada = entry.getKey().toLowerCase();
            max_porSector.put(claveNormalizada, cliente.getMontoMaximo() * entry.getValue() / 100);
        }
        for (Map.Entry<String, Double> entry : cliente.getPreferenciasTipoActivo().entrySet()) {
            String claveNormalizada = entry.getKey().toLowerCase();
            max_porTipo.put(claveNormalizada, cliente.getMontoMaximo() * entry.getValue() / 100.0);
        }

        this.preferenciasSectorCliente = cliente.getPreferenciasSector();
        this.preferenciasTipoActivoCliente = cliente.getPreferenciasTipoActivo();
        this.prefiereOtrosSectorCliente = this.preferenciasSectorCliente.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));

        this.prefiereOtrosTipoCliente = this.preferenciasTipoActivoCliente.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        this.activosElegibles = procesarActivos(todosLosActivos, cliente);

        this.activosOrdenadosPorRetorno = this.activosElegibles.stream()
                .sorted(Comparator.comparingDouble(Activo::getRetornoEsperado).reversed())
                .collect(Collectors.toList());


        Map<String, Double> gastoSectorInicial = new HashMap<>();
        Map<String, Double> gastoTipoInicial = new HashMap<>();


        for (String sector : cliente.getPreferenciasSector().keySet()) {
            gastoSectorInicial.put(sector.toLowerCase(), 0.0);
        }
        for (String tipo : cliente.getPreferenciasTipoActivo().keySet()) {
            gastoTipoInicial.put(tipo.toLowerCase(), 0.0);
        }


        //Inicializamos el arbol
        backtrack(0, new ArrayList<Activo>(), gastoSectorInicial, gastoTipoInicial, 0.0);


        List<Solucion> ranking = new ArrayList<>(this.solucionesUnicas);

        Collections.sort(ranking);


        List<Portafolio> portafoliosTop = new ArrayList<>();
        int count = 0;
        for (Solucion s : ranking) {
            if (count >= CANTIDAD_ALTERNATIVAS) {
                break; // Ya tenemos el Top 3
            }
            portafoliosTop.add(s.getPortafolio());
            count++;
        }
        return portafoliosTop;
    }

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


        @Override
        public int hashCode() {
            return Objects.hash(nombresActivos);
        }
    }

    public double calcularRiesgoTotal(List<Activo> activosVivos) {
        double riesgoTotal = 0.0;
        double montoTotal = calcularCostoTotal(activosVivos);
        if (montoTotal == 0) {
            return 0.0;
        }
        Set<String> activosProcesados = new HashSet<>();
        for (int i = 0; i < activosVivos.size(); i++) {
            double riesgoActivo = 0.0;
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
        if (portafolio.isEmpty()) {
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

    private double calcularCostoTotal(List<Activo> portafolio) {
        double costo = 0.0;
        for (Activo a : portafolio) {
            costo += a.getMontoMinimo();
        }
        return costo;
    }

    private double calcularCotaSuperior(int idx, List<Activo> portafolioActual,
                                        double presupuestoUsado) {
        double costoPortafolioActual = calcularCostoTotal(portafolioActual);
        double retornoActual = calcularRetornoTotal(portafolioActual);
        List<Activo> candidatos = this.activosOrdenadosPorRetorno.subList(idx, this.activosElegibles.size());
        double presupuestoRestante = this.PRESUPUESTO_MAX - presupuestoUsado;
        double retornoEstimado = completarPorRetorno(candidatos, costoPortafolioActual, retornoActual, presupuestoRestante);
        return retornoEstimado;
    }

    public double completarPorRetorno(List<Activo> candidatos, double costoPortafolioActual, double retornoActual, double presupuestoRestante) {
        if (candidatos.isEmpty()) {
            return (costoPortafolioActual == 0) ? 0.0 : retornoActual;
        }

        Activo mejorCandidato = candidatos.get(0);
        if (mejorCandidato.getMontoMinimo() <= 0) {

            return (costoPortafolioActual == 0) ? 0.0 : retornoActual;
        }

        double cantidadActivo = (presupuestoRestante / mejorCandidato.getMontoMinimo());
        double costoMejorActivo = mejorCandidato.getMontoMinimo() * cantidadActivo;

        double costoPortafolioSimulado = costoPortafolioActual + costoMejorActivo;
        if (costoPortafolioSimulado == 0) return 0.0;


        double retornoAbsolutoActual = retornoActual * costoPortafolioActual;

        double retornoActivoAdicional = costoMejorActivo * mejorCandidato.getRetornoEsperado();
        double retornoFinal = (retornoAbsolutoActual + retornoActivoAdicional) / costoPortafolioSimulado;

        return retornoFinal;
    }

    public List<Activo> procesarActivos(List<Activo> todosLosActivos, Cliente cliente) {
        boolean prefiereOtrosSector = cliente.getPreferenciasSector().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));
        boolean prefiereOtrosTipo = cliente.getPreferenciasTipoActivo().keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase("Otros"));


        Set<String> sectoresPermitidos = cliente.getPreferenciasSector().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> tiposPermitidos = cliente.getPreferenciasTipoActivo().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());


        Stream<Activo> streamFiltrado = todosLosActivos.stream();


        if (!prefiereOtrosSector) {
            streamFiltrado = streamFiltrado.filter(a ->
                    sectoresPermitidos.contains(a.getSector().toLowerCase())
            );
        }


        if (!prefiereOtrosTipo) {
            streamFiltrado = streamFiltrado.filter(a ->
                    tiposPermitidos.contains(a.getTipo().toLowerCase())
            );
        }


        streamFiltrado = streamFiltrado.filter(a ->
                a.getMontoMinimo() <= cliente.getMontoMaximo() && a.getMontoMinimo() > 0
        );


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

        double tol = this.MARGEN_DIVERSIFICACION_PORCENTUAL;
        double gastoOtrosSector = 0.0;
        double gastoOtrosTipo = 0.0;
        double margenError = 0.01;


        for (Map.Entry<String, Double> entry : gastoSector.entrySet()) {
            String sector = entry.getKey();
            double gastoActual = entry.getValue();


            if (this.preferenciasSectorCliente.containsKey(sector)) {
                double obj = this.preferenciasSectorCliente.get(sector); // ej. 20.0
                double maxMonto = ((obj + tol) / 100.0) * this.PRESUPUESTO_MAX; // ej. (25/100) * 150000

                if (gastoActual > maxMonto + margenError) {
                    return false;
                }
            } else {

                gastoOtrosSector += gastoActual;
            }
        }


        if (this.preferenciasSectorCliente.containsKey("otros")) {
            double obj = this.preferenciasSectorCliente.get("otros");
            double maxMontoOtros = ((obj + tol) / 100.0) * this.PRESUPUESTO_MAX;
            if (gastoOtrosSector > maxMontoOtros + margenError) {
                return false;
            }
        }


        for (Map.Entry<String, Double> entry : gastoTipo.entrySet()) {
            String tipo = entry.getKey();
            double gastoActual = entry.getValue();

            if (this.preferenciasTipoActivoCliente.containsKey(tipo)) {
                double obj = this.preferenciasTipoActivoCliente.get(tipo);
                double maxMonto = ((obj + tol) / 100.0) * this.PRESUPUESTO_MAX;
                if (gastoActual > maxMonto + margenError) {
                    return false;
                }
            } else {
                gastoOtrosTipo += gastoActual;
            }
        }

        if (this.preferenciasTipoActivoCliente.containsKey("otros")) {
            double obj = this.preferenciasTipoActivoCliente.get("otros");
            double maxMontoOtros = ((obj + tol) / 100.0) * this.PRESUPUESTO_MAX;
            if (gastoOtrosTipo > maxMontoOtros + margenError) {
                return false;
            }
        }

        return true;
    }


    private boolean esDiversificacionFinalValida(Map<String, Double> gastoSector, Map<String, Double> gastoTipo, double costoTotal) {


        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return false;
        }

        if (costoTotal == 0) return true;

        double tol = this.MARGEN_DIVERSIFICACION_PORCENTUAL;
        double margenError = 0.0001;


        double gastoTotalOtrosSector = 0.0;
        for (Map.Entry<String, Double> entry : gastoSector.entrySet()) {
            if (!this.preferenciasSectorCliente.containsKey(entry.getKey())) {
                gastoTotalOtrosSector += entry.getValue();
            }
        }
        double pctRealOtrosSector = (gastoTotalOtrosSector / costoTotal) * 100.0;


        for (Map.Entry<String, Double> pref : this.preferenciasSectorCliente.entrySet()) {
            String sector = pref.getKey().toLowerCase();
            double obj = pref.getValue();

            double val;
            if (sector.equals("otros")) {
                val = pctRealOtrosSector;
            } else {
                double gastoRealEnSector = gastoSector.getOrDefault(sector, 0.0);
                val = (gastoRealEnSector / costoTotal) * 100.0;
            }

            if (val < obj - tol - margenError || val > obj + tol + margenError) {
                return false;
            }
        }


        double gastoTotalOtrosTipo = 0.0;
        for (Map.Entry<String, Double> entry : gastoTipo.entrySet()) {
            if (!this.preferenciasTipoActivoCliente.containsKey(entry.getKey())) {
                gastoTotalOtrosTipo += entry.getValue();
            }
        }
        double pctRealOtrosTipo = (gastoTotalOtrosTipo / costoTotal) * 100.0;

        for (Map.Entry<String, Double> pref : this.preferenciasTipoActivoCliente.entrySet()) {
            String tipo = pref.getKey().toLowerCase();
            double obj = pref.getValue();

            double val;
            if (tipo.equals("otros")) {
                val = pctRealOtrosTipo;
            } else {
                double gastoRealEnTipo = gastoTipo.getOrDefault(tipo, 0.0);
                val = (gastoRealEnTipo / costoTotal) * 100.0;
            }

            if (val < obj - tol - margenError || val > obj + tol + margenError) {
                return false;
            }
        }

        return true; // Pasó todas las validaciones
    }
    //-------------------------------------------------BACKTRACKING----------------------------------------------------------//

    private void backtrack(int idx, List<Activo> portafolioActual,
                           Map<String, Double> gastoSector, Map<String, Double> gastoTipo,
                           double presupuestoUsado) {

        // 1. PODAS

        // Poda 1: Presupuesto
        if (presupuestoUsado > this.PRESUPUESTO_MAX) {
            return; // Se pasó del presupuesto
        }

        // Poda 2: Diversificacion

        if (!cumpleDiversificacionParcial(gastoSector, gastoTipo)) {
            return;
        }

        // Poda 3: Cotas
        double cotaSuperior = calcularCotaSuperior(idx, portafolioActual, presupuestoUsado);
        double scoreParaVencer = this.RETORNO_MIN;

        if (cotaSuperior < scoreParaVencer) {
            return;
        }


        // 2. EVALUAR Y GUARDAR SOLUCIÓN CANDIDATA

        int nroActivos = portafolioActual.size();
        double retornoActual = calcularRetornoTotal(portafolioActual);
        double costoActual = calcularCostoTotal(portafolioActual);
        double riesgoActual = calcularRiesgoTotal(portafolioActual);
        if (nroActivos >= this.MIN_ACTIVOS) {
            if (retornoActual >= this.RETORNO_MIN &&
                    riesgoActual <= this.RIESGO_MAX &&
                    esDiversificacionFinalValida(gastoSector, gastoTipo, costoActual)) {

                // 1. Creamos el objeto Solucion

                Solucion nuevaSolucion = new Solucion(
                        new Portafolio(new ArrayList<>(portafolioActual), retornoActual, riesgoActual, costoActual),
                        retornoActual);

                // 2. Añadimos al Set.
                this.solucionesUnicas.add(nuevaSolucion);
            }
        }

        // 3. CORTE (CASO BASE)

        if (nroActivos == this.MAX_ACTIVOS ||
                idx == this.activosElegibles.size()) {
            return;
        }

        // 4. RECURSIÓN (ramificacion)

        // Decisión 1: NO INCLUIR el activo (idx)
        backtrack(idx + 1, portafolioActual, gastoSector, gastoTipo, presupuestoUsado);

        Activo activoActual = this.activosElegibles.get(idx);
        double nuevoCosto = activoActual.getMontoMinimo();

        if (portafolioActual.size() < this.MAX_ACTIVOS &&
                presupuestoUsado + nuevoCosto <= this.PRESUPUESTO_MAX) {

            String sector = activoActual.getSector().toLowerCase();
            String tipo = activoActual.getTipo().toLowerCase();

            portafolioActual.add(activoActual);
            gastoSector.put(sector, gastoSector.getOrDefault(sector, 0.0) + nuevoCosto);
            gastoTipo.put(tipo, gastoTipo.getOrDefault(tipo, 0.0) + nuevoCosto);

            // Decisión 2: INCLUIR el activo (idx)
            backtrack(idx, portafolioActual, gastoSector, gastoTipo, presupuestoUsado + nuevoCosto);

            portafolioActual.remove(nroActivos);

            double gastoActualSector = gastoSector.get(sector);
            if (gastoActualSector - nuevoCosto == 0.0) {
                gastoSector.remove(sector);
            } else {
                gastoSector.put(sector, gastoActualSector - nuevoCosto);
            }

            double gastoActualTipo = gastoTipo.get(tipo);
            if (gastoActualTipo - nuevoCosto == 0.0) {
                gastoTipo.remove(tipo);
            } else {
                gastoTipo.put(tipo, gastoActualTipo - nuevoCosto);
            }
        }
    }
}








