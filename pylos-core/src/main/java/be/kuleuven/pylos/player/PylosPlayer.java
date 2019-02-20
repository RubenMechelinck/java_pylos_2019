package be.kuleuven.pylos.player;

import be.kuleuven.pylos.game.*;

import java.util.Random;

/**
 * Created by Jan on 16/02/2015.
 */
public abstract class PylosPlayer {

	private PylosPlayerObserver OBSERVER;
	private Random RANDOM;
	public PylosPlayerColor PLAYER_COLOR;
	public PylosPlayer OTHER;

	public void init(PylosPlayerColor playerColor, PylosPlayer other, PylosPlayerObserver observer, Random random) {
		this.PLAYER_COLOR = playerColor;
		this.OTHER = other;
		this.OBSERVER = observer;
		this.RANDOM = random;
	}

	public abstract void doMove(PylosGameIF game, PylosBoard board);

	public abstract void doRemove(PylosGameIF game, PylosBoard board);

	public abstract void doRemoveOrPass(PylosGameIF game, PylosBoard board);

	protected Random getRandom() {
		return RANDOM;
	}

	protected PylosPlayerObserver getObserver(){
		return OBSERVER;
	}

	public String toString() {
		return PLAYER_COLOR.toString();
	}

}
