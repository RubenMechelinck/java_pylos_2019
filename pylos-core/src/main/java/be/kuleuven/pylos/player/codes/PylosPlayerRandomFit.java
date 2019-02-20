package be.kuleuven.pylos.player.codes;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;

/**
 * Created by Jan on 16/02/2015.
 */
public class PylosPlayerRandomFit extends PylosPlayer {

	@Override
	public void doMove(PylosGameIF game, PylosBoard board) {
		/* add a reserve sphere to a feasible random location */
		ArrayList<PylosLocation> allPossibleLocations = new ArrayList<>();
		for (PylosLocation bl : board.getLocations()) {
			if (bl.isUsable()) {
				allPossibleLocations.add(bl);
			}
		}
		PylosSphere reserveSphere = board.getReserve(this);
		PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
		game.moveSphere(reserveSphere, location);
	}

	@Override
	public void doRemove(PylosGameIF game, PylosBoard board) {
		/* removeSphere a random sphere */
		ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
		for (PylosSphere ps : board.getSpheres(this)) {
			if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
				removableSpheres.add(ps);
			}
		}
		PylosSphere sphereToRemove;
		if (removableSpheres.size() == 1) {
			sphereToRemove = removableSpheres.get(0);
		} else {
			sphereToRemove = removableSpheres.get(getRandom().nextInt(removableSpheres.size() - 1));
		}
		game.removeSphere(sphereToRemove);
	}

	@Override
	public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
		/* always pass */
		game.pass();
	}
}
