package gameshelf.model;

/** Un juego dentro del ranking, con el promedio de calificaciones de todos los usuarios (votos == 0: sin calificaciones). */
public class EntradaRanking {
    private final Videojuego juego;
    private final double promedio;
    private final int votos;

    public EntradaRanking(Videojuego juego, double promedio, int votos) {
        this.juego = juego;
        this.promedio = promedio;
        this.votos = votos;
    }

    public Videojuego getJuego() { return juego; }
    /** Promedio de todos los usuarios; solo tiene sentido si hayVotos(). */
    public double getPromedio() { return promedio; }
    public int getVotos() { return votos; }
    public boolean hayVotos() { return votos > 0; }
}
