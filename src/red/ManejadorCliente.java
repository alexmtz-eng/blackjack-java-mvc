package red;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import modelo.Jugador;

/**
 * Hilo dedicado a gestionar la comunicación TCP de un cliente concreto dentro del servidor.
 *
 * @author Alejandro Martínez Torres
 * @version 2.0
 * @since 2026-11-19
 */
public class ManejadorCliente implements Runnable {

    private final Socket socket;
    private final ServidorBlackjack servidor;

    private BufferedReader entrada;
    private PrintWriter salida;

    private Jugador jugador;
    private boolean autenticado = false;
    private boolean listo = false;
    private boolean enRonda = false;
    private boolean turnoTerminado = false;
    private volatile boolean activo = true;

    public ManejadorCliente(Socket socket, ServidorBlackjack servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                servidor.procesarMensajeCliente(this, linea);
            }
        } catch (IOException e) {
            if (activo) {
                servidor.registrarEventoRed("Conexión interrumpida con " + getNombreSeguro() + ".");
            }
        } finally {
            activo = false;
            servidor.desconectarCliente(this);
            cerrar();
        }
    }

    public synchronized void enviar(String mensaje) {
        if (salida != null && activo) {
            salida.println(mensaje);
        }
    }

    public void cerrar() {
        activo = false;
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

    public String getNombreSeguro() {
        return (jugador != null) ? jugador.getNombre() : socket.getRemoteSocketAddress().toString();
    }

    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }

    public boolean isAutenticado() { return autenticado; }
    public void setAutenticado(boolean autenticado) { this.autenticado = autenticado; }

    public boolean isListo() { return listo; }
    public void setListo(boolean listo) { this.listo = listo; }

    public boolean isEnRonda() { return enRonda; }
    public void setEnRonda(boolean enRonda) { this.enRonda = enRonda; }

    public boolean isTurnoTerminado() { return turnoTerminado; }
    public void setTurnoTerminado(boolean turnoTerminado) { this.turnoTerminado = turnoTerminado; }

    public boolean isActivo() { return activo; }
}