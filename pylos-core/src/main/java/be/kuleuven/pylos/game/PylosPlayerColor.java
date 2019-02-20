package be.kuleuven.pylos.game;

/**
 * Created by Jan on 13/02/2015.
 */
public enum PylosPlayerColor {

	LIGHT("Light"), DARK("Dark");

	private final String name;

	private PylosPlayerColor(String name) {
		this.name = name;
	}

	public PylosPlayerColor other() {
		return this == LIGHT ? DARK : LIGHT;
	}

	public String toString() {
		return name;
	}

}
