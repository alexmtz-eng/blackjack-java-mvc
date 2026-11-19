package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * Interfaz gráfica de usuario (GUI) común para los clientes del Blackjack multijugador.
 * 
 * Permite la conexión al servidor mediante IP, nombre de usuario y contraseña
 * (login o registro automático), muestra el estado de la mesa en tiempo real
 * (cartas y puntuación del crupier y del jugador), habilita las acciones de turno
 * (pedir carta, plantarse y modo automático) y ofrece acceso a estadísticas personales
 * y al ranking global.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */
public class VistaCliente extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;

    // Panel de conexión y login
    private JTextField txtHost;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnConectar;
    private JButton btnDesconectar;

    // Panel de mesa (Crupier y Jugador)
    private JLabel lblEstadoPartida;
    private JLabel lblCartasCrupier;
    private JLabel lblPuntosCrupier;
    private JLabel lblCartasJugador;
    private JLabel lblPuntosJugador;

    // Registro de mensajes, resultados, estadísticas y ranking
    private JTextArea txtMensajes;

    // Controles de turno y consultas
    private JButton btnPedirCarta;
    private JButton btnPlantarse;
    private JButton btnListo;
    private JCheckBox chkModoAuto;
    private JButton btnMisEstadisticas;
    private JButton btnVerRanking;

    public VistaCliente() {
        setTitle("Cliente - Casino Blackjack");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(120, 120, 820, 560);
        setMinimumSize(new Dimension(760, 500));

        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // --- PANEL SUPERIOR: Conexión y Acceso ---
        JPanel panelConexion = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        panelConexion.setBorder(new TitledBorder("Conexión y Acceso"));

        panelConexion.add(new JLabel("IP:"));
        txtHost = new JTextField("127.0.0.1", 9);
        panelConexion.add(txtHost);

        panelConexion.add(new JLabel("Usuario:"));
        txtUsuario = new JTextField(10);
        panelConexion.add(txtUsuario);

        panelConexion.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField(10);
        panelConexion.add(txtPassword);

        btnConectar = new JButton("Conectar");
        btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setEnabled(false);

        panelConexion.add(btnConectar);
        panelConexion.add(btnDesconectar);

        contentPane.add(panelConexion, BorderLayout.NORTH);

        // --- PANEL CENTRAL: Mesa arriba + Consola de eventos abajo ---
        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
        contentPane.add(panelCentral, BorderLayout.CENTER);

        // Mesa de Blackjack
        JPanel panelMesa = new JPanel(new BorderLayout(8, 8));
        panelMesa.setBorder(new TitledBorder("Mesa de Juego"));

        lblEstadoPartida = new JLabel("Estado: Desconectado. Introduce usuario y contraseña para entrar.");
        lblEstadoPartida.setFont(new Font("SansSerif", Font.BOLD, 13));
        panelMesa.add(lblEstadoPartida, BorderLayout.NORTH);

        JPanel panelManos = new JPanel(new GridLayout(2, 1, 5, 8));

        // Zona Crupier
        JPanel panelCrupier = new JPanel(new BorderLayout(10, 5));
        panelCrupier.setBorder(new TitledBorder("Crupier"));
        lblCartasCrupier = new JLabel("Cartas: -");
        lblCartasCrupier.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lblPuntosCrupier = new JLabel("Puntos: 0");
        lblPuntosCrupier.setFont(new Font("SansSerif", Font.BOLD, 13));
        panelCrupier.add(lblCartasCrupier, BorderLayout.CENTER);
        panelCrupier.add(lblPuntosCrupier, BorderLayout.EAST);

        // Zona Jugador
        JPanel panelJugador = new JPanel(new BorderLayout(10, 5));
        panelJugador.setBorder(new TitledBorder("Tu Mano"));
        lblCartasJugador = new JLabel("Cartas: -");
        lblCartasJugador.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lblPuntosJugador = new JLabel("Puntos: 0");
        lblPuntosJugador.setFont(new Font("SansSerif", Font.BOLD, 13));
        panelJugador.add(lblCartasJugador, BorderLayout.CENTER);
        panelJugador.add(lblPuntosJugador, BorderLayout.EAST);

        panelManos.add(panelCrupier);
        panelManos.add(panelJugador);
        panelMesa.add(panelManos, BorderLayout.CENTER);

        panelCentral.add(panelMesa, BorderLayout.NORTH);

        // Área de mensajes del servidor, estadísticas y ranking
        JPanel panelMensajes = new JPanel(new BorderLayout());
        panelMensajes.setBorder(new TitledBorder("Mensajes del Servidor y Estadísticas"));

        txtMensajes = new JTextArea();
        txtMensajes.setEditable(false);
        txtMensajes.setLineWrap(true);
        txtMensajes.setWrapStyleWord(true);
        txtMensajes.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollMensajes = new JScrollPane(txtMensajes);
        panelMensajes.add(scrollMensajes, BorderLayout.CENTER);

        panelCentral.add(panelMensajes, BorderLayout.CENTER);

        // --- PANEL INFERIOR: Acciones de juego y consultas ---
        JPanel panelInferior = new JPanel(new GridLayout(2, 1, 5, 5));
        panelInferior.setBorder(new TitledBorder("Acciones"));

        JPanel filaJuego = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 3));
        btnPedirCarta = new JButton("Pedir Carta");
        btnPlantarse = new JButton("Plantarse");
        btnListo = new JButton("Listo / Nueva Ronda");
        chkModoAuto = new JCheckBox("Modo Automático");

        btnPedirCarta.setEnabled(false);
        btnPlantarse.setEnabled(false);
        btnListo.setEnabled(false);
        chkModoAuto.setEnabled(false);

        filaJuego.add(btnPedirCarta);
        filaJuego.add(btnPlantarse);
        filaJuego.add(btnListo);
        filaJuego.add(chkModoAuto);

        JPanel filaConsultas = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 3));
        btnMisEstadisticas = new JButton("Mis Estadísticas");
        btnVerRanking = new JButton("Ver Ranking");

        btnMisEstadisticas.setEnabled(false);
        btnVerRanking.setEnabled(false);

        filaConsultas.add(btnMisEstadisticas);
        filaConsultas.add(btnVerRanking);

        panelInferior.add(filaJuego);
        panelInferior.add(filaConsultas);

        contentPane.add(panelInferior, BorderLayout.SOUTH);
    }

    // --- MÉTODOS DE ACTUALIZACIÓN VISUAL (Seguros para hilos de Sockets) ---

    public void setConectado(boolean conectado) {
        SwingUtilities.invokeLater(() -> {
            txtHost.setEnabled(!conectado);
            txtUsuario.setEnabled(!conectado);
            txtPassword.setEnabled(!conectado);
            btnConectar.setEnabled(!conectado);

            btnDesconectar.setEnabled(conectado);
            btnListo.setEnabled(conectado);
            chkModoAuto.setEnabled(conectado);
            btnMisEstadisticas.setEnabled(conectado);
            btnVerRanking.setEnabled(conectado);

            if (!conectado) {
                btnPedirCarta.setEnabled(false);
                btnPlantarse.setEnabled(false);
                chkModoAuto.setSelected(false);
                lblEstadoPartida.setText("Estado: Desconectado.");
            }
        });
    }

    public void setTurnoActivo(boolean esMiTurno) {
        SwingUtilities.invokeLater(() -> {
            boolean permitirManual = esMiTurno && !chkModoAuto.isSelected();
            btnPedirCarta.setEnabled(permitirManual);
            btnPlantarse.setEnabled(permitirManual);
        });
    }

    public void setListoHabilitado(boolean habilitado) {
        SwingUtilities.invokeLater(() -> btnListo.setEnabled(habilitado));
    }

    public void actualizarEstado(String texto) {
        SwingUtilities.invokeLater(() -> lblEstadoPartida.setText("Estado: " + texto));
    }

    public void actualizarCrupier(String cartas, int puntos) {
        SwingUtilities.invokeLater(() -> {
            lblCartasCrupier.setText("Cartas: " + cartas);
            lblPuntosCrupier.setText("Puntos: " + puntos);
        });
    }

    public void actualizarJugador(String cartas, int puntos) {
        SwingUtilities.invokeLater(() -> {
            lblCartasJugador.setText("Cartas: " + cartas);
            lblPuntosJugador.setText("Puntos: " + puntos);
        });
    }

    public void limpiarMesa() {
        actualizarCrupier("-", 0);
        actualizarJugador("-", 0);
    }

    public void agregarMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtMensajes.append(mensaje + "\n");
            txtMensajes.setCaretPosition(txtMensajes.getDocument().getLength());
        });
    }

    // --- GETTERS PARA EL CONTROLADOR / RED ---

    public String getHost() {
        return txtHost.getText().trim();
    }

    public String getNombreUsuario() {
        return txtUsuario.getText().trim();
    }

    public String getPassword() {
        return new String(txtPassword.getPassword()).trim();
    }

    public boolean isModoAutoActivo() {
        return chkModoAuto.isSelected();
    }

    public JButton getBtnConectar() {
        return btnConectar;
    }

    public JButton getBtnDesconectar() {
        return btnDesconectar;
    }

    public JButton getBtnPedirCarta() {
        return btnPedirCarta;
    }

    public JButton getBtnPlantarse() {
        return btnPlantarse;
    }

    public JButton getBtnListo() {
        return btnListo;
    }

    public JCheckBox getChkModoAuto() {
        return chkModoAuto;
    }

    public JButton getBtnMisEstadisticas() {
        return btnMisEstadisticas;
    }

    public JButton getBtnVerRanking() {
        return btnVerRanking;
    }

    // Lanzador para previsualizar la ventana de forma independiente
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                VistaCliente frame = new VistaCliente();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}