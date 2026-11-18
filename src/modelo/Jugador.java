package modelo;
import java.util.ArrayList;

/**
 * Clase que representa a un participante en la partida de Blackjack (jugador o crupier).
 * Gestiona el estado de su mano de cartas y contiene la lógica matemática para 
 * calcular la puntuación en tiempo real, adaptando dinámicamente el valor de los Ases.
 * 
 * @author Alejandro Martínez Torres
 * @version 1.1
 * @since 2026-11-17
 */

public class Jugador {
	private String nombre;
	private ArrayList<Carta> mano;
	private int saldo;
	private int apuestaActual;
	
	
	
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public void setMano(ArrayList<Carta> mano) {
		this.mano = mano;
	}
	public Jugador(String nombre) {
        this.nombre = nombre;
        this.mano = new ArrayList<>();
        this.saldo = 1000; // Banca inicial por defecto
        this.apuestaActual = 0;
    }
	
	// Añade una carta a la lista de la mano
    public void recibirCarta(Carta c) {
        mano.add(c);
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public ArrayList<Carta> getMano() {
        return mano;
    }
    
    /**
     * Calcula el valor total de la mano actual.
     * Gestiona dinámicamente el valor del As (11 o 1) para no pasarse de 21.
     */
    public int calcularPuntos() {
        int puntosTotales = 0;
        int numeroDeAses = 0;

        // Sumamos todo a lo bruto (asumiendo que el As vale 11 por defecto)
        for (Carta c : mano) {
            puntosTotales += c.getValor(); 
            
            // Si la carta es un As, llevamos la cuenta
            if (c.getRango().equals("As")) {
                numeroDeAses++;
            }
        }

        // Si nos hemos pasado de 21, y tenemos al menos un As en la mano...
        while (puntosTotales > 21 && numeroDeAses > 0) {
            puntosTotales -= 10; // Convertimos un As de 11 a 1 (le restamos 10 al total)
            numeroDeAses--;      // Descontamos ese As porque ya lo hemos "usado" como un 1
        }

        return puntosTotales;
    }
    
 // --- MÉTODOS DE LA BANCA ---

    public int getSaldo() {
        return saldo;
    }

    public void apostar(int cantidad) {
        this.apuestaActual = cantidad;
        this.saldo -= cantidad; // Restamos las fichas del bolsillo al ponerlas en la mesa
    }

    public void ganarApuesta() {
        this.saldo += (this.apuestaActual * 2); // Recuperas lo apostado y ganas lo mismo del casino
    }

    public void recuperarApuesta() {
        this.saldo += this.apuestaActual; // En caso de empate, la ficha vuelve a tu bolsillo
    }
    // Hacemos un método para que al jugador se le quiten las cartas antes de la siguiente mano.
    public void vaciarMano() {
        this.mano.clear(); 
    }

}

