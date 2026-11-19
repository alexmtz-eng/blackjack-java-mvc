package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import bbdd.JugadorDAO;

/**
 * Interfaz gráfica de usuario (GUI) del servidor del juego Blackjack multijugador.
 * 
 * Actúa como panel de control para supervisar el estado de la partida en red
 * (estado del servidor, jugadores conectados, turno actual y registro de eventos)
 * y gestionar la base de datos MySQL a través de {@link JugadorDAO}: consulta de
 * perfiles mediante desplegable, eliminación de jugadores registrados y visualización
 * del ranking ordenado por porcentaje de victorias.
 *
 * @author Alejandro Martínez Torres
 * @version 1.1
 * @since 2026-11-19
 */
public class VistaServidor extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;
    private JLabel lblEstado;
    private JLabel lblConectados;
    private JLabel lblTurno;
    private JTextArea txtLog;
    private JComboBox<String> comboJugadores;
    private JTextArea txtInfoBBDD;
    private JButton btnIniciarServidor;
    private JButton btnActualizarLista;
    private JButton btnEliminarJugador;
    private JButton btnMostrarRanking;

    public VistaServidor() {
        setTitle("Servidor - Casino Blackjack");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 820, 520);
        setMinimumSize(new Dimension(760, 460));

        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // --- PANEL SUPERIOR: Estado del servidor y de la partida ---
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 5));
        panelSuperior.setBorder(new TitledBorder("Estado del Servidor"));

        JPanel panelEtiquetas = new JPanel(new GridLayout(1, 3, 10, 0));
        lblEstado = new JLabel("Estado: Detenido");
        lblConectados = new JLabel("Conectados: 0");
        lblTurno = new JLabel("Turno actual: Ninguno");

        Font fuenteEstado = new Font("SansSerif", Font.BOLD, 13);
        lblEstado.setFont(fuenteEstado);
        lblConectados.setFont(fuenteEstado);
        lblTurno.setFont(fuenteEstado);

        panelEtiquetas.add(lblEstado);
        panelEtiquetas.add(lblConectados);
        panelEtiquetas.add(lblTurno);

        btnIniciarServidor = new JButton("Iniciar Servidor");

        panelSuperior.add(panelEtiquetas, BorderLayout.CENTER);
        panelSuperior.add(btnIniciarServidor, BorderLayout.EAST);
        contentPane.add(panelSuperior, BorderLayout.NORTH);

        // --- PANEL CENTRAL: Izquierda (Log en vivo) | Derecha (Gestión BBDD) ---
        JPanel panelCentral = new JPanel(new GridLayout(1, 2, 10, 0));
        contentPane.add(panelCentral, BorderLayout.CENTER);

        // Panel izquierdo: Registro de actividad del servidor
        JPanel panelLog = new JPanel(new BorderLayout());
        panelLog.setBorder(new TitledBorder("Registro de Actividad y Turnos"));

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollLog = new JScrollPane(txtLog);
        panelLog.add(scrollLog, BorderLayout.CENTER);
        panelCentral.add(panelLog);

        // Panel derecho: Control de Base de Datos (ComboBox, Eliminar y Ranking)
        JPanel panelBBDD = new JPanel(new BorderLayout(5, 8));
        panelBBDD.setBorder(new TitledBorder("Gestión de Base de Datos (MySQL)"));

        JPanel panelControlesBBDD = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel filaSelector = new JPanel(new BorderLayout(5, 0));
        filaSelector.add(new JLabel("Jugador:"), BorderLayout.WEST);
        comboJugadores = new JComboBox<>();
        filaSelector.add(comboJugadores, BorderLayout.CENTER);

        btnActualizarLista = new JButton("Recargar");
        filaSelector.add(btnActualizarLista, BorderLayout.EAST);

        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        btnEliminarJugador = new JButton("Eliminar Jugador");
        btnMostrarRanking = new JButton("Mostrar Ranking");
        filaBotones.add(btnEliminarJugador);
        filaBotones.add(btnMostrarRanking);

        panelControlesBBDD.add(filaSelector);
        panelControlesBBDD.add(filaBotones);
        panelBBDD.add(panelControlesBBDD, BorderLayout.NORTH);

        txtInfoBBDD = new JTextArea();
        txtInfoBBDD.setEditable(false);
        txtInfoBBDD.setLineWrap(true);
        txtInfoBBDD.setWrapStyleWord(true);
        txtInfoBBDD.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollInfoBBDD = new JScrollPane(txtInfoBBDD);
        panelBBDD.add(scrollInfoBBDD, BorderLayout.CENTER);
        panelCentral.add(panelBBDD);

        // --- EVENTOS DE LA INTERFAZ ---
        configurarEventos();

        // Carga inicial de jugadores desde MySQL al abrir la ventana
        cargarJugadores();
        agregarLog("Panel de servidor listo. Conectado a la BBDD local.");
    }

    private void configurarEventos() {
        // Mostrar datos del jugador al seleccionarlo en el desplegable
        comboJugadores.addActionListener(e -> {
            String seleccionado = (String) comboJugadores.getSelectedItem();
            if (seleccionado != null) {
                txtInfoBBDD.setText(JugadorDAO.obtenerDatosJugador(seleccionado));
            }
        });

        // Recargar lista de jugadores desde MySQL
        btnActualizarLista.addActionListener(e -> {
            cargarJugadores();
            agregarLog("Lista de jugadores recargada desde MySQL.");
        });

        // Eliminar el jugador seleccionado con confirmación
        btnEliminarJugador.addActionListener(e -> {
            String seleccionado = (String) comboJugadores.getSelectedItem();
            if (seleccionado == null) {
                JOptionPane.showMessageDialog(this, "No hay ningún jugador seleccionado.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int opcion = JOptionPane.showConfirmDialog(this,
                    "¿Seguro que quieres eliminar al jugador '" + seleccionado + "' de la base de datos?",
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

            if (opcion == JOptionPane.YES_OPTION) {
                if (JugadorDAO.eliminarJugador(seleccionado)) {
                    agregarLog("Jugador eliminado de la BBDD: " + seleccionado);
                    cargarJugadores();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar al jugador.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Mostrar el ranking de jugadores por porcentaje de victorias
        btnMostrarRanking.addActionListener(e -> {
            txtInfoBBDD.setText(JugadorDAO.obtenerRanking());
            agregarLog("Consulta de ranking global ejecutada.");
        });
    }

    // --- MÉTODOS PÚBLICOS (Seguros para llamar desde hilos de Sockets) ---

    public void cargarJugadores() {
        SwingUtilities.invokeLater(() -> {
            comboJugadores.removeAllItems();
            ArrayList<String> nombres = JugadorDAO.obtenerNombresJugadores();
            if (nombres.isEmpty()) {
                txtInfoBBDD.setText("No hay jugadores registrados en la base de datos.");
            } else {
                for (String nombre : nombres) {
                    comboJugadores.addItem(nombre);
                }
                String primero = (String) comboJugadores.getSelectedItem();
                if (primero != null) {
                    txtInfoBBDD.setText(JugadorDAO.obtenerDatosJugador(primero));
                }
            }
        });
    }

    public void agregarLog(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(mensaje + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    public void actualizarEstado(String estado) {
        SwingUtilities.invokeLater(() -> lblEstado.setText("Estado: " + estado));
    }

    public void actualizarConectados(int cantidad, String nombres) {
        SwingUtilities.invokeLater(() -> {
            String detalle = (nombres == null || nombres.isEmpty()) ? "" : " (" + nombres + ")";
            String texto = "Conectados: " + cantidad + detalle;
            lblConectados.setText(texto);
            lblConectados.setToolTipText(texto);
        });
    }

    public void actualizarTurno(String nombreJugador) {
        SwingUtilities.invokeLater(() -> lblTurno.setText("Turno actual: " + nombreJugador));
    }

    public JButton getBtnIniciarServidor() {
        return btnIniciarServidor;
    }

    // Lanzador para probar la ventana de forma independiente
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                VistaServidor frame = new VistaServidor();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}