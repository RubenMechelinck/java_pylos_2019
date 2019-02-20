package be.kuleuven.pylos.game;

/**
 * Created by Jan on 20/02/2015.
 */
public class PylosGameSimulator {

	private final PylosBoard board;

	private PylosGameState currentState;
	private PylosPlayerColor currentColor;
	private PylosPlayerColor winner = null;

	public PylosGameSimulator(PylosGameState gameState, PylosPlayerColor playerColor, PylosBoard board) {
		this.board = board;
		this.currentState = gameState;
		this.currentColor = playerColor;
	}

	/* public getters --------------------------------------------------------------------------------------------- */

	public PylosGameState getState() {
		return currentState;
	}

	public PylosPlayerColor getColor() {
		return currentColor;
	}

	public PylosPlayerColor getWinner() {
		return winner;
	}

	/* public game methods ---------------------------------------------------------------------------------------- */

	/* do */

	public void moveSphere(PylosSphere pylosSphere, PylosLocation toLocation) {
		assert currentState == PylosGameState.MOVE : "Method not supported in this state (" + currentState + ")";
		assert pylosSphere.PLAYER_COLOR == currentColor : currentColor + " can not move a sphere of " + currentColor.other();

		boolean completedSquare;

		if (pylosSphere.isReserve()) {
			completedSquare = board.add(pylosSphere, toLocation);
		} else {
			completedSquare = board.move(pylosSphere, toLocation);
		}

		if (completedSquare) {
			setState(PylosGameState.REMOVE_FIRST);
		} else {
			if (!checkFinished()) {
				setState(PylosGameState.MOVE);
				switchPlayerColor();
			}
		}
	}

	public void removeSphere(PylosSphere pylosSphere) {
		assert currentState != PylosGameState.MOVE : "Method not supported in this state (" + currentState + ")";
		assert pylosSphere.PLAYER_COLOR == currentColor : currentColor + "can't remove a sphere of " + currentColor.other();

		board.remove(pylosSphere);
		if (currentState == PylosGameState.REMOVE_FIRST) {
			setState(PylosGameState.REMOVE_SECOND);
		} else {
			setState(PylosGameState.MOVE);
			switchPlayerColor();
		}
	}

	public void pass() {
		assert currentState == PylosGameState.REMOVE_SECOND : "Method not supported in this state (" + currentState + ")";
		setState(PylosGameState.MOVE);
		switchPlayerColor();
	}

	/* undo */

	public void undoMoveSphere(PylosSphere pylosSphere, PylosLocation prevLocation, PylosGameState prevState, PylosPlayerColor prevColor) {
		assert currentState != PylosGameState.REMOVE_SECOND : currentState;
		board.moveDown(pylosSphere, prevLocation);
		reset(prevState, prevColor);
	}

	public void undoAddSphere(PylosSphere reserveSphere, PylosGameState prevState, PylosPlayerColor prevColor) {
		assert currentState != PylosGameState.REMOVE_SECOND : currentState;
		board.remove(reserveSphere);
		reset(prevState, prevColor);
	}

	public void undoRemoveFirstSphere(PylosSphere pylosSphere, PylosLocation prevLocation, PylosGameState prevState, PylosPlayerColor prevColor) {
		assert currentState == PylosGameState.REMOVE_SECOND;
		board.add(pylosSphere, prevLocation);
		reset(prevState, prevColor);
	}

	public void undoRemoveSecondSphere(PylosSphere pylosSphere, PylosLocation prevLocation, PylosGameState prevState, PylosPlayerColor prevColor) {
		assert currentState == PylosGameState.MOVE;
		board.add(pylosSphere, prevLocation);
		reset(prevState, prevColor);
	}

	public void undoPass(PylosGameState prevState, PylosPlayerColor prevColor) {
		assert currentState == PylosGameState.MOVE;
		reset(prevState, prevColor);
	}

	/* privates --------------------------------------------------------------------------------------------------- */

	private void switchPlayerColor() {
		currentColor = currentColor.other();
	}

	private void reset(PylosGameState state, PylosPlayerColor color) {
		currentState = state;
		currentColor = color;
	}

	private void setState(PylosGameState newState) {
		if (currentState != PylosGameState.ABORTED && currentState != PylosGameState.DRAW) {
			currentState = newState;
		}
	}

	private boolean checkFinished() {
		if (!board.hasReserves(currentColor)) {
			currentState = PylosGameState.COMPLETED;
			winner = currentColor.other();
			return true;
		} else {
			return false;
		}
	}
}
