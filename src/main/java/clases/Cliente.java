package clases;
import java.util.Map;

public class Cliente {
    private String nombre;
    private double montoMaximo;
    private int plazoInversion;
    private PerfilRiesgo perfil;
    private  Map<String, Double> preferenciasSector;//ES ESTO O UNA CLASE INTERMEDIA LLAMADA PREFERENCIAS
    private Map<String, Double> preferenciasTipoActivo;

    public Cliente(String nombre, double montoMaximo, int plazoInversion, PerfilRiesgo perfil, Map<String, Double> preferenciasSector, Map<String, Double> preferenciasTipoActivo) {
        this.nombre = nombre;
        this.montoMaximo = montoMaximo;
        this.plazoInversion = plazoInversion;
        this.perfil = perfil;
        this.preferenciasSector = preferenciasSector;
        this.preferenciasTipoActivo = preferenciasTipoActivo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getMontoMaximo() {
        return montoMaximo;
    }

    public void setMontoMaximo(double montoMaximo) {
        this.montoMaximo = montoMaximo;
    }

    public int getPlazoInversion() {
        return plazoInversion;
    }

    public void setPlazoInversion(int plazoInversion) {
        this.plazoInversion = plazoInversion;
    }


    public PerfilRiesgo getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilRiesgo perfil) {
        this.perfil = perfil;
    }

    public Map<String, Double> getPreferenciasSector() {
        return preferenciasSector;
    }

    public void setPreferenciasSector(Map<String, Double> preferenciasSector) {
        this.preferenciasSector = preferenciasSector;
    }

    public Map<String, Double> getPreferenciasTipoActivo() {
        return preferenciasTipoActivo;
    }

    public void setPreferenciasTipoActivo(Map<String, Double> preferenciasTipoActivo) {
        this.preferenciasTipoActivo = preferenciasTipoActivo;
    }


    @Override
    public String toString() {
        return "Cliente{" +
                "nombre='" + nombre + '\'' +
                ", montoMaximo=" + montoMaximo +
                ", plazoInversion=" + plazoInversion +
                ", perfil=" + perfil +
                ", preferenciasSector=" + preferenciasSector +
                ", preferenciasTipoActivo=" + preferenciasTipoActivo +
                '}';
    }
    //Metodos (que tipo de metodos implementaria)
}

