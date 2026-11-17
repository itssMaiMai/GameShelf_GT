package gameshelf.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector/escritor JSON mínimo (sin dependencias externas).
 * Tipos soportados: Map (objeto), List (arreglo), String, Number, Boolean y null.
 * Al leer, todos los números se devuelven como Double.
 */
public final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------ escritura

    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void escribir(Object v, StringBuilder sb, int nivel) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            cadena((String) v, sb);
        } else if (v instanceof Boolean || v instanceof Number) {
            sb.append(v.toString());
        } else if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sangria(sb, nivel + 1);
                cadena(String.valueOf(e.getKey()), sb);
                sb.append(": ");
                escribir(e.getValue(), sb, nivel + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            sangria(sb, nivel);
            sb.append('}');
        } else if (v instanceof List) {
            List<?> l = (List<?>) v;
            if (l.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < l.size(); i++) {
                sangria(sb, nivel + 1);
                escribir(l.get(i), sb, nivel + 1);
                if (i < l.size() - 1) sb.append(',');
                sb.append('\n');
            }
            sangria(sb, nivel);
            sb.append(']');
        } else {
            cadena(v.toString(), sb);
        }
    }

    private static void sangria(StringBuilder sb, int nivel) {
        for (int i = 0; i < nivel; i++) sb.append("  ");
    }

    private static void cadena(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ lectura

    public static Object leer(String texto) {
        Parser p = new Parser(texto);
        Object valor = p.valor();
        p.espacios();
        if (!p.fin()) {
            throw p.error("contenido inesperado después del valor");
        }
        return valor;
    }

    private static final class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        boolean fin() {
            return i >= s.length();
        }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON inválido en la posición " + i + ": " + msg);
        }

        void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            // Se tolera un BOM al inicio del archivo.
            if (i == 0 && i < s.length() && s.charAt(0) == '\uFEFF') {
                i++;
                espacios();
            }
        }

        Object valor() {
            espacios();
            if (fin()) throw error("fin de texto inesperado");
            char c = s.charAt(i);
            switch (c) {
                case '{': return objeto();
                case '[': return arreglo();
                case '"': return cadena();
                case 't': return literal("true", Boolean.TRUE);
                case 'f': return literal("false", Boolean.FALSE);
                case 'n': return literal("null", null);
                default:  return numero();
            }
        }

        Object literal(String texto, Object valor) {
            if (!s.startsWith(texto, i)) throw error("se esperaba " + texto);
            i += texto.length();
            return valor;
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            espacios();
            if (!fin() && s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                espacios();
                if (fin() || s.charAt(i) != '"') throw error("se esperaba una clave entre comillas");
                String clave = cadena();
                espacios();
                if (fin() || s.charAt(i) != ':') throw error("se esperaba ':'");
                i++;
                m.put(clave, valor());
                espacios();
                if (fin()) throw error("objeto sin cerrar");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw error("se esperaba ',' o '}'");
            }
        }

        List<Object> arreglo() {
            List<Object> l = new ArrayList<>();
            i++; // [
            espacios();
            if (!fin() && s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(valor());
                espacios();
                if (fin()) throw error("arreglo sin cerrar");
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw error("se esperaba ',' o ']'");
            }
        }

        String cadena() {
            StringBuilder sb = new StringBuilder();
            i++; // comilla inicial
            while (true) {
                if (fin()) throw error("cadena sin cerrar");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (fin()) throw error("escape incompleto");
                char e = s.charAt(i++);
                switch (e) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw error("escape \\u incompleto");
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("escape \\u inválido");
                        }
                        i += 4;
                        break;
                    default:
                        throw error("escape desconocido \\" + e);
                }
            }
        }

        Double numero() {
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (inicio == i) throw error("valor inesperado '" + s.charAt(i) + "'");
            try {
                return Double.valueOf(s.substring(inicio, i));
            } catch (NumberFormatException ex) {
                throw error("número inválido");
            }
        }
    }
}
