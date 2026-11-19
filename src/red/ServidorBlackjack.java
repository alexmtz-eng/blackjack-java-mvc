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
 * Servidor TCP multihilo para el Blackjack multijugador con gestión de fichas y banca.
 *
 * @author Alejandro Martínez Torres
 * @version 2.0
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

    private static final int APUESTA_DEFECTO = 10;

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

    public synchronized void detenerServidor() {
        if (!servidorActivo) return;
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

    public synchronized void procesarMensajeCliente(ManejadorCliente cliente, String linea) {
        if (linea == null || linea.trim().isEmpty()) return;

        String[] partes = linea.split(Protocolo.SEP_REGEX, -1);
        String comando = partes[0];

        if (!cliente.isAutenticado()) {
            if (Protocolo.LOGIN.equals(comando) && partes.length >= 3) {
                gestionarLogin(cliente, partes[1].trim(), partes[2].trim());
            } else if (Protocolo.REGISTRO.equals(comando) && partes.length >= 3) {
                gestionarRegistro(cliente, partes[1].trim(), partes[2].trim());
            } else {
                cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Debes iniciar sesión o registrarte.");
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
                break;
        }
    }

    private void gestionarLogin(ManejadorCliente cliente, String nombre, String password) {
        for (ManejadorCliente conectado : clientesConectados) {
            if (conectado.getJugador() != null && conectado.getJugador().getNombre().equalsIgnoreCase(nombre)) {
                cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Ese usuario ya tiene una sesión activa.");
                cliente.cerrar();
                return;
            }
        }

        String resultado = JugadorDAO.iniciarSesion(nombre, password);
        if ("LOGIN_OK".equals(resultado)) {
            cliente.setJugador(new Jugador(nombre));
            cliente.setAutenticado(true);
            clientesConectados.add(cliente);

            int saldo = JugadorDAO.obtenerSaldo(nombre);
            cliente.enviar(Protocolo.LOGIN_OK + Protocolo.SEP + nombre + Protocolo.SEP + saldo);
            vista.agregarLog("Login exitoso: " + nombre + " (Fichas: " + saldo + ")");
            actualizarResumenConectados();
            difundirInfo(nombre + " se ha unido a la mesa.");
        } else if ("PASSWORD_INCORRECTA".equals(resultado)) {
            cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "Contraseña incorrecta.");
            cliente.cerrar();
        } else {
            cliente.enviar(Protocolo.LOGIN_ERROR + Protocolo.SEP + "El usuario no existe. Regístrate primero.");
            cliente.cerrar();
        }
    }

    private void gestionarRegistro(ManejadorCliente cliente, String nombre, String password) {
        if (nombre.isEmpty() || password.isEmpty()) {
            cliente.enviar(Protocolo.REGISTRO_ERROR + Protocolo.SEP + "Rellena todos los campos.");
            cliente.cerrar();
            return;
        }

        String resultado = JugadorDAO.registrarJugador(nombre, password);
        if ("REGISTRO_OK".equals(resultado)) {
            cliente.setJugador(new Jugador(nombre));
            cliente.setAutenticado(true);
            clientesConectados.add(cliente);

            cliente.enviar(Protocolo.REGISTRO_OK + Protocolo.SEP + nombre + Protocolo.SEP + "100");
            vista.cargarJugadores();
            vista.agregarLog("Nuevo usuario registrado: " + nombre + " (100 fichas iniciales)");
            actualizarResumenConectados();
            difundirInfo(nombre + " se ha registrado y unido a la mesa.");
        } else {
            cliente.enviar(Protocolo.REGISTRO_ERROR + Protocolo.SEP + "El nombre de usuario ya está en uso.");
            cliente.cerrar();
        }
    }

    private void gestionarListo(ManejadorCliente cliente) {
        if (cliente.isEnRonda() || cliente.isListo()) return;

        int saldoActual = JugadorDAO.obtenerSaldo(cliente.getJugador().getNombre());
        if (saldoActual < APUESTA_DEFECTO) {
            cliente.enviar(Protocolo.INFO + Protocolo.SEP + "No tienes suficientes fichas (mínimo " + APUESTA_DEFECTO + ").");
            return;
        }

        cliente.setListo(true);
        String nombre = cliente.getJugador().getNombre();
        vista.agregarLog(nombre + " está listo (Apuesta: " + APUESTA_DEFECTO + " fichas).");
        difundirInfo(nombre + " está listo.");

        comprobarInicioRonda();
    }

    private void comprobarInicioRonda() {
        if (rondaEnCurso || clientesConectados.isEmpty()) return;
        for (ManejadorCliente c : clientesConectados) {
            if (!c.isListo()) return;
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
        if (!rondaEnCurso || !esTurnoDe(cliente)) return;

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
            cliente.enviar(Protocolo.TURNO + Protocolo.SEP + Protocolo.TURNO_TUYO);
        }
    }

    private void gestionarPlantarse(ManejadorCliente cliente) {
        if (!rondaEnCurso || !esTurnoDe(cliente)) return;

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

        boolean hayJugadoresVivos = false;
        for (ManejadorCliente p : participantesRonda) {
            if (p.isActivo() && p.getJugador().calcularPuntos() <= 21) {
                hayJugadoresVivos = true;
                break;
            }
        }

        while (hayJugadoresVivos && crupier.calcularPuntos() < 17) {
            crupier.recibirCarta(sacarCarta());
        }

        int puntosCrupier = crupier.calcularPuntos();
        vista.agregarLog("Mano final del Crupier: " + formatearMano(crupier.getMano(), false) + " (" + puntosCrupier + " puntos)");

        for (ManejadorCliente p : participantesRonda) {
            String nombre = p.getJugador().getNombre();
            int puntosJugador = p.getJugador().calcularPuntos();

            String resultadoBD;
            String mensajeFinal;
            int cambioSaldo;

            if (puntosJugador > 21) {
                resultadoBD = "PERDIDA";
                cambioSaldo = -APUESTA_DEFECTO;
                mensajeFinal = "Resultado: DERROTA (-" + APUESTA_DEFECTO + " 🪙). Te pasaste con " + puntosJugador + " vs " + puntosCrupier + ".";
            } else if (puntosCrupier > 21 || puntosJugador > puntosCrupier) {
                resultadoBD = "GANADA";
                cambioSaldo = APUESTA_DEFECTO;
                mensajeFinal = "Resultado: ¡VICTORIA! (+" + APUESTA_DEFECTO + " 🪙). " + puntosJugador + " vs " + puntosCrupier + ".";
            } else if (puntosJugador == puntosCrupier) {
                resultadoBD = "EMPATADA";
                cambioSaldo = 0;
                mensajeFinal = "Resultado: EMPATE (0 🪙). " + puntosJugador + " puntos ambos.";
            } else {
                resultadoBD = "PERDIDA";
                cambioSaldo = -APUESTA_DEFECTO;
                mensajeFinal = "Resultado: DERROTA (-" + APUESTA_DEFECTO + " 🪙). " + puntosJugador + " vs " + puntosCrupier + ".";
            }

            JugadorDAO.registrarResultadoYSaldo(nombre, resultadoBD, cambioSaldo);
            int nuevoSaldo = JugadorDAO.obtenerSaldo(nombre);

            vista.agregarLog("  -> " + nombre + ": " + resultadoBD + " (" + puntosJugador + " pts) | Nuevo saldo: " + nuevoSaldo);

            if (p.isActivo()) {
                enviarEstadoMesa(p, false);
                p.enviar(Protocolo.FIN_RONDA + Protocolo.SEP + Protocolo.codificarTexto(mensajeFinal) + Protocolo.SEP + nuevoSaldo);
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
        comprobarInicioRonda();
    }

    public synchronized void desconectarCliente(ManejadorCliente cliente) {
        boolean estabaEnLista = clientesConectados.remove(cliente);
        if (!estabaEnLista) return;

        String nombre = cliente.getNombreSeguro();
        vista.agregarLog("Jugador desconectado: " + nombre);
        actualizarResumenConectados();
        difundirInfo(nombre + " ha abandonado la mesa.");

        if (rondaEnCurso && cliente.isEnRonda()) {
            boolean eraSuTurno = esTurnoDe(cliente);
            cliente.setEnRonda(false);
            cliente.setTurnoTerminado(true);
            if (eraSuTurno) avanzarTurno();
        } else if (!rondaEnCurso) {
            comprobarInicioRonda();
        }
    }

    private boolean esTurnoDe(ManejadorCliente cliente) {
        return indiceTurnoActual >= 0 && indiceTurnoActual < participantesRonda.size() && participantesRonda.get(indiceTurnoActual) == cliente;
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
        if (mano.isEmpty()) return "-";
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
    public void registrarEventoRed(String mensaje) {
        vista.agregarLog(mensaje);
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