package gameshelf.service;

import gameshelf.model.Estado;
import gameshelf.model.Seguimiento;
import gameshelf.model.Usuario;
import gameshelf.persistence.SeguimientoRepository;
import gameshelf.persistence.VideojuegoRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Datos personales de cada usuario sobre cada juego (estado, horas, favorito, notas). Cualquier usuario con
 * sesión puede editar los SUYOS; nadie puede ver ni modificar los de otros.
 */
public class SeguimientoService {
    public static final int MAX_NOTAS = 2000;
    public static final double MAX_HORAS = 100000;

    private final VideojuegoRepository juegos;
    private final SeguimientoRepository seguimientos;

    public SeguimientoService(VideojuegoRepository juegos, SeguimientoRepository seguimientos) {
        this.juegos = juegos;
        this.seguimientos = seguimientos;
    }

    public Seguimiento guardar(Usuario actor, int videojuegoId, Estado estado, Double horas, boolean favorito, String notas) {
        if (actor == null) {
            throw new ValidacionException("Tenés que iniciar sesión para guardar tu seguimiento.");
        }
        if (juegos.buscarPorId(videojuegoId) == null) {
            throw new ValidacionException("El videojuego seleccionado ya no existe.");
        }
        Seguimiento s = new Seguimiento();
        s.setEstado(estado);
        s.setHorasJugadas(horas);
        s.setFavorito(favorito);
        s.setNotas(notas);
        s = normalizarYValidar(s);
        Seguimiento anterior = seguimientos.deUsuario(actor.getId()).get(videojuegoId);
        s.setEstado(estadoSegunHoras(s.getEstado(), s.getHorasJugadas(), anterior));
        s.setUsuarioId(actor.getId());
        s.setVideojuegoId(videojuegoId);
        seguimientos.guardar(s);
        return s;
    }

    public Seguimiento alternarFavorito(Usuario actor, int videojuegoId) {
        if (actor == null) {
            throw new ValidacionException("Tenés que iniciar sesión para marcar favoritos.");
        }
        if (juegos.buscarPorId(videojuegoId) == null) {
            throw new ValidacionException("El videojuego seleccionado ya no existe.");
        }
        Seguimiento actual = seguimientos.deUsuario(actor.getId()).get(videojuegoId);
        Seguimiento s = actual != null ? actual : new Seguimiento();
        s.setUsuarioId(actor.getId());
        s.setVideojuegoId(videojuegoId);
        s.setFavorito(!s.isFavorito());
        seguimientos.guardar(s);
        return s;
    }

    /** Cambia solo el estado del usuario para ese juego (null = sin estado), conservando horas, favorito y notas. */
    public Seguimiento cambiarEstado(Usuario actor, int videojuegoId, Estado estado) {
        if (actor == null) {
            throw new ValidacionException("Tenés que iniciar sesión para cambiar el estado.");
        }
        if (juegos.buscarPorId(videojuegoId) == null) {
            throw new ValidacionException("El videojuego seleccionado ya no existe.");
        }
        Seguimiento actual = seguimientos.deUsuario(actor.getId()).get(videojuegoId);
        Seguimiento s = actual != null ? actual : new Seguimiento();
        s.setUsuarioId(actor.getId());
        s.setVideojuegoId(videojuegoId);
        s.setEstado(estado);
        seguimientos.guardar(s);
        return s;
    }

    /** Seguimiento del usuario (idJuego -> datos). Nunca incluye los de otros usuarios. */
    public Map<Integer, Seguimiento> mios(Usuario actor) {
        if (actor == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(seguimientos.deUsuario(actor.getId()));
    }

    /**
     * Regla: cuando el usuario le pone horas de juego a un juego que está Pendiente o sin estado, pasa automáticamente a
     * Jugando. Solo se aplica cuando las horas se cargan o cambian, así que no se repite al guardar solo las notas ni
     * afecta a los juegos Terminado, Abandonado o ya Jugando.
     *
     * @param pedido el estado que llega para guardar (null = sin estado).
     * @param anterior el seguimiento que el usuario ya tenía de ese juego (null si no tenía).
     */
    static Estado estadoSegunHoras(Estado pedido, Double horas, Seguimiento anterior) {
        boolean pendienteOSinEstado = pedido == null || pedido == Estado.PENDIENTE;
        if (!pendienteOSinEstado || horas == null || horas <= 0) {
            return pedido;
        }
        boolean cambioLasHoras = anterior == null || anterior.getHorasJugadas() == null || !anterior.getHorasJugadas().equals(horas);
        return cambioLasHoras ? Estado.JUGANDO : pedido;
    }

    /** Devuelve una copia con los textos recortados; lanza ValidacionException si los datos no son válidos. */
    static Seguimiento normalizarYValidar(Seguimiento original) {
        Seguimiento s = original.copy();
        s.setNotas(s.getNotas().trim());
        Double horas = s.getHorasJugadas();
        if (horas != null && (horas.isNaN() || horas.isInfinite() || horas < 0 || horas > MAX_HORAS)) {
            throw new ValidacionException("Las horas jugadas deben estar entre 0 y " + (int) MAX_HORAS + ".");
        }
        if (s.getNotas().length() > MAX_NOTAS) {
            throw new ValidacionException("Las notas no pueden superar los " + MAX_NOTAS + " caracteres.");
        }
        return s;
    }
}
