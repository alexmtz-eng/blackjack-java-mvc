package modelo;

public class Main {

	public static void main(String[] args) {
		Baraja miMazo = new Baraja();
		miMazo.barajar();

		System.out.println("Primera carta robada: " + miMazo.repartirCarta());
		System.out.println("Segunda carta robada: " + miMazo.repartirCarta());

	}

}
