package gameshelf.model;

/** Resumen general de la colección completa. */
public class Estadisticas {
    private final int total;
    private final int pendientes;
    private final int jugando;
    private final int terminados;
    private final int abandonados;
    private final int sinEstado;
    private final int favoritos;
    private final double horasTotales;
    private final int calificados;
    private final double promedioPuntuacion;

    public Estadisticas(int total, int pendientes, int jugando, int terminados, int abandonados, int sinEstado,
                        int favoritos, double horasTotales, int calificados, double promedioPuntuacion) {
        this.total = total;
        this.pendientes = pendientes;
        this.jugando = jugando;
        this.terminados = terminados;
        this.abandonados = abandonados;
        this.sinEstado = sinEstado;
        this.favoritos = favoritos;
        this.horasTotales = horasTotales;
        this.calificados = calificados;
        this.promedioPuntuacion = promedioPuntuacion;
    }

    public int getTotal() { return total; }
    public int getPendientes() { return pendientes; }
    public int getJugando() { return jugando; }
    public int getTerminados() { return terminados; }
    public int getAbandonados() { return abandonados; }
    /** Juegos a los que el usuario todavía no les puso estado. */
    public int getSinEstado() { return sinEstado; }
    public int getFavoritos() { return favoritos; }
    public double getHorasTotales() { return horasTotales; }
    public int getCalificados() { return calificados; }
    /** Promedio de las puntuaciones de los juegos calificados (0 si no hay ninguno). */
    public double getPromedioPuntuacion() { return promedioPuntuacion; }
}
