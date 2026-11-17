package gameshelf.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Criterios de búsqueda y filtrado. Un valor null (o vacío) significa "sin filtrar por ese criterio".
 * El texto se busca en el nombre y en el desarrollador del juego.
 */
public class Filtro {
    private String texto;
    private String plataforma;
    private final List<String> generos = new ArrayList<>();
    private Estado estado;
    private boolean sinEstado;
    private boolean soloFavoritos;
    private EstadoCalificacion calificacion = EstadoCalificacion.TODOS;

    public String getTexto() { return texto; }
    public Filtro setTexto(String texto) { this.texto = texto; return this; }

    public String getPlataforma() { return plataforma; }
    public Filtro setPlataforma(String plataforma) { this.plataforma = plataforma; return this; }

    /** Géneros marcados: el juego coincide si tiene CUALQUIERA de ellos. Vacío = sin filtrar por género. */
    public List<String> getGeneros() { return generos; }
    public Filtro setGeneros(Collection<String> nuevos) {
        generos.clear();
        if (nuevos != null) {
            generos.addAll(nuevos);
        }
        return this;
    }
    public Filtro setGenero(String genero) {
        return setGeneros(genero == null || genero.trim().isEmpty() ? null : Collections.singletonList(genero));
    }

    public Estado getEstado() { return estado; }
    public Filtro setEstado(Estado estado) { this.estado = estado; return this; }

    /** Si es true, solo los juegos a los que el usuario todavía no les puso ningún estado. */
    public boolean isSinEstado() { return sinEstado; }
    public Filtro setSinEstado(boolean sinEstado) { this.sinEstado = sinEstado; return this; }

    public boolean isSoloFavoritos() { return soloFavoritos; }
    public Filtro setSoloFavoritos(boolean soloFavoritos) { this.soloFavoritos = soloFavoritos; return this; }

    /** Filtra según la calificación del usuario que consulta (nunca la de otros). */
    public EstadoCalificacion getCalificacion() { return calificacion; }
    public Filtro setCalificacion(EstadoCalificacion calificacion) {
        this.calificacion = calificacion == null ? EstadoCalificacion.TODOS : calificacion;
        return this;
    }
}
