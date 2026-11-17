package gameshelf.model;

import java.time.LocalDateTime;

/** Calificación (1 a 10) que UN usuario le dio a UN juego. Es privada de ese usuario. */
public class Calificacion {
    private final int usuarioId;
    private final int videojuegoId;
    private final int puntuacion;
    private final LocalDateTime fecha;

    public Calificacion(int usuarioId, int videojuegoId, int puntuacion, LocalDateTime fecha) {
        this.usuarioId = usuarioId;
        this.videojuegoId = videojuegoId;
        this.puntuacion = puntuacion;
        this.fecha = fecha;
    }

    public int getUsuarioId() { return usuarioId; }
    public int getVideojuegoId() { return videojuegoId; }
    public int getPuntuacion() { return puntuacion; }
    public LocalDateTime getFecha() { return fecha; }
}
