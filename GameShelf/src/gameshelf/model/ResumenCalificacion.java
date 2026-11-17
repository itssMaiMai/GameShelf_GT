package gameshelf.model;

/** Resumen ANÓNIMO de las calificaciones de un juego: solo el promedio y la cantidad de votos. */
public class ResumenCalificacion {
    private final double promedio;
    private final int votos;

    public ResumenCalificacion(double promedio, int votos) {
        this.promedio = promedio;
        this.votos = votos;
    }

    public double getPromedio() { return promedio; }
    public int getVotos() { return votos; }
}
