package red;

/**
 * Define las constantes y utilidades del protocolo de comunicación TCP basado en texto
 * entre el servidor y los clientes del Blackjack multijugador.
 * Los mensajes siguen el formato COMANDO|parametro1|parametro2 delimitados por salto de línea.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */
public final class Protocolo {

    public static final int PUERTO = 5555;
    public static final String SEP = "|";
    public static final String SEP_REGEX = "\\|";

    // --- Comandos Cliente -> Servidor ---
    public static final String LOGIN = "LOGIN";
    public static final String LISTO = "LISTO";
    public static final String PEDIR = "PEDIR";
    public static final String PLANTARSE = "PLANTARSE";
    public static final String STATS = "STATS";
    public static final String RANKING = "RANKING";
    public static final String DESCONECTAR = "DESCONECTAR";

    // --- Comandos Servidor -> Cliente ---
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_ERROR = "LOGIN_ERROR";
    public static final String ESTADO = "ESTADO";
    public static final String MESA = "MESA";
    public static final String TURNO = "TURNO";
    public static final String FIN_RONDA = "FIN_RONDA";
    public static final String INFO = "INFO";

    // --- Subcomandos de turno ---
    public static final String TURNO_TUYO = "TUYO";
    public static final String TURNO_ESPERA = "ESPERA";

    private Protocolo() {
        // Clase de utilidades: evita instanciación
    }

    /**
     * Codifica saltos de línea para enviar bloques multilínea (estadísticas o ranking)
     * dentro de un único mensaje TCP leído con readLine().
     */
    public static String codificarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\r", "").replace("\n", "\\n");
    }

    /**
     * Restaura los saltos de línea originales al recibir un mensaje codificado.
     */
    public static String decodificarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\n", "\n");
    }
}