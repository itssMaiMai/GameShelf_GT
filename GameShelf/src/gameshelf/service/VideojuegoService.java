package gameshelf.service;

import gameshelf.model.Calificacion;
import gameshelf.model.Estadisticas;
import gameshelf.model.Estado;
import gameshelf.model.Etiquetas;
import gameshelf.model.EstadoCalificacion;
import gameshelf.model.Filtro;
import gameshelf.model.Seguimiento;
import gameshelf.model.Usuario;
import gameshelf.model.Videojuego;
import gameshelf.persistence.AlmacenImagenes;
import gameshelf.persistence.CalificacionRepository;
import gameshelf.persistence.SeguimientoRepository;
import gameshelf.persistence.VideojuegoRepository;

import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reglas de la biblioteca compartida: alta de juegos, modificación y baja (solo administrador),
 * búsquedas, filtros y estadísticas. Los datos personales (estado, calificación, etc.) se leen siempre
 * desde el punto de vista del usuario que consulta.
 */
public class VideojuegoService {
    public static final int MAX_NOMBRE = 100;
    public static final int MAX_TEXTO = 60;
    public static final int MAX_ETIQUETAS = 10;
    public static final int ANIO_MINIMO = 1950;

    private final VideojuegoRepository repository;
    private final AlmacenImagenes imagenes;
    private final CalificacionRepository calificaciones;
    private final SeguimientoRepository seguimientos;

    public VideojuegoService(VideojuegoRepository repository, AlmacenImagenes imagenes,
                             CalificacionRepository calificaciones, SeguimientoRepository seguimientos) {
        this.repository = repository;
        this.imagenes = imagenes;
        this.calificaciones = calificaciones;
        this.seguimientos = seguimientos;
    }

    // ------------------------------------------------------------------ altas, bajas y modificaciones

    /**
     * Cualquier usuario con sesión puede agregar juegos (con o sin imagen); quedan visibles para todos.
     * Los datos personales iniciales (estado, horas, favorito, notas) y la calificación inicial opcional
     * quedan registrados solo para el autor.
     */
    public Videojuego agregar(Videojuego videojuego, Usuario actor, Path imagenOrigen,
                              Seguimiento personal, int puntuacionInicial) {
        exigirSesion(actor);
        if (puntuacionInicial < 0 || puntuacionInicial > 10) {
            throw new ValidacionException("La calificación debe estar entre 1 y 10 (0 = sin calificar todavía).");
        }
        Seguimiento propio = SeguimientoService.normalizarYValidar(personal == null ? new Seguimiento() : personal);
        propio.setEstado(SeguimientoService.estadoSegunHoras(propio.getEstado(), propio.getHorasJugadas(), null));
        Videojuego v = prepararYValidar(videojuego);
        v.setImagen(imagenOrigen != null ? importarImagen(imagenOrigen, v.getNombre()) : "");
        v.setCreadoPor(actor.getNombre());
        v.setFechaCreacion(LocalDateTime.now());
        try {
            repository.guardar(v);
            propio.setUsuarioId(actor.getId());
            propio.setVideojuegoId(v.getId());
            seguimientos.guardar(propio);
            if (puntuacionInicial > 0) {
                calificaciones.guardar(new Calificacion(actor.getId(), v.getId(), puntuacionInicial, LocalDateTime.now()));
            }
        } catch (RuntimeException e) {
            repository.eliminar(v.getId());
            calificaciones.eliminarDeJuego(v.getId());
            seguimientos.eliminarDeJuego(v.getId());
            imagenes.eliminar(v.getImagen());
            throw e;
        }
        return v;
    }

    /**
     * Modifica los datos del juego (nombre, plataforma, género, desarrollador, año, imagen).
     * Solo un administrador puede hacerlo; ni siquiera quien lo agregó.
     * La imagen actual se conserva salvo que se indique una nueva (imagenNueva) o se pida quitarla.
     */
    public Videojuego editar(Videojuego videojuego, Usuario actor, Path imagenNueva, boolean quitarImagen) {
        exigirSesion(actor);
        if (!actor.isAdmin()) {
            throw new ValidacionException("Solo un administrador puede modificar los datos de un juego.");
        }
        if (videojuego == null) {
            throw new ValidacionException("No hay datos para guardar.");
        }
        Videojuego existente = repository.buscarPorId(videojuego.getId());
        if (existente == null) {
            throw new ValidacionException("El videojuego que intentás editar ya no existe.");
        }
        Videojuego v = prepararYValidar(videojuego);
        v.setFechaCreacion(existente.getFechaCreacion());
        v.setCreadoPor(existente.getCreadoPor());

        String anterior = existente.getImagen();
        String importada = null;
        String finalImagen = anterior;
        if (imagenNueva != null) {
            importada = importarImagen(imagenNueva, v.getNombre());
            finalImagen = importada;
        } else if (quitarImagen) {
            finalImagen = "";
        }
        v.setImagen(finalImagen);
        try {
            repository.actualizar(v);
        } catch (RuntimeException e) {
            if (importada != null) imagenes.eliminar(importada);
            throw e;
        }
        if (!anterior.isEmpty() && !anterior.equals(finalImagen)) {
            imagenes.eliminar(anterior);
        }
        try {
            // Si cambió el nombre del juego, la imagen se renombra para que siga coincidiendo con él.
            imagenes.sincronizarNombre(v.getId());
            v.setImagen(repository.buscarPorId(v.getId()).getImagen());
        } catch (RuntimeException e) {
            // el cambio ya quedó guardado; si no se pudo renombrar la imagen, se reintenta al abrir la aplicación
        }
        return v;
    }

    /** Solo un administrador puede dar de baja un juego de la plataforma. */
    public void eliminar(int id, Usuario actor) {
        exigirSesion(actor);
        if (!actor.isAdmin()) {
            throw new ValidacionException("Solo un administrador puede dar de baja un juego de la plataforma.");
        }
        Videojuego existente = repository.buscarPorId(id);
        if (existente == null || !repository.eliminar(id)) {
            throw new ValidacionException("El videojuego que intentás eliminar ya no existe.");
        }
        calificaciones.eliminarDeJuego(id);
        seguimientos.eliminarDeJuego(id);
        imagenes.eliminar(existente.getImagen());
    }

    public void validarImagen(Path origen) {
        try {
            imagenes.validar(origen);
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(e.getMessage());
        }
    }

    /** Bytes de la imagen del juego (guardada en la base de datos), o null si no tiene imagen. */
    public byte[] imagenDe(Videojuego v) {
        return v == null ? null : imagenes.leer(v.getImagen());
    }

    private String importarImagen(Path origen, String nombreJuego) {
        try {
            return imagenes.importar(origen, nombreJuego);
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(e.getMessage());
        }
    }

    private static void exigirSesion(Usuario actor) {
        if (actor == null) {
            throw new ValidacionException("Tenés que iniciar sesión para realizar esta acción.");
        }
    }

    // ------------------------------------------------------------------ consultas

    /**
     * Busca por nombre (sin distinguir mayúsculas ni acentos) y aplica los filtros. Los filtros de estado,
     * favoritos y calificación se evalúan con los datos PERSONALES del usuario que consulta.
     */
    public List<Videojuego> consultar(Filtro filtro, Usuario actor) {
        Filtro f = filtro == null ? new Filtro() : filtro;
        String texto = normalizar(f.getTexto());
        Map<Integer, Seguimiento> propios = propios(actor);
        Map<Integer, Integer> puntos = puntuaciones(actor);
        List<Videojuego> resultado = new ArrayList<>();
        for (Videojuego v : repository.obtenerTodos()) {
            if (!texto.isEmpty() && !normalizar(v.getNombre()).contains(texto)
                    && !normalizar(v.getDesarrollador()).contains(texto)) continue;
            if (!vacio(f.getPlataforma()) && !Etiquetas.contiene(v.getPlataforma(), f.getPlataforma())) continue;
            if (!coincideAlgunGenero(v.getGenero(), f.getGeneros())) continue;
            Seguimiento s = propios.get(v.getId());
            Estado estado = s == null ? null : s.getEstado();
            if (f.isSinEstado() && estado != null) continue;
            if (f.getEstado() != null && estado != f.getEstado()) continue;
            if (f.isSoloFavoritos() && (s == null || !s.isFavorito())) continue;
            if (f.getCalificacion() == EstadoCalificacion.SIN_CALIFICAR && puntos.containsKey(v.getId())) continue;
            if (f.getCalificacion() == EstadoCalificacion.CALIFICADOS && !puntos.containsKey(v.getId())) continue;
            resultado.add(v);
        }
        resultado.sort(Comparator.comparing((Videojuego v) -> v.getNombre().toLowerCase(Locale.ROOT))
                .thenComparingInt(Videojuego::getId));
        return resultado;
    }

    /** Plataformas individuales de la biblioteca (un juego en "PC, Switch" aporta "PC" y "Switch"), sin repetir. */
    public List<String> plataformas() {
        TreeMap<String, String> distintas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Videojuego v : repository.obtenerTodos()) {
            for (String p : Etiquetas.separar(v.getPlataforma())) {
                distintas.putIfAbsent(p, p);
            }
        }
        return new ArrayList<>(distintas.values());
    }

    /** Géneros individuales de la biblioteca (un juego en "RPG, Aventura" aporta "RPG" y "Aventura"), sin repetir. */
    public List<String> generos() {
        TreeMap<String, String> distintos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Videojuego v : repository.obtenerTodos()) {
            for (String g : Etiquetas.separar(v.getGenero())) {
                distintos.putIfAbsent(g, g);
            }
        }
        return new ArrayList<>(distintos.values());
    }

    /** Desarrolladores individuales de la biblioteca, sin repetir. */
    public List<String> desarrolladores() {
        TreeMap<String, String> distintos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Videojuego v : repository.obtenerTodos()) {
            for (String d : Etiquetas.separar(v.getDesarrollador())) {
                distintos.putIfAbsent(d, d);
            }
        }
        return new ArrayList<>(distintos.values());
    }

    /** @return true si no hay géneros marcados o el juego tiene al menos uno de los marcados. */
    private static boolean coincideAlgunGenero(String generosDelJuego, List<String> marcados) {
        if (marcados == null || marcados.isEmpty()) {
            return true;
        }
        for (String g : marcados) {
            if (Etiquetas.contiene(generosDelJuego, g)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estadísticas de la colección desde el punto de vista del usuario: el total de juegos es común, y los
     * estados, favoritos, horas y puntuación media son los propios (un juego que nunca tocó queda "sin estado").
     */
    public Estadisticas estadisticas(Usuario actor) {
        Map<Integer, Seguimiento> propios = propios(actor);
        Map<Integer, Integer> puntos = puntuaciones(actor);
        int total = 0, pendientes = 0, jugando = 0, terminados = 0, abandonados = 0, sinEstado = 0, favoritos = 0;
        int calificados = 0, sumaPuntos = 0;
        double horas = 0;
        for (Videojuego v : repository.obtenerTodos()) {
            total++;
            Seguimiento s = propios.get(v.getId());
            Estado estado = s == null ? null : s.getEstado();
            if (estado == null) {
                sinEstado++;
            } else {
                switch (estado) {
                    case PENDIENTE: pendientes++; break;
                    case JUGANDO: jugando++; break;
                    case TERMINADO: terminados++; break;
                    case ABANDONADO: abandonados++; break;
                    default: break;
                }
            }
            if (s != null && s.isFavorito()) favoritos++;
            if (s != null && s.getHorasJugadas() != null) horas += s.getHorasJugadas();
            Integer propia = puntos.get(v.getId());
            if (propia != null) {
                calificados++;
                sumaPuntos += propia;
            }
        }
        double promedio = calificados == 0 ? 0 : (double) sumaPuntos / calificados;
        return new Estadisticas(total, pendientes, jugando, terminados, abandonados, sinEstado, favoritos, horas, calificados, promedio);
    }

    private Map<Integer, Seguimiento> propios(Usuario actor) {
        return actor == null ? Collections.<Integer, Seguimiento>emptyMap() : seguimientos.deUsuario(actor.getId());
    }

    private Map<Integer, Integer> puntuaciones(Usuario actor) {
        return actor == null ? Collections.<Integer, Integer>emptyMap() : calificaciones.puntuacionesDeUsuario(actor.getId());
    }

    // ------------------------------------------------------------------ validación

    /** Devuelve una copia normalizada (sin espacios sobrantes) y validada; lanza ValidacionException si hay errores. */
    private Videojuego prepararYValidar(Videojuego original) {
        if (original == null) {
            throw new ValidacionException("No hay datos para guardar.");
        }
        Videojuego v = original.copy();
        v.setNombre(limpiar(v.getNombre()));
        v.setPlataforma(normalizarEtiquetas(v.getPlataforma(), plataformas(), "plataforma", "plataformas"));
        v.setGenero(normalizarEtiquetas(v.getGenero(), generos(), "g\u00e9nero", "g\u00e9neros"));
        v.setDesarrollador(normalizarEtiquetas(v.getDesarrollador(), desarrolladores(), "desarrollador", "desarrolladores"));

        if (v.getNombre().isEmpty()) {
            throw new ValidacionException("El nombre del videojuego es obligatorio.");
        }
        if (v.getNombre().length() > MAX_NOMBRE) {
            throw new ValidacionException("El nombre no puede superar los " + MAX_NOMBRE + " caracteres.");
        }
        int anioMaximo = Year.now().getValue() + 5;
        if (v.getAnio() != 0 && (v.getAnio() < ANIO_MINIMO || v.getAnio() > anioMaximo)) {
            throw new ValidacionException("El año de lanzamiento debe estar entre " + ANIO_MINIMO + " y " + anioMaximo + ".");
        }
        // Un mismo juego no puede repetirse en una misma plataforma (aunque esté cargado junto con otras).
        List<String> nuevas = Etiquetas.separar(v.getPlataforma());
        for (Videojuego otro : repository.obtenerTodos()) {
            if (otro.getId() == v.getId() || !otro.getNombre().equalsIgnoreCase(v.getNombre())) {
                continue;
            }
            List<String> existentes = Etiquetas.separar(otro.getPlataforma());
            if (nuevas.isEmpty() && existentes.isEmpty()) {
                throw new ValidacionException("Ya existe \"" + otro.getNombre() + "\" en la biblioteca.");
            }
            String comun = Etiquetas.enComun(nuevas, existentes);
            if (comun != null) {
                throw new ValidacionException("Ya existe \"" + otro.getNombre() + "\" en " + comun + " en la biblioteca.");
            }
        }
        return v;
    }

    /**
     * Separa los valores por coma, descarta vacíos y repetidos (sin distinguir mayúsculas) y unifica la escritura
     * con la que ya existe en la biblioteca: si ya hay "PC", escribir "pc" queda como "PC". Sirve para plataforma,
     * género y desarrollador.
     * @return los valores unidos con ", " (o "" si no hay ninguno).
     */
    private String normalizarEtiquetas(String texto, List<String> conocidas, String singular, String plural) {
        List<String> escritas = Etiquetas.separar(texto);
        if (escritas.size() > MAX_ETIQUETAS) {
            throw new ValidacionException("Un juego puede tener hasta " + MAX_ETIQUETAS + " " + plural + ".");
        }
        List<String> resultado = new ArrayList<>();
        for (String p : escritas) {
            if (p.length() > MAX_TEXTO) {
                throw new ValidacionException("Cada " + singular + " puede tener hasta " + MAX_TEXTO + " caracteres.");
            }
            String canonica = p;
            for (String c : conocidas) {
                if (c.equalsIgnoreCase(p)) {
                    canonica = c;
                    break;
                }
            }
            if (!Etiquetas.contiene(resultado, canonica)) {
                resultado.add(canonica);
            }
        }
        return String.join(", ", resultado);
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.trim().toLowerCase(Locale.ROOT);
    }
}
