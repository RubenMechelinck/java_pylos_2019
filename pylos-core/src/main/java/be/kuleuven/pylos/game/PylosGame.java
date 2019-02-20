package be.kuleuven.pylos.game;

import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerObserver;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by Jan on 16/02/2015.
 */
public class PylosGame implements PylosGameIF {

	private static final int MAX_BOARD_STATE_COUNT = 3;

	private final PylosBoard board;
	private final PylosPlayer playerLight;
	private final PylosPlayer playerDark;
	private final PylosGameObserver gameObserver;
	private final PylosPlayerObserver playerObserver;
	private final HashMap<Long, Integer> boardStateCounts;

	private PylosPlayer currentPlayer;
	private PylosGameState currentState;
	private PylosPlayer winner = null;
	private int nReservesOfWinner = -1;
	private boolean abortFlag = false;

	/* constructor ------------------------------------------------------------------------------------------------ */

//	public PylosGame(PylosBoard board, PylosPlayer playerLight, PylosPlayer playerDark) {
//		this(board, playerLight, playerDark, new Random(0), PylosGameObserver.NONE, PylosPlayerObserver.NONE);
//	}

	public PylosGame(PylosBoard board, PylosPlayer playerLight, PylosPlayer playerDark, Random random) {
		this(board, playerLight, playerDark, random, PylosGameObserver.NONE, PylosPlayerObserver.NONE);
	}

//	public PylosGame(PylosBoard board, PylosPlayer playerLight, PylosPlayer playerDark, PylosGameObserver gameObserver, PylosPlayerObserver playerObserver) {
//		this(board, playerLight, playerDark, new Random(0), gameObserver, playerObserver);
//	}

	public PylosGame(PylosBoard board, PylosPlayer playerLight, PylosPlayer playerDark, Random random, PylosGameObserver gameObserver, PylosPlayerObserver playerObserver) {
		assert playerLight != playerDark : "The players are the same object";
		this.board = board;
		this.playerLight = playerLight;
		this.playerDark = playerDark;
		this.playerLight.init(PylosPlayerColor.LIGHT, playerDark, playerObserver, random);
		this.playerDark.init(PylosPlayerColor.DARK, playerLight, playerObserver, random);
		this.currentPlayer = playerLight;
		this.gameObserver = gameObserver;
		this.playerObserver = playerObserver;
		this.currentState = PylosGameState.MOVE;
		this.boardStateCounts = new HashMap<>();
	}

	/* public methods --------------------------------------------------------------------------------------------- */

	public void play() {
		while (!isFinished()) {
			doStep();
		}
	}

	public void abort() {
		abortFlag = true;
	}

	/* player interface ------------------------------------------------------------------------------------------- */

	@Override
	public PylosGameState getState() {
		return currentState;
	}

	@Override
	public void moveSphere(PylosSphere pylosSphere, PylosLocation toLocation) {
		assert currentState == PylosGameState.MOVE : "Method not supported in this state (" + currentState + ")";
		assert pylosSphere.PLAYER_COLOR == currentPlayer.PLAYER_COLOR : currentPlayer.PLAYER_COLOR + " can not move a sphere of " + currentPlayer.PLAYER_COLOR.other();

		boolean completedSquare;

		if (pylosSphere.isReserve()) {
			completedSquare = board.add(pylosSphere, toLocation);
			gameObserver.move(pylosSphere, null);
			gameObserver.println("  > add " + pylosSphere.ID);
		} else {
			PylosLocation fromLocation = pylosSphere.getLocation();
			completedSquare = board.move(pylosSphere, toLocation);
			gameObserver.move(pylosSphere, fromLocation);
			gameObserver.println("  > move " + pylosSphere.ID);
		}

		if (!isDrawState()) {
			if (completedSquare) {
				setState(PylosGameState.REMOVE_FIRST);
			} else {
				if (!checkFinishedAfterMove()) {
					setState(PylosGameState.MOVE);
					switchPlayer();
				}
			}
		}
	}

	@Override
	public void removeSphere(PylosSphere pylosSphere) {
		PylosLocation fromLocation = pylosSphere.getLocation();
		assert currentState != PylosGameState.MOVE : "Method not supported in this state (" + currentState + ")";
		assert pylosSphere.PLAYER_COLOR == currentPlayer.PLAYER_COLOR : currentPlayer.PLAYER_COLOR + "can't remove a sphere of " + currentPlayer.PLAYER_COLOR.other();

		board.remove(pylosSphere);
		gameObserver.move(pylosSphere, fromLocation);
		gameObserver.println("  > remove " + pylosSphere.ID);

		if (!isDrawState()) {
			if (currentState == PylosGameState.REMOVE_FIRST) {
				setState(PylosGameState.REMOVE_SECOND);
			} else {
				setState(PylosGameState.MOVE);
				switchPlayer();
			}
		}
	}

	@Override
	public void pass() {
		assert currentState == PylosGameState.REMOVE_SECOND : "Method not supported in this state (" + currentState + ")";

		gameObserver.println("  > pass");

//		if (!isDrawState()) {
			setState(PylosGameState.MOVE);
			switchPlayer();
//		}
	}

	@Override
	public boolean moveSphereIsDraw(PylosSphere pylosSphere, PylosLocation toLocation) {
		assert currentState == PylosGameState.MOVE : "Method not supported in this state (" + currentState + ")";
		assert pylosSphere.PLAYER_COLOR == currentPlayer.PLAYER_COLOR : currentPlayer.PLAYER_COLOR + " can not move a sphere of " + currentPlayer.PLAYER_COLOR.other();

		long resultState = pylosSphere.isReserve() ? board.toLongIfAdd(pylosSphere, toLocation) : board.toLongIfMove(pylosSphere, toLocation);
		return isDrawState(resultState);
	}

	@Override
	public boolean removeSphereIsDraw(PylosSphere pylosSphere) {
		assert currentState != PylosGameState.MOVE : "Method not supported in this state (" + currentState + ")";
		assert pylosSphere.PLAYER_COLOR == currentPlayer.PLAYER_COLOR : currentPlayer.PLAYER_COLOR + "can't remove a sphere of " + currentPlayer.PLAYER_COLOR.other();

		long resultState = board.toLongIfRemove(pylosSphere);
		return isDrawState(resultState);
	}

	@Override
	public boolean passIsDraw() {
		assert currentState == PylosGameState.REMOVE_SECOND : "Method not supported in this state (" + currentState + ")";

		return isDrawState(board.toLong());
	}

	@Override
	public PylosPlayer getWinner() {
		return winner;
	}

	@Override
	public int getReserveSizeOfWinner() {
		return nReservesOfWinner;
	}

	@Override
	public boolean isFinished() {
		return currentState == PylosGameState.COMPLETED || currentState == PylosGameState.ABORTED || currentState == PylosGameState.DRAW;
	}

	/* private methods -------------------------------------------------------------------------------------------- */

	private void doStep() {
		PylosPlayer cPlayer = currentPlayer;
		switch (currentState) {
			case MOVE:
				gameObserver.aboutToCall(PylosGameState.MOVE, currentPlayer);
				gameObserver.println(currentPlayer + ": add/move");
				currentPlayer.doMove(this, board);
				gameObserver.callPerformed();
				assert isFinished() || cPlayer != currentPlayer || currentState == PylosGameState.REMOVE_FIRST : "Player " + cPlayer + " did not perform an add or move";
				break;
			case REMOVE_FIRST:
				gameObserver.aboutToCall(PylosGameState.REMOVE_FIRST, currentPlayer);
				gameObserver.println(currentPlayer + ": remove 1st");
				currentPlayer.doRemove(this, board);
				gameObserver.callPerformed();
				assert isFinished() || currentState == PylosGameState.REMOVE_SECOND : "Player " + cPlayer + " did not removeSphere a sphere";
				break;
			case REMOVE_SECOND:
				gameObserver.aboutToCall(PylosGameState.REMOVE_SECOND, currentPlayer);
				gameObserver.println(currentPlayer + ": remove 2nd");
				currentPlayer.doRemoveOrPass(this, board);
				gameObserver.callPerformed();
				assert isFinished() || currentPlayer != cPlayer : "Player " + cPlayer + " did not removeSphere a sphere nor passed";
				break;
			case COMPLETED:
			case ABORTED:
			case DRAW:
				throw new IllegalStateException("The game is finished... " + currentState);
		}
		if (abortFlag) setState(PylosGameState.ABORTED);
		if (isFinished()) {
			signalFinished();
		}
	}

	private void setState(PylosGameState newState) {
		if (!isFinished()) {
			currentState = newState;
		}
	}

	private void switchPlayer() {
		currentPlayer = currentPlayer == playerLight ? playerDark : playerLight;
	}

	private boolean checkFinishedAfterMove() {
		if (!board.hasReserves(currentPlayer.PLAYER_COLOR)) {
			switchPlayer();
			winner = currentPlayer;
			nReservesOfWinner = board.getReservesSize(winner);
			finishGame();
			return true;
		} else {
			return false;
		}
	}

	private boolean isDrawState(long state) {
		Integer stateCount = boardStateCounts.get(state);
		return stateCount != null && stateCount + 1 >= MAX_BOARD_STATE_COUNT;
	}

	private boolean isDrawState() {
		long boardState = board.toLong();
		Integer stateCount = boardStateCounts.get(boardState);
		if (stateCount == null) {
			boardStateCounts.put(boardState, 1);
		} else {
			boardStateCounts.put(boardState, ++stateCount);
			if (stateCount == MAX_BOARD_STATE_COUNT) {
				setState(PylosGameState.DRAW);
				return true;
			}
		}
		return false;
	}

	private void finishGame() {
		setState(PylosGameState.COMPLETED);
		/* put all spheres of winning player */
		for (PylosLocation bl : board.getLocations()) {
			if (bl.isUsable()) {
				PylosSphere reserveSphere = board.getReserve(currentPlayer);
				board.add(reserveSphere, bl);
				gameObserver.move(reserveSphere, null);
			}
		}
	}

	private void signalFinished() {
		switch (currentState) {
			case COMPLETED:
				gameObserver.completed(winner);
				break;
			case ABORTED:
				gameObserver.aborted();
				break;
			case DRAW:
				gameObserver.draw();
				break;
			default:
				throw new IllegalStateException("Not a finished state: " + currentState);
		}
	}

}
