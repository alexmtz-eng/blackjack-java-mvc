package modelo;

import java.util.ArrayList;
import java.util.Collections;
/**
 * Clase que representa una baraja francesa completa para el juego de Blackjack.
 * Se encarga de instanciar las 52 cartas iniciales, mezclarlas de forma aleatoria
 * y gestionar el reparto durante la partida.
 * 
 * El apartado con el código sobre el valor del As en la partida (1 u 11) viene en la clase Jugador .
 * 
 * 
 * 
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-17
 */

public class Baraja {
	private ArrayList<Carta> listaCartas;
	public Baraja() {
        listaCartas = new ArrayList<>();
        
        String[] palos = {"Corazones", "Picas", "Tréboles", "Diamantes"};
        String[] rangos = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "As"};
        int[] valores = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};
        
        // Bucle anidado: por cada palo, generamos los 13 rangos
        for (String palo : palos) {
            for (int i = 0; i < rangos.length; i++) {
                // Instanciamos el objeto y lo añadimos a la lista
                Carta nuevaCarta = new Carta(palo, rangos[i], valores[i]);
                listaCartas.add(nuevaCarta);
            }
        }
    }
	/**
     * Mezcla las cartas actuales usando la librería Collections.
     */
    public void barajar() {
        Collections.shuffle(listaCartas);
    }
    
    /**
     * Extrae y devuelve la carta de la parte superior del mazo.
     * @return Objeto Carta o null si la baraja está vacía.
     */
    public Carta repartirCarta() {
        if (!listaCartas.isEmpty()) {
            return listaCartas.remove(0); // Borra y devuelve la primera
        }
        return null; 
    }
}
