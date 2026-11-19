package vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla independiente de inicio de sesión y registro de usuarios.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */
public class VistaLogin extends JFrame {

    private static final long serialVersionUID = 1L;
    private JTextField txtHost;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIniciarSesion;
    private JButton btnRegistrarse;

    private boolean datosValidados = false;
    private String accionSeleccionada = "";

    public VistaLogin() {
        setTitle("Acceso - Casino Blackjack");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(360, 260);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        JPanel panelCampos = new JPanel(new GridLayout(4, 2, 8, 10));
        panelCampos.add(new JLabel("Servidor IP:"));
        txtHost = new JTextField("127.0.0.1");
        panelCampos.add(txtHost);

        panelCampos.add(new JLabel("Usuario:"));
        txtUsuario = new JTextField();
        panelCampos.add(txtUsuario);

        panelCampos.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField();
        panelCampos.add(txtPassword);
        contentPane.add(panelCampos, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnIniciarSesion = new JButton("Iniciar Sesión");
        btnRegistrarse = new JButton("Crear Cuenta");

        panelBotones.add(btnIniciarSesion);
        panelBotones.add(btnRegistrarse);
        contentPane.add(panelBotones, BorderLayout.SOUTH);

        btnIniciarSesion.addActionListener(e -> {
            accionSeleccionada = "LOGIN";
            datosValidados = validarCampos();
        });

        btnRegistrarse.addActionListener(e -> {
            accionSeleccionada = "REGISTRO";
            datosValidados = validarCampos();
        });
    }

    private boolean validarCampos() {
        if (getHost().isEmpty() || getUsuario().isEmpty() || getPassword().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public String getHost() { return txtHost.getText().trim(); }
    public String getUsuario() { return txtUsuario.getText().trim(); }
    public String getPassword() { return new String(txtPassword.getPassword()).trim(); }
    public boolean isDatosValidados() { return datosValidados; }
    public String getAccionSeleccionada() { return accionSeleccionada; }
    public JButton getBtnIniciarSesion() { return btnIniciarSesion; }
    public JButton getBtnRegistrarse() { return btnRegistrarse; }
}
