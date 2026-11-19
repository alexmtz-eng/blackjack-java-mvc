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

/**
 * Controlador de red del cliente para el Blackjack multijugador.
 * Establece la conexión TCP con el servidor, gestiona el envío de acciones desde
 * VistaCliente, mantiene un hilo de escucha para los mensajes entrantes e implementa
 * la lógica de decisión del modo automático.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */
public class ClienteBlackjack {

    private final VistaCliente vista;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    private volatile boolean conectado = false;
    private volatile boolean esMiTurno = false;
    private volatile int puntosActuales = 0;
    private volatile boolean accionAutoPendiente = false;

    public ClienteBlackjack(VistaCliente vista) {
        this.vista = vista;
        configurarEventos();
    }

    private void configurarEventos() {
        vista.getBtnConectar().addActionListener(e -> conectar());
        vista.getBtnDesconectar().addActionListener(e -> desconectar());

        vista.getBtnListo().addActionListener(e -> {
            if (conectado) {
                vista.setListoHabilitado(false);
                vista.limpiarMesa();
                vista.actualizarEstado("Listo. Esperando al resto de jugadores...");
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
                vista.agregarMensaje("[Sistema] Modo Automático activado (se planta con 17 o más).");
                if (esMiTurno) {
                    programarJugadaAutomatica();
                }
            } else {
                vista.agregarMensaje("[Sistema] Modo Automático desactivado (control manual).");
            }
        });

        vista.getBtnMisEstadisticas().addActionListener(e -> {
            if (conectado) {
                enviar(Protocolo.STATS);
            }
        });

        vista.getBtnVerRanking().addActionListener(e -> {
            if (conectado) {
                enviar(Protocolo.RANKING);
            }
        });

        // Cierre limpio del socket si el usuario cierra la ventana con la X
        vista.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectar();
            }
        });
    }

    /**
     * Valida los campos de acceso y abre el socket TCP en un hilo secundario
     * para no bloquear el hilo gráfico de Swing.
     */
    private void conectar() {
        String host = vista.getHost();
        String usuario = vista.getNombreUsuario();
        String password = vista.getPassword();

        if (host.isEmpty() || usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "Debes rellenar IP, usuario y contraseña.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (usuario.contains(Protocolo.SEP) || password.contains(Protocolo.SEP)) {
            JOptionPane.showMessageDialog(vista,
                    "El usuario y la contraseña no pueden contener el carácter '" + Protocolo.SEP + "'.",
                    "Carácter no permitido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        vista.getBtnConectar().setEnabled(false);
        vista.actualizarEstado("Conectando a " + host + ":" + Protocolo.PUERTO + "...");

        Thread hiloConexion = new Thread(() -> {
            try {
                socket = new Socket(host, Protocolo.PUERTO);
                entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                salida = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                conectado = true;

                // Enviamos las credenciales nada más abrir el canal
                enviar(Protocolo.LOGIN + Protocolo.SEP + usuario + Protocolo.SEP + password);

                // Bucle bloqueante de escucha de mensajes del servidor
                escucharServidor();

            } catch (IOException ex) {
                if (conectado) {
                    vista.agregarMensaje("[Red] Se perdió la conexión con el servidor.");
                } else {
                    vista.agregarMensaje("[Red] No se pudo conectar con " + host + ":" + Protocolo.PUERTO
                            + " (" + ex.getMessage() + ")");
                }
                cerrarRecursosRed();
                vista.setConectado(false);
            }
        }, "Hilo-Escucha-Cliente");

        hiloConexion.start();
    }

    /**
     * Lee línea a línea los comandos que llegan por el socket hasta que el servidor
     * cierra la conexión o el cliente se desconecta.
     */
    private void escucharServidor() throws IOException {
        String linea;
        while (conectado && (linea = entrada.readLine()) != null) {
            procesarMensaje(linea);
        }

        // Si readLine() devuelve null, el servidor cerró la conexión limpiamente
        if (conectado) {
            vista.agregarMensaje("[Red] El servidor ha cerrado la conexión.");
            cerrarRecursosRed();
            vista.setConectado(false);
        }
    }

    /**
     * Interpreta cada mensaje recibido según las reglas de Protocolo y actualiza la vista.
     */
    private void procesarMensaje(String linea) {
        String[] partes = linea.split(Protocolo.SEP_REGEX, -1);
        String comando = partes[0];

        switch (comando) {
            case Protocolo.LOGIN_OK:
                String nombreValidado = partes.length > 1 ? partes[1] : vista.getNombreUsuario();
                vista.setConectado(true);
                vista.limpiarMesa();
                vista.actualizarEstado("Conectado como " + nombreValidado + ". Pulsa 'Listo / Nueva Ronda'.");
                vista.agregarMensaje("=== Sesión iniciada correctamente como " + nombreValidado + " ===");
                break;

            case Protocolo.LOGIN_ERROR:
                String motivo = partes.length > 1 ? partes[1] : "Credenciales inválidas.";
                vista.agregarMensaje("[Acceso denegado] " + motivo);
                desconectar();
                break;

            case Protocolo.ESTADO:
                if (partes.length > 1) {
                    vista.actualizarEstado(partes[1]);
                }
                break;

            case Protocolo.MESA:
                // Formato: MESA|cartasJugador|puntosJugador|cartasCrupier|puntosCrupier
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
                    if (vista.isModoAutoActivo()) {
                        programarJugadaAutomatica();
                    }
                } else {
                    esMiTurno = false;
                    vista.setTurnoActivo(false);
                    String otroJugador = partes.length >= 3 ? partes[2] : "otro jugador";
                    vista.actualizarEstado("Esperando turno de " + otroJugador + "...");
                }
                break;

            case Protocolo.FIN_RONDA:
                esMiTurno = false;
                vista.setTurnoActivo(false);
                vista.setListoHabilitado(true);
                if (partes.length > 1) {
                    vista.agregarMensaje(Protocolo.decodificarTexto(partes[1]));
                }
                vista.actualizarEstado("Ronda finalizada. Pulsa 'Listo / Nueva Ronda' para jugar otra.");
                break;

            case Protocolo.INFO:
                if (partes.length > 1) {
                    vista.agregarMensaje(Protocolo.decodificarTexto(partes[1]));
                }
                break;

            default:
                vista.agregarMensaje("[Servidor] " + linea);
                break;
        }
    }

    /**
     * Ejecuta una jugada automática tras una breve pausa visual (800 ms):
     * pide carta si la mano suma menos de 17 y se planta con 17 o más.
     */
    private void programarJugadaAutomatica() {
        if (accionAutoPendiente) {
            return;
        }
        accionAutoPendiente = true;

        Timer temporizador = new Timer(800, e -> {
            accionAutoPendiente = false;
            if (!conectado || !esMiTurno || !vista.isModoAutoActivo()) {
                return;
            }
            esMiTurno = false;
            vista.setTurnoActivo(false);

            if (puntosActuales < 17) {
                vista.agregarMensaje("[Modo Auto] " + puntosActuales + " puntos (< 17) -> Pidiendo carta...");
                enviar(Protocolo.PEDIR);
            } else {
                vista.agregarMensaje("[Modo Auto] " + puntosActuales + " puntos (>= 17) -> Plantándose.");
                enviar(Protocolo.PLANTARSE);
            }
        });
        temporizador.setRepeats(false);
        temporizador.start();
    }

    private synchronized void enviar(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }

    private void desconectar() {
        boolean estabaConectado = conectado;
        conectado = false;
        esMiTurno = false;

        if (estabaConectado) {
            enviar(Protocolo.DESCONECTAR);
            vista.agregarMensaje("[Red] Desconectado del servidor.");
        }

        cerrarRecursosRed();
        vista.setConectado(false);
    }

    private void cerrarRecursosRed() {
        try {
            if (entrada != null) entrada.close();
        } catch (IOException ignored) {}
        if (salida != null) {
            salida.close();
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private int parseEnteroSeguro(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            VistaCliente vista = new VistaCliente();
            vista.setLocationRelativeTo(null);
            new ClienteBlackjack(vista);
            vista.setVisible(true);
        });
    }
}