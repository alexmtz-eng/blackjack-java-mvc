package red;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import vista.VistaCliente;
import vista.VistaLogin;

/**
 * Controlador de red del cliente para el Blackjack multijugador.
 * Gestiona la comunicación entre VistaLogin, VistaCliente y el servidor TCP.
 *
 * @author Alejandro Martínez Torres
 * @version 3.0
 * @since 2026-11-19
 */
public class ClienteBlackjack {

    private final VistaCliente vista;
    private final VistaLogin vistaLogin;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    private volatile boolean conectado = false;
    private volatile boolean esMiTurno = false;
    private volatile int puntosActuales = 0;
    private volatile boolean accionAutoPendiente = false;

    public ClienteBlackjack(VistaLogin vistaLogin, VistaCliente vista) {
        this.vistaLogin = vistaLogin;
        this.vista = vista;
        configurarEventosVista();
    }

    private void configurarEventosVista() {
        vista.getBtnDesconectar().addActionListener(e -> desconectar());

        vista.getBtnListo().addActionListener(e -> {
            if (conectado) {
                vista.setListoHabilitado(false);
                vista.limpiarMesa();
                vista.actualizarEstado("Apuesta realizada (10 🪙). Esperando al resto...");
                enviar(Protocolo.LISTO);
            }
        });

        vista.getBtnPedirCarta().addActionListener(e -> {
            if (conectado && esMiTurno) {
                esMiTurno = false;
                vista.setTurnoActivo(false);
                enviar(Protocolo.PEDIR);
            }
        });

        vista.getBtnPlantarse().addActionListener(e -> {
            if (conectado && esMiTurno) {
                esMiTurno = false;
                vista.setTurnoActivo(false);
                enviar(Protocolo.PLANTARSE);
            }
        });

        vista.getChkModoAuto().addActionListener(e -> {
            vista.setTurnoActivo(esMiTurno);
            if (vista.isModoAutoActivo()) {
                vista.agregarMensaje("[Sistema] Modo Automático activado.");
                if (esMiTurno) {
                    programarJugadaAutomatica();
                }
            } else {
                vista.agregarMensaje("[Sistema] Modo Automático desactivado.");
            }
        });

        vista.getBtnMisEstadisticas().addActionListener(e -> {
            if (conectado) enviar(Protocolo.STATS);
        });

        vista.getBtnVerRanking().addActionListener(e -> {
            if (conectado) enviar(Protocolo.RANKING);
        });

        vista.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectar();
            }
        });
    }

    public void conectar(String host, String usuario, String password, String tipoAccion) {
        if (usuario.contains(Protocolo.SEP) || password.contains(Protocolo.SEP)) {
            JOptionPane.showMessageDialog(vistaLogin,
                    "El usuario y la contraseña no pueden contener el carácter '" + Protocolo.SEP + "'.",
                    "Carácter no permitido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Thread hiloConexion = new Thread(() -> {
            try {
                socket = new Socket(host, Protocolo.PUERTO);
                entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                salida = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                conectado = true;

                String comando = "LOGIN".equals(tipoAccion) ? Protocolo.LOGIN : Protocolo.REGISTRO;
                enviar(comando + Protocolo.SEP + usuario + Protocolo.SEP + password);

                escucharServidor();

            } catch (IOException ex) {
                if (!conectado) {
                    JOptionPane.showMessageDialog(vistaLogin,
                            "No se pudo conectar con el servidor en " + host + ":" + Protocolo.PUERTO,
                            "Error de conexión", JOptionPane.ERROR_MESSAGE);
                    vistaLogin.setVisible(true);
                }
                cerrarRecursosRed();
            }
        }, "Hilo-Escucha-Cliente");

        hiloConexion.start();
    }

    private void escucharServidor() throws IOException {
        String linea;
        while (conectado && (linea = entrada.readLine()) != null) {
            procesarMensaje(linea);
        }

        if (conectado) {
            vista.agregarMensaje("[Red] Se perdió la conexión con el servidor.");
            cerrarRecursosRed();
            vista.setConectado(false);
            vista.setVisible(false);
            vistaLogin.setVisible(true);
        }
    }

    private void procesarMensaje(String linea) {
        String[] partes = linea.split(Protocolo.SEP_REGEX, -1);
        String comando = partes[0];

        switch (comando) {
            case Protocolo.LOGIN_OK:
            case Protocolo.REGISTRO_OK:
                String usuario = partes.length > 1 ? partes[1] : "Jugador";
                String saldo = partes.length > 2 ? partes[2] : "100";

                vistaLogin.dispose();
                vista.setVisible(true);
                vista.setConectado(true);
                vista.actualizarSaldo(saldo);
                vista.limpiarMesa();
                vista.actualizarEstado("Conectado como " + usuario + ". Pulsa 'Listo / Apostar'.");
                vista.agregarMensaje("=== Sesión iniciada. Fichas: " + saldo + " 🪙 ===");
                break;

            case Protocolo.LOGIN_ERROR:
            case Protocolo.REGISTRO_ERROR:
                String motivo = partes.length > 1 ? partes[1] : "Error de autenticación.";
                JOptionPane.showMessageDialog(vistaLogin, motivo, "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
                cerrarRecursosRed();
                vistaLogin.setVisible(true);
                break;

            case Protocolo.ESTADO:
                if (partes.length > 1) vista.actualizarEstado(partes[1]);
                break;

            case Protocolo.MESA:
                if (partes.length >= 5) {
                    String cartasJugador = partes[1];
                    int ptsJugador = parseEnteroSeguro(partes[2]);
                    String cartasCrupier = partes[3];
                    int ptsCrupier = parseEnteroSeguro(partes[4]);

                    this.puntosActuales = ptsJugador;
                    vista.actualizarJugador(cartasJugador, ptsJugador);
                    vista.actualizarCrupier(cartasCrupier, ptsCrupier);
                }
                break;

            case Protocolo.TURNO:
                if (partes.length >= 2 && Protocolo.TURNO_TUYO.equals(partes[1])) {
                    esMiTurno = true;
                    vista.actualizarEstado("¡Es tu turno!");
                    vista.setTurnoActivo(true);
                    if (vista.isModoAutoActivo()) programarJugadaAutomatica();
                } else {
                    esMiTurno = false;
                    vista.setTurnoActivo(false);
                    String otro = partes.length >= 3 ? partes[2] : "otro jugador";
                    vista.actualizarEstado("Esperando turno de " + otro + "...");
                }
                break;

            case Protocolo.FIN_RONDA:
                esMiTurno = false;
                vista.setTurnoActivo(false);
                vista.setListoHabilitado(true);
                if (partes.length >= 3) {
                    vista.agregarMensaje(Protocolo.decodificarTexto(partes[1]));
                    vista.actualizarSaldo(partes[2]);
                } else if (partes.length > 1) {
                    vista.agregarMensaje(Protocolo.decodificarTexto(partes[1]));
                }
                vista.actualizarEstado("Ronda finalizada. Pulsa 'Listo / Apostar' para otra mano.");
                break;

            case Protocolo.INFO:
                if (partes.length > 1) vista.agregarMensaje(Protocolo.decodificarTexto(partes[1]));
                break;

            default:
                break;
        }
    }

    private void programarJugadaAutomatica() {
        if (accionAutoPendiente) return;
        accionAutoPendiente = true;

        Timer t = new Timer(800, e -> {
            accionAutoPendiente = false;
            if (!conectado || !esMiTurno || !vista.isModoAutoActivo()) return;
            esMiTurno = false;
            vista.setTurnoActivo(false);

            if (puntosActuales < 17) {
                vista.agregarMensaje("[Modo Auto] " + puntosActuales + " pts (< 17) -> Pidiendo carta...");
                enviar(Protocolo.PEDIR);
            } else {
                vista.agregarMensaje("[Modo Auto] " + puntosActuales + " pts (>= 17) -> Plantándose.");
                enviar(Protocolo.PLANTARSE);
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private synchronized void enviar(String mensaje) {
        if (salida != null) salida.println(mensaje);
    }

    private void desconectar() {
        conectado = false;
        esMiTurno = false;
        enviar(Protocolo.DESCONECTAR);
        cerrarRecursosRed();
        vista.setVisible(false);
        vistaLogin.setVisible(true);
    }

    private void cerrarRecursosRed() {
        try { if (entrada != null) entrada.close(); } catch (IOException ignored) {}
        if (salida != null) salida.close();
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }

    private int parseEnteroSeguro(String v) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            VistaLogin vistaLogin = new VistaLogin();
            VistaCliente vistaCliente = new VistaCliente();
            ClienteBlackjack controlador = new ClienteBlackjack(vistaLogin, vistaCliente);

            vistaLogin.getBtnIniciarSesion().addActionListener(e -> {
                if (vistaLogin.isDatosValidados()) {
                    controlador.conectar(
                        vistaLogin.getHost(), 
                        vistaLogin.getUsuario(), 
                        vistaLogin.getPassword(), 
                        "LOGIN"
                    );
                }
            });

            vistaLogin.getBtnRegistrarse().addActionListener(e -> {
                if (vistaLogin.isDatosValidados()) {
                    controlador.conectar(
                        vistaLogin.getHost(), 
                        vistaLogin.getUsuario(), 
                        vistaLogin.getPassword(), 
                        "REGISTRO"
                    );
                }
            });

            vistaLogin.setVisible(true);
        });
    }
}