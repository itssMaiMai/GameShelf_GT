package gameshelf.view;

import gameshelf.controller.Resultado;
import gameshelf.controller.VideojuegoController;
import gameshelf.model.Usuario;
import gameshelf.persistence.ConexionDB;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

/** Pantalla de inicio de sesión. */
public class LoginDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient VideojuegoController controller;
    private final transient ConexionDB conexion;
    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtClave = new JPasswordField(20);
    private boolean autenticado;
    private boolean reconectar;

    public LoginDialog(VideojuegoController controller, ConexionDB conexion) {
        super((Frame) null, "GameShelf \u2013 Iniciar sesi\u00f3n", true);
        this.controller = controller;
        this.conexion = conexion;
        construir();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /** @return true si el usuario inició sesión (o creó una cuenta); false si cerró la ventana. */
    public boolean isAutenticado() {
        return autenticado;
    }

    /** @return true si el usuario cambió la conexión a la base de datos (hay que volver a arrancar con la nueva). */
    public boolean isReconectar() {
        return reconectar;
    }

    private void construir() {
        JLabel titulo = new JLabel("GameShelf", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        JLabel subtitulo = new JLabel("Biblioteca de videojuegos compartida", SwingConstants.CENTER);

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));
        cabecera.add(titulo, BorderLayout.CENTER);
        cabecera.add(subtitulo, BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0; c.gridx = 0;
        form.add(new JLabel("Usuario"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtUsuario, c);
        c.gridy = 1; c.gridx = 0; c.weightx = 0;
        form.add(new JLabel("Contrase\u00f1a"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtClave, c);

        JButton btnIngresar = new JButton("Iniciar sesi\u00f3n");
        JButton btnCrear = new JButton("Crear cuenta");
        JButton btnSalir = new JButton("Salir");
        JButton btnConexion = new JButton("Conexi\u00f3n...");
        btnConexion.addActionListener(e -> configurarConexion());
        btnIngresar.addActionListener(e -> ingresar());
        btnCrear.addActionListener(e -> crearCuenta());
        btnSalir.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        botones.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        botones.add(btnConexion);
        botones.add(btnSalir);
        botones.add(btnCrear);
        botones.add(btnIngresar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cabecera, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        JLabel base = new JLabel("Base de datos: " + conexion.descripcion(), SwingConstants.CENTER);
        base.setFont(base.getFont().deriveFont(11f));
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(botones, BorderLayout.CENTER);
        sur.add(base, BorderLayout.SOUTH);
        sur.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        getContentPane().add(sur, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnIngresar);
    }

    private void ingresar() {
        char[] clave = txtClave.getPassword();
        Resultado<Usuario> r;
        try {
            r = controller.iniciarSesion(txtUsuario.getText(), clave);
        } finally {
            Arrays.fill(clave, '\0');
        }
        if (!r.isOk()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo iniciar sesi\u00f3n", JOptionPane.WARNING_MESSAGE);
            txtClave.setText("");
            txtClave.requestFocusInWindow();
            return;
        }
        autenticado = true;
        dispose();
    }

    private void configurarConexion() {
        ConexionDialog dialogo = new ConexionDialog(this, conexion, null);
        dialogo.setVisible(true);
        if (dialogo.isGuardado()) {
            reconectar = true;
            dispose();
        }
    }

    private void crearCuenta() {
        RegistroDialog dialogo = new RegistroDialog(this, controller, false);
        dialogo.setVisible(true);
        if (dialogo.isCreado()) {
            autenticado = true;
            dispose();
        }
    }
}
