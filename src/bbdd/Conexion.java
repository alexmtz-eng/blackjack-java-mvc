package bbdd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 * Gestiona la conexión JDBC entre el servidor del juego y la base de datos local MySQL
 * ({@code casino_blackjack}).
 *
 * Centraliza los parámetros de acceso (URL, usuario y contraseña) y carga el driver
 * oficial {@code com.mysql.cj.jdbc.Driver} para proporcionar objetos {@link Connection}
 * listos para ser consumidos por las clases DAO.
 *
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-19
 */

public class Conexion {

    private static final String URL = "jdbc:mysql://localhost:3306/casino_blackjack?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "";

    public static Connection conectar() {
        Connection conexion = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
            System.out.println("¡Conexión a MySQL establecida correctamente!");
        } catch (ClassNotFoundException e) {
            System.out.println("Error: No se encuentra el driver JDBC en el Build Path.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error al conectar con MySQL (¿está el servicio arrancado?).");
            e.printStackTrace();
        }
        return conexion;
    }

    // Prueba rápida independiente
    public static void main(String[] args) {
        conectar();
    }
}