package be.kuleuven.pylos.player.codes;

import be.kuleuven.pylos.battle.Battle;
import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by Jan on 20/02/2015.
 */
public class PylosPlayerMiniMax extends PylosPlayer {

	/* four highest significant bits are used as follows:
	 * ...x	0=Light, 1=Dark
	 * ..x. MOVE
	 * .x..	REMOVE_FIRST
	 * x...	REMOVE_SECOND
	 * 000.	COMPLETED */
	private static final long COLOR_FLAG = 1l << 60;
	private static final long MOVE_FLAG = 1l << 61;
	private static final long REMOVE_FIRST_FLAG = 1l << 62;
	private static final long REMOVE_SECOND_FLAG = 1l << 63;

	private final double WIN_THRESHOLD_THIS = 1000;
	private final double WIN_THRESHOLD_OTHER = -1000;
	private final double WIN_THIS = 2000;        // decremented with branch depth (winning earlier is better)
	private final double WIN_OTHER = -2000;        // incremented with branch depth (winning later is better)
	private final double INITIAL_THIS = -9999;
	private final double INITIAL_OTHER = 9999;

	private final boolean PRINT_MINIMAX_RESULT = false;
	private final boolean PRUNE_TEST = false;
	private boolean PRUNE_ENABLE = true;
	private final boolean VAR_BRANCH_DEPTH;
	private final int VAR_BRANCH_START_DEPTH;       // set to 7 for best fit, 11 for human
	private int MAX_BRANCH_DEPTH = 4;                    // set to 5 for human, set to 3 for best fit, 10 is possible

	private final boolean USE_RANDOM = true;
	private final boolean SAVE_STATES = true;

	private PylosGameSimulator simulator;
	private PylosBoard board;
	private int branchDepth = 0;

	/* we try to maximize the difference (reserves_this - reserves_other) */
	private double bestMinimax;
	private PylosSphere bestSphere;
	private PylosLocation bestLocation;

	private HashMap<Long, Double> minimaxResults;

	public PylosPlayerMiniMax() {
		VAR_BRANCH_DEPTH = true;
		VAR_BRANCH_START_DEPTH = 10;
		MAX_BRANCH_DEPTH = VAR_BRANCH_START_DEPTH;
	}

	public PylosPlayerMiniMax(Integer branchDepth) {
		VAR_BRANCH_DEPTH = false;
		VAR_BRANCH_START_DEPTH = Integer.MAX_VALUE;    // not used
		MAX_BRANCH_DEPTH = branchDepth;
	}

	@Override
	public void doMove(PylosGameIF game, PylosBoard board) {

		if (PRUNE_TEST) PRUNE_ENABLE = false;
		init(game.getState(), board);

		PylosSphere myReserveSphere = board.getReserve(this);
		PylosSphere[] mySpheres = board.getSpheres(this);
		PylosLocation[] locations = board.getLocations();

		/* shuffle */
		ArrayList<PylosLocation> locationsList = new ArrayList(Arrays.asList(locations));
		if(USE_RANDOM) Collections.shuffle(locationsList, getRandom());
		locations = new PylosLocation[locations.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsList.get(i);
		}
		ArrayList<PylosSphere> sphereList = new ArrayList<>(Arrays.asList(mySpheres));
		if(USE_RANDOM) Collections.shuffle(locationsList, getRandom());
		mySpheres = new PylosSphere[mySpheres.length];
		for (int i = 0; i < mySpheres.length; i++) {
			mySpheres[i] = sphereList.get(i);
		}

		/* try to move a sphere to higher level */
		for (int sphereId = 0; sphereId < mySpheres.length; sphereId++) {
			PylosSphere sphere = mySpheres[sphereId];
			if (!sphere.isReserve()) {
				for (int locationId = 0; locationId < locations.length; locationId++) {
					PylosLocation location = locations[locationId];
					if (sphere.canMoveTo(location)) {
						PylosLocation prevLocation = sphere.getLocation();
						getObserver().checkingMoveSphere(sphere, location);
						simulator.moveSphere(sphere, location);
						double minimax = branchStep(bestMinimax, bestMinimax);
						eval(minimax, sphere, location);
						simulator.undoMoveSphere(sphere, prevLocation, PylosGameState.MOVE, this.PLAYER_COLOR);
					}
				}
			}
		}

		/* try to add a reserve sphere */
		for (int locationId = 0; locationId < locations.length; locationId++) {
			PylosLocation location = locations[locationId];
			if (location.isUsable()) {
				getObserver().checkingMoveSphere(myReserveSphere, location);
				simulator.moveSphere(myReserveSphere, location);
				double minimax = branchStep(bestMinimax, bestMinimax);
				eval(minimax, myReserveSphere, location);
				simulator.undoAddSphere(myReserveSphere, PylosGameState.MOVE, this.PLAYER_COLOR);
			}
		}

		double tmpBestMinimax = bestMinimax;
		PylosSphere tmpBestSphere = bestSphere;
		PylosLocation tmpBestLocation = bestLocation;

		/* ----------------------------------- */

		if (PRUNE_TEST) {
			PRUNE_ENABLE = true;
			init(game.getState(), board);

		/* try to move a sphere to higher level */
			for (int sphereId = 0; sphereId < mySpheres.length; sphereId++) {
				PylosSphere sphere = mySpheres[sphereId];
				if (!sphere.isReserve()) {
					for (int locationId = 0; locationId < locations.length; locationId++) {
						PylosLocation location = locations[locationId];
						if (sphere.canMoveTo(location)) {
							PylosLocation prevLocation = sphere.getLocation();
							simulator.moveSphere(sphere, location);
							double minimax = branchStep(bestMinimax, bestMinimax);
							eval(minimax, sphere, location);
							simulator.undoMoveSphere(sphere, prevLocation, PylosGameState.MOVE, this.PLAYER_COLOR);
						}
					}
				}
			}

		/* try to add a reserve sphere */
			for (int locationId = 0; locationId < locations.length; locationId++) {
				PylosLocation location = locations[locationId];
				if (location.isUsable()) {
					simulator.moveSphere(myReserveSphere, location);
					double minimax = branchStep(bestMinimax, bestMinimax);
					eval(minimax, myReserveSphere, location);
					simulator.undoAddSphere(myReserveSphere, PylosGameState.MOVE, this.PLAYER_COLOR);
				}
			}

			assert tmpBestMinimax == bestMinimax;
			assert tmpBestSphere == bestSphere;
			assert tmpBestLocation == bestLocation;
		}

		/* ----------------------------- */

		shoutIfWinnerIsKnown();

		/* execute the best move */
		assert bestSphere != null;
		if (PRINT_MINIMAX_RESULT) System.out.println("-------> " + bestMinimax);
		game.moveSphere(bestSphere, bestLocation);
	}

	@Override
	public void doRemove(PylosGameIF game, PylosBoard board) {
		init(game.getState(), board);

		for (PylosSphere sphere : board.getSpheres(PLAYER_COLOR)) {
			if (sphere.canRemove()) {
				PylosLocation prevLocation = sphere.getLocation();
				getObserver().checkingRemoveSphere(sphere);
				simulator.removeSphere(sphere);
				double minimax = branchStep(bestMinimax, bestMinimax);
				eval(minimax, sphere, null);
				simulator.undoRemoveFirstSphere(sphere, prevLocation, PylosGameState.REMOVE_FIRST, this.PLAYER_COLOR);
			}
		}

		shoutIfWinnerIsKnown();
		/* execute the best move */
		if (PRINT_MINIMAX_RESULT) System.out.println("-------> " + bestMinimax);
		game.removeSphere(bestSphere);
	}

	@Override
	public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
		init(game.getState(), board);

		for (PylosSphere sphere : board.getSpheres(PLAYER_COLOR)) {
			if (sphere.canRemove()) {
				PylosLocation prevLocation = sphere.getLocation();
				getObserver().checkingRemoveSphere(sphere);
				simulator.removeSphere(sphere);
				double minimax = branchStep(bestMinimax, bestMinimax);
				eval(minimax, sphere, null);
				simulator.undoRemoveSecondSphere(sphere, prevLocation, PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);
			}
		}

		getObserver().checkingPass();
		simulator.pass();
		double chance = branchStep(bestMinimax, bestMinimax);
		eval(chance, null, null);
		simulator.undoPass(PylosGameState.REMOVE_SECOND, this.PLAYER_COLOR);

		shoutIfWinnerIsKnown();
		if (PRINT_MINIMAX_RESULT) System.out.println("-------> " + bestMinimax);

		/* execute the best move */
		if (bestSphere != null) {
			game.removeSphere(bestSphere);
		} else {
			game.pass();
		}
	}

	private void init(PylosGameState state, PylosBoard board) {
		this.simulator = new PylosGameSimulator(state, PLAYER_COLOR, board);
		this.board = board;
		this.bestMinimax = INITIAL_THIS;
		this.bestSphere = null;
		this.bestLocation = null;
		this.branchDepth = 0;
		this.minimaxResults = new HashMap<>();
		setBranchDepth();
	}

	private void setBranchDepth() {
		if (VAR_BRANCH_DEPTH) {
			MAX_BRANCH_DEPTH = VAR_BRANCH_START_DEPTH + board.getNumberOfSpheresOnBoard() / 3;
		}
		getObserver().shout("Thinking... depth: " + MAX_BRANCH_DEPTH);
	}

	private void eval(double minimax, PylosSphere sphere, PylosLocation location) {
		if (PRINT_MINIMAX_RESULT) System.out.println(minimax + "  best: " + bestMinimax);
		if (minimax > bestMinimax) {
			bestMinimax = minimax;
			bestSphere = sphere;
			bestLocation = location;
		}
		if (PRINT_MINIMAX_RESULT) {
			if (minimax < WIN_THRESHOLD_OTHER) {
				System.out.println("Other can win in " + (minimax - WIN_OTHER) + " steps (" + minimax + ")");
			}
			if (minimax > WIN_THRESHOLD_THIS) {
				System.out.println("Minimax can win in " + (WIN_THIS - minimax) + " steps (" + minimax + ")");
			}
		}
	}

	private void shoutIfWinnerIsKnown() {
		if (bestMinimax < WIN_THRESHOLD_OTHER) {
			getObserver().shoutGood("If you do optimal moves\nYou can win in " + (int) (bestMinimax - WIN_OTHER) + " steps :)");
		} else if (bestMinimax > WIN_THRESHOLD_THIS) {
			getObserver().shoutBad("Even if you do optimal moves\nI'll win in " + (int) (WIN_THIS - bestMinimax) + " steps :)");
		} else {
			String shoutString = "In the worst case,\nI'll have " + Math.abs((int) bestMinimax) + " spheres ";
			shoutString += (bestMinimax < 0 ? "less" : "more") + " than you";
			getObserver().shout(shoutString);
		}
	}

	/* ------------------------------------------------------------------------------------------------------------ */

	private double branchDoMove(double siblingMinimax) {

		final PylosPlayerColor currentColor = simulator.getColor();
		double minimax = currentColor == PLAYER_COLOR ? INITIAL_THIS : INITIAL_OTHER;
		boolean prune = false;

		PylosSphere myReserveSphere = board.getReserve(currentColor);
		PylosSphere[] mySpheres = board.getSpheres(currentColor);
		PylosLocation[] locations = board.getLocations();

		/* try to move a sphere to higher level */
		for (int sphereId = 0; sphereId < mySpheres.length && !(PRUNE_ENABLE && prune); sphereId++) {
			PylosSphere sphere = mySpheres[sphereId];
			if (!sphere.isReserve()) {
				for (int locationId = 0; locationId < locations.length && !(PRUNE_ENABLE && prune); locationId++) {
					PylosLocation location = locations[locationId];
					if (sphere.canMoveTo(location)) {
						// check chance
						PylosLocation prevLocation = sphere.getLocation();
						simulator.moveSphere(sphere, location);
						double result = branchStep(minimax, siblingMinimax);
						if (currentColor == PLAYER_COLOR) {
							if (result > minimax) minimax = result;
							if (minimax >= siblingMinimax) prune = true;
						} else {
							if (result < minimax) minimax = result;
							if (minimax <= siblingMinimax) prune = true;
						}
						simulator.undoMoveSphere(sphere, prevLocation, PylosGameState.MOVE, currentColor);
						assert simulator.getState() == PylosGameState.MOVE && simulator.getColor() == currentColor : simulator.getState() + " " + simulator.getColor() + "\tshould be: " + PylosGameState.MOVE + " " + currentColor;
					}
				}
			}
		}

		/* try to add a reserve sphere */
		for (int locationId = 0; locationId < locations.length && !(PRUNE_ENABLE && prune); locationId++) {
			PylosLocation location = locations[locationId];
			if (location.isUsable()) {
				// check chance
				simulator.moveSphere(myReserveSphere, location);
				double result = branchStep(minimax, siblingMinimax);
				if (currentColor == PLAYER_COLOR) {
					if (result > minimax) minimax = result;
					if (minimax >= siblingMinimax) prune = true;
				} else {
					if (result < minimax) minimax = result;
					if (minimax <= siblingMinimax) prune = true;
				}
				simulator.undoAddSphere(myReserveSphere, PylosGameState.MOVE, currentColor);
				assert simulator.getState() == PylosGameState.MOVE && simulator.getColor() == currentColor : simulator.getState() + " " + simulator.getColor() + "\tshould be: " + PylosGameState.MOVE + " " + currentColor;
			}
		}

		assert !(currentColor == PLAYER_COLOR && minimax == INITIAL_THIS);
		assert !(currentColor != PLAYER_COLOR && minimax == INITIAL_OTHER);

		return minimax;
	}

	private double branchDoRemove(double parentSiblingMinimax) {

		final PylosPlayerColor currentColor = simulator.getColor();
		double minimax = currentColor == PLAYER_COLOR ? INITIAL_THIS : INITIAL_OTHER;
		boolean prune = false;

		PylosSphere[] mySpheres = board.getSpheres(currentColor);

		/* remove a sphere */
		for (int sphereId = 0; sphereId < mySpheres.length && !(PRUNE_ENABLE && prune); sphereId++) {
			PylosSphere sphere = mySpheres[sphereId];
			if (sphere.canRemove()) {
				PylosLocation prevLocation = sphere.getLocation();
				simulator.removeSphere(sphere);
				double result = branchStep(parentSiblingMinimax, parentSiblingMinimax);
				if (currentColor == PLAYER_COLOR) {
					if (result > minimax) minimax = result;
					if (minimax >= parentSiblingMinimax) prune = true;
				} else {
					if (result < minimax) minimax = result;
					if (minimax <= parentSiblingMinimax) prune = true;
				}
				simulator.undoRemoveFirstSphere(sphere, prevLocation, PylosGameState.REMOVE_FIRST, currentColor);
				assert simulator.getState() == PylosGameState.REMOVE_FIRST && simulator.getColor() == currentColor : simulator.getState() + " " + simulator.getColor() + "\tshould be: " + PylosGameState.REMOVE_FIRST + " " + currentColor;
			}
		}

		assert !(currentColor == PLAYER_COLOR && minimax == INITIAL_THIS);
		assert !(currentColor != PLAYER_COLOR && minimax == INITIAL_OTHER);

		return minimax;
	}

	private double branchDoRemoveOrPass(double parentSiblingMinimax) {

		final PylosPlayerColor currentColor = simulator.getColor();
		double minimax = currentColor == PLAYER_COLOR ? INITIAL_THIS : INITIAL_OTHER;
		boolean prune = false;

		PylosSphere[] mySpheres = board.getSpheres(currentColor);

		/* remove a sphere */
		for (int sphereId = 0; sphereId < mySpheres.length && !(PRUNE_ENABLE && prune); sphereId++) {
			PylosSphere sphere = mySpheres[sphereId];
			if (sphere.canRemove()) {
				PylosLocation prevLocation = sphere.getLocation();
				simulator.removeSphere(sphere);
				double result = branchStep(minimax, minimax);
				if (currentColor == PLAYER_COLOR) {
					if (result > minimax) minimax = result;
					if (minimax >= parentSiblingMinimax) prune = true;
				} else {
					if (result < minimax) minimax = result;
					if (minimax <= parentSiblingMinimax) prune = true;
				}
				simulator.undoRemoveSecondSphere(sphere, prevLocation, PylosGameState.REMOVE_SECOND, currentColor);
				assert simulator.getState() == PylosGameState.REMOVE_SECOND && simulator.getColor() == currentColor : simulator.getState() + " " + simulator.getColor() + "\tshould be: " + PylosGameState.REMOVE_SECOND + " " + currentColor;
			}
		}

		/* pass */
		simulator.pass();
		double result = board.getReservesSize(this.PLAYER_COLOR.other()) - board.getReservesSize(this.PLAYER_COLOR);
		if (currentColor == PLAYER_COLOR) {
			if (result > minimax) minimax = result;
		} else {
			if (result < minimax) minimax = result;
		}
		simulator.undoPass(PylosGameState.REMOVE_SECOND, currentColor);
		assert simulator.getState() == PylosGameState.REMOVE_SECOND && simulator.getColor() == currentColor : simulator.getState() + " " + simulator.getColor() + "\tshould be: " + PylosGameState.REMOVE_SECOND + " " + currentColor;

		assert !(currentColor == PLAYER_COLOR && minimax == INITIAL_THIS);
		assert !(currentColor != PLAYER_COLOR && minimax == INITIAL_OTHER);

		return minimax;
	}

//	private void printDo(String s){
//		System.out.println(getPadding(depth++) + observer.getColor() + " " + observer.getState() + " > " + s);
//	}
//
//	private void printUndo(String s){
//		System.out.println(getPadding(--depth) + "-" + observer.getColor() + " " + observer.getState() + " > " + s);
//	}
//
//	private void printState(){
//		System.out.println(getPadding(depth-1) + observer.getColor() + " " + observer.getState());
//	}
//
//	private String getPadding(int p){
//		String padding = "";
//		for(int i=0; i<p; i++){
//			padding += " ";
//		}
//		return padding;
//	}

//	private double branchStep(double siblingMinimax){
//		return branchStep(siblingMinimax, 0);
//	}

	private long addGameState(long boardState, PylosGameState gameState, PylosPlayerColor color) {

		if (color == PylosPlayerColor.DARK) {
			boardState |= COLOR_FLAG;
		}
		switch (gameState) {
			case MOVE:
				boardState |= MOVE_FLAG;
				break;
			case REMOVE_FIRST:
				boardState |= REMOVE_FIRST_FLAG;
				break;
			case REMOVE_SECOND:
				boardState |= REMOVE_SECOND_FLAG;
				break;
			case COMPLETED:
				break;
			default:
				throw new IllegalStateException("Game state is: " + gameState);
		}

		return boardState;
	}

	private double branchStep(double siblingMinimax, double parentSiblingMinimax) {

		if (branchDepth == MAX_BRANCH_DEPTH) {
			return board.getReservesSize(PLAYER_COLOR) - board.getReservesSize(PLAYER_COLOR.other());
		}

		final PylosPlayerColor color = simulator.getColor();
		final PylosGameState state = simulator.getState();
		final Long minimaxBranchState = addGameState(board.toLong(), state, color);

		Double result;
		if (SAVE_STATES) {
			result = minimaxResults.get(minimaxBranchState);
			if (result != null) {
				return result;
			}
		}

		branchDepth++;

		switch (state) {
			case MOVE:
				result = branchDoMove(siblingMinimax);
				assert simulator.getColor() == color && simulator.getState() == state;
				break;
			case REMOVE_FIRST:
				result = branchDoRemove(parentSiblingMinimax);
				assert simulator.getColor() == color && simulator.getState() == state;
				break;
			case REMOVE_SECOND:
				result = branchDoRemoveOrPass(parentSiblingMinimax);
				assert simulator.getColor() == color && simulator.getState() == state;
				break;
			case COMPLETED:
				result = simulator.getWinner() == PLAYER_COLOR ? WIN_THIS : WIN_OTHER;
				if (simulator.getWinner() == PLAYER_COLOR) {
					result -= branchDepth;
				} else {
					result += branchDepth;
				}
				assert simulator.getColor() == color && simulator.getState() == state;
				break;
			case DRAW:
				result = simulator.getWinner() == PLAYER_COLOR ? WIN_OTHER : WIN_THIS;
				break;
			default:
				throw new IllegalStateException("Game state is: " + state);
		}

		if (SAVE_STATES) {
			minimaxResults.put(minimaxBranchState, result);
		}

		branchDepth--;
		return result;
	}

	public static void main(String[] args) {
		Battle.play(new PylosPlayerMiniMax(), new PylosPlayerBestFit(), 50);
	}
}
