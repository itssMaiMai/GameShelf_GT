package gameshelf.view;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utilidades para mostrar imágenes escaladas dentro de un recuadro. */
final class ImagenUtil {

    private ImagenUtil() {
    }

    /** Imagen desde un archivo del disco (la que el usuario eligió y todavía no se guardó). */
    static ImageIcon cargarIcono(Path ruta, int maxAncho, int maxAlto) {
        if (ruta == null) return null;
        try {
            return cargarIcono(Files.readAllBytes(ruta), maxAncho, maxAlto);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** @return la imagen escalada para entrar en el recuadro (manteniendo proporción), o null si no se puede leer. */
    static ImageIcon cargarIcono(byte[] datos, int maxAncho, int maxAlto) {
        if (datos == null) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(datos));
            if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return null;
            double escala = Math.min((double) maxAncho / img.getWidth(), (double) maxAlto / img.getHeight());
            int ancho = Math.max(1, (int) Math.round(img.getWidth() * escala));
            int alto = Math.max(1, (int) Math.round(img.getHeight() * escala));
            return new ImageIcon(img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
