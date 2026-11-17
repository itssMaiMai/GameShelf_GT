package gameshelf.controller;

import gameshelf.model.EntradaRanking;
import gameshelf.model.Estadisticas;
import gameshelf.model.Estado;
import gameshelf.model.Filtro;
import gameshelf.model.Seguimiento;
import gameshelf.model.Usuario;
import gameshelf.model.Videojuego;
import gameshelf.persistence.RepositorioException;
import gameshelf.service.CalificacionService;
import gameshelf.service.SeguimientoService;
import gameshelf.service.UsuarioService;
import gameshelf.service.ValidacionException;
import gameshelf.service.VideojuegoService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Intermediario entre la vista y los servicios. Guarda la sesión del usuario actual y traduce las
 * excepciones en mensajes claros (informa la causa real del fallo, no un mensaje genérico).
 */
public class VideojuegoController {
    private final VideojuegoService service;
    private final UsuarioService usuarios;
    private final CalificacionService calificaciones;
    private final SeguimientoService seguimiento;
    private Usuario usuarioActual;

    public VideojuegoController(VideojuegoService service, UsuarioService usuarios, CalificacionService calificaciones,
                                SeguimientoService seguimiento) {
        this.service = service;
        this.usuarios = usuarios;
        this.calificaciones = calificaciones;
        this.seguimiento = seguimiento;
    }

    // ------------------------------------------------------------------ sesión

    public boolean hayUsuarios() {
        return usuarios.hayUsuarios();
    }

    public Resultado<Usuario> iniciarSesion(String nombre, char[] clave) {
        return ejecutar(() -> {
            usuarioActual = usuarios.iniciarSesion(nombre, clave);
            return Resultado.exito("Sesión iniciada.", usuarioActual);
        });
    }

    /** Crea la cuenta del administrador inicial e inicia sesión con ella. */
    public Resultado<Usuario> crearAdministradorInicial(String nombre, char[] clave, char[] confirmacion) {
        return ejecutar(() -> {
            usuarioActual = usuarios.crearAdministradorInicial(nombre, clave, confirmacion);
            return Resultado.exito("Cuenta de administrador creada.", usuarioActual);
        });
    }

    /** Crea una cuenta de usuario e inicia sesión con ella. */
    public Resultado<Usuario> registrarUsuario(String nombre, char[] clave, char[] confirmacion) {
        return ejecutar(() -> {
            usuarioActual = usuarios.registrar(nombre, clave, confirmacion);
            return Resultado.exito("Cuenta creada.", usuarioActual);
        });
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean esAdmin() {
        return usuarioActual != null && usuarioActual.isAdmin();
    }

    // ------------------------------------------------------------------ juegos (datos compartidos)

    /**
     * Agrega un juego a la biblioteca compartida.
     * @param personal datos personales iniciales del autor (estado, horas, favorito, notas); puede ser null.
     * @param puntuacionInicial calificación propia opcional del autor (0 = sin calificar todavía).
     */
    public Resultado<Videojuego> agregar(Videojuego videojuego, Path imagen, Seguimiento personal, int puntuacionInicial) {
        return ejecutar(() -> {
            Videojuego guardado = service.agregar(videojuego, usuarioActual, imagen, personal, puntuacionInicial);
            return Resultado.exito("Videojuego \"" + guardado.getNombre() + "\" guardado correctamente.", guardado);
        });
    }

    /** Modifica los datos del juego. Solo un administrador puede hacerlo (lo exige el servicio). */
    public Resultado<Videojuego> editar(Videojuego videojuego, Path imagenNueva, boolean quitarImagen) {
        return ejecutar(() -> {
            Videojuego guardado = service.editar(videojuego, usuarioActual, imagenNueva, quitarImagen);
            return Resultado.exito("Cambios en \"" + guardado.getNombre() + "\" guardados correctamente.", guardado);
        });
    }

    public Resultado<Void> eliminar(int id) {
        return ejecutar(() -> {
            service.eliminar(id, usuarioActual);
            return Resultado.<Void>exito("Videojuego eliminado.", null);
        });
    }

    public Resultado<Void> validarImagen(Path imagen) {
        return ejecutar(() -> {
            service.validarImagen(imagen);
            return Resultado.<Void>exito("Imagen válida.", null);
        });
    }

    /** Bytes de la imagen del juego (guardada en la base de datos), o null si no tiene. */
    public byte[] imagenDe(Videojuego videojuego) {
        return service.imagenDe(videojuego);
    }

    // ------------------------------------------------------------------ datos personales (privados por usuario)

    /** Guarda el estado, las horas, el favorito y las notas del usuario actual para ese juego. */
    public Resultado<Seguimiento> guardarSeguimiento(int videojuegoId, Estado estado, Double horas, boolean favorito, String notas) {
        return ejecutar(() -> {
            Seguimiento s = seguimiento.guardar(usuarioActual, videojuegoId, estado, horas, favorito, notas);
            return Resultado.exito("Tu seguimiento fue guardado.", s);
        });
    }

    /** Cambia solo el estado del usuario actual para ese juego (null = sin estado). */
    public Resultado<Seguimiento> cambiarEstado(int videojuegoId, Estado estado) {
        return ejecutar(() -> {
            Seguimiento s = seguimiento.cambiarEstado(usuarioActual, videojuegoId, estado);
            return Resultado.exito(estado == null ? "Estado quitado." : "Estado cambiado a " + estado + ".", s);
        });
    }

    public Resultado<Seguimiento> alternarFavorito(int videojuegoId) {
        return ejecutar(() -> {
            Seguimiento s = seguimiento.alternarFavorito(usuarioActual, videojuegoId);
            return Resultado.exito(s.isFavorito() ? "Marcado como favorito." : "Quitado de favoritos.", s);
        });
    }

    /** Seguimiento del usuario actual (idJuego -> datos). Nunca incluye los de otros usuarios. */
    public Map<Integer, Seguimiento> miSeguimiento() {
        return seguimiento.mios(usuarioActual);
    }

    /** Califica un juego con la cuenta actual, o cambia la calificación que ya le había dado. */
    public Resultado<Void> calificar(int videojuegoId, int puntuacion) {
        return ejecutar(() -> {
            boolean cambio = calificaciones.calificar(usuarioActual, videojuegoId, puntuacion);
            return Resultado.<Void>exito((cambio ? "Calificación actualizada: " : "Calificación guardada: ") + puntuacion + "/10.", null);
        });
    }

    /** Juegos para elegir al calificar: los no calificados y, si se indica, también ese (para cambiarle la nota). */
    public List<Videojuego> juegosParaCalificar(Integer incluirId) {
        return calificaciones.juegosParaCalificar(usuarioActual, incluirId);
    }

    /** Calificaciones del usuario actual (idJuego -> puntuación). Nunca incluye las de otros usuarios. */
    public Map<Integer, Integer> misPuntuaciones() {
        return calificaciones.misPuntuaciones(usuarioActual);
    }

    /** Ranking de todos los juegos por promedio de calificaciones de todos los usuarios (solo promedios, anónimo). */
    public List<EntradaRanking> ranking() {
        return calificaciones.ranking(usuarioActual);
    }

    /** Juegos que el usuario actual todavía no calificó (los únicos que se le ofrecen para calificar). */
    public List<Videojuego> pendientesDeCalificar() {
        return calificaciones.pendientesDeCalificar(usuarioActual);
    }

    // ------------------------------------------------------------------ consultas

    public List<Videojuego> listar(Filtro filtro) {
        return service.consultar(filtro, usuarioActual);
    }

    public Estadisticas estadisticas() {
        return service.estadisticas(usuarioActual);
    }

    public List<String> plataformas() {
        return service.plataformas();
    }

    public List<String> generos() {
        return service.generos();
    }

    private <T> Resultado<T> ejecutar(Supplier<Resultado<T>> operacion) {
        try {
            return operacion.get();
        } catch (ValidacionException e) {
            return Resultado.error(e.getMessage());
        } catch (RepositorioException e) {
            String causa = e.getCause() != null && e.getCause().getMessage() != null ? " (" + e.getCause().getMessage() + ")" : "";
            return Resultado.error(e.getMessage() + causa);
        } catch (RuntimeException e) {
            return Resultado.error("Ocurrió un error inesperado: " + e.getMessage());
        }
    }
}
