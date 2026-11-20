package gameshelf.persistence;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.text.Normalizer;

/**
 * Guarda las imágenes de los juegos DENTRO de la base de datos (tabla "imagen", como BLOB). Así la imagen viaja con la
 * base: quien reciba la base (o se conecte al servidor) ve todas las imágenes, sin carpetas aparte.
 * Las validaciones fallan con IllegalArgumentException (mensaje apto para el usuario).
 */
public class AlmacenImagenes {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_LADO = 4096;
    private static final Set<String> EXTENSIONES = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "bmp"));

    private final ConexionDB db;

    public AlmacenImagenes(ConexionDB db) {
        this.db = db;
    }

    /** Comprueba que el archivo sea una imagen aceptable. */
    public void validar(Path origen) {
        if (origen == null) {
            throw new IllegalArgumentException("No se seleccion\u00f3 ninguna imagen.");
        }
        if (!Files.isRegularFile(origen)) {
            throw new IllegalArgumentException("El archivo seleccionado no existe.");
        }
        if (!EXTENSIONES.contains(extension(origen.getFileName().toString()))) {
            throw new IllegalArgumentException("Formato no soportado. Us\u00e1 PNG, JPG, GIF o BMP.");
        }
        try {
            if (Files.size(origen) > MAX_BYTES) {
                throw new IllegalArgumentException("La imagen supera el tama\u00f1o m\u00e1ximo de 5 MB.");
            }
            validarDimensiones(origen.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer la imagen seleccionada.");
        }
    }

    private static void validarDimensiones(File archivo) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(archivo)) {
            Iterator<ImageReader> lectores = in == null ? null : ImageIO.getImageReaders(in);
            if (lectores == null || !lectores.hasNext()) {
                throw new IllegalArgumentException("El archivo no es una imagen v\u00e1lida.");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(in);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho > MAX_LADO || alto > MAX_LADO) {
                    throw new IllegalArgumentException("La imagen es demasiado grande (m\u00e1ximo " + MAX_LADO + "x" + MAX_LADO + " p\u00edxeles).");
                }
            } finally {
                lector.dispose();
            }
        }
    }

    /**
     * Valida el archivo y lo guarda en la base con el nombre del juego separado por guiones bajos
     * (por ejemplo, "Donkey_Kong_64.jpeg"). Si ya existe una imagen con ese nombre (otro juego con el mismo título),
     * se agrega un número: "Celeste_2.png".
     *
     * @return el nombre con el que quedó guardada.
     */
    public synchronized String importar(Path origen, String nombreJuego) {
        validar(origen);
        String extension = extension(origen.getFileName().toString());
        String base = nombreSeguro(nombreJuego);
        String nombre = base + "." + extension;
        for (int n = 2; existe(nombre); n++) {
            nombre = base + "_" + n + "." + extension;
        }
        try {
            guardarBytes(nombre, extension, Files.readAllBytes(origen));
        } catch (IOException e) {
            throw new RepositorioException("No se pudo leer la imagen " + origen, e);
        }
        return nombre;
    }

    /**
     * Convierte el nombre de un juego en un nombre de archivo seguro: sin tildes, con los espacios reemplazados por "_"
     * y sin caracteres especiales ("Crisis Core: Final Fantasy VII" pasa a "Crisis_Core_Final_Fantasy_VII").
     * Si no queda ningún carácter válido (por ejemplo, un título solo con símbolos), devuelve "juego".
     */
    public static String nombreSeguro(String nombreJuego) {
        String s = Normalizer.normalize(nombreJuego == null ? "" : nombreJuego, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.trim().replaceAll("\\s+", "_");
        s = s.replaceAll("[^A-Za-z0-9_-]", "");
        s = s.replaceAll("[_-]{2,}", "_").replaceAll("^[_-]+|[_-]+$", "");
        if (s.length() > 60) {
            s = s.substring(0, 60).replaceAll("[_-]+$", "");
        }
        return s.isEmpty() ? "juego" : s;
    }

    // ------------------------------------------------------------------ nombres coherentes con los juegos

    /**
     * @return true si el nombre de la imagen corresponde al juego: es el nombre del juego (sin tildes ni símbolos,
     *         espacios como "_") más la extensión, con o sin el número que se agrega cuando hay títulos repetidos.
     */
    public static boolean coincide(String nombreImagen, String nombreJuego) {
        if (nombreImagen == null || nombreImagen.isEmpty()) return false;
        int punto = nombreImagen.lastIndexOf('.');
        String base = punto < 0 ? nombreImagen : nombreImagen.substring(0, punto);
        String esperado = nombreSeguro(nombreJuego);
        return base.equalsIgnoreCase(esperado) || base.replaceAll("_\\d+$", "").equalsIgnoreCase(esperado);
    }

    /**
     * Revisa todos los juegos con imagen y renombra las imágenes cuyo nombre no corresponde al juego (por ejemplo las
     * que tienen un nombre aleatorio de una versión anterior). Es seguro repetirlo: lo que ya coincide no se toca.
     *
     * @return cuántas imágenes se renombraron.
     */
    public synchronized int sincronizarNombres() {
        int renombradas = 0;
        try {
            List<Object[]> juegos = new ArrayList<>();
            try (Statement st = db.conectar().createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, nombre, imagen FROM videojuego WHERE imagen <> '' ORDER BY id")) {
                while (rs.next()) {
                    juegos.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
                }
            }
            for (Object[] j : juegos) {
                if (renombrarSiHaceFalta((Integer) j[0], (String) j[1], (String) j[2])) {
                    renombradas++;
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudieron renombrar las im\u00e1genes", e);
        }
        return renombradas;
    }

    /** Igual que {@link #sincronizarNombres()}, pero solo para un juego (se usa cuando el administrador le cambia el nombre). */
    public synchronized boolean sincronizarNombre(int idJuego) {
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT nombre, imagen FROM videojuego WHERE id = ? AND imagen <> ''")) {
            ps.setInt(1, idJuego);
            String nombre;
            String imagen;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                nombre = rs.getString(1);
                imagen = rs.getString(2);
            }
            return renombrarSiHaceFalta(idJuego, nombre, imagen);
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo renombrar la imagen del juego", e);
        }
    }

    private boolean renombrarSiHaceFalta(int idJuego, String nombreJuego, String imagen) throws SQLException {
        if (!existe(imagen) || coincide(imagen, nombreJuego)) {
            return false;   // no hay imagen guardada para renombrar, o ya tiene el nombre que corresponde
        }
        String extension = extension(imagen);
        String base = nombreSeguro(nombreJuego);
        String nuevo = extension.isEmpty() ? base : base + "." + extension;
        for (int n = 2; existe(nuevo); n++) {
            nuevo = base + "_" + n + (extension.isEmpty() ? "" : "." + extension);
        }
        // Primero se copia, luego se apunta el juego a la copia y por último se borra la anterior (si nadie más la usa).
        try (PreparedStatement ps = db.conectar().prepareStatement("INSERT INTO imagen (nombre, tipo, datos) SELECT ?, tipo, datos FROM imagen WHERE nombre = ?")) {
            ps.setString(1, nuevo);
            ps.setString(2, imagen);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = db.conectar().prepareStatement("UPDATE videojuego SET imagen = ? WHERE id = ?")) {
            ps.setString(1, nuevo);
            ps.setInt(2, idJuego);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT COUNT(*) FROM videojuego WHERE imagen = ?")) {
            ps.setString(1, imagen);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement del = db.conectar().prepareStatement("DELETE FROM imagen WHERE nombre = ?")) {
                        del.setString(1, imagen);
                        del.executeUpdate();
                    }
                }
            }
        }
        return true;
    }

    /** Guarda los bytes de una imagen con un nombre dado (si ya existe una con ese nombre, no la reemplaza). */
    public synchronized void guardarBytes(String nombre, String tipo, byte[] datos) {
        try {
            if (existe(nombre)) {
                return;
            }
            try (PreparedStatement ps = db.conectar().prepareStatement("INSERT INTO imagen (nombre, tipo, datos) VALUES (?, ?, ?)")) {
                ps.setString(1, nombre);
                ps.setString(2, tipo);
                ps.setBytes(3, datos);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo guardar la imagen en la base de datos", e);
        }
    }

    public synchronized boolean existe(String nombre) {
        if (!nombreValido(nombre)) return false;
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT 1 FROM imagen WHERE nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo consultar la imagen en la base de datos", e);
        }
    }

    /** @return los bytes de la imagen, o null si no hay nombre, es inválido o la imagen no está en la base. */
    public synchronized byte[] leer(String nombre) {
        if (!nombreValido(nombre)) return null;
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT datos FROM imagen WHERE nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo leer la imagen de la base de datos", e);
        }
    }

    /** Borra la imagen de la base (si existe). */
    public synchronized void eliminar(String nombre) {
        if (!nombreValido(nombre)) return;
        try (PreparedStatement ps = db.conectar().prepareStatement("DELETE FROM imagen WHERE nombre = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo borrar la imagen de la base de datos", e);
        }
    }

    private static boolean nombreValido(String nombre) {
        return nombre != null && !nombre.isEmpty() && nombre.length() <= 80 && nombre.matches("[A-Za-z0-9._-]+") && !nombre.contains("..");
    }

    /** Extensión en minúsculas (sin el punto), o "" si no tiene. */
    public static String extension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto < 0 ? "" : nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
