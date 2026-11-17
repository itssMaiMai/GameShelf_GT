package gameshelf.persistence;

import gameshelf.model.Rol;
import gameshelf.model.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Repositorio de cuentas de usuario sobre la base de datos. El nombre es único sin distinguir mayúsculas. */
public class UsuarioRepository {
    private final ConexionDB db;

    public UsuarioRepository(ConexionDB db) {
        this.db = db;
    }

    public synchronized int cantidad() {
        try (Statement st = db.conectar().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo leer la tabla de usuarios", e);
        }
    }

    /** Busca por nombre sin distinguir mayúsculas. */
    public synchronized Usuario buscarPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return null;
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT id, nombre, rol, salt, hash FROM usuario WHERE nombre = ?")) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                u.setRol(Rol.desdeTexto(rs.getString("rol")));
                u.setSalt(rs.getString("salt"));
                u.setHash(rs.getString("hash"));
                return u;
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo leer el usuario de la base de datos", e);
        }
    }

    /** Inserta el usuario y le asigna el id generado por la base. */
    public synchronized void guardar(Usuario u) {
        try (PreparedStatement ps = db.conectar().prepareStatement(
                "INSERT INTO usuario (nombre, rol, salt, hash) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getRol().name());
            ps.setString(3, u.getSalt());
            ps.setString(4, u.getHash());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo guardar el usuario en la base de datos", e);
        }
    }
}
