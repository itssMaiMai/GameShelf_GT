package gameshelf.view;

import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.EntradaRanking;
import gameshelf.model.Estado;
import gameshelf.model.Etiquetas;
import gameshelf.model.Seguimiento;
import gameshelf.model.Videojuego;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pestaña "Ranking de juegos": muestra el promedio de las calificaciones de TODOS los usuarios (solo promedios y
 * cantidad de votos, nunca las notas individuales) junto con el estado y la nota propios. Desde acá se pueden ver
 * los juegos que todavía no tienen estado y ponerles uno, calificarlos, marcarlos como favoritos, etc.
 */
public class RankingPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final String VER_TODOS = "Todos los juegos";
    private static final String VER_SIN_ESTADO = "Solo los que no tienen estado";
    private static final String VER_SIN_CALIFICAR = "Solo los que todav\u00eda no califiqu\u00e9";

    private final transient JFrame owner;
    private final transient VideojuegoController controller;
    private final transient Runnable alCambiar;

    private final FilasModelo modelo = new FilasModelo();
    private final JTable tabla = new JTable(modelo);
    private final JTextField txtBuscar = new JTextField(16);
    private final SelectorMultiple selGeneros;
    private final JComboBox<String> cmbVer = new JComboBox<>(new String[]{VER_TODOS, VER_SIN_ESTADO, VER_SIN_CALIFICAR});
    private final JLabel lblMensaje = new JLabel(" ");
    private final JLabel lblContador = new JLabel(" ");
    private final JButton btnSeguimiento = new JButton("Mi seguimiento...");
    private final JButton btnCalificar = new JButton("Calificar juego...");
    private final JButton btnFavorito = new JButton("\u2605 Favorito");
    private final List<JButton> botonesEstado = new ArrayList<>();

    private boolean actualizando;

    /**
     * @param owner ventana principal (dueña de los diálogos).
     * @param alCambiar se invoca después de cualquier cambio, para que la ventana principal refresque todo.
     */
    public RankingPanel(JFrame owner, VideojuegoController controller, Runnable alCambiar) {
        this.owner = owner;
        this.controller = controller;
        this.alCambiar = alCambiar;
        this.selGeneros = new SelectorMultiple("G\u00e9neros", () -> {
            if (!actualizando) refrescar();
        });
        construir();
        configurarTabla();
    }

    // ------------------------------------------------------------------ construcción

    private void construir() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel explicacion = new JLabel("<html><b>Ranking de juegos</b> \u2013 promedio de las calificaciones de todos los usuarios.<br>"
                + "Filtr\u00e1 por \"sin estado\" para ver los juegos que todav\u00eda no tocaste y ponerles un estado o calificarlos.</html>");
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filtro.add(new JLabel("Buscar por nombre o desarrollador:"));
        filtro.add(txtBuscar);
        filtro.add(selGeneros);
        filtro.add(new JLabel("Mostrar:"));
        filtro.add(cmbVer);
        JButton btnQuitarFiltros = new JButton("Quitar filtros");
        btnQuitarFiltros.addActionListener(e -> quitarFiltros());
        filtro.add(btnQuitarFiltros);
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { if (!actualizando) refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { if (!actualizando) refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { if (!actualizando) refrescar(); }
        });
        cmbVer.addActionListener(e -> {
            if (!actualizando) refrescar();
        });

        JPanel norte = new JPanel(new BorderLayout(0, 4));
        norte.add(explicacion, BorderLayout.NORTH);
        norte.add(filtro, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        btnSeguimiento.addActionListener(e -> miSeguimiento());
        btnCalificar.addActionListener(e -> calificar());
        btnFavorito.addActionListener(e -> alternarFavorito());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        acciones.add(btnSeguimiento);
        acciones.add(btnCalificar);
        acciones.add(btnFavorito);

        JPanel estados = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        estados.setBorder(BorderFactory.createTitledBorder("Poner estado al juego seleccionado"));
        for (Estado estado : Estado.values()) {
            estados.add(botonEstado(estado.toString(), estado));
        }
        estados.add(botonEstado("Sin estado", null));

        JPanel estadoBarra = new JPanel(new BorderLayout());
        estadoBarra.add(lblMensaje, BorderLayout.WEST);
        estadoBarra.add(lblContador, BorderLayout.EAST);

        JPanel centroSur = new JPanel(new BorderLayout(0, 4));
        centroSur.add(acciones, BorderLayout.NORTH);
        centroSur.add(estados, BorderLayout.CENTER);
        centroSur.add(estadoBarra, BorderLayout.SOUTH);
        add(centroSur, BorderLayout.SOUTH);
    }

    private JButton botonEstado(String texto, Estado estado) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> ponerEstado(estado));
        botonesEstado.add(boton);
        return boton;
    }

    private void configurarTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        Centrada centrada = new Centrada();
        for (int col : new int[]{FilasModelo.COL_POSICION, FilasModelo.COL_PROMEDIO, FilasModelo.COL_VOTOS, FilasModelo.COL_MI_NOTA}) {
            tabla.getColumnModel().getColumn(col).setCellRenderer(centrada);
        }
        tabla.getColumnModel().getColumn(FilasModelo.COL_MI_ESTADO).setCellRenderer(new EstadoRenderer());

        int[] anchos = {45, 230, 120, 140, 150, 90, 60, 100, 70};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(FilasModelo.COL_POSICION).setMaxWidth(60);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarBotones();
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0) miSeguimiento();
            }
        });
    }

    // ------------------------------------------------------------------ datos

    /** Deja todos los filtros de la pestaña sin aplicar: búsqueda, géneros y "Mostrar". */
    private void quitarFiltros() {
        actualizando = true;
        try {
            txtBuscar.setText("");
            selGeneros.limpiar();
            cmbVer.setSelectedIndex(0);
        } finally {
            actualizando = false;
        }
        refrescar();
    }

    /** Vuelve a leer el ranking y los datos propios, conservando la selección si el juego sigue visible. */
    public void refrescar() {
        Videojuego anterior = seleccionado();
        Integer idAnterior = anterior == null ? null : anterior.getId();

        List<EntradaRanking> ranking = controller.ranking();
        Map<Integer, Seguimiento> mios = controller.miSeguimiento();
        Map<Integer, Integer> notas = controller.misPuntuaciones();
        selGeneros.setOpciones(controller.generos());
        List<String> generosMarcados = selGeneros.getMarcadas();
        Object ver = cmbVer.getSelectedItem();
        String texto = normalizar(txtBuscar.getText());

        List<Fila> filas = new ArrayList<>();
        int posicion = 0;
        for (EntradaRanking e : ranking) {
            int id = e.getJuego().getId();
            int lugar = e.hayVotos() ? ++posicion : 0;       // la posición es la del ranking general, no la del filtro
            Seguimiento s = mios.get(id);
            Estado estado = s == null ? null : s.getEstado();
            Integer nota = notas.get(id);
            if (!texto.isEmpty() && !normalizar(e.getJuego().getNombre()).contains(texto)
                    && !normalizar(e.getJuego().getDesarrollador()).contains(texto)) continue;
            if (!generosMarcados.isEmpty() && !tieneAlgunGenero(e.getJuego().getGenero(), generosMarcados)) continue;
            if (VER_SIN_ESTADO.equals(ver) && estado != null) continue;
            if (VER_SIN_CALIFICAR.equals(ver) && nota != null) continue;
            filas.add(new Fila(lugar, e, estado, nota == null ? 0 : nota));
        }
        modelo.setFilas(filas);

        if (idAnterior != null) {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (modelo.getFila(i).entrada.getJuego().getId() == idAnterior) {
                    int vista = tabla.convertRowIndexToView(i);
                    tabla.setRowSelectionInterval(vista, vista);
                    break;
                }
            }
        }
        lblContador.setText("Mostrando " + filas.size() + " de " + ranking.size() + (ranking.size() == 1 ? " juego" : " juegos"));
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean hay = seleccionado() != null;
        btnSeguimiento.setEnabled(hay);
        btnCalificar.setEnabled(hay);
        btnFavorito.setEnabled(hay);
        for (JButton b : botonesEstado) {
            b.setEnabled(hay);
        }
    }

    private Videojuego seleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) return null;
        int fila = tabla.convertRowIndexToModel(vista);
        return fila < 0 || fila >= modelo.getRowCount() ? null : modelo.getFila(fila).entrada.getJuego();
    }

    // ------------------------------------------------------------------ acciones

    private void miSeguimiento() {
        Videojuego v = seleccionado();
        if (v == null) {
            avisarSinSeleccion();
            return;
        }
        SeguimientoDialog dialogo = new SeguimientoDialog(owner, controller, v,
                controller.miSeguimiento().get(v.getId()), controller.misPuntuaciones().get(v.getId()));
        dialogo.setVisible(true);
        if (dialogo.isGuardado()) {
            alCambiar.run();
            mensaje("\"" + v.getNombre() + "\": " + dialogo.getMensaje());
        }
    }

    private void calificar() {
        Videojuego v = seleccionado();
        Integer idSeleccionado = v == null ? null : v.getId();
        // Se ofrecen los juegos sin calificar y, si hay uno seleccionado, también ese (para poder cambiarle la nota).
        List<Videojuego> opciones = controller.juegosParaCalificar(idSeleccionado);
        if (opciones.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ya calificaste todos los juegos de la biblioteca.\n"
                    + "Seleccion\u00e1 uno de la tabla para cambiar su calificaci\u00f3n.", "Calificar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CalificarDialog dialogo = new CalificarDialog(owner, controller, opciones, idSeleccionado, controller.misPuntuaciones());
        dialogo.setVisible(true);
        Videojuego calificado = dialogo.getJuegoCalificado();
        if (dialogo.isCalificado() && calificado != null) {
            alCambiar.run();
            mensaje("Calificaste \"" + calificado.getNombre() + "\".");
        }
    }

    private void alternarFavorito() {
        Videojuego v = seleccionado();
        if (v == null) {
            avisarSinSeleccion();
            return;
        }
        Resultado<Seguimiento> r = controller.alternarFavorito(v.getId());
        alCambiar.run();
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo actualizar", JOptionPane.ERROR_MESSAGE);
        } else {
            mensaje("\"" + v.getNombre() + "\": " + r.getMensaje());
        }
    }

    private void ponerEstado(Estado estado) {
        Videojuego v = seleccionado();
        if (v == null) {
            avisarSinSeleccion();
            return;
        }
        Resultado<Seguimiento> r = controller.cambiarEstado(v.getId(), estado);
        alCambiar.run();
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo cambiar el estado", JOptionPane.ERROR_MESSAGE);
        } else {
            mensaje("\"" + v.getNombre() + "\": " + (estado == null ? "sin estado" : estado.toString()) + ".");
        }
    }

    private static boolean tieneAlgunGenero(String generosDelJuego, List<String> marcados) {
        for (String g : marcados) {
            if (Etiquetas.contiene(generosDelJuego, g)) {
                return true;
            }
        }
        return false;
    }

    /** Minúsculas y sin acentos, para buscar sin distinguir mayúsculas ni tildes. */
    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").trim().toLowerCase(Locale.ROOT);
    }

    private void avisarSinSeleccion() {
        JOptionPane.showMessageDialog(this, "Seleccion\u00e1 un videojuego de la tabla.", "Sin selecci\u00f3n", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mensaje(String texto) {
        lblMensaje.setText(texto);
    }

    // ------------------------------------------------------------------ tabla

    /** Una fila del ranking: la entrada (juego, promedio, votos) más el estado y la nota del usuario actual. */
    private static final class Fila {
        final int posicion;
        final EntradaRanking entrada;
        final Estado miEstado;
        final int miNota;

        Fila(int posicion, EntradaRanking entrada, Estado miEstado, int miNota) {
            this.posicion = posicion;
            this.entrada = entrada;
            this.miEstado = miEstado;
            this.miNota = miNota;
        }
    }

    private static final class FilasModelo extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        static final int COL_POSICION = 0;
        static final int COL_PROMEDIO = 5;
        static final int COL_VOTOS = 6;
        static final int COL_MI_ESTADO = 7;
        static final int COL_MI_NOTA = 8;

        private static final String[] COLUMNAS = {"#", "Nombre", "Plataforma", "G\u00e9nero", "Desarrollador", "Promedio", "Votos", "Mi estado", "Mi nota"};
        private List<Fila> filas = new ArrayList<>();

        void setFilas(List<Fila> filas) {
            this.filas = new ArrayList<>(filas);
            fireTableDataChanged();
        }

        Fila getFila(int indice) {
            return filas.get(indice);
        }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return COLUMNAS.length; }
        @Override public String getColumnName(int c) { return COLUMNAS[c]; }
        @Override public boolean isCellEditable(int f, int c) { return false; }

        @Override
        public Class<?> getColumnClass(int c) {
            switch (c) {
                case COL_POSICION:
                case COL_VOTOS:
                case COL_MI_NOTA:
                    return Integer.class;
                case COL_PROMEDIO:
                    return Double.class;
                case COL_MI_ESTADO:
                    return Estado.class;
                default:
                    return String.class;
            }
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Fila f = filas.get(fila);
            Videojuego v = f.entrada.getJuego();
            switch (columna) {
                case COL_POSICION: return f.posicion;
                case 1: return v.getNombre();
                case 2: return v.getPlataforma();
                case 3: return v.getGenero();
                case 4: return v.getDesarrollador();
                case COL_PROMEDIO: return f.entrada.hayVotos() ? Double.valueOf(f.entrada.getPromedio()) : null;
                case COL_VOTOS: return f.entrada.getVotos();
                case COL_MI_ESTADO: return f.miEstado;
                case COL_MI_NOTA: return f.miNota;
                default: return null;
            }
        }
    }

    /** Centra el valor; muestra "—" para vacíos y ceros, y los promedios con un decimal. */
    private static final class Centrada extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        Centrada() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        protected void setValue(Object valor) {
            if (valor == null || (valor instanceof Integer && ((Integer) valor) == 0)) {
                setText("\u2014");
            } else if (valor instanceof Double) {
                setText(String.format("%.1f", (Double) valor));
            } else {
                setText(valor.toString());
            }
        }
    }

    private static final class EstadoRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionado,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionado, foco, fila, columna);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (valor == null) {
                setText("\u2014");
            }
            if (!seleccionado) {
                c.setForeground(valor instanceof Estado ? MainFrame.colorDe((Estado) valor) : t.getForeground());
            }
            return c;
        }
    }
}
