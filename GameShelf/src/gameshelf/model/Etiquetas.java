package gameshelf.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para los campos que admiten varios valores separados por coma (o punto y coma): plataforma, género
 * y desarrollador. Por ejemplo "PC, Switch" son dos plataformas. Las comparaciones no distinguen mayúsculas.
 */
public final class Etiquetas {

    private Etiquetas() {
    }

    /** Separa el texto en valores individuales: sin espacios sobrantes, sin vacíos y sin repetidos. */
    public static List<String> separar(String texto) {
        List<String> resultado = new ArrayList<>();
        if (texto == null) {
            return resultado;
        }
        for (String parte : texto.split("[,;]")) {
            String plataforma = parte.trim().replaceAll("\\s+", " ");
            if (!plataforma.isEmpty() && !contiene(resultado, plataforma)) {
                resultado.add(plataforma);
            }
        }
        return resultado;
    }

    /** @return true si la lista incluye el valor buscado (sin distinguir mayúsculas). */
    public static boolean contiene(List<String> valores, String buscada) {
        for (String p : valores) {
            if (p.equalsIgnoreCase(buscada.trim())) {
                return true;
            }
        }
        return false;
    }

    /** @return true si el texto (valores separados por coma) incluye el valor buscado. */
    public static boolean contiene(String texto, String buscada) {
        return buscada != null && contiene(separar(texto), buscada);
    }

    /** @return el primer valor de {@code a} que también está en {@code b}, o null si no tienen ninguno en común. */
    public static String enComun(List<String> a, List<String> b) {
        for (String p : a) {
            if (contiene(b, p)) {
                return p;
            }
        }
        return null;
    }
}
