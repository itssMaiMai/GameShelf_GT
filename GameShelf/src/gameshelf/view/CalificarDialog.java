package gameshelf.view;

import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.Videojuego;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

/**
 * Permite calificar un juego o cambiar la calificación que ya se le dio. La calificación es privada: ningún otro
 * usuario puede verla (solo cuenta para el promedio general, que es anónimo).
 */
public class CalificarDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient VideojuegoController controller;
    private final transient Map<Integer, Integer> misNotas;
    private final JComboBox<Videojuego> cmbJuegos;
    private final JSpinner spnPuntuacion = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
    private boolean calificado;
    private transient Videojuego juegoCalificado;

    /**
     * @param opciones juegos entre los que se puede elegir.
     * @param preseleccionadoId id del juego a marcar (o null).
     * @param misNotas calificaciones actuales del usuario (idJuego -> nota), para mostrarlas y poder cambiarlas.
     */
    public CalificarDialog(JFrame owner, VideojuegoController controller, List<Videojuego> opciones,
                           Integer preseleccionadoId, Map<Integer, Integer> misNotas) {
        super(owner, "Calificar un juego", true);
        this.controller = controller;
        this.misNotas = misNotas;
        this.cmbJuegos = new JComboBox<>(opciones.toArray(new Videojuego[0]));
        cmbJuegos.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean seleccionado, boolean foco) {
                Component c = super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor instanceof Videojuego) {
                    Videojuego v = (Videojuego) valor;
                    String texto = v.getPlataforma().isEmpty() ? v.getNombre() : v.getNombre() + "  (" + v.getPlataforma() + ")";
                    Integer nota = CalificarDialog.this.misNotas.get(v.getId());
                    setText(nota == null ? texto : texto + "  \u2013  tu nota: " + nota + "/10");
                }
                return c;
            }
        });
        if (preseleccionadoId != null) {
            for (Videojuego v : opciones) {
                if (v.getId() == preseleccionadoId) {
                    cmbJuegos.setSelectedItem(v);
                    break;
                }
            }
        }
        cmbJuegos.addActionListener(e -> cargarNotaActual());
        construir();
        cargarNotaActual();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public boolean isCalificado() {
        return calificado;
    }

    public Videojuego getJuegoCalificado() {
        return juegoCalificado;
    }

    /** Al elegir un juego ya calificado, el selector muestra la nota actual para poder cambiarla. */
    private void cargarNotaActual() {
        Videojuego elegido = (Videojuego) cmbJuegos.getSelectedItem();
        Integer nota = elegido == null ? null : misNotas.get(elegido.getId());
        spnPuntuacion.setValue(nota != null ? nota : 5);
    }

    private void construir() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        form.add(new JLabel("<html>Tu calificaci\u00f3n es <b>privada</b>: nadie m\u00e1s puede ver tu nota.<br>"
                + "Si ya calificaste un juego, pod\u00e9s cambiar la nota cuando quieras.</html>"), c);
        c.gridwidth = 1;
        c.gridy = 1; c.gridx = 0;
        form.add(new JLabel("Juego"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(cmbJuegos, c);
        c.gridy = 2; c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Calificaci\u00f3n (1 a 10)"), c);
        c.gridx = 1;
        form.add(spnPuntuacion, c);

        JButton btnGuardar = new JButton("Guardar calificaci\u00f3n");
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
    }

    private void guardar() {
        Videojuego elegido = (Videojuego) cmbJuegos.getSelectedItem();
        if (elegido == null) {
            JOptionPane.showMessageDialog(this, "Eleg\u00ed un juego para calificar.", "Calificar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int puntos = ((Number) spnPuntuacion.getValue()).intValue();
        Resultado<Void> r = controller.calificar(elegido.getId(), puntos);
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo calificar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        juegoCalificado = elegido;
        calificado = true;
        dispose();
    }
}
