package modelo;
import java.util.Scanner;
/**
 * Clase principal que actúa como Controlador temporal por consola.
 * Implementa el bucle de juego, la IA del crupier y el sistema de apuestas.
 * 
 * @author Alejandro Martínez Torres
 * @version 1.2
 * @since 2026-11-17 
 */

public class Main {
    public static void main(String[] args) {
        
        Scanner teclado = new Scanner(System.in);
        
        System.out.println("¡Bienvenido al casino!");
        String nombreIntroducido = "";
        boolean nombreValido = false;

        // 1. Validación del nombre (Fuera del bucle de juego)
        while (!nombreValido) {
            System.out.print("Introduce tu nombre (solo letras): ");
            nombreIntroducido = teclado.nextLine();
            
            if (nombreIntroducido.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
                nombreValido = true;
            } else {
                System.out.println("Error: El nombre solo puede contener letras. Inténtalo de nuevo.");
            }
        }
        
        // 2. Sentamos a los jugadores UNA SOLA VEZ (Fuera del bucle)
        Jugador jugador1 = new Jugador(nombreIntroducido);
        Jugador crupier = new Jugador("Croupier");
        
        boolean seguirJugando = true;

        // --- INICIO DEL BUCLE DE JUEGO ---
        while (seguirJugando && jugador1.getSaldo() > 0) {
            
            // 3. Preparamos la mesa para la nueva mano
            Baraja miMazo = new Baraja();
            miMazo.barajar();
            jugador1.vaciarMano();
            crupier.vaciarMano();
            
            System.out.println("\n=================================");
            System.out.println("Bienvenido a la mesa, " + jugador1.getNombre() + ".");
            System.out.println("Saldo actual: " + jugador1.getSaldo() + " fichas.");
            System.out.print("¿Cuántas fichas deseas apostar en esta mano? ");
            
            int cantidadApuesta = teclado.nextInt();
            
            // Validación básica para no apostar más de lo que se tiene
            while (cantidadApuesta > jugador1.getSaldo() || cantidadApuesta <= 0) {
                System.out.println("Apuesta no válida. Tu saldo es de " + jugador1.getSaldo() + " fichas.");
                System.out.print("Introduce una cantidad válida: ");
                cantidadApuesta = teclado.nextInt();
            }
            
            jugador1.apostar(cantidadApuesta);
            System.out.println("Apuesta fijada en " + cantidadApuesta + ". ¡Repartiendo cartas!");
            
            // Repartimos las cartas iniciales
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

            // 6. Resolución de la partida y turno automático del crupier.
            int puntosJugador = jugador1.calcularPuntos();

            if (puntosJugador > 21) {
                System.out.println("\n¡Te has pasado de 21! Has perdido.");
                // No llamamos a recuperarApuesta(), el dinero ya se restó
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
                
                // Final de partida y reparto de fichas 
                if (puntosCrupier > 21) {
                    System.out.println("¡El Croupier se ha pasado! ¡HAS GANADO!");
                    jugador1.ganarApuesta();
                } else if (puntosJugador > puntosCrupier) {
                    System.out.println("¡Tus " + puntosJugador + " superan los " + puntosCrupier + " del Croupier! ¡HAS GANADO!");
                    jugador1.ganarApuesta();
                } else if (puntosJugador < puntosCrupier) {
                    System.out.println("El Croupier te supera. Has perdido.");
                    // No hacemos nada, la apuesta ya se restó al principio
                } else {
                    System.out.println("Empate a " + puntosJugador + ".");
                    jugador1.recuperarApuesta();
                }
            }
            
            // 7. Resultado final de la banca y control del bucle
            System.out.println("\n--- RESULTADO DE LA BANCA ---");
            System.out.println("Tu saldo final es de: " + jugador1.getSaldo() + " fichas.");
            
            if (jugador1.getSaldo() <= 0) {
                System.out.println("\n¡BANCARROTA! Te has quedado sin fichas.");
                System.out.println("El personal de seguridad te invita amablemente a abandonar el casino.");
                seguirJugando = false; 
            } else {
                System.out.print("\n¿Quieres jugar otra mano? (s/n): ");
                String respuesta = teclado.next();
                if (respuesta.equalsIgnoreCase("n")) {
                    seguirJugando = false;
                    System.out.println("Te retiras de la mesa con " + jugador1.getSaldo() + " fichas. ¡Hasta pronto!");
                }
            }
        } 
        // --- FIN DEL BUCLE DE JUEGO ---
       
        teclado.close(); 
    }
}