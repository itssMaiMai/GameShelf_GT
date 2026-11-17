package gameshelf.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Conexión a la base de datos (H2). Se configura con la URL JDBC:
 * <ul>
 *   <li><b>Archivo local</b> (por defecto): {@code jdbc:h2:file:<ruta>/gameshelf-db;AUTO_SERVER=TRUE}. Es un único archivo
 *       ({@code gameshelf-db.mv.db}) que contiene TODO: juegos, usuarios, calificaciones e imágenes. Pasando ese archivo
 *       se pasan también las imágenes. Con AUTO_SERVER varias copias de la app en la misma PC pueden usarlo a la vez.</li>
 *   <li><b>Servidor</b>: {@code jdbc:h2:tcp://host:9092/gameshelf-db}, contra un servidor iniciado con
 *       {@code java -jar GameShelf.jar --servidor} (ver README).</li>
 * </ul>
 * Orden de configuración: propiedades del sistema ({@code -Dgameshelf.db.url=...}), luego el archivo
 * {@code gameshelf.properties} (claves db.url, db.usuario, db.clave) y por último los valores por defecto.
 */
public class ConexionDB {
    public static final String ARCHIVO_CONFIG = "gameshelf.properties";
    public static final String BASE_POR_DEFECTO = "gameshelf-db";
    public static final String USUARIO_POR_DEFECTO = "gameshelf";
    public static final String CLAVE_POR_DEFECTO = "gameshelf";

    private String url;
    private String usuario;
    private String clave;
    private Connection conexion;

    /** Carga la configuración (propiedades del sistema, gameshelf.properties o valores por defecto). */
    public ConexionDB() {
        Properties archivo = new Properties();
        Path ruta = Paths.get(ARCHIVO_CONFIG).toAbsolutePath();
        if (Files.isRegularFile(ruta)) {
            try (InputStream in = Files.newInputStream(ruta)) {
                archivo.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                // Si no se puede leer, se usan los valores por defecto.
            }
        }
        this.url = valor("gameshelf.db.url", archivo.getProperty("db.url"), urlLocal(BASE_POR_DEFECTO));
        this.usuario = valor("gameshelf.db.usuario", archivo.getProperty("db.usuario"), USUARIO_POR_DEFECTO);
        this.clave = valor("gameshelf.db.clave", archivo.getProperty("db.clave"), CLAVE_POR_DEFECTO);
    }

    public ConexionDB(String url, String usuario, String clave) {
        setUrl(url);
        this.usuario = usuario == null ? "" : usuario;
        this.clave = clave == null ? "" : clave;
    }

    /** Conexión a una base en un archivo local (rutaBase sin extensión), con las credenciales por defecto. */
    public static ConexionDB local(String rutaBase) {
        return new ConexionDB(urlLocal(rutaBase), USUARIO_POR_DEFECTO, CLAVE_POR_DEFECTO);
    }

    public static String urlLocal(String rutaBase) {
        return "jdbc:h2:file:" + Paths.get(rutaBase).toAbsolutePath().normalize() + ";AUTO_SERVER=TRUE";
    }

    public static String urlServidor(String host, int puerto, String base) {
        return "jdbc:h2:tcp://" + host.trim() + ":" + puerto + "/" + base.trim();
    }

    private static String valor(String propiedadSistema, String delArchivo, String porDefecto) {
        String s = System.getProperty(propiedadSistema);
        if (s != null && !s.trim().isEmpty()) return s.trim();
        if (delArchivo != null && !delArchivo.trim().isEmpty()) return delArchivo.trim();
        return porDefecto;
    }

    public String getUrl() {
        return url;
    }

    /** Corrige el error intencional original: ya no acepta null ni valores vacíos. */
    public synchronized void setUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección de la base de datos no puede estar vacía.");
        }
        if (!url.trim().startsWith("jdbc:h2:")) {
            throw new IllegalArgumentException("Solo se admiten bases H2 (jdbc:h2:...).");
        }
        cerrar();
        this.url = url.trim();
    }

    public String getUsuario() {
        return usuario;
    }

    public String getClave() {
        return clave;
    }

    public synchronized void setCredenciales(String usuario, String clave) {
        cerrar();
        this.usuario = usuario == null ? "" : usuario.trim();
        this.clave = clave == null ? "" : clave;
    }

    public boolean esRemota() {
        return url.contains(":tcp:") || url.contains(":ssl:");
    }

    /** Texto corto para mostrar al usuario. */
    public String descripcion() {
        if (esRemota()) {
            String s = url.substring(url.indexOf("//") + 2);
            int corte = s.indexOf('/');
            return "servidor " + (corte < 0 ? s : s.substring(0, corte));
        }
        return "archivo local";
    }

    /**
     * Devuelve una conexión abierta (la crea o la reabre si hace falta) con el esquema ya creado.
     * La misma conexión se comparte entre los repositorios.
     */
    public synchronized Connection conectar() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(2)) {
            cerrar();
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Falta el controlador de base de datos H2.", e);
            }
            Connection nueva = DriverManager.getConnection(url, usuario, clave);
            try {
                Esquema.crear(nueva);
            } catch (SQLException e) {
                try {
                    nueva.close();
                } catch (SQLException ignorada) {
                    // se informa el error original
                }
                throw e;
            }
            conexion = nueva;
        }
        return conexion;
    }

    public synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                // no hay nada más que hacer
            }
            conexion = null;
        }
    }

    /** Guarda la configuración en gameshelf.properties (junto al programa) para los próximos arranques. */
    public synchronized void guardarConfiguracion() throws IOException {
        Properties p = new Properties();
        p.setProperty("db.url", url);
        p.setProperty("db.usuario", usuario);
        p.setProperty("db.clave", clave);
        try (OutputStream out = Files.newOutputStream(Paths.get(ARCHIVO_CONFIG).toAbsolutePath())) {
            p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "Configuracion de GameShelf");
        }
    }
}
