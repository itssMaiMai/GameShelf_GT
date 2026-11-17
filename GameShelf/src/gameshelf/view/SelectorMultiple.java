package gameshelf.view;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Botón desplegable con una lista deslizable de casillas (checkbox) para marcar varias opciones, por ejemplo
 * los géneros a buscar. El texto del botón resume lo marcado ("Géneros: Todos", "Géneros: RPG", "Géneros: 3 marcados").
 * No tiene botón propio para quitar la selección: eso se hace con el botón "Quitar filtros" de la pestaña
 * (que llama a {@link #limpiar()}).
 */
public class SelectorMultiple extends JButton {
    private static final long serialVersionUID = 1L;
    private static final int ALTO_MAXIMO = 240;

    private final String titulo;
    private final transient Runnable alCambiar;
    private final List<String> opciones = new ArrayList<>();
    private final List<String> marcadas = new ArrayList<>();
    private final transient List<JCheckBox> casillas = new ArrayList<>();
    private final JPopupMenu popup = new JPopupMenu();

    /** @param alCambiar se invoca cada vez que el usuario marca o desmarca una opción. */
    public SelectorMultiple(String titulo, Runnable alCambiar) {
        this.titulo = titulo;
        this.alCambiar = alCambiar;
        addActionListener(e -> popup.show(this, 0, getHeight()));
        reconstruir();
        actualizarTexto();
    }

    /** Actualiza las opciones disponibles, conservando las marcadas que siguen existiendo. No dispara alCambiar. */
    public void setOpciones(List<String> nuevas) {
        if (opciones.equals(nuevas)) {
            return;
        }
        opciones.clear();
        opciones.addAll(nuevas);
        List<String> siguen = new ArrayList<>();
        for (String o : opciones) {
            if (contiene(marcadas, o)) {
                siguen.add(o);
            }
        }
        marcadas.clear();
        marcadas.addAll(siguen);
        reconstruir();
        actualizarTexto();
    }

    /** @return las opciones marcadas (vacío = ninguna marcada, es decir, sin filtrar). */
    public List<String> getMarcadas() {
        return new ArrayList<>(marcadas);
    }

    /** Desmarca todo. No dispara alCambiar. */
    public void limpiar() {
        marcadas.clear();
        for (JCheckBox c : casillas) {
            c.setSelected(false);
        }
        actualizarTexto();
    }

    private void reconstruir() {
        popup.removeAll();
        casillas.clear();

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        if (opciones.isEmpty()) {
            lista.add(new JLabel("Todav\u00eda no hay opciones."));
        }
        for (String opcion : opciones) {
            JCheckBox casilla = new JCheckBox(opcion, contiene(marcadas, opcion));
            casilla.addActionListener(e -> alternar(opcion, casilla.isSelected()));
            casillas.add(casilla);
            lista.add(casilla);
        }

        JScrollPane desplazable = new JScrollPane(lista);
        desplazable.setBorder(null);
        desplazable.getVerticalScrollBar().setUnitIncrement(16);
        Dimension pref = lista.getPreferredSize();
        desplazable.setPreferredSize(new Dimension(Math.max(210, pref.width + 28), Math.min(pref.height + 4, ALTO_MAXIMO)));

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.add(desplazable, BorderLayout.CENTER);
        popup.add(contenido);
    }

    private void alternar(String opcion, boolean marcada) {
        if (marcada && !contiene(marcadas, opcion)) {
            marcadas.add(opcion);
        } else if (!marcada) {
            marcadas.removeIf(m -> m.equalsIgnoreCase(opcion));
        }
        actualizarTexto();
        alCambiar.run();
    }

    private void actualizarTexto() {
        String resumen;
        if (marcadas.isEmpty()) {
            resumen = "Todos";
        } else if (marcadas.size() == 1) {
            String unica = marcadas.get(0);
            resumen = unica.length() > 18 ? unica.substring(0, 17) + "\u2026" : unica;
        } else {
            resumen = marcadas.size() + " marcados";
        }
        setText(titulo + ": " + resumen + "  \u25BE");
        setToolTipText(marcadas.isEmpty() ? "Marc\u00e1 uno o varios para filtrar (se muestran los que tengan cualquiera de los marcados)"
                : String.join(", ", marcadas));
    }

    private static boolean contiene(List<String> lista, String valor) {
        for (String s : lista) {
            if (s.equalsIgnoreCase(valor)) {
                return true;
            }
        }
        return false;
    }
}
