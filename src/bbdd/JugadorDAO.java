package bbdd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
/**
 * Objeto de Acceso a Datos (DAO) encargado de todas las operaciones CRUD y consultas
 * estadísticas sobre la tabla {@code jugadores} en MySQL.
 *
 * Encapsula las sentencias SQL necesarias para el servidor del Blackjack multijugador:
 * autenticación y registro automático de usuarios, actualización de partidas
 * (jugadas, ganadas, empatadas y perdidas), consulta de perfiles individuales,
 * obtención del listado de jugadores registrados, eliminación de cuentas y cálculo
 * del ranking ordenado por porcentaje de victorias.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */

public class JugadorDAO {

    /**
     * Valida el acceso o registra automáticamente si el usuario no existe.
     * Devuelve: "LOGIN_OK", "REGISTRO_OK", "PASSWORD_INCORRECTA" o "ERROR".
     */
    public static String autenticarORegistrar(String nombre, String password) {
        String sqlBuscar = "SELECT password FROM jugadores WHERE nombre = ?";
        String sqlInsertar = "INSERT INTO jugadores (nombre, password) VALUES (?, ?)";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return "ERROR";

            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
                ps.setString(1, nombre);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String passGuardada = rs.getString("password");
                    return passGuardada.equals(password) ? "LOGIN_OK" : "PASSWORD_INCORRECTA";
                }
            }

            try (PreparedStatement psInsert = con.prepareStatement(sqlInsertar)) {
                psInsert.setString(1, nombre);
                psInsert.setString(2, password);
                psInsert.executeUpdate();
                return "REGISTRO_OK";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * Actualiza las estadísticas tras una partida.
     * resultado debe ser: "GANADA", "PERDIDA" o "EMPATADA".
     */
    public static boolean registrarResultado(String nombre, String resultado) {
        String columna;
        switch (resultado.toUpperCase()) {
            case "GANADA":   columna = "partidas_ganadas"; break;
            case "PERDIDA":  columna = "partidas_perdidas"; break;
            case "EMPATADA": columna = "partidas_empatadas"; break;
            default: return false;
        }

        String sql = "UPDATE jugadores SET partidas_jugadas = partidas_jugadas + 1, "
                   + columna + " = " + columna + " + 1 WHERE nombre = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return false;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Devuelve todos los nombres registrados para llenar el JComboBox del Servidor.
     */
    public static ArrayList<String> obtenerNombresJugadores() {
        ArrayList<String> nombres = new ArrayList<>();
        String sql = "SELECT nombre FROM jugadores ORDER BY nombre ASC";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return nombres;
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nombres.add(rs.getString("nombre"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nombres;
    }

    /**
     * Devuelve las estadísticas formateadas de un jugador concreto.
     */
    public static String obtenerDatosJugador(String nombre) {
        String sql = "SELECT * FROM jugadores WHERE nombre = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return "Error de conexión con la BBDD.";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int jugadas = rs.getInt("partidas_jugadas");
                    int ganadas = rs.getInt("partidas_ganadas");
                    int perdidas = rs.getInt("partidas_perdidas");
                    int empatadas = rs.getInt("partidas_empatadas");
                    double porcentaje = (jugadas > 0) ? (ganadas * 100.0 / jugadas) : 0.0;

                    return String.format(
                        "Jugador: %s | Jugadas: %d | Ganadas: %d | Empatadas: %d | Perdidas: %d | %% Victorias: %.1f%%",
                        nombre, jugadas, ganadas, empatadas, perdidas, porcentaje
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Jugador no encontrado.";
    }

    /**
     * Elimina un jugador de la base de datos.
     */
    public static boolean eliminarJugador(String nombre) {
        String sql = "DELETE FROM jugadores WHERE nombre = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return false;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Genera el ranking ordenado por mejor porcentaje de partidas ganadas.
     */
    public static String obtenerRanking() {
        StringBuilder sb = new StringBuilder("=== RANKING DE JUGADORES ===\n");
        String sql = "SELECT nombre, partidas_jugadas, partidas_ganadas, "
                   + "CASE WHEN partidas_jugadas > 0 THEN (partidas_ganadas * 100.0 / partidas_jugadas) ELSE 0 END AS porcentaje "
                   + "FROM jugadores ORDER BY porcentaje DESC, partidas_ganadas DESC";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return "Error de conexión con la BBDD.";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                int pos = 1;
                while (rs.next()) {
                    sb.append(String.format(
                        "%d. %s - %.1f%% victorias (%d/%d)\n",
                        pos++,
                        rs.getString("nombre"),
                        rs.getDouble("porcentaje"),
                        rs.getInt("partidas_ganadas"),
                        rs.getInt("partidas_jugadas")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al consultar el ranking.";
        }
        return sb.toString();
    }

    // Prueba rápida para verificar todas las operaciones antes de seguir
    public static void main(String[] args) {
        System.out.println("1. Registro: " + autenticarORegistrar("Alex", "1234"));
        System.out.println("2. Clave mal: " + autenticarORegistrar("Alex", "0000"));
        System.out.println("3. Login OK: " + autenticarORegistrar("Alex", "1234"));

        registrarResultado("Alex", "GANADA");
        registrarResultado("Alex", "PERDIDA");

        System.out.println("4. Datos: " + obtenerDatosJugador("Alex"));
        System.out.println("5. Lista: " + obtenerNombresJugadores());
        System.out.println("6. Ranking:\n" + obtenerRanking());
        System.out.println("7. Borrado de prueba: " + eliminarJugador("Alex"));
    }
}