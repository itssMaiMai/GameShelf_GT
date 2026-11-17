package gameshelf.view;

import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;

/** Alta de cuenta. En el primer uso crea la cuenta de administrador; luego, cuentas de usuario. */
public class RegistroDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient VideojuegoController controller;
    private final boolean administrador;
    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtClave = new JPasswordField(20);
    private final JPasswordField txtRepetir = new JPasswordField(20);
    private boolean creado;

    public RegistroDialog(Window owner, VideojuegoController controller, boolean administrador) {
        super(owner, administrador ? "Crear cuenta de administrador" : "Crear cuenta", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.administrador = administrador;
        construir();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public boolean isCreado() {
        return creado;
    }

    private void construir() {
        JLabel info = new JLabel("<html>" + (administrador
                ? "Es la primera vez que se usa GameShelf.<br>Esta cuenta ser\u00e1 la <b>administradora</b> y podr\u00e1 dar de baja juegos."
                : "Cre\u00e1 tu cuenta para usar la biblioteca compartida.") + "</html>");
        JLabel ayuda = new JLabel("<html><small>Usuario: 3 a 30 caracteres (letras, n\u00fameros, punto, guion o guion bajo).<br>"
                + "Contrase\u00f1a: m\u00ednimo 6 caracteres.</small></html>");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        form.add(info, c);
        c.gridwidth = 1;
        agregar(form, c, 1, "Usuario", txtUsuario);
        agregar(form, c, 2, "Contrase\u00f1a", txtClave);
        agregar(form, c, 3, "Repetir contrase\u00f1a", txtRepetir);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        form.add(ayuda, c);

        JButton btnCrear = new JButton("Crear cuenta");
        JButton btnCancelar = new JButton("Cancelar");
        btnCrear.addActionListener(e -> crear());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.add(btnCancelar);
        botones.add(btnCrear);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnCrear);
    }

    private static void agregar(JPanel panel, GridBagConstraints c, int fila, String etiqueta, java.awt.Component campo) {
        c.gridy = fila;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(campo, c);
    }

    private void crear() {
        char[] clave = txtClave.getPassword();
        char[] repetir = txtRepetir.getPassword();
        Resultado<Usuario> r;
        try {
            r = administrador
                    ? controller.crearAdministradorInicial(txtUsuario.getText(), clave, repetir)
                    : controller.registrarUsuario(txtUsuario.getText(), clave, repetir);
        } finally {
            Arrays.fill(clave, '\0');
            Arrays.fill(repetir, '\0');
        }
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo crear la cuenta", JOptionPane.WARNING_MESSAGE);
            txtClave.setText("");
            txtRepetir.setText("");
            return;
        }
        creado = true;
        dispose();
    }
}
