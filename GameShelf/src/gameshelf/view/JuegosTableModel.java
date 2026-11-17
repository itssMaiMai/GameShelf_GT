package gameshelf.view;

import gameshelf.model.Estado;
import gameshelf.model.Seguimiento;
import gameshelf.model.Videojuego;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Modelo de tabla de solo lectura para mostrar la lista de videojuegos. */
public class JuegosTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    public static final int COL_FAVORITO = 0;
    public static final int COL_NOMBRE = 1;
    public static final int COL_PLATAFORMA = 2;
    public static final int COL_GENERO = 3;
    public static final int COL_DESARROLLADOR = 4;
    public static final int COL_ANIO = 5;
    public static final int COL_ESTADO = 6;
    public static final int COL_HORAS = 7;
    public static final int COL_PUNTUACION = 8;

    private static final String[] COLUMNAS = {
            "\u2605", "Nombre", "Plataforma", "G\u00e9nero", "Desarrollador", "A\u00f1o", "Estado", "Horas", "Mi puntuaci\u00f3n"
    };

    private List<Videojuego> juegos = new ArrayList<>();
    private Map<Integer, Integer> puntuaciones = Collections.emptyMap();
    private Map<Integer, Seguimiento> seguimientos = Collections.emptyMap();

    /**
     * @param puntuaciones calificaciones del usuario actual (idJuego -> puntuación); 0 = sin calificar.
     * @param seguimientos datos personales del usuario actual (estado, horas, favorito); sin registro = sin estado.
     */
    public void setJuegos(List<Videojuego> juegos, Map<Integer, Integer> puntuaciones, Map<Integer, Seguimiento> seguimientos) {
        this.juegos = new ArrayList<>(juegos);
        this.puntuaciones = puntuaciones == null ? Collections.<Integer, Integer>emptyMap() : puntuaciones;
        this.seguimientos = seguimientos == null ? Collections.<Integer, Seguimiento>emptyMap() : seguimientos;
        fireTableDataChanged();
    }

    public Videojuego getJuegoAt(int fila) {
        return juegos.get(fila);
    }

    @Override
    public int getRowCount() {
        return juegos.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Class<?> getColumnClass(int columna) {
        switch (columna) {
            case COL_ANIO:
            case COL_PUNTUACION:
                return Integer.class;
            case COL_HORAS:
                return Double.class;
            case COL_ESTADO:
                return Estado.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Videojuego v = juegos.get(fila);
        Seguimiento s = seguimientos.get(v.getId());
        switch (columna) {
            case COL_FAVORITO: return s != null && s.isFavorito() ? "\u2605" : "";
            case COL_NOMBRE: return v.getNombre();
            case COL_PLATAFORMA: return v.getPlataforma();
            case COL_GENERO: return v.getGenero();
            case COL_DESARROLLADOR: return v.getDesarrollador();
            case COL_ANIO: return v.getAnio();
            case COL_ESTADO: return s == null ? null : s.getEstado();
            case COL_HORAS: return s == null ? null : s.getHorasJugadas();
            case COL_PUNTUACION: return puntuaciones.containsKey(v.getId()) ? puntuaciones.get(v.getId()) : 0;
            default: return null;
        }
    }
}
