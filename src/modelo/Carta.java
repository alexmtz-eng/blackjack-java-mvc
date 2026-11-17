package modelo;

/**
 * Clase que representa una carta individual para el juego de Blackjack.
 * Gestiona el palo, el rango y el valor numérico de la carta.
 * 
 * @author Alejandro Martínez Torres
 * @version 1.0
 * @since 2026-11-17
 */

public class Carta {
	private String palo;
	private String rango;
	private int valor;
	
	public Carta(String palo, String rango, int valor) {
		super();
		this.palo = palo;
		this.rango = rango;
		this.valor = valor;
	}

	public String getPalo() {
		return palo;
	}

	public void setPalo(String palo) {
		this.palo = palo;
	}

	public String getRango() {
		return rango;
	}

	public void setRango(String rango) {
		this.rango = rango;
	}

	public int getValor() {
		return valor;
	}

	public void setValor(int valor) {
		this.valor = valor;
	}
	
	@Override
	public String toString() {
	    return rango + " de " + palo;
	}
	
	

}
