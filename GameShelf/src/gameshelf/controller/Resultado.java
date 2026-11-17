package gameshelf.controller;

/** Resultado de una operación: éxito o error, con un mensaje listo para mostrar y, opcionalmente, datos. */
public class Resultado<T> {
    private final boolean ok;
    private final String mensaje;
    private final T datos;

    private Resultado(boolean ok, String mensaje, T datos) {
        this.ok = ok;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    public static <T> Resultado<T> exito(String mensaje, T datos) {
        return new Resultado<>(true, mensaje, datos);
    }

    public static <T> Resultado<T> error(String mensaje) {
        return new Resultado<>(false, mensaje, null);
    }

    public boolean isOk() { return ok; }
    public String getMensaje() { return mensaje; }
    public T getDatos() { return datos; }
}
