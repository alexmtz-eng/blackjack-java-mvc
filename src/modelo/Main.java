package modelo;
import java.util.Scanner;
/**
 * Clase principal temporal para probar la lógica del Modelo (Baraja y Jugador).
 * 
 * @author Alejandro Martínez Torres
 * @version 1.1
 * @since 2026-11-17
 */

public class Main {
    public static void main(String[] args) {
        
        Scanner teclado = new Scanner(System.in);
        
        System.out.println("¡Bienvenido al casino!");
        String nombreIntroducido = "";
        boolean nombreValido = false;

        // 1. Validación del nombre
        while (!nombreValido) {
            System.out.print("Introduce tu nombre (solo letras): ");
            nombreIntroducido = teclado.nextLine();
            
            if (nombreIntroducido.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
                nombreValido = true;
            } else {
                System.out.println("Error: El nombre solo puede contener letras. Inténtalo de nuevo.");
            }
        }
        
        // 2. Preparamos el mazo
        Baraja miMazo = new Baraja();
        miMazo.barajar();
        
        // 3. Sentamos a AMBOS jugadores y les repartimos ANTES de imprimir nada
        Jugador jugador1 = new Jugador(nombreIntroducido);
        Jugador crupier = new Jugador("Croupier");
        
        jugador1.recibirCarta(miMazo.repartirCarta());
        jugador1.recibirCarta(miMazo.repartirCarta());
        
        crupier.recibirCarta(miMazo.repartirCarta());
        crupier.recibirCarta(miMazo.repartirCarta());

        // 4. Mostramos la mesa inicial
        System.out.println("\n--- MESA DE BLACKJACK ---");
        System.out.println("El Croupier muestra: [" + crupier.getMano().get(0) + ", *Carta Oculta*]");
        System.out.println("\nTurno de " + jugador1.getNombre() + ":");
        System.out.println("Cartas en la mano: " + jugador1.getMano());
        System.out.println("Puntos totales: " + jugador1.calcularPuntos());

        // 5. Bucle del turno del jugador
        boolean turnoTerminado = false;

        while (!turnoTerminado && jugador1.calcularPuntos() < 21) {
            System.out.println("\nTienes " + jugador1.calcularPuntos() + " puntos. ¿Qué deseas hacer?");
            System.out.println("1. Pedir carta");
            System.out.println("2. Plantarse");
            System.out.print("Elige opción: ");
            
            int opcion = teclado.nextInt();
            
            if (opcion == 1) {
                jugador1.recibirCarta(miMazo.repartirCarta());
                System.out.println("Te ha tocado una carta. Tu mano: " + jugador1.getMano());
            } else if (opcion == 2) {
                turnoTerminado = true;
                System.out.println(jugador1.getNombre() + " se planta con " + jugador1.calcularPuntos());
            }
        }

        // 6. Resolución de la partida y turno automático del crupier
        int puntosJugador = jugador1.calcularPuntos();

        if (puntosJugador > 21) {
            System.out.println("\n¡Te has pasado de 21! Has perdido.");
        } else {
            if (puntosJugador == 21) {
                System.out.println("\n¡BLACKJACK!");
            }
            
            System.out.println("\n--- TURNO DEL CROUPIER ---");
            System.out.println("El Croupier revela su carta oculta. Mano completa: " + crupier.getMano());
            
            while (crupier.calcularPuntos() < 17) {
                System.out.println("El Croupier tiene " + crupier.calcularPuntos() + " y se ve obligado a pedir carta...");
                crupier.recibirCarta(miMazo.repartirCarta());
                System.out.println("Mano del Croupier: " + crupier.getMano());
            }
            
            int puntosCrupier = crupier.calcularPuntos();
            System.out.println("\nEl Croupier se planta con " + puntosCrupier + " puntos.");
            
            if (puntosCrupier > 21) {
                System.out.println("¡El Croupier se ha pasado! ¡HAS GANADO!");
            } else if (puntosJugador > puntosCrupier) {
                System.out.println("¡Tus " + puntosJugador + " superan los " + puntosCrupier + " del Croupier! ¡HAS GANADO!");
            } else if (puntosJugador < puntosCrupier) {
                System.out.println("El Croupier te supera. Has perdido.");
            } else {
                System.out.println("Empate a " + puntosJugador + ".");
            }
        }
       
        teclado.close(); 
    }
}