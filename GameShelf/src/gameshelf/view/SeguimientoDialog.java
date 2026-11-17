package gameshelf.view;

import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.Estado;
import gameshelf.model.Seguimiento;
import gameshelf.model.Videojuego;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

/**
 * "Mi seguimiento": estado, horas, favorito, notas y calificación del usuario actual sobre un juego.
 * Son datos privados. Si el usuario todavía no calificó el juego, puede hacerlo desde acá; si ya lo calificó,
 * se muestra su nota actual y puede cambiarla cuando quiera.
 */
public class SeguimientoDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient VideojuegoController controller;
    private final transient Videojuego juego;
    private static final String SIN_ESTADO = "Sin estado";

    private final JComboBox<Object> cmbEstado = crearComboEstado();
    private final JTextField txtHoras = new JTextField(8);
    private final JCheckBox chkFavorito = new JCheckBox("Marcar como favorito");
    private final JTextArea txtNotas = new JTextArea(6, 30);
    private final JSpinner spnCalificacion = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final transient Integer calificacionActual;
    private boolean guardado;

    /** @param calificacionActual la calificación que el usuario ya le dio al juego, o null si todavía no lo calificó. */
    public SeguimientoDialog(JFrame owner, VideojuegoController controller, Videojuego juego, Seguimiento actual,
                             Integer calificacionActual) {
        super(owner, "Mi seguimiento \u2013 " + juego.getNombre(), true);
        this.controller = controller;
        this.juego = juego;
        this.calificacionActual = calificacionActual;
        construir();
        if (actual != null) {
            cmbEstado.setSelectedItem(actual.getEstado() == null ? SIN_ESTADO : actual.getEstado());
            if (actual.getHorasJugadas() != null) {
                double h = actual.getHorasJugadas();
                txtHoras.setText(h == Math.floor(h) ? String.valueOf((long) h) : String.valueOf(h));
            }
            chkFavorito.setSelected(actual.isFavorito());
            txtNotas.setText(actual.getNotas());
            txtNotas.setCaretPosition(0);
        }
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private static JComboBox<Object> crearComboEstado() {
        JComboBox<Object> combo = new JComboBox<>();
        combo.addItem(SIN_ESTADO);
        for (Estado e : Estado.values()) {
            combo.addItem(e);
        }
        return combo;
    }

    public boolean isGuardado() {
        return guardado;
    }

    private void construir() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        form.add(new JLabel("<html>Estos datos son <b>solo tuyos</b>: cada usuario tiene los suyos y nadie m\u00e1s los ve.<br>"
                + "Los datos del juego (nombre, plataforma, etc.) solo los puede modificar un administrador.</html>"), c);
        c.gridwidth = 1;
        fila(form, c, 1, "Estado", cmbEstado, false);
        fila(form, c, 2, "Horas jugadas (opcional)", txtHoras, false);
        fila(form, c, 3, "", chkFavorito, false);
        txtNotas.setLineWrap(true);
        txtNotas.setWrapStyleWord(true);
        // Si ya calificó el juego el mínimo es 1 (puede cambiar la nota); si no, 0 significa "sin calificar todavía".
        spnCalificacion.setModel(new SpinnerNumberModel(calificacionActual != null ? calificacionActual.intValue() : 0,
                calificacionActual != null ? 1 : 0, 10, 1));
        JPanel panelCalificacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelCalificacion.add(spnCalificacion);
        panelCalificacion.add(Box.createHorizontalStrut(10));
        panelCalificacion.add(new JLabel(calificacionActual != null
                ? "<html><small>1 a 10. Pod\u00e9s cambiarla cuando quieras.</small></html>"
                : "<html><small>1 a 10 (0 = sin calificar a\u00fan).</small></html>"));
        fila(form, c, 4, "Mi calificaci\u00f3n", panelCalificacion, false);
        fila(form, c, 5, "Mis notas", new JScrollPane(txtNotas), true);

        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnGuardar);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");
        getRootPane().getActionMap().put("cancelar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private static void fila(JPanel panel, GridBagConstraints c, int fila, String etiqueta, JComponent campo, boolean expandir) {
        c.gridy = fila;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = expandir ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        c.weighty = expandir ? 1 : 0;
        panel.add(campo, c);
    }

    private void guardar() {
        Double horas = null;
        String texto = txtHoras.getText().trim().replace(',', '.');
        if (!texto.isEmpty()) {
            if (!texto.matches("\\d{1,6}(\\.\\d{1,2})?")) {
                JOptionPane.showMessageDialog(this, "Las horas jugadas deben ser un n\u00famero positivo (por ejemplo, 12 o 12,5).",
                        "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
                txtHoras.requestFocusInWindow();
                return;
            }
            horas = Double.valueOf(texto);
        }
        Resultado<Seguimiento> r = controller.guardarSeguimiento(juego.getId(), cmbEstado.getSelectedItem() instanceof Estado ? (Estado) cmbEstado.getSelectedItem() : null,
                horas, chkFavorito.isSelected(), txtNotas.getText());
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        guardado = true;
        int puntos = ((Number) spnCalificacion.getValue()).intValue();
        if (puntos > 0 && (calificacionActual == null || puntos != calificacionActual.intValue())) {
            Resultado<Void> calificacion = controller.calificar(juego.getId(), puntos);
            if (!calificacion.isOk()) {
                JOptionPane.showMessageDialog(this, "Tu seguimiento se guard\u00f3, pero no se pudo guardar la calificaci\u00f3n:\n"
                        + calificacion.getMensaje(), "Calificaci\u00f3n", JOptionPane.WARNING_MESSAGE);
            }
        }
        dispose();
    }
}
