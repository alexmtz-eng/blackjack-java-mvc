package red;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import bbdd.JugadorDAO;
import modelo.Baraja;
import modelo.Carta;
import modelo.Jugador;
import vista.VistaServidor;

/**
 * Servidor TCP multihilo para el Blackjack multijugador.
 * Abre el ServerSocket en el puerto definido en Protocolo, coordina a los clientes
 * conectados mediante hilos ManejadorCliente, controla la baraja, los turnos y la lógica
 * del crupier, persiste resultados en MySQL a través de JugadorDAO y actualiza VistaServidor.
 *
 * @author Alejandro Martínez Torres
 * @version 1.1
 * @since 2026-11-19
 */
public class ServidorBlackjack {

    private final VistaServidor vista;

    private ServerSocket serverSocket;
    private volatile boolean servidorActivo = false;

    private final List<ManejadorCliente> clientesConectados = new ArrayList<>();
    private final List<ManejadorCliente> participantesRonda = new ArrayList<>();

    private Baraja baraja;
    private final Jugador crupier = new Jugador("Crupier");
    private boolean rondaEnCurso = false;
    private int indiceTurnoActual = -1;

    public ServidorBlackjack(VistaServidor vista) {
        this.vista = vista;
        configurarEventos();
    }

    private void configurarEventos() {
        vista.getBtnIniciarServidor().addActionListener(e -> {
            if (!servidorActivo) {
                iniciarServidor();
            } else {
                detenerServidor();
            }
        });

        vista.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                detenerServidor();
            }
        });
    }

    /**
     * Abre el ServerSocket y lanza el hilo aceptador para recibir clientes sin bloquear Swing.
     */
    public void iniciarServidor() {
        try {
            serverSocket = new ServerSocket(Protocolo.PUERTO);
            servidorActivo = true;

            vista.actualizarEstado("Activo (" + Protocolo.PUERTO + ")");
            vista.getBtnIniciarServidor().setText("Detener Servidor");
            vista.agregarLog("Servidor iniciado en el puerto " + Protocolo.PUERTO + ". Esperando jugadores...");

            Thread hiloAceptador = new Thread(() -> {
                while (servidorActivo) {
                    try {
                        Socket socketCliente = serverSocket.accept();
                        vista.agregarLog("Nueva conexión entrante desde " + socketCliente.getRemoteSocketAddress());

                        ManejadorCliente manejador = new ManejadorCliente(socketCliente, this);
                        Thread hiloCliente = new Thread(manejador, "Cliente-" + socketCliente.getPort());
                        hiloCliente.start();
                    } catch (IOException e) {
                        if (servidorActivo) {
                            vista.agregarLog("Error aceptando conexión: " + e.getMessage());
                        }
                    }
                }
            }, "Hilo-Aceptador-Servidor");

            hiloAceptador.start();

        } catch (IOException e) {
            vista.agregarLog("No se pudo abrir el puerto " + Protocolo.PUERTO + ": " + e.getMessage());
        }
    }

    /**
     * Cierra el ServerSocket y desconecta limpiamente a todos los clientes activos.
     */
    public synchronized void detenerServidor() {
        if (!servidorActivo) {
            return;
        }
        servidorActivo = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        for (ManejadorCliente cliente : new ArrayList<>(clientesConectados)) {
            cliente.enviar(Protocolo.INFO + Protocolo.SEP + "El servidor se ha detenido.");
            cliente.cerrar();
        }

        clientesConectados.clear();
        participantesRonda.clear();
        rondaEnCurso = false;
        indiceTurnoActual = -1;

        vista.actualizarEstado("Detenido");
        vista.actualizarConectados(0, "");
        vista.actualizarTurno("Ninguno");
        vista.getBtnIniciarServidor().setText("Iniciar Servidor");
        vista.agregarLog("Servidor detenido.");
    }

    /**
     * Punto de entrada sincronizado para procesar cualquier comando recibido desde un cliente.
     */
    public synchronized void procesarMensajeCliente(ManejadorCliente cliente, String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            return;
        }

        String[] partes = linea.split(Protocolo.SEP_REGEX, -1);
        String comando = partes[0];

        if (!cliente.isAutenticado()) {
            if (Protocolo.LOGIN.equals(comando) && partes.length >= 3) {
                gestionarLogin(cliente, partes[1].trim(), partes[2].trim());
            } else {
                cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Debes autenticarte primero.");
                cliente.cerrar();
            }
            return;
        }

        switch (comando) {
            case Protocolo.LISTO:
                gestionarListo(cliente);
                break;

            case Protocolo.PEDIR:
                gestionarPedirCarta(cliente);
                break;

            case Protocolo.PLANTARSE:
                gestionarPlantarse(cliente);
                break;

            case Protocolo.STATS:
                String datos = JugadorDAO.obtenerDatosJugador(cliente.getJugador().getNombre());
                cliente.enviar(Protocolo.INFO + Protocolo.SEP + Protocolo.codificarTexto(datos));
                break;

            case Protocolo.RANKING:
                String ranking = JugadorDAO.obtenerRanking();
                cliente.enviar(Protocolo.INFO + Protocolo.SEP + Protocolo.codificarTexto(ranking));
                break;

            case Protocolo.DESCONECTAR:
                cliente.cerrar();
                break;

            default:
                cliente.enviar(Protocolo.INFO + Protocolo.SEP + "Comando no reconocido: " + comando);
                break;
        }
    }

    private void gestionarLogin(ManejadorCliente cliente, String nombre, String password) {
        if (nombre.isEmpty() || password.isEmpty()) {
            cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Usuario y contraseña obligatorios.");
            cliente.cerrar();
            return;
        }

        // Evita sesiones duplicadas con el mismo usuario
        for (ManejadorCliente conectado : clientesConectados) {
            if (conectado.getJugador() != null
                    && conectado.getJugador().getNombre().equalsIgnoreCase(nombre)) {
                cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Ese usuario ya está conectado.");
                cliente.cerrar();
                return;
            }
        }

        String resultado = JugadorDAO.autenticarORegistrar(nombre, password);

        if ("LOGIN_OK".equals(resultado) || "REGISTRO_OK".equals(resultado)) {
            cliente.setJugador(new Jugador(nombre));
            cliente.setAutenticado(true);
            clientesConectados.add(cliente);

            cliente.enviar(Protocolo.LOGIN_OK + Protocolo.SEP + nombre);

            if ("REGISTRO_OK".equals(resultado)) {
                cliente.enviar(Protocolo.INFO + Protocolo.SEP + "Cuenta nueva registrada automáticamente.");
                vista.cargarJugadores();
                vista.agregarLog("Nuevo jugador registrado y conectado: " + nombre);
            } else {
                vista.agregarLog("Jugador autenticado: " + nombre);
            }

            if (rondaEnCurso) {
                cliente.enviar(Protocolo.ESTADO + Protocolo.SEP
                        + "Hay una ronda en curso. Pulsa 'Listo / Nueva Ronda' para unirte a la siguiente.");
            }

            actualizarResumenConectados();
            difundirInfo(nombre + " ha entrado en la mesa.");

        } else if ("PASSWORD_INCORRECTA".equals(resultado)) {
            cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Contraseña incorrecta.");
            vista.agregarLog("Intento fallido de login para: " + nombre);
            cliente.cerrar();
        } else {
            cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Error de servidor al consultar la BBDD.");
            cliente.cerrar();
        }
    }

    private void gestionarListo(ManejadorCliente cliente) {
        if (cliente.isEnRonda() || cliente.isListo()) {
            return;
        }

        cliente.setListo(true);
        String nombre = cliente.getJugador().getNombre();
        vista.agregarLog(nombre + " está listo para jugar.");
        difundirInfo(nombre + " está listo.");

        comprobarInicioRonda();
    }

    private void comprobarInicioRonda() {
        if (rondaEnCurso || clientesConectados.isEmpty()) {
            return;
        }

        for (ManejadorCliente c : clientesConectados) {
            if (!c.isListo()) {
                return;
            }
        }

        iniciarRonda();
    }

    private void iniciarRonda() {
        rondaEnCurso = true;
        participantesRonda.clear();
        baraja = new Baraja();
        baraja.barajar();
        crupier.vaciarMano();

        for (ManejadorCliente c : clientesConectados) {
            if (c.isListo()) {
                c.setListo(false);
                c.setEnRonda(true);
                c.setTurnoTerminado(false);
                c.getJugador().vaciarMano();
                participantesRonda.add(c);
            }
        }

        // Reparto inicial: 2 cartas para cada jugador y 2 para el crupier
        for (int i = 0; i < 2; i++) {
            for (ManejadorCliente c : participantesRonda) {
                c.getJugador().recibirCarta(sacarCarta());
            }
            crupier.recibirCarta(sacarCarta());
        }

        vista.agregarLog("=== NUEVA RONDA INICIADA (" + participantesRonda.size() + " jugador/es) ===");
        difundirInfo("=== Nueva ronda iniciada ===");

        for (ManejadorCliente c : participantesRonda) {
            enviarEstadoMesa(c, true);
        }

        indiceTurnoActual = -1;
        avanzarTurno();
    }

    private void avanzarTurno() {
        while (true) {
            indiceTurnoActual++;

            if (indiceTurnoActual >= participantesRonda.size()) {
                resolverRondaCrupier();
                return;
            }

            ManejadorCliente actual = participantesRonda.get(indiceTurnoActual);
            if (!actual.isActivo() || !actual.isEnRonda() || actual.isTurnoTerminado()) {
                continue;
            }

            // Si sale con 21 de inicio (Blackjack natural), cierra turno automáticamente
            if (actual.getJugador().calcularPuntos() == 21) {
                actual.setTurnoTerminado(true);
                actual.enviar(Protocolo.INFO + Protocolo.SEP + "¡Blackjack natural (21)! Te plantas automáticamente.");
                vista.agregarLog(actual.getJugador().getNombre() + " obtiene Blackjack natural (21).");
                continue;
            }

            String nombreTurno = actual.getJugador().getNombre();
            vista.actualizarTurno(nombreTurno);
            vista.agregarLog("Turno de " + nombreTurno + ".");

            for (ManejadorCliente p : participantesRonda) {
                if (p == actual) {
                    p.enviar(Protocolo.TURNO + Protocolo.SEP + Protocolo.TURNO_TUYO);
                } else {
                    p.enviar(Protocolo.TURNO + Protocolo.SEP + Protocolo.TURNO_ESPERA + Protocolo.SEP + nombreTurno);
                }
            }
            return;
        }
    }

    private void gestionarPedirCarta(ManejadorCliente cliente) {
        if (!rondaEnCurso || !esTurnoDe(cliente)) {
            return;
        }

        Carta nueva = sacarCarta();
        cliente.getJugador().recibirCarta(nueva);
        int puntos = cliente.getJugador().calcularPuntos();
        String nombre = cliente.getJugador().getNombre();

        vista.agregarLog(nombre + " pide carta: " + nueva + " (Total: " + puntos + ")");
        enviarEstadoMesa(cliente, true);

        if (puntos > 21) {
            cliente.setTurnoTerminado(true);
            cliente.enviar(Protocolo.INFO + Protocolo.SEP + "Sacaste " + nueva + ". Te has pasado con " + puntos + " puntos.");
            difundirInfo(nombre + " se ha pasado con " + puntos + " puntos.");
            avanzarTurno();
        } else if (puntos == 21) {
            cliente.setTurnoTerminado(true);
            cliente.enviar(Protocolo.INFO + Protocolo.SEP + "Sacaste " + nueva + ". ¡21 exactos! Turno completado.");
            difundirInfo(nombre + " ha llegado a 21.");
            avanzarTurno();
        } else {
            // Sigue siendo su turno: rehabilitamos controles (o siguiente paso de Modo Auto)
            cliente.enviar(Protocolo.TURNO + Protocolo.SEP + Protocolo.TURNO_TUYO);
        }
    }

    private void gestionarPlantarse(ManejadorCliente cliente) {
        if (!rondaEnCurso || !esTurnoDe(cliente)) {
            return;
        }

        cliente.setTurnoTerminado(true);
        int puntos = cliente.getJugador().calcularPuntos();
        String nombre = cliente.getJugador().getNombre();

        vista.agregarLog(nombre + " se planta con " + puntos + " puntos.");
        cliente.enviar(Protocolo.INFO + Protocolo.SEP + "Te plantas con " + puntos + " puntos.");
        difundirInfo(nombre + " se planta con " + puntos + " puntos.");

        avanzarTurno();
    }

    private void resolverRondaCrupier() {
        vista.actualizarTurno("Crupier");

        // Comprobamos si queda algún jugador sin pasarse de 21
        boolean hayJugadoresVivos = false;
        for (ManejadorCliente p : participantesRonda) {
            if (p.isActivo() && p.getJugador().calcularPuntos() <= 21) {
                hayJugadoresVivos = true;
                break;
            }
        }

        // El crupier roba hasta alcanzar al menos 17 puntos
        while (hayJugadoresVivos && crupier.calcularPuntos() < 17) {
            crupier.recibirCarta(sacarCarta());
        }

        int puntosCrupier = crupier.calcularPuntos();
        vista.agregarLog("Mano final del Crupier: " + formatearMano(crupier.getMano(), false)
                + " (" + puntosCrupier + " puntos)");

        for (ManejadorCliente p : participantesRonda) {
            String nombre = p.getJugador().getNombre();
            int puntosJugador = p.getJugador().calcularPuntos();

            String resultadoBD;
            String mensajeFinal;

            if (puntosJugador > 21) {
                resultadoBD = "PERDIDA";
                mensajeFinal = "Resultado: DERROTA (te pasaste con " + puntosJugador
                        + " vs " + puntosCrupier + " del crupier).";
            } else if (puntosCrupier > 21 || puntosJugador > puntosCrupier) {
                resultadoBD = "GANADA";
                mensajeFinal = "Resultado: ¡VICTORIA! (" + puntosJugador
                        + " vs " + puntosCrupier + " del crupier).";
            } else if (puntosJugador == puntosCrupier) {
                resultadoBD = "EMPATADA";
                mensajeFinal = "Resultado: EMPATE (" + puntosJugador + " puntos ambos).";
            } else {
                resultadoBD = "PERDIDA";
                mensajeFinal = "Resultado: DERROTA (" + puntosJugador
                        + " vs " + puntosCrupier + " del crupier).";
            }

            JugadorDAO.registrarResultado(nombre, resultadoBD);
            vista.agregarLog("  -> " + nombre + ": " + resultadoBD + " (" + puntosJugador + " pts)");

            if (p.isActivo()) {
                enviarEstadoMesa(p, false);
                p.enviar(Protocolo.FIN_RONDA + Protocolo.SEP + Protocolo.codificarTexto(mensajeFinal));
            }

            p.setEnRonda(false);
            p.setTurnoTerminado(false);
            p.setListo(false);
        }

        participantesRonda.clear();
        rondaEnCurso = false;
        indiceTurnoActual = -1;
        vista.actualizarTurno("Ninguno");
        vista.cargarJugadores();

        // Por si algún jugador que entró a mitad ya había pulsado Listo
        comprobarInicioRonda();
    }

    /**
     * Limpia un cliente desconectado y avanza el turno si abandonó en mitad de su jugada.
     */
    public synchronized void desconectarCliente(ManejadorCliente cliente) {
        boolean estabaEnLista = clientesConectados.remove(cliente);
        if (!estabaEnLista) {
            return;
        }

        String nombre = cliente.getNombreSeguro();
        vista.agregarLog("Jugador desconectado: " + nombre);
        actualizarResumenConectados();
        difundirInfo(nombre + " ha abandonado la mesa.");

        if (rondaEnCurso && cliente.isEnRonda()) {
            boolean eraSuTurno = esTurnoDe(cliente);
            cliente.setEnRonda(false);
            cliente.setTurnoTerminado(true);
            if (eraSuTurno) {
                avanzarTurno();
            }
        } else if (!rondaEnCurso) {
            comprobarInicioRonda();
        }
    }

    public void registrarEventoRed(String mensaje) {
        vista.agregarLog(mensaje);
    }

    // --- UTILIDADES INTERNAS ---

    private boolean esTurnoDe(ManejadorCliente cliente) {
        return indiceTurnoActual >= 0
                && indiceTurnoActual < participantesRonda.size()
                && participantesRonda.get(indiceTurnoActual) == cliente;
    }

    private Carta sacarCarta() {
        Carta carta = baraja.repartirCarta();
        if (carta == null) {
            baraja = new Baraja();
            baraja.barajar();
            carta = baraja.repartirCarta();
        }
        return carta;
    }

    private void enviarEstadoMesa(ManejadorCliente cliente, boolean ocultarSegundaCrupier) {
        String cartasJugador = formatearMano(cliente.getJugador().getMano(), false);
        int ptsJugador = cliente.getJugador().calcularPuntos();

        String cartasCrupier = formatearMano(crupier.getMano(), ocultarSegundaCrupier);
        int ptsCrupier = ocultarSegundaCrupier
                ? (crupier.getMano().isEmpty() ? 0 : crupier.getMano().get(0).getValor())
                : crupier.calcularPuntos();

        cliente.enviar(Protocolo.MESA
                + Protocolo.SEP + cartasJugador
                + Protocolo.SEP + ptsJugador
                + Protocolo.SEP + cartasCrupier
                + Protocolo.SEP + ptsCrupier);
    }

    private String formatearMano(List<Carta> mano, boolean ocultarSegunda) {
        if (mano.isEmpty()) {
            return "-";
        }
        if (ocultarSegunda && mano.size() >= 2) {
            return mano.get(0).toString() + ", [Oculta]";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mano.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(mano.get(i).toString());
        }
        return sb.toString();
    }

    private void difundirInfo(String mensaje) {
        String payload = Protocolo.INFO + Protocolo.SEP + Protocolo.codificarTexto(mensaje);
        for (ManejadorCliente c : clientesConectados) {
            c.enviar(payload);
        }
    }

    private void actualizarResumenConectados() {
        StringBuilder nombres = new StringBuilder();
        for (int i = 0; i < clientesConectados.size(); i++) {
            if (i > 0) nombres.append(", ");
            nombres.append(clientesConectados.get(i).getJugador().getNombre());
        }
        vista.actualizarConectados(clientesConectados.size(), nombres.toString());
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            VistaServidor vista = new VistaServidor();
            vista.setLocationRelativeTo(null);
            new ServidorBlackjack(vista);
            vista.setVisible(true);
        });
    }
}