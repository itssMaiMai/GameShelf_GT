package gameshelf.persistence;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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

    /** Valida el archivo y lo guarda en la base. @return el nombre con el que quedó guardada. */
    public synchronized String importar(Path origen) {
        validar(origen);
        String extension = extension(origen.getFileName().toString());
        String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            guardarBytes(nombre, extension, Files.readAllBytes(origen));
        } catch (IOException e) {
            throw new RepositorioException("No se pudo leer la imagen " + origen, e);
        }
        return nombre;
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
