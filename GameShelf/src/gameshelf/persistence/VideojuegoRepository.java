package gameshelf.persistence;

import gameshelf.model.Videojuego;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Repositorio de videojuegos (biblioteca compartida) sobre la base de datos. */
public class VideojuegoRepository {
    private static final String COLUMNAS = "id, nombre, plataforma, genero, desarrollador, anio, fecha_creacion, creado_por, imagen";

    private final ConexionDB db;

    public VideojuegoRepository(ConexionDB db) {
        this.db = db;
    }

    /** Inserta el juego y le asigna el id generado por la base. */
    public synchronized void guardar(Videojuego v) {
        String sql = "INSERT INTO videojuego (nombre, plataforma, genero, desarrollador, anio, fecha_creacion, creado_por, imagen)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.conectar().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getNombre());
            ps.setString(2, v.getPlataforma());
            ps.setString(3, v.getGenero());
            ps.setString(4, v.getDesarrollador());
            ps.setInt(5, v.getAnio());
            ps.setTimestamp(6, Timestamp.valueOf(v.getFechaCreacion() == null ? LocalDateTime.now() : v.getFechaCreacion()));
            ps.setString(7, v.getCreadoPor());
            ps.setString(8, v.getImagen());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo guardar el videojuego en la base de datos", e);
        }
    }

    public synchronized void actualizar(Videojuego v) {
        String sql = "UPDATE videojuego SET nombre = ?, plataforma = ?, genero = ?, desarrollador = ?, anio = ?, imagen = ? WHERE id = ?";
        try (PreparedStatement ps = db.conectar().prepareStatement(sql)) {
            ps.setString(1, v.getNombre());
            ps.setString(2, v.getPlataforma());
            ps.setString(3, v.getGenero());
            ps.setString(4, v.getDesarrollador());
            ps.setInt(5, v.getAnio());
            ps.setString(6, v.getImagen());
            ps.setInt(7, v.getId());
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("El videojuego no existe.");
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo actualizar el videojuego en la base de datos", e);
        }
    }

    /** Elimina el juego (la base borra en cascada su seguimiento y sus calificaciones). @return true si existía. */
    public synchronized boolean eliminar(int id) {
        try (PreparedStatement ps = db.conectar().prepareStatement("DELETE FROM videojuego WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo eliminar el videojuego de la base de datos", e);
        }
    }

    public synchronized Videojuego buscarPorId(int id) {
        try (PreparedStatement ps = db.conectar().prepareStatement("SELECT " + COLUMNAS + " FROM videojuego WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? fila(rs) : null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo leer el videojuego de la base de datos", e);
        }
    }

    public synchronized List<Videojuego> obtenerTodos() {
        List<Videojuego> lista = new ArrayList<>();
        try (Statement st = db.conectar().createStatement();
             ResultSet rs = st.executeQuery("SELECT " + COLUMNAS + " FROM videojuego ORDER BY id")) {
            while (rs.next()) {
                lista.add(fila(rs));
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudieron leer los videojuegos de la base de datos", e);
        }
        return lista;
    }

    private static Videojuego fila(ResultSet rs) throws SQLException {
        Videojuego v = new Videojuego();
        v.setId(rs.getInt("id"));
        v.setNombre(rs.getString("nombre"));
        v.setPlataforma(rs.getString("plataforma"));
        v.setGenero(rs.getString("genero"));
        v.setDesarrollador(rs.getString("desarrollador"));
        v.setAnio(rs.getInt("anio"));
        v.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
        v.setCreadoPor(rs.getString("creado_por"));
        v.setImagen(rs.getString("imagen"));
        return v;
    }
}
