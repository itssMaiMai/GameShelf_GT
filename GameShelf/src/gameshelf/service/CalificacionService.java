package gameshelf.service;

import gameshelf.model.Calificacion;
import gameshelf.model.EntradaRanking;
import gameshelf.model.ResumenCalificacion;
import gameshelf.model.Usuario;
import gameshelf.model.Videojuego;
import gameshelf.persistence.CalificacionRepository;
import gameshelf.persistence.VideojuegoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calificaciones independientes por usuario. Un usuario solo puede ver y modificar SUS calificaciones: puede
 * calificar un juego y cambiar la nota cuando quiera (queda una sola calificación por usuario y juego).
 */
public class CalificacionService {
    private final VideojuegoRepository juegos;
    private final CalificacionRepository calificaciones;

    public CalificacionService(VideojuegoRepository juegos, CalificacionRepository calificaciones) {
        this.juegos = juegos;
        this.calificaciones = calificaciones;
    }

    /**
     * Califica un juego o cambia la calificación que el usuario ya le había dado.
     * @return true si reemplazó una calificación anterior; false si es la primera vez.
     */
    public boolean calificar(Usuario actor, int videojuegoId, int puntuacion) {
        if (actor == null) {
            throw new ValidacionException("Tenés que iniciar sesión para calificar.");
        }
        if (puntuacion < 1 || puntuacion > 10) {
            throw new ValidacionException("La calificación debe estar entre 1 y 10.");
        }
        if (juegos.buscarPorId(videojuegoId) == null) {
            throw new ValidacionException("El videojuego que intentás calificar ya no existe.");
        }
        return calificaciones.guardar(new Calificacion(actor.getId(), videojuegoId, puntuacion, LocalDateTime.now()));
    }

    /** Las calificaciones del usuario (idJuego -> puntuación). Nunca incluye las de otros usuarios. */
    public Map<Integer, Integer> misPuntuaciones(Usuario actor) {
        if (actor == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(calificaciones.puntuacionesDeUsuario(actor.getId()));
    }

    /**
     * Ranking de TODOS los juegos según el promedio de las calificaciones de todos los usuarios (de mayor a menor;
     * a igual promedio, más votos primero). Los juegos sin calificaciones van al final. Solo se exponen promedios y
     * cantidad de votos: nunca la nota que puso cada usuario.
     */
    public List<EntradaRanking> ranking(Usuario actor) {
        List<EntradaRanking> ranking = new ArrayList<>();
        if (actor == null) {
            return ranking;
        }
        Map<Integer, ResumenCalificacion> resumenes = calificaciones.resumenesPorJuego();
        for (Videojuego v : juegos.obtenerTodos()) {
            ResumenCalificacion r = resumenes.get(v.getId());
            ranking.add(r == null ? new EntradaRanking(v, 0, 0) : new EntradaRanking(v, r.getPromedio(), r.getVotos()));
        }
        ranking.sort(Comparator
                .comparing((EntradaRanking e) -> !e.hayVotos())                                // con votos primero
                .thenComparing((EntradaRanking e) -> -e.getPromedio())
                .thenComparing((EntradaRanking e) -> -e.getVotos())
                .thenComparing((EntradaRanking e) -> e.getJuego().getNombre().toLowerCase(Locale.ROOT))
                .thenComparingInt(e -> e.getJuego().getId()));
        return ranking;
    }

    /**
     * Juegos para elegir en "Calificar": los que el usuario todavía no calificó y, si se indica, también ese juego
     * aunque ya lo haya calificado (para poder cambiarle la nota).
     */
    public List<Videojuego> juegosParaCalificar(Usuario actor, Integer incluirId) {
        List<Videojuego> opciones = pendientesDeCalificar(actor);
        if (actor != null && incluirId != null) {
            boolean ya = false;
            for (Videojuego v : opciones) {
                if (v.getId() == incluirId) ya = true;
            }
            Videojuego elegido = ya ? null : juegos.buscarPorId(incluirId);
            if (elegido != null) {
                opciones.add(elegido);
                opciones.sort(Comparator.comparing((Videojuego v) -> v.getNombre().toLowerCase(Locale.ROOT))
                        .thenComparingInt(Videojuego::getId));
            }
        }
        return opciones;
    }

    /** Juegos que el usuario todavía no calificó. */
    public List<Videojuego> pendientesDeCalificar(Usuario actor) {
        List<Videojuego> pendientes = new ArrayList<>();
        if (actor == null) {
            return pendientes;
        }
        Map<Integer, Integer> propias = calificaciones.puntuacionesDeUsuario(actor.getId());
        for (Videojuego v : juegos.obtenerTodos()) {
            if (!propias.containsKey(v.getId())) {
                pendientes.add(v);
            }
        }
        pendientes.sort(Comparator.comparing((Videojuego v) -> v.getNombre().toLowerCase(Locale.ROOT))
                .thenComparingInt(Videojuego::getId));
        return pendientes;
    }
}
