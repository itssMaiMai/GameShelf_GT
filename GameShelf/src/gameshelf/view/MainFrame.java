package gameshelf.view;

import gameshelf.Main;
import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.Estadisticas;
import gameshelf.model.Estado;
import gameshelf.model.EstadoCalificacion;
import gameshelf.model.Filtro;
import gameshelf.model.Seguimiento;
import gameshelf.model.Usuario;
import gameshelf.model.Videojuego;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ventana principal de GameShelf: estadísticas, búsqueda y filtros, tabla de juegos y acciones. */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final String TODAS = "Todas";
    private static final String TODOS = "Todos";
    private static final String SIN_ESTADO = "Sin estado";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);

    private final transient VideojuegoController controller;

    private final JuegosTableModel modelo = new JuegosTableModel();
    private final JTable tabla = new JTable(modelo);
    private final JTextField txtBuscar = new JTextField(16);
    private final JComboBox<String> cmbPlataforma = new JComboBox<>();
    private final SelectorMultiple selGeneros;
    private final JComboBox<Object> cmbEstado = new JComboBox<>();
    private final JCheckBox chkFavoritos = new JCheckBox("Solo mis favoritos");
    private final JComboBox<EstadoCalificacion> cmbCalificacion = new JComboBox<>(EstadoCalificacion.values());
    private final JTextArea txtDetalle = new JTextArea(4, 40);
    private final JLabel lblContador = new JLabel(" ");
    private final JLabel lblMensaje = new JLabel(" ");
    private final JLabel lblImagen = new JLabel("", SwingConstants.CENTER);
    private final JLabel lblSesion = new JLabel(" ");
    private final Map<String, JLabel> valoresEstadisticas = new LinkedHashMap<>();

    private final JButton btnEditar = new JButton("Editar datos del juego");
    private final JButton btnSeguimiento = new JButton("Mi seguimiento...");
    private final JButton btnEliminar = new JButton("Eliminar");
    private final JButton btnFavorito = new JButton("\u2605 Favorito");
    private final JButton btnCalificar = new JButton("Calificar juego...");
    private static final int LADO_IMAGEN = 130;

    private boolean actualizandoCombos = false;
    private RankingPanel ranking;
    private JTabbedPane pestanas;

    public MainFrame(VideojuegoController controller) {
        super("GameShelf \u2013 Administrador de biblioteca de videojuegos");
        this.controller = controller;
        this.selGeneros = new SelectorMultiple("G\u00e9neros", () -> {
            if (!actualizandoCombos) aplicarFiltros(null);
        });

        construirInterfaz();
        Usuario actual = controller.getUsuarioActual();
        lblSesion.setText(actual == null ? " " : "Sesi\u00f3n: " + actual.getNombre());
        configurarTabla();
        configurarEventos();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Sin botón "Actualizar": los cambios que hagan otros usuarios se leen solos al volver a esta ventana o cambiar de pestaña.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refrescar(null);
            }
        });
        setSize(1150, 720);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);

        refrescar(null);
    }

    // ------------------------------------------------------------------ construcción de la interfaz

    private void construirInterfaz() {
        JButton btnCerrarSesion = new JButton("Cerrar sesi\u00f3n");
        btnCerrarSesion.addActionListener(e -> cerrarSesion());
        JPanel cabecera = new JPanel(new BorderLayout(8, 0));
        cabecera.add(new JLabel("Biblioteca compartida"), BorderLayout.WEST);
        JPanel sesion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sesion.add(lblSesion);
        sesion.add(btnCerrarSesion);
        cabecera.add(sesion, BorderLayout.EAST);

        JPanel estadisticasYFiltros = new JPanel(new BorderLayout(0, 8));
        estadisticasYFiltros.add(crearPanelEstadisticas(), BorderLayout.NORTH);
        estadisticasYFiltros.add(crearPanelFiltros(), BorderLayout.CENTER);

        cabecera.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        norte.add(estadisticasYFiltros, BorderLayout.CENTER);

        txtDetalle.setEditable(false);
        txtDetalle.setLineWrap(true);
        txtDetalle.setWrapStyleWord(true);
        txtDetalle.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        lblImagen.setPreferredSize(new Dimension(LADO_IMAGEN + 8, LADO_IMAGEN + 8));
        lblImagen.setBorder(BorderFactory.createLineBorder(new Color(0x9E9E9E)));
        JPanel marcoImagen = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        marcoImagen.add(lblImagen);

        JPanel scrollDetalle = new JPanel(new BorderLayout());
        scrollDetalle.setBorder(BorderFactory.createTitledBorder("Detalle del juego seleccionado y mis datos"));
        scrollDetalle.add(marcoImagen, BorderLayout.WEST);
        scrollDetalle.add(new JScrollPane(txtDetalle), BorderLayout.CENTER);
        scrollDetalle.setPreferredSize(new Dimension(100, LADO_IMAGEN + 50));

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(scrollDetalle, BorderLayout.SOUTH);

        JButton btnAgregar = new JButton("Agregar juego");
        btnAgregar.addActionListener(e -> agregar());
        btnEditar.addActionListener(e -> editar());
        btnSeguimiento.setToolTipText("Tu estado, horas, favorito, notas y calificaci\u00f3n de este juego (son privados)");
        btnSeguimiento.addActionListener(e -> miSeguimiento());
        btnEliminar.addActionListener(e -> eliminar());
        btnFavorito.addActionListener(e -> alternarFavorito());
        btnCalificar.setToolTipText("Califica un juego o cambia tu calificaci\u00f3n (es privada)");
        btnCalificar.addActionListener(e -> calificar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.add(btnAgregar);
        botones.add(btnSeguimiento);
        botones.add(btnFavorito);
        botones.add(btnCalificar);
        botones.add(btnEditar);
        botones.add(btnEliminar);

        JPanel estado = new JPanel(new BorderLayout());
        estado.add(lblMensaje, BorderLayout.WEST);
        estado.add(lblContador, BorderLayout.EAST);

        JPanel sur = new JPanel(new BorderLayout(0, 6));
        sur.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        sur.add(botones, BorderLayout.NORTH);
        sur.add(estado, BorderLayout.SOUTH);

        JPanel panelBiblioteca = new JPanel(new BorderLayout());
        panelBiblioteca.add(norte, BorderLayout.NORTH);
        panelBiblioteca.add(centro, BorderLayout.CENTER);
        panelBiblioteca.add(sur, BorderLayout.SOUTH);

        ranking = new RankingPanel(this, controller, () -> refrescar(null));
        pestanas = new JTabbedPane();
        pestanas.addTab("Biblioteca", panelBiblioteca);
        pestanas.addTab("Ranking de juegos", ranking);
        pestanas.addChangeListener(e -> refrescar(null));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cabecera, BorderLayout.NORTH);
        getContentPane().add(pestanas, BorderLayout.CENTER);
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new GridLayout(1, 0, 8, 0));
        String[] titulos = {"Total", "Terminados", "Jugando", "Pendientes", "Abandonados", "Sin estado", "Favoritos", "Horas jugadas", "Mi nota media"};
        for (String titulo : titulos) {
            JLabel valor = new JLabel("0", SwingConstants.CENTER);
            valor.setFont(valor.getFont().deriveFont(Font.BOLD, 20f));
            JLabel etiqueta = new JLabel(titulo, SwingConstants.CENTER);
            etiqueta.setFont(etiqueta.getFont().deriveFont(11f));

            JPanel tarjeta = new JPanel(new BorderLayout());
            tarjeta.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x9E9E9E)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            tarjeta.add(valor, BorderLayout.CENTER);
            tarjeta.add(etiqueta, BorderLayout.SOUTH);
            panel.add(tarjeta);
            valoresEstadisticas.put(titulo, valor);
        }
        return panel;
    }

    private JPanel crearPanelFiltros() {
        cmbEstado.addItem(TODOS);
        cmbEstado.addItem(SIN_ESTADO);
        for (Estado e : Estado.values()) {
            cmbEstado.addItem(e);
        }
        JButton btnLimpiar = new JButton("Quitar filtros");
        btnLimpiar.addActionListener(e -> limpiarFiltros());

        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        fila1.add(new JLabel("Buscar por nombre o desarrollador:"));
        fila1.add(txtBuscar);
        fila1.add(Box.createHorizontalStrut(6));
        fila1.add(new JLabel("Plataforma:"));
        fila1.add(cmbPlataforma);
        fila1.add(selGeneros);
        fila1.add(new JLabel("Mi estado:"));
        fila1.add(cmbEstado);

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        fila2.add(new JLabel("Mi calificaci\u00f3n:"));
        fila2.add(cmbCalificacion);
        fila2.add(chkFavoritos);
        fila2.add(btnLimpiar);

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 0));
        panel.add(fila1);
        panel.add(fila2);
        return panel;
    }

    private void configurarTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object valor) {
                if (valor == null || (valor instanceof Integer && ((Integer) valor) == 0)) {
                    setText("\u2014");
                } else if (valor instanceof Double) {
                    double d = (Double) valor;
                    setText(d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.1f", d));
                } else {
                    setText(valor.toString());
                }
            }
        };
        centrado.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer renderEstado = new DefaultTableCellRenderer() {
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
                    c.setForeground(valor instanceof Estado ? colorDe((Estado) valor) : t.getForeground());
                }
                return c;
            }
        };

        tabla.getColumnModel().getColumn(JuegosTableModel.COL_FAVORITO).setCellRenderer(centrado);
        tabla.getColumnModel().getColumn(JuegosTableModel.COL_ANIO).setCellRenderer(centrado);
        tabla.getColumnModel().getColumn(JuegosTableModel.COL_HORAS).setCellRenderer(centrado);
        tabla.getColumnModel().getColumn(JuegosTableModel.COL_PUNTUACION).setCellRenderer(centrado);
        tabla.getColumnModel().getColumn(JuegosTableModel.COL_ESTADO).setCellRenderer(renderEstado);

        int[] anchos = {40, 260, 120, 110, 150, 60, 100, 70, 90};
        for (int i = 0; i < anchos.length; i++) {
            TableColumn col = tabla.getColumnModel().getColumn(i);
            col.setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(JuegosTableModel.COL_FAVORITO).setMaxWidth(50);
    }

    static Color colorDe(Estado estado) {
        switch (estado) {
            case JUGANDO: return new Color(0x1E88E5);
            case TERMINADO: return new Color(0x43A047);
            case ABANDONADO: return new Color(0xE53935);
            default: return new Color(0x9E9E9E);
        }
    }

    private void configurarEventos() {
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltros(null); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltros(null); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltros(null); }
        });
        cmbPlataforma.addActionListener(e -> { if (!actualizandoCombos) aplicarFiltros(null); });
        cmbEstado.addActionListener(e -> { if (!actualizandoCombos) aplicarFiltros(null); });
        chkFavoritos.addActionListener(e -> aplicarFiltros(null));
        cmbCalificacion.addActionListener(e -> { if (!actualizandoCombos) aplicarFiltros(null); });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarSeleccion();
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0) miSeguimiento();
            }
        });
        tabla.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "eliminar");
        tabla.getActionMap().put("eliminar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (controller.esAdmin()) {
                    eliminar();
                }
            }
        });
    }

    // ------------------------------------------------------------------ acciones

    private void agregar() {
        JuegoDialog dialogo = new JuegoDialog(this, controller, null);
        dialogo.setVisible(true);
        Videojuego nuevo = dialogo.getGuardado();
        if (nuevo != null) {
            // Si los filtros activos ocultarían el juego nuevo, se limpian para que el usuario lo vea.
            refrescar(nuevo.getId());
            if (tabla.getSelectedRow() < 0) {
                limpiarFiltros();
                refrescar(nuevo.getId());
            }
            mensaje("Videojuego \"" + nuevo.getNombre() + "\" agregado a la biblioteca.");
        }
    }

    /** Modifica los datos del juego: solo administrador. */
    private void editar() {
        if (!controller.esAdmin()) {
            JOptionPane.showMessageDialog(this, "Solo un administrador puede modificar los datos de un juego.",
                    "Acci\u00f3n no permitida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Videojuego seleccionado = juegoSeleccionado();
        if (seleccionado == null) {
            avisarSinSeleccion("editar");
            return;
        }
        JuegoDialog dialogo = new JuegoDialog(this, controller, seleccionado);
        dialogo.setVisible(true);
        Videojuego editado = dialogo.getGuardado();
        if (editado != null) {
            refrescar(editado.getId());
            mensaje("Cambios en \"" + editado.getNombre() + "\" guardados.");
        }
    }

    /** Estado, horas, favorito y notas del usuario actual sobre el juego seleccionado. */
    private void miSeguimiento() {
        Videojuego seleccionado = juegoSeleccionado();
        if (seleccionado == null) {
            avisarSinSeleccion("ver tu seguimiento");
            return;
        }
        SeguimientoDialog dialogo = new SeguimientoDialog(this, controller, seleccionado,
                controller.miSeguimiento().get(seleccionado.getId()), controller.misPuntuaciones().get(seleccionado.getId()));
        dialogo.setVisible(true);
        if (dialogo.isGuardado()) {
            refrescar(seleccionado.getId());
            mensaje("\"" + seleccionado.getNombre() + "\": " + dialogo.getMensaje());
        }
    }

    private void eliminar() {
        if (!controller.esAdmin()) {
            JOptionPane.showMessageDialog(this, "Solo un administrador puede dar de baja un juego de la plataforma.",
                    "Acci\u00f3n no permitida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Videojuego seleccionado = juegoSeleccionado();
        if (seleccionado == null) {
            avisarSinSeleccion("eliminar");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "\u00bfEliminar \"" + seleccionado.getNombre() + "\" de tu biblioteca?\nEsta acci\u00f3n no se puede deshacer.",
                "Confirmar eliminaci\u00f3n", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        Resultado<Void> r = controller.eliminar(seleccionado.getId());
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
        } else {
            mensaje("\"" + seleccionado.getNombre() + "\" eliminado de la biblioteca.");
        }
        refrescar(null);
    }

    private void alternarFavorito() {
        Videojuego seleccionado = juegoSeleccionado();
        if (seleccionado == null) {
            avisarSinSeleccion("marcar como favorito");
            return;
        }
        Resultado<Seguimiento> r = controller.alternarFavorito(seleccionado.getId());
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo actualizar", JOptionPane.ERROR_MESSAGE);
            refrescar(null);
            return;
        }
        refrescar(seleccionado.getId());
        mensaje(r.getMensaje());
    }

    private void calificar() {
        // Se ofrecen los juegos sin calificar y, si hay uno seleccionado, también ese (para poder cambiarle la nota).
        List<Videojuego> opciones = controller.juegosParaCalificar(idSeleccionado());
        if (opciones.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ya calificaste todos los juegos de la biblioteca.\n"
                    + "Seleccion\u00e1 uno de la tabla para cambiar su calificaci\u00f3n.", "Calificar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CalificarDialog dialogo = new CalificarDialog(this, controller, opciones, idSeleccionado(), controller.misPuntuaciones());
        dialogo.setVisible(true);
        Videojuego calificado = dialogo.getJuegoCalificado();
        if (dialogo.isCalificado() && calificado != null) {
            refrescar(calificado.getId());
            mensaje("Calificaste \"" + calificado.getNombre() + "\".");
        }
    }

    private void cerrarSesion() {
        controller.cerrarSesion();
        setVisible(false);
        Main.abrirConLogin(controller);
        dispose();
    }

    private void limpiarFiltros() {
        actualizandoCombos = true;
        try {
            txtBuscar.setText("");
            cmbPlataforma.setSelectedIndex(0);
            selGeneros.limpiar();
            cmbEstado.setSelectedIndex(0);
            chkFavoritos.setSelected(false);
            cmbCalificacion.setSelectedIndex(0);
        } finally {
            actualizandoCombos = false;
        }
        aplicarFiltros(null);
    }

    private void avisarSinSeleccion(String accion) {
        JOptionPane.showMessageDialog(this, "Seleccion\u00e1 un videojuego de la tabla para " + accion + ".",
                "Sin selecci\u00f3n", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------ actualización de la vista

    /** Recarga combos, tabla y estadísticas; opcionalmente deja seleccionado el juego con ese id. */
    private void refrescar(Integer idASeleccionar) {
        Integer id = idASeleccionar != null ? idASeleccionar : idSeleccionado();
        actualizandoCombos = true;
        try {
            rellenarCombo(cmbPlataforma, TODAS, controller.plataformas());
            selGeneros.setOpciones(controller.generos());
        } finally {
            actualizandoCombos = false;
        }
        aplicarFiltros(id);
        actualizarEstadisticas();
        if (ranking != null) {
            ranking.refrescar();
        }
    }

    private static void rellenarCombo(JComboBox<String> combo, String opcionTodos, List<String> valores) {
        Object anterior = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(opcionTodos);
        for (String valor : valores) {
            combo.addItem(valor);
        }
        if (anterior != null) {
            combo.setSelectedItem(anterior);
        }
        if (combo.getSelectedIndex() < 0) {
            combo.setSelectedIndex(0);
        }
    }

    private Filtro construirFiltro() {
        Filtro f = new Filtro();
        f.setTexto(txtBuscar.getText());
        Object plataforma = cmbPlataforma.getSelectedItem();
        if (plataforma != null && !TODAS.equals(plataforma)) f.setPlataforma(plataforma.toString());
        f.setGeneros(selGeneros.getMarcadas());
        Object estado = cmbEstado.getSelectedItem();
        if (estado instanceof Estado) f.setEstado((Estado) estado);
        f.setSinEstado(SIN_ESTADO.equals(estado));
        f.setSoloFavoritos(chkFavoritos.isSelected());
        f.setCalificacion((EstadoCalificacion) cmbCalificacion.getSelectedItem());
        return f;
    }

    private void aplicarFiltros(Integer idASeleccionar) {
        Integer id = idASeleccionar != null ? idASeleccionar : idSeleccionado();
        List<Videojuego> lista = controller.listar(construirFiltro());
        modelo.setJuegos(lista, controller.misPuntuaciones(), controller.miSeguimiento());
        if (id != null) {
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (modelo.getJuegoAt(fila).getId() == id) {
                    int vista = tabla.convertRowIndexToView(fila);
                    tabla.setRowSelectionInterval(vista, vista);
                    tabla.scrollRectToVisible(tabla.getCellRect(vista, 0, true));
                    break;
                }
            }
        }
        int total = controller.estadisticas().getTotal();
        lblContador.setText("Mostrando " + lista.size() + " de " + total + (total == 1 ? " juego" : " juegos"));
        actualizarSeleccion();
    }

    private void actualizarEstadisticas() {
        Estadisticas e = controller.estadisticas();
        valoresEstadisticas.get("Total").setText(String.valueOf(e.getTotal()));
        valoresEstadisticas.get("Terminados").setText(String.valueOf(e.getTerminados()));
        valoresEstadisticas.get("Jugando").setText(String.valueOf(e.getJugando()));
        valoresEstadisticas.get("Pendientes").setText(String.valueOf(e.getPendientes()));
        valoresEstadisticas.get("Abandonados").setText(String.valueOf(e.getAbandonados()));
        valoresEstadisticas.get("Sin estado").setText(String.valueOf(e.getSinEstado()));
        valoresEstadisticas.get("Favoritos").setText(String.valueOf(e.getFavoritos()));
        double horas = e.getHorasTotales();
        valoresEstadisticas.get("Horas jugadas").setText(horas == Math.floor(horas)
                ? String.valueOf((long) horas) : String.format("%.1f", horas));
        valoresEstadisticas.get("Mi nota media").setText(e.getCalificados() == 0
                ? "\u2014" : String.format("%.1f", e.getPromedioPuntuacion()));
    }

    private void actualizarSeleccion() {
        Videojuego v = juegoSeleccionado();
        boolean hay = v != null;
        // Editar datos y Eliminar solo existen para el administrador: un usuario normal ni siquiera los ve.
        btnEditar.setVisible(controller.esAdmin());
        btnEditar.setEnabled(hay && controller.esAdmin());
        btnSeguimiento.setEnabled(hay);
        // El botón Eliminar solo existe para el administrador: un usuario normal ni siquiera lo ve.
        btnEliminar.setVisible(controller.esAdmin());
        btnEliminar.setEnabled(hay && controller.esAdmin());
        btnFavorito.setEnabled(hay);
        if (!hay) {
            txtDetalle.setText("Seleccion\u00e1 un juego para ver sus detalles.");
            mostrarImagen(null);
            return;
        }
        mostrarImagen(controller.imagenDe(v));
        StringBuilder sb = new StringBuilder();
        sb.append(v.getNombre());
        if (!v.getCreadoPor().isEmpty()) {
            sb.append("  \u2022  agregado por ").append(v.getCreadoPor());
        }
        if (v.getFechaCreacion() != null) {
            sb.append(" el ").append(v.getFechaCreacion().format(FORMATO_FECHA));
        }
        sb.append("\n");
        Seguimiento sg = controller.miSeguimiento().get(v.getId());
        sb.append("Mi estado: ").append(sg == null || sg.getEstado() == null ? "sin estado" : sg.getEstado().toString());
        if (sg != null && sg.getHorasJugadas() != null) {
            double h = sg.getHorasJugadas();
            sb.append("  \u2022  ").append(h == Math.floor(h) ? String.valueOf((long) h) : String.valueOf(h)).append(" h jugadas");
        }
        if (sg != null && sg.isFavorito()) {
            sb.append("  \u2022  \u2605 favorito");
        }
        sb.append("\n");
        Integer mia = controller.misPuntuaciones().get(v.getId());
        sb.append(mia != null ? "Mi calificaci\u00f3n: " + mia + "/10" : "Todav\u00eda no calificaste este juego.").append("\n");
        sb.append(sg == null || sg.getNotas().isEmpty() ? "(sin notas personales)" : sg.getNotas());
        txtDetalle.setText(sb.toString());
        txtDetalle.setCaretPosition(0);
    }

    private void mostrarImagen(byte[] datos) {
        ImageIcon icono = ImagenUtil.cargarIcono(datos, LADO_IMAGEN, LADO_IMAGEN);
        lblImagen.setIcon(icono);
        lblImagen.setText(icono == null ? "Sin imagen" : "");
    }

    private Videojuego juegoSeleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) return null;
        int fila = tabla.convertRowIndexToModel(vista);
        return fila < 0 || fila >= modelo.getRowCount() ? null : modelo.getJuegoAt(fila);
    }

    private Integer idSeleccionado() {
        Videojuego v = juegoSeleccionado();
        return v == null ? null : v.getId();
    }

    private void mensaje(String texto) {
        lblMensaje.setText(texto);
    }
}
