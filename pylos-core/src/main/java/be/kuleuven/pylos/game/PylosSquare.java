package be.kuleuven.pylos.game;

import be.kuleuven.pylos.player.PylosPlayer;

import java.util.Arrays;

/**
 * Created by Jan on 16/02/2015.
 */
public class PylosSquare {

	private int n;
	private int nOfColor[] = new int[2]; // [0] for Player.LIGHT, [1] for Player.DARK
	private PylosLocation[] locations = new PylosLocation[4];
	private PylosLocation topLocation;

	PylosSquare(PylosLocation bl00, PylosLocation bl10, PylosLocation bl01, PylosLocation bl11, PylosLocation top) {
		locations[0] = bl00;
		locations[1] = bl10;
		locations[2] = bl01;
		locations[3] = bl11;
		topLocation = top;
		for (PylosLocation bl : locations) {
			bl.squares.add(this);
		}
	}

	/* public methods --------------------------------------------------------------------------------------------- */

	/**
	 * returns true if this square contains 4 spheres
	 *
	 * @return
	 */
	public boolean isSquare() {
		return n == 4;
	}

	/**
	 * returns true if this square contains 4 spheres of 'player'
	 *
	 * @param player
	 * @return
	 */
	public boolean isSquare(PylosPlayer player) {
		return isSquare(player.PLAYER_COLOR);
	}

	/**
	 * returns true if this square contains 4 spheres of 'color'
	 *
	 * @param color
	 * @return
	 */
	public boolean isSquare(PylosPlayerColor color) {
		return nOfColor[color.ordinal()] == 4;
	}

	/**
	 * returns the number of spheres in this square
	 *
	 * @return
	 */
	public int getInSquare() {
		return n;
	}

	/**
	 * returns the number of spheres of the 'player' in this square
	 *
	 * @param player
	 * @return
	 */
	public int getInSquare(PylosPlayer player) {
		return getInSquare(player.PLAYER_COLOR);
	}

	/**
	 * returns the number of spheres of 'color' in this square
	 *
	 * @param color
	 * @return
	 */
	public int getInSquare(PylosPlayerColor color) {
		return nOfColor[color.ordinal()];
	}

	/**
	 * returns an array containing the 4 locations in this square
	 *
	 * @return
	 */
	public PylosLocation[] getLocations() {
		return locations;
	}

	/**
	 * returns the location on top of this square
	 * @return
	 */
	public PylosLocation getTopLocation(){
		return topLocation;
	}

	/* package methods -------------------------------------------------------------------------------------------- */

	boolean inc(PylosPlayerColor color) {
		nOfColor[color.ordinal()]++;
		n++;
		assert n == nOfColor[0] + nOfColor[1] : "Total number of spheres is not equal to sum of colors: n=" + n + ", nOfCol=" + Arrays.toString(nOfColor);
		return isSquare(color);
	}

	void dec(PylosPlayerColor color) {
		nOfColor[color.ordinal()]--;
		n--;
		assert n == nOfColor[0] + nOfColor[1] : "Total number of spheres is not equal to sum of colors: n=" + n + ", nOfCol=" + Arrays.toString(nOfColor);
	}

}
