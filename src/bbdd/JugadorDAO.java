package bbdd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Objeto de Acceso a Datos (DAO) encargado de las operaciones CRUD, autenticación,
 * gestión de saldo y estadísticas en la tabla jugadores de MySQL.
 *
 * @author Alejandro Martínez Torres
 * @version 2.0
 * @since 2026-11-19
 */
public class JugadorDAO {

    /**
     * Valida el acceso de un usuario existente.
     * Devuelve: "LOGIN_OK", "PASSWORD_INCORRECTA", "NO_EXISTE" o "ERROR".
     */
    public static String iniciarSesion(String nombre, String password) {
        String sql = "SELECT password FROM jugadores WHERE nombre = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return "ERROR";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String passGuardada = rs.getString("password");
                    return passGuardada.equals(password) ? "LOGIN_OK" : "PASSWORD_INCORRECTA";
                }
                return "NO_EXISTE";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * Registra un nuevo usuario con 100 fichas iniciales.
     * Devuelve: "REGISTRO_OK", "YA_EXISTE" o "ERROR".
     */
    public static String registrarJugador(String nombre, String password) {
        String sqlVerificar = "SELECT nombre FROM jugadores WHERE nombre = ?";
        String sqlInsertar = "INSERT INTO jugadores (nombre, password, saldo) VALUES (?, ?, 100)";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return "ERROR";

            try (PreparedStatement ps = con.prepareStatement(sqlVerificar)) {
                ps.setString(1, nombre);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return "YA_EXISTE";
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
     * Obtiene el saldo actual de fichas de un jugador.
     */
    public static int obtenerSaldo(String nombre) {
        String sql = "SELECT saldo FROM jugadores WHERE nombre = ?";
        try (Connection con = Conexion.conectar()) {
            if (con == null) return 100;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt("saldo");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 100;
    }

    /**
     * Actualiza el saldo y las estadísticas tras una ronda.
     */
    public static boolean registrarResultadoYSaldo(String nombre, String resultado, int cambioSaldo) {
        String columnaPartida;
        switch (resultado.toUpperCase()) {
            case "GANADA":   columnaPartida = "partidas_ganadas"; break;
            case "PERDIDA":  columnaPartida = "partidas_perdidas"; break;
            case "EMPATADA": columnaPartida = "partidas_empatadas"; break;
            default: return false;
        }

        String sql = "UPDATE jugadores SET partidas_jugadas = partidas_jugadas + 1, "
                   + columnaPartida + " = " + columnaPartida + " + 1, "
                   + "saldo = saldo + ? WHERE nombre = ?";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return false;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, cambioSaldo);
                ps.setString(2, nombre);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

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
                    int saldo = rs.getInt("saldo");
                    double porcentaje = (jugadas > 0) ? (ganadas * 100.0 / jugadas) : 0.0;

                    return String.format(
                        "Jugador: %s | Fichas: %d | Jugadas: %d | Ganadas: %d | Empatadas: %d | Perdidas: %d | %% Victorias: %.1f%%",
                        nombre, saldo, jugadas, ganadas, empatadas, perdidas, porcentaje
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Jugador no encontrado.";
    }

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

    public static String obtenerRanking() {
        StringBuilder sb = new StringBuilder("=== RANKING DE JUGADORES ===\n");
        String sql = "SELECT nombre, saldo, partidas_jugadas, partidas_ganadas, "
                   + "CASE WHEN partidas_jugadas > 0 THEN (partidas_ganadas * 100.0 / partidas_jugadas) ELSE 0 END AS porcentaje "
                   + "FROM jugadores ORDER BY porcentaje DESC, saldo DESC";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return "Error de conexión con la BBDD.";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                int pos = 1;
                while (rs.next()) {
                    sb.append(String.format(
                        "%d. %s - Fichas: %d - %.1f%% victorias (%d/%d)\n",
                        pos++,
                        rs.getString("nombre"),
                        rs.getInt("saldo"),
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
}