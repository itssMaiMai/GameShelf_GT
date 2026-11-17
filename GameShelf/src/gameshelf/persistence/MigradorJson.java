package gameshelf.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Importa a la base de datos la información de la versión anterior de GameShelf (archivos gameshelf.json, usuarios.json,
 * calificaciones.json, seguimiento.json y la carpeta "imagenes"). Conserva los ids, las contraseñas y las fechas, y
 * guarda cada imagen DENTRO de la base. Es seguro ejecutarlo más de una vez: los datos solo se importan si la base está
 * vacía, y las imágenes que falten se completan siempre que existan en la carpeta indicada.
 */
public final class MigradorJson {

    /** Resumen de lo importado. */
    public static final class Resultado {
        public int usuarios;
        public int juegos;
        public int seguimientos;
        public int calificaciones;
        public int imagenesImportadas;
        public final List<String> imagenesFaltantes = new ArrayList<>();
        public boolean datosImportados;

        public String resumen() {
            StringBuilder sb = new StringBuilder();
            if (datosImportados) {
                sb.append("Datos importados: ").append(usuarios).append(" usuarios, ").append(juegos).append(" juegos, ")
                        .append(seguimientos).append(" registros de seguimiento y ").append(calificaciones).append(" calificaciones.\n");
            } else {
                sb.append("La base ya tiene datos: no se importaron usuarios ni juegos.\n");
            }
            sb.append("Im\u00e1genes guardadas en la base: ").append(imagenesImportadas);
            if (!imagenesFaltantes.isEmpty()) {
                sb.append("\nIm\u00e1genes que no se encontraron en la carpeta \"imagenes\" (").append(imagenesFaltantes.size()).append("): ")
                        .append(String.join(", ", imagenesFaltantes));
            }
            return sb.toString();
        }
    }

    private MigradorJson() {
    }

    /** @return true si la carpeta tiene archivos de la versión anterior para importar. */
    public static boolean hayArchivosAntiguos(Path carpeta) {
        return carpeta != null && Files.isRegularFile(carpeta.resolve("gameshelf.json")) && Files.isRegularFile(carpeta.resolve("usuarios.json"));
    }

    public static Resultado migrar(ConexionDB db, Path carpeta) throws IOException, SQLException {
        Resultado r = new Resultado();
        Connection c = db.conectar();
        if (estaVacia(c) && hayArchivosAntiguos(carpeta)) {
            importarDatos(c, carpeta, r);
        }
        completarImagenes(db, c, carpeta, r);
        return r;
    }

    // ------------------------------------------------------------------ datos

    private static boolean estaVacia(Connection c) throws SQLException {
        return contar(c, "usuario") == 0 && contar(c, "videojuego") == 0;
    }

    private static int contar(Connection c, String tabla) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void importarDatos(Connection c, Path carpeta, Resultado r) throws IOException, SQLException {
        Map<?, ?> usuarios = leer(carpeta.resolve("usuarios.json"));
        Map<?, ?> juegos = leer(carpeta.resolve("gameshelf.json"));
        Map<?, ?> seguimiento = Files.isRegularFile(carpeta.resolve("seguimiento.json")) ? leer(carpeta.resolve("seguimiento.json")) : null;
        Map<?, ?> calificaciones = Files.isRegularFile(carpeta.resolve("calificaciones.json")) ? leer(carpeta.resolve("calificaciones.json")) : null;

        boolean autoCommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            int maxUsuario = 0;
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO usuario (id, nombre, rol, salt, hash) VALUES (?, ?, ?, ?, ?)")) {
                for (Map<?, ?> u : lista(usuarios, "usuarios")) {
                    int id = entero(u.get("id"));
                    ps.setInt(1, id);
                    ps.setString(2, texto(u.get("nombre")));
                    ps.setString(3, "ADMIN".equalsIgnoreCase(texto(u.get("rol"))) ? "ADMIN" : "USUARIO");
                    ps.setString(4, texto(u.get("salt")));
                    ps.setString(5, texto(u.get("hash")));
                    ps.executeUpdate();
                    maxUsuario = Math.max(maxUsuario, id);
                    r.usuarios++;
                }
            }
            reiniciarIdentidad(c, "usuario", maxUsuario + 1);

            int maxJuego = entero(juegos.get("ultimoId"));
            String sqlJuego = "INSERT INTO videojuego (id, nombre, plataforma, genero, desarrollador, anio, fecha_creacion, creado_por, imagen)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sqlJuego)) {
                for (Map<?, ?> j : lista(juegos, "videojuegos")) {
                    int id = entero(j.get("id"));
                    ps.setInt(1, id);
                    ps.setString(2, texto(j.get("nombre")));
                    ps.setString(3, texto(j.get("plataforma")));
                    ps.setString(4, texto(j.get("genero")));
                    ps.setString(5, texto(j.get("desarrollador")));
                    ps.setInt(6, entero(j.get("anio")));
                    ps.setTimestamp(7, Timestamp.valueOf(fecha(j.get("fechaCreacion"))));
                    ps.setString(8, texto(j.get("creadoPor")));
                    ps.setString(9, texto(j.get("imagen")));
                    ps.executeUpdate();
                    maxJuego = Math.max(maxJuego, id);
                    r.juegos++;
                }
            }
            reiniciarIdentidad(c, "videojuego", maxJuego + 1);   // los ids de juegos eliminados no se reutilizan

            if (seguimiento != null) {
                String sql = "INSERT INTO seguimiento (usuario_id, videojuego_id, estado, horas_jugadas, favorito, notas) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (Map<?, ?> s : lista(seguimiento, "seguimiento")) {
                        ps.setInt(1, entero(s.get("usuarioId")));
                        ps.setInt(2, entero(s.get("videojuegoId")));
                        String estado = texto(s.get("estado"));
                        if (estado.isEmpty()) ps.setNull(3, Types.VARCHAR); else ps.setString(3, estado.toUpperCase());
                        if (s.get("horasJugadas") instanceof Number) ps.setDouble(4, ((Number) s.get("horasJugadas")).doubleValue());
                        else ps.setNull(4, Types.DOUBLE);
                        ps.setBoolean(5, Boolean.TRUE.equals(s.get("favorito")));
                        ps.setString(6, texto(s.get("notas")));
                        ps.executeUpdate();
                        r.seguimientos++;
                    }
                }
            }
            if (calificaciones != null) {
                String sql = "INSERT INTO calificacion (usuario_id, videojuego_id, puntuacion, fecha) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (Map<?, ?> k : lista(calificaciones, "calificaciones")) {
                        ps.setInt(1, entero(k.get("usuarioId")));
                        ps.setInt(2, entero(k.get("videojuegoId")));
                        ps.setInt(3, entero(k.get("puntuacion")));
                        ps.setTimestamp(4, Timestamp.valueOf(fecha(k.get("fecha"))));
                        ps.executeUpdate();
                        r.calificaciones++;
                    }
                }
            }
            c.commit();
            r.datosImportados = true;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(autoCommit);
        }
    }

    private static void reiniciarIdentidad(Connection c, String tabla, int siguiente) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE " + tabla + " ALTER COLUMN id RESTART WITH " + siguiente);
        }
    }

    // ------------------------------------------------------------------ imágenes

    /** Guarda en la base las imágenes que los juegos referencian y todavía no están en ella, buscándolas en carpeta/imagenes. */
    private static void completarImagenes(ConexionDB db, Connection c, Path carpeta, Resultado r) throws IOException, SQLException {
        AlmacenImagenes almacen = new AlmacenImagenes(db);
        List<String> nombres = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT DISTINCT imagen FROM videojuego WHERE imagen <> ''")) {
            while (rs.next()) {
                nombres.add(rs.getString(1));
            }
        }
        Path carpetaImagenes = carpeta == null ? null : carpeta.resolve("imagenes");
        for (String nombre : nombres) {
            if (almacen.existe(nombre)) {
                continue;
            }
            Path archivo = carpetaImagenes == null ? null : carpetaImagenes.resolve(nombre).normalize();
            if (archivo != null && archivo.startsWith(carpetaImagenes.normalize()) && Files.isRegularFile(archivo)) {
                almacen.guardarBytes(nombre, AlmacenImagenes.extension(nombre), Files.readAllBytes(archivo));
                r.imagenesImportadas++;
            } else {
                r.imagenesFaltantes.add(nombre);
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static Map<?, ?> leer(Path archivo) throws IOException {
        Object raiz = Json.leer(new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8));
        if (!(raiz instanceof Map)) {
            throw new IOException("El archivo " + archivo.getFileName() + " no tiene el formato esperado.");
        }
        return (Map<?, ?>) raiz;
    }

    private static List<Map<?, ?>> lista(Map<?, ?> raiz, String clave) throws IOException {
        Object valor = raiz.get(clave);
        if (!(valor instanceof List)) {
            throw new IOException("Falta la lista \"" + clave + "\" en el archivo de datos.");
        }
        List<Map<?, ?>> filas = new ArrayList<>();
        for (Object o : (List<?>) valor) {
            if (!(o instanceof Map)) {
                throw new IOException("Hay un registro con formato inesperado en \"" + clave + "\".");
            }
            filas.add((Map<?, ?>) o);
        }
        return filas;
    }

    private static String texto(Object o) {
        return o instanceof String ? (String) o : "";
    }

    private static int entero(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private static LocalDateTime fecha(Object o) {
        try {
            return o instanceof String ? LocalDateTime.parse((String) o) : LocalDateTime.now();
        } catch (RuntimeException e) {
            return LocalDateTime.now();
        }
    }
}
