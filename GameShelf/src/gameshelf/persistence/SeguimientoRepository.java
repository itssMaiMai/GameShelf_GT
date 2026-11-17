package gameshelf.persistence;

import gameshelf.model.Estado;
import gameshelf.model.Seguimiento;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Datos personales por usuario: estado, horas, favorito y notas de cada usuario sobre cada juego.
 * Por diseño solo permite consultar los registros de UN usuario concreto (el que tiene la sesión iniciada).
 */
public class SeguimientoRepository {
    private final ConexionDB db;

    public SeguimientoRepository(ConexionDB db) {
        this.db = db;
    }

    /** @return idJuego -> seguimiento, solo del usuario indicado. */
    public synchronized Map<Integer, Seguimiento> deUsuario(int usuarioId) {
        Map<Integer, Seguimiento> mapa = new HashMap<>();
        String sql = "SELECT videojuego_id, estado, horas_jugadas, favorito, notas FROM seguimiento WHERE usuario_id = ?";
        try (PreparedStatement ps = db.conectar().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seguimiento s = new Seguimiento();
                    s.setUsuarioId(usuarioId);
                    s.setVideojuegoId(rs.getInt("videojuego_id"));
                    String estado = rs.getString("estado");
                    s.setEstado(estado == null || estado.trim().isEmpty() ? null : Estado.desdeTexto(estado));
                    double horas = rs.getDouble("horas_jugadas");
                    s.setHorasJugadas(rs.wasNull() ? null : Double.valueOf(horas));
                    s.setFavorito(rs.getBoolean("favorito"));
                    s.setNotas(rs.getString("notas"));
                    mapa.put(s.getVideojuegoId(), s);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo leer el seguimiento de la base de datos", e);
        }
        return mapa;
    }

    /** Crea o reemplaza el seguimiento de ese usuario para ese juego. */
    public synchronized void guardar(Seguimiento s) {
        try {
            int filas;
            String update = "UPDATE seguimiento SET estado = ?, horas_jugadas = ?, favorito = ?, notas = ? WHERE usuario_id = ? AND videojuego_id = ?";
            try (PreparedStatement ps = db.conectar().prepareStatement(update)) {
                parametros(ps, s);
                filas = ps.executeUpdate();
            }
            if (filas == 0) {
                String insert = "INSERT INTO seguimiento (estado, horas_jugadas, favorito, notas, usuario_id, videojuego_id) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = db.conectar().prepareStatement(insert)) {
                    parametros(ps, s);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo guardar el seguimiento en la base de datos", e);
        }
    }

    private static void parametros(PreparedStatement ps, Seguimiento s) throws SQLException {
        if (s.getEstado() == null) {
            ps.setNull(1, Types.VARCHAR);
        } else {
            ps.setString(1, s.getEstado().name());
        }
        if (s.getHorasJugadas() == null) {
            ps.setNull(2, Types.DOUBLE);
        } else {
            ps.setDouble(2, s.getHorasJugadas());
        }
        ps.setBoolean(3, s.isFavorito());
        ps.setString(4, s.getNotas());
        ps.setInt(5, s.getUsuarioId());
        ps.setInt(6, s.getVideojuegoId());
    }

    /** Borra los registros de un juego (la base ya lo hace en cascada al eliminar el juego). */
    public synchronized void eliminarDeJuego(int videojuegoId) {
        try (PreparedStatement ps = db.conectar().prepareStatement("DELETE FROM seguimiento WHERE videojuego_id = ?")) {
            ps.setInt(1, videojuegoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo borrar el seguimiento del juego", e);
        }
    }
}
