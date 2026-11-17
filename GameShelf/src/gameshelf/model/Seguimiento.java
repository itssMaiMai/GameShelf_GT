package gameshelf.model;

/**
 * Datos personales de UN usuario sobre UN juego: estado, horas jugadas, favorito y notas.
 * Cada usuario tiene los suyos y no puede ver los de otros. El estado es opcional: null significa "sin estado"
 * (un juego que el usuario nunca tocó). "Pendiente" es un estado que el usuario elige (juego que quiere jugar).
 */
public class Seguimiento {
    private int usuarioId;
    private int videojuegoId;
    private Estado estado;
    private Double horasJugadas;
    private boolean favorito;
    private String notas = "";

    public Seguimiento copy() {
        Seguimiento s = new Seguimiento();
        s.usuarioId = usuarioId;
        s.videojuegoId = videojuegoId;
        s.estado = estado;
        s.horasJugadas = horasJugadas;
        s.favorito = favorito;
        s.notas = notas;
        return s;
    }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public int getVideojuegoId() { return videojuegoId; }
    public void setVideojuegoId(int videojuegoId) { this.videojuegoId = videojuegoId; }

    /** @return el estado elegido por el usuario, o null si todavía no eligió ninguno. */
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public Double getHorasJugadas() { return horasJugadas; }
    public void setHorasJugadas(Double horasJugadas) { this.horasJugadas = horasJugadas; }

    public boolean isFavorito() { return favorito; }
    public void setFavorito(boolean favorito) { this.favorito = favorito; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas == null ? "" : notas; }
}
