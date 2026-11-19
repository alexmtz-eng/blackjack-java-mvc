package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import modelo.Carta;

/**
 * Ventana principal de la mesa de juego del Cliente para el Blackjack multijugador.
 * Presenta una interfaz limpia y despejada sin campos de acceso directos,
 * mostrando el saldo de fichas, tapete gráfico con cartas del crupier y jugador,
 * puntuación dual inteligente para los Ases y controles de juego y estadísticas.
 *
 * @author Alejandro Martínez Torres
 * @version 3.0
 * @since 2026-11-19
 */
public class VistaCliente extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color COLOR_TAPETE = new Color(22, 105, 58);
    private static final Color COLOR_TAPETE_OSCURO = new Color(16, 82, 45);
    private static final Color COLOR_TEXTO_MESA = new Color(245, 245, 240);

    private JPanel contentPane;

    // Panel superior de información de sesión y fichas
    private JLabel lblSaldoFichas;
    private JButton btnDesconectar;

    // Mesa de juego gráfica
    private JLabel lblEstadoPartida;
    private JLabel lblPuntosCrupier;
    private JPanel panelCartasCrupier;
    private JLabel lblPuntosJugador;
    private JPanel panelCartasJugador;

    // Mensajes y estadísticas
    private JTextArea txtMensajes;

    // Acciones de juego y consultas
    private JButton btnPedirCarta;
    private JButton btnPlantarse;
    private JButton btnListo;
    private JCheckBox chkModoAuto;
    private JButton btnMisEstadisticas;
    private JButton btnVerRanking;

    public VistaCliente() {
        setTitle("Mesa de Juego - Casino Blackjack");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 820, 640);
        setMinimumSize(new Dimension(760, 580));

        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // --- 1. PANEL SUPERIOR LIMPIO: FICHAS Y SALIR ---
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(new EmptyBorder(0, 5, 0, 5));
        
        lblSaldoFichas = new JLabel("Fichas: 100 🪙");
        lblSaldoFichas.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSaldoFichas.setForeground(new Color(180, 130, 0));
        panelSuperior.add(lblSaldoFichas, BorderLayout.WEST);

        btnDesconectar = new JButton("Cerrar Sesión / Salir");
        panelSuperior.add(btnDesconectar, BorderLayout.EAST);
        
        contentPane.add(panelSuperior, BorderLayout.NORTH);

        // --- 2. PANEL CENTRAL: TAPETE DE JUEGO + MENSAJES ---
        JPanel panelCentral = new JPanel(new BorderLayout(8, 8));
        contentPane.add(panelCentral, BorderLayout.CENTER);

        JPanel panelMesa = new JPanel(new BorderLayout(8, 8));
        panelMesa.setBackground(COLOR_TAPETE);
        TitledBorder bordeMesa = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(212, 175, 55), 2),
                "Tapete de Blackjack");
        bordeMesa.setTitleColor(COLOR_TEXTO_MESA);
        bordeMesa.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        panelMesa.setBorder(bordeMesa);
        panelCentral.add(panelMesa, BorderLayout.CENTER);

        lblEstadoPartida = new JLabel("Estado: Esperando inicio de ronda...");
        lblEstadoPartida.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblEstadoPartida.setForeground(new Color(255, 235, 150));
        lblEstadoPartida.setBorder(new EmptyBorder(4, 8, 4, 8));
        panelMesa.add(lblEstadoPartida, BorderLayout.NORTH);

        JPanel panelManos = new JPanel(new GridLayout(2, 1, 8, 8));
        panelManos.setOpaque(false);
        panelManos.setBorder(new EmptyBorder(4, 8, 8, 8));
        panelMesa.add(panelManos, BorderLayout.CENTER);

        // Zona Crupier
        JPanel panelCrupier = new JPanel(new BorderLayout(5, 5));
        panelCrupier.setBackground(COLOR_TAPETE_OSCURO);
        panelCrupier.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        lblPuntosCrupier = new JLabel("Crupier — Puntos: 0");
        lblPuntosCrupier.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPuntosCrupier.setForeground(COLOR_TEXTO_MESA);
        panelCrupier.add(lblPuntosCrupier, BorderLayout.NORTH);

        panelCartasCrupier = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelCartasCrupier.setOpaque(false);
        panelCartasCrupier.setPreferredSize(new Dimension(600, GestorCartas.ALTO_CARTA + 12));
        panelCrupier.add(panelCartasCrupier, BorderLayout.CENTER);
        panelManos.add(panelCrupier);

        // Zona Jugador
        JPanel panelJugador = new JPanel(new BorderLayout(5, 5));
        panelJugador.setBackground(COLOR_TAPETE_OSCURO);
        panelJugador.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        lblPuntosJugador = new JLabel("Tu Mano — Puntos: 0");
        lblPuntosJugador.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPuntosJugador.setForeground(COLOR_TEXTO_MESA);
        panelJugador.add(lblPuntosJugador, BorderLayout.NORTH);

        panelCartasJugador = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelCartasJugador.setOpaque(false);
        panelCartasJugador.setPreferredSize(new Dimension(600, GestorCartas.ALTO_CARTA + 12));
        panelJugador.add(panelCartasJugador, BorderLayout.CENTER);
        panelManos.add(panelJugador);

        // Panel de mensajes del servidor
        JPanel panelMensajes = new JPanel(new BorderLayout());
        panelMensajes.setBorder(new TitledBorder("Registro de Actividad"));
        txtMensajes = new JTextArea(5, 40);
        txtMensajes.setEditable(false);
        txtMensajes.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtMensajes.setLineWrap(true);
        txtMensajes.setWrapStyleWord(true);
        panelMensajes.add(new JScrollPane(txtMensajes), BorderLayout.CENTER);
        panelCentral.add(panelMensajes, BorderLayout.SOUTH);

        // --- 3. PANEL INFERIOR: ACCIONES ---
        JPanel panelInferior = new JPanel(new GridLayout(2, 1, 5, 5));
        panelInferior.setBorder(new TitledBorder("Controles"));
        contentPane.add(panelInferior, BorderLayout.SOUTH);

        JPanel filaJuego = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btnPedirCarta = new JButton("Pedir Carta");
        btnPlantarse = new JButton("Plantarse");
        btnListo = new JButton("Listo / Apostar (10 🪙)");
        chkModoAuto = new JCheckBox("Modo Automático");

        btnPedirCarta.setEnabled(false);
        btnPlantarse.setEnabled(false);
        btnListo.setEnabled(false);

        filaJuego.add(btnPedirCarta);
        filaJuego.add(btnPlantarse);
        filaJuego.add(btnListo);
        filaJuego.add(chkModoAuto);
        panelInferior.add(filaJuego);

        JPanel filaConsultas = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btnMisEstadisticas = new JButton("Mis Estadísticas");
        btnVerRanking = new JButton("Ver Ranking");

        filaConsultas.add(btnMisEstadisticas);
        filaConsultas.add(btnVerRanking);
        panelInferior.add(filaConsultas);
    }

    // --- MÉTODOS DE ACTUALIZACIÓN SEGURA ---

    public void setConectado(boolean conectado) {
        SwingUtilities.invokeLater(() -> {
            btnListo.setEnabled(conectado);
            chkModoAuto.setEnabled(conectado);
            btnMisEstadisticas.setEnabled(conectado);
            btnVerRanking.setEnabled(conectado);

            if (!conectado) {
                btnPedirCarta.setEnabled(false);
                btnPlantarse.setEnabled(false);
                chkModoAuto.setSelected(false);
            }
        });
    }

    public void setTurnoActivo(boolean esMiTurno) {
        SwingUtilities.invokeLater(() -> {
            boolean manual = esMiTurno && !chkModoAuto.isSelected();
            btnPedirCarta.setEnabled(manual);
            btnPlantarse.setEnabled(manual);
        });
    }

    public void setListoHabilitado(boolean habilitado) {
        SwingUtilities.invokeLater(() -> btnListo.setEnabled(habilitado));
    }

    public void actualizarEstado(String texto) {
        SwingUtilities.invokeLater(() -> lblEstadoPartida.setText("Estado: " + texto));
    }

    public void actualizarSaldo(String saldo) {
        SwingUtilities.invokeLater(() -> lblSaldoFichas.setText("Fichas: " + saldo + " 🪙"));
    }

    public void actualizarCrupier(String cartasTexto, int puntos) {
        SwingUtilities.invokeLater(() -> {
            lblPuntosCrupier.setText("Crupier — Puntos: " + puntos);
            pintarCartas(panelCartasCrupier, cartasTexto);
        });
    }

    public void actualizarJugador(String cartasTexto, int puntos) {
        SwingUtilities.invokeLater(() -> {
            // Evaluamos puntuación dual si hay Ases en la mano
            String textoPuntos = formatearPuntosDual(cartasTexto, puntos);
            lblPuntosJugador.setText("Tu Mano — Puntos: " + textoPuntos);
            pintarCartas(panelCartasJugador, cartasTexto);
        });
    }

    /**
     * Comprueba si la mano contiene un As y muestra formato dual (ej: 6 / 16).
     */
    private String formatearPuntosDual(String cartasTexto, int puntosOptimos) {
        if (cartasTexto == null || cartasTexto.isEmpty()) return String.valueOf(puntosOptimos);
        
        boolean tieneAs = cartasTexto.contains("As");
        // Si hay As y se puede restar 10 sin bajar de 1 (es decir, el As está contando como 11)
        if (tieneAs && puntosOptimos <= 21 && (puntosOptimos - 10) > 0) {
            int valorAlternativo = puntosOptimos - 10;
            return valorAlternativo + " / " + puntosOptimos;
        }
        return String.valueOf(puntosOptimos);
    }

    public void limpiarMesa() {
        SwingUtilities.invokeLater(() -> {
            lblPuntosCrupier.setText("Crupier — Puntos: 0");
            lblPuntosJugador.setText("Tu Mano — Puntos: 0");
            panelCartasCrupier.removeAll();
            panelCartasJugador.removeAll();
            panelCartasCrupier.revalidate();
            panelCartasCrupier.repaint();
            panelCartasJugador.revalidate();
            panelCartasJugador.repaint();
        });
    }

    public void agregarMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtMensajes.append(mensaje + "\n");
            txtMensajes.setCaretPosition(txtMensajes.getDocument().getLength());
        });
    }

    private void pintarCartas(JPanel contenedor, String cartasTexto) {
        contenedor.removeAll();

        if (cartasTexto != null && !cartasTexto.trim().isEmpty() && !"-".equals(cartasTexto.trim())) {
            String[] cartas = cartasTexto.split(",");
            for (String carta : cartas) {
                String nombreLimpio = carta.trim();
                if (!nombreLimpio.isEmpty()) {
                    JLabel lblCarta = new JLabel(GestorCartas.obtenerIconoCarta(nombreLimpio));
                    lblCarta.setToolTipText(nombreLimpio);
                    contenedor.add(lblCarta);
                }
            }
        }

        contenedor.revalidate();
        contenedor.repaint();
    }

    // --- GETTERS ---

    public boolean isModoAutoActivo() { return chkModoAuto.isSelected(); }

    public JButton getBtnDesconectar() { return btnDesconectar; }
    public JButton getBtnPedirCarta() { return btnPedirCarta; }
    public JButton getBtnPlantarse() { return btnPlantarse; }
    public JButton getBtnListo() { return btnListo; }
    public JCheckBox getChkModoAuto() { return chkModoAuto; }
    public JButton getBtnMisEstadisticas() { return btnMisEstadisticas; }
    public JButton getBtnVerRanking() { return btnVerRanking; }
}