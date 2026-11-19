package red;

public final class Protocolo {

    public static final int PUERTO = 5555;
    public static final String SEP = "|";
    public static final String SEP_REGEX = "\\|";

    // --- Comandos Cliente -> Servidor ---
    public static final String LOGIN = "LOGIN";
    public static final String REGISTRO = "REGISTRO";
    public static final String LISTO = "LISTO";
    public static final String PEDIR = "PEDIR";
    public static final String PLANTARSE = "PLANTARSE";
    public static final String STATS = "STATS";
    public static final String RANKING = "RANKING";
    public static final String DESCONECTAR = "DESCONECTAR";

    // --- Comandos Servidor -> Cliente ---
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_ERROR = "LOGIN_ERROR";
    public static final String REGISTRO_OK = "REGISTRO_OK";
    public static final String REGISTRO_ERROR = "REGISTRO_ERROR";
    public static final String ESTADO = "ESTADO";
    public static final String MESA = "MESA";
    public static final String TURNO = "TURNO";
    public static final String FIN_RONDA = "FIN_RONDA";
    public static final String INFO = "INFO";

    public static final String TURNO_TUYO = "TUYO";
    public static final String TURNO_ESPERA = "ESPERA";

    private Protocolo() {}

    public static String codificarTexto(String texto) {
        if (texto == null) return "";
        return texto.replace("\r", "").replace("\n", "\\n");
    }

    public static String decodificarTexto(String texto) {
        if (texto == null) return "";
        return texto.replace("\\n", "\n");
    }
}