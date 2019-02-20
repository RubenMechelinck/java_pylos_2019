package be.kuleuven.pylos.game;

import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Jan on 13/02/2015.
 */
public class PylosLocation {

	public final int X, Y, Z;
	final ArrayList<PylosLocation> above = new ArrayList<>();
	final ArrayList<PylosLocation> below = new ArrayList<>();
	final ArrayList<PylosSquare> squares = new ArrayList<>();
	private PylosSphere pylosSphere;

	private int nUsedAbove = 0;
	private int nUsedBelow;

	/* package constructor ---------------------------------------------------------------------------------------- */

	PylosLocation(int x, int y, int z) {
		X = x;
		Y = y;
		Z = z;
		if (z == 0) nUsedBelow = 4;
	}

	/* package methods -------------------------------------------------------------------------------------------- */

	boolean put(PylosSphere pylosSphere) {
		assert isUsable() : toString() + " is not usable";
		boolean completedSquare = false;
		for (PylosLocation blAbove : above) {
			blAbove.nUsedBelow++;
		}
		for (PylosLocation blBelow : below) {
			blBelow.nUsedAbove++;
		}
		for (PylosSquare bsInSquare : squares) {
			bsInSquare.inc(pylosSphere.PLAYER_COLOR);
			completedSquare |= bsInSquare.isSquare(pylosSphere.PLAYER_COLOR);
		}
		this.pylosSphere = pylosSphere;
		this.pylosSphere.pylosLocation = this;
		return completedSquare;
	}

	PylosSphere remove() {
		assert isUsed() : toString() + " is not used";
		for (PylosLocation blAbove : above) {
			blAbove.nUsedBelow--;
		}
		for (PylosLocation blBelow : below) {
			blBelow.nUsedAbove--;
		}
		for (PylosSquare bsInSquare : squares) {
			bsInSquare.dec(pylosSphere.PLAYER_COLOR);
		}
		PylosSphere tmpPylosSphere = pylosSphere;
		pylosSphere.pylosLocation = null;
		pylosSphere = null;
		return tmpPylosSphere;
	}

	/* public methods --------------------------------------------------------------------------------------------- */

	public int getMaxInSquare(PylosPlayer player) {
		int maxInSquare = 0;
		for (PylosSquare bs : squares) {
			maxInSquare = Math.max(maxInSquare, bs.getInSquare(player.PLAYER_COLOR));
		}
		return maxInSquare;
	}

	public PylosSquare getFilledSquare(PylosPlayer player) {
		for (PylosSquare bs : squares) {
			if (bs.isSquare(player.PLAYER_COLOR)) {
				return bs;
			}
		}
		return null;
	}

	public PylosSphere getSphere() {
		return pylosSphere;
	}

	public boolean isUsed() {
		return pylosSphere != null;
	}

	public boolean hasAbove() {
		return nUsedAbove > 0;
	}

	public boolean isUsable() {
		return !isUsed() && nUsedBelow == 4;
	}

	public boolean isBelow(PylosLocation pylosLocation) {
		return (pylosLocation.Z == Z + 1) &&
				(pylosLocation.X == X || pylosLocation.X == X - 1) &&
				(pylosLocation.Y == Y || pylosLocation.Y == Y - 1);
	}

	public String toString() {
		return "BoardLocation[x=" + X + ", y=" + Y + ", z=" + Z + ", nUsedAbove=" + nUsedAbove + ", nUsedBelow=" + nUsedBelow + ", pylosSphere=" + pylosSphere + "]";
	}

	public String toStringCoords() {
		return "BoardLocation[x=" + X + ", y=" + Y + ", z=" + Z + "]";
	}

}
