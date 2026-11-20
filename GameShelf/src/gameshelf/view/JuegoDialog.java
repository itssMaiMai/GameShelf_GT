package gameshelf.view;

import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.Estado;
import gameshelf.model.Seguimiento;
import gameshelf.model.Videojuego;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formulario de videojuego. Al AGREGAR (original == null) pide los datos del juego y los datos personales del autor
 * (estado, horas, favorito, notas, calificación). Al EDITAR (solo administrador) permite modificar únicamente los
 * datos del juego (nombre, plataforma, género, desarrollador, año e imagen). Se mantiene abierto mientras haya errores.
 */
public class JuegoDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private static final int ANCHO_VISTA = 150;
    private static final int ALTO_VISTA = 150;

    private static File ultimaCarpeta;

    private final transient VideojuegoController controller;
    private final transient Videojuego original;
    private final transient byte[] imagenActual;

    private final JTextField txtNombre = new JTextField(28);
    private final JTextField txtPlataforma = new JTextField(28);
    private final JTextField txtGenero = new JTextField(28);
    private final JTextField txtDesarrollador = new JTextField(28);
    private final JTextField txtAnio = new JTextField(6);
    private static final String SIN_ESTADO = "Sin estado";

    private final JComboBox<Object> cmbEstado = crearComboEstado();
    private final JTextField txtHoras = new JTextField(8);
    private final JSpinner spnPuntuacion = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final JCheckBox chkFavorito = new JCheckBox("Marcar como favorito");
    private final JTextArea txtNotas = new JTextArea(5, 28);
    private final JLabel lblVistaImagen = new JLabel("", SwingConstants.CENTER);
    private final JButton btnQuitarImagen = new JButton("Quitar imagen");

    private transient Path imagenNueva;
    private boolean quitarImagen;
    private transient Videojuego guardado;

    /** @param original juego a editar, o null para crear uno nuevo. */
    public JuegoDialog(JFrame owner, VideojuegoController controller, Videojuego original) {
        super(owner, original == null ? "Agregar videojuego" : "Editar datos del juego", true);
        this.controller = controller;
        this.original = original;
        this.imagenActual = original == null ? null : controller.imagenDe(original);

        construirInterfaz();
        if (original != null) {
            cargarDatos(original);
        }
        actualizarVistaImagen();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /** @return el juego guardado, o null si el usuario canceló. */
    public Videojuego getGuardado() {
        return guardado;
    }

    /** Si se escriben horas mayores que 0 y el estado es "Sin estado" o "Pendiente", pasa a "Jugando". */
    private void horasEscritas() {
        Object elegido = cmbEstado.getSelectedItem();
        if (!SIN_ESTADO.equals(elegido) && elegido != Estado.PENDIENTE) {
            return;
        }
        try {
            if (Double.parseDouble(txtHoras.getText().trim().replace(',', '.')) > 0) {
                cmbEstado.setSelectedItem(Estado.JUGANDO);
            }
        } catch (NumberFormatException e) {
            // todavía no es un número válido: no se cambia nada
        }
    }

    private static JComboBox<Object> crearComboEstado() {
        JComboBox<Object> combo = new JComboBox<>();
        combo.addItem(SIN_ESTADO);
        for (Estado e : Estado.values()) {
            combo.addItem(e);
        }
        return combo;
    }

    private void construirInterfaz() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));

        int fila = 0;
        agregarFila(form, fila++, "Nombre *", txtNombre, false);
        JPanel panelPlataforma = new JPanel(new BorderLayout(0, 2));
        panelPlataforma.add(txtPlataforma, BorderLayout.CENTER);
        panelPlataforma.add(new JLabel("<html><small>Si est\u00e1 en varias, separalas con coma (ej.: PC, Switch)</small></html>"), BorderLayout.SOUTH);
        agregarFila(form, fila++, "Plataforma", panelPlataforma, false);
        agregarFila(form, fila++, "G\u00e9nero", conAyuda(txtGenero, "Si tiene varios, separalos con coma (ej.: RPG, Aventura)"), false);
        agregarFila(form, fila++, "Desarrollador", conAyuda(txtDesarrollador, "Si son varios, separalos con coma (ej.: Nintendo EPD, Monolith Soft)"), false);

        agregarFila(form, fila++, "A\u00f1o de lanzamiento", txtAnio, false);
        agregarFila(form, fila++, "Imagen (opcional)", crearPanelImagen(), false);

        if (original != null && original.getFechaCreacion() != null) {
            String fecha = original.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT));
            String autor = original.getCreadoPor().isEmpty() ? "" : " por " + original.getCreadoPor();
            agregarFila(form, fila++, "Registrado", new JLabel(fecha + autor), false);
        }

        if (original == null) {
            agregarFila(form, fila++, "", new JLabel("<html><b>Mis datos</b> <small>(solo tuyos: nadie m\u00e1s los ve)</small></html>"), false);
            JPanel estadoHoras = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            estadoHoras.add(cmbEstado);
            estadoHoras.add(Box.createHorizontalStrut(16));
            estadoHoras.add(new JLabel("Horas jugadas (opcional)  "));
            estadoHoras.add(txtHoras);
            agregarFila(form, fila++, "Estado", estadoHoras, false);
            txtHoras.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { horasEscritas(); }
                @Override public void removeUpdate(DocumentEvent e) { horasEscritas(); }
                @Override public void changedUpdate(DocumentEvent e) { horasEscritas(); }
            });

            JPanel favoritoNota = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            favoritoNota.add(chkFavorito);
            favoritoNota.add(Box.createHorizontalStrut(16));
            favoritoNota.add(new JLabel("Mi calificaci\u00f3n (0 = sin calificar a\u00fan)  "));
            favoritoNota.add(spnPuntuacion);
            agregarFila(form, fila++, "", favoritoNota, false);

            txtNotas.setLineWrap(true);
            txtNotas.setWrapStyleWord(true);
            agregarFila(form, fila, "Mis notas", new JScrollPane(txtNotas), true);
        }

        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnGuardar);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");
        getRootPane().getActionMap().put("cancelar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    /** Campo de texto con una línea de ayuda debajo. */
    private static JPanel conAyuda(JTextField campo, String ayuda) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.add(campo, BorderLayout.CENTER);
        panel.add(new JLabel("<html><small>" + ayuda + "</small></html>"), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelImagen() {
        lblVistaImagen.setPreferredSize(new Dimension(ANCHO_VISTA, ALTO_VISTA));
        lblVistaImagen.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0x9E9E9E)));

        JButton btnElegir = new JButton("Elegir imagen...");
        btnElegir.addActionListener(e -> elegirImagen());
        btnQuitarImagen.addActionListener(e -> quitarImagen());

        JPanel botones = new JPanel();
        botones.setLayout(new BoxLayout(botones, BoxLayout.Y_AXIS));
        botones.add(btnElegir);
        botones.add(Box.createVerticalStrut(6));
        botones.add(btnQuitarImagen);
        botones.add(Box.createVerticalStrut(6));
        JLabel nota = new JLabel("<html><small>PNG, JPG, GIF o BMP<br>hasta 5 MB</small></html>");
        botones.add(nota);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(lblVistaImagen);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(botones);
        return panel;
    }

    private void elegirImagen() {
        JFileChooser selector = new JFileChooser(ultimaCarpeta);
        selector.setDialogTitle("Elegir imagen del juego");
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter("Im\u00e1genes (png, jpg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        ultimaCarpeta = archivo.getParentFile();
        Resultado<Void> r = controller.validarImagen(archivo.toPath());
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "Imagen no v\u00e1lida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        imagenNueva = archivo.toPath();
        quitarImagen = false;
        actualizarVistaImagen();
    }

    private void quitarImagen() {
        imagenNueva = null;
        quitarImagen = imagenActual != null;
        actualizarVistaImagen();
    }

    private void actualizarVistaImagen() {
        ImageIcon icono = imagenNueva != null
                ? ImagenUtil.cargarIcono(imagenNueva, ANCHO_VISTA - 4, ALTO_VISTA - 4)
                : (quitarImagen ? null : ImagenUtil.cargarIcono(imagenActual, ANCHO_VISTA - 4, ALTO_VISTA - 4));
        lblVistaImagen.setIcon(icono);
        lblVistaImagen.setText(icono == null ? "Sin imagen" : "");
        btnQuitarImagen.setEnabled(icono != null);
    }

    private void agregarFila(JPanel panel, int fila, String etiqueta, JComponent campo, boolean expandir) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(etiqueta), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = expandir ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        c.weighty = expandir ? 1 : 0;
        panel.add(campo, c);
    }

    private void cargarDatos(Videojuego v) {
        txtNombre.setText(v.getNombre());
        txtPlataforma.setText(v.getPlataforma());
        txtGenero.setText(v.getGenero());
        txtDesarrollador.setText(v.getDesarrollador());
        txtAnio.setText(v.getAnio() == 0 ? "" : String.valueOf(v.getAnio()));
    }

    private void guardar() {
        int anio = 0;
        String textoAnio = txtAnio.getText().trim();
        if (!textoAnio.isEmpty()) {
            if (!textoAnio.matches("\\d{1,4}")) {
                avisar("El a\u00f1o de lanzamiento debe ser un n\u00famero entero (por ejemplo, 2018).", txtAnio);
                return;
            }
            anio = Integer.parseInt(textoAnio);
        }

        Videojuego v = original == null ? new Videojuego() : original.copy();
        v.setNombre(txtNombre.getText());
        v.setPlataforma(txtPlataforma.getText());
        v.setGenero(txtGenero.getText());
        v.setDesarrollador(txtDesarrollador.getText());
        v.setAnio(anio);

        Resultado<Videojuego> r;
        if (original == null) {
            Double horas = null;
            String textoHoras = txtHoras.getText().trim().replace(',', '.');
            if (!textoHoras.isEmpty()) {
                if (!textoHoras.matches("\\d{1,6}(\\.\\d{1,2})?")) {
                    avisar("Las horas jugadas deben ser un n\u00famero positivo (por ejemplo, 12 o 12,5).", txtHoras);
                    return;
                }
                horas = Double.valueOf(textoHoras);
            }
            Seguimiento personal = new Seguimiento();
            personal.setEstado(cmbEstado.getSelectedItem() instanceof Estado ? (Estado) cmbEstado.getSelectedItem() : null);
            personal.setHorasJugadas(horas);
            personal.setFavorito(chkFavorito.isSelected());
            personal.setNotas(txtNotas.getText());
            r = controller.agregar(v, imagenNueva, personal, ((Number) spnPuntuacion.getValue()).intValue());
        } else {
            r = controller.editar(v, imagenNueva, quitarImagen);
        }
        if (!r.isOk()) {
            avisar(r.getMensaje(), txtNombre);
            return;
        }
        guardado = r.getDatos();
        dispose();
    }

    private void avisar(String mensaje, JComponent enfocar) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
        enfocar.requestFocusInWindow();
    }
}
