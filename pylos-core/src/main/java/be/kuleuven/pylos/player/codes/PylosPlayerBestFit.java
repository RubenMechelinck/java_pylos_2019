package be.kuleuven.pylos.player.codes;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Jan on 16/02/2015.
 */
public class PylosPlayerBestFit extends PylosPlayer {

	//	public static Random random = new Random(218);
	private PylosLocation lastPylosLocation;

	@Override
	public void doMove(PylosGameIF game, PylosBoard board) {
		/* collect all possible locations */
		ArrayList<PylosLocation> allUsableLocations = new ArrayList<>();
		for (PylosLocation bl : board.getLocations()) {
			if (bl.isUsable()) {
				allUsableLocations.add(bl);
			}
		}

		Collections.shuffle(allUsableLocations, getRandom());

		/* get the location with maximum in square of this color */
		PylosLocation toMaxThis = getMaxInSquare(allUsableLocations, this);

		/* get the location with maximum in square of the other color */
		PylosLocation toMaxOther = getMaxInSquare(allUsableLocations, this.OTHER);

		/* decide what to do */
		PylosSphere sphere = null;
		PylosLocation toLocation = null;

		if (toMaxOther.getMaxInSquare(this.OTHER) == 3) {
			// we should sabotage this square
			sphere = getMovableSphereOrReserve(toMaxOther, board);
			toLocation = toMaxOther;
		} else if (toMaxThis.getMaxInSquare(this) == 3) {
			// we should create this square
			sphere = getMovableSphereOrReserve(toMaxThis, board);
			toLocation = toMaxThis;
		} else {
			// try to move a used sphere
			// prefer higher locations, than max in square
			// than pick a sphere with minimum in square and which does not enable the other player to create a square
			sortZorMaxInSquare(allUsableLocations, this);
			for (int i = 0; i < allUsableLocations.size(); i++) {
				PylosLocation bl = allUsableLocations.get(i);
				PylosSphere usedSphere = getMovableSphereMinInSquare(bl, board);
				if (usedSphere != null && usedSphere.getLocation().getMaxInSquare(this.OTHER) < 3) {
					sphere = usedSphere;
					toLocation = bl;
					break;
				}
			}
			if (toLocation == null) {
				// we couldn't move a used sphere, add a reserve sphere
				// put it on the highest location and the max in square on the same level
				toLocation = getMaxZorMaxInSquare(allUsableLocations, this);
				sphere = board.getReserve(this);
			}
		}

		game.moveSphere(sphere, toLocation);
		lastPylosLocation = toLocation;
	}

	private PylosLocation getMaxInSquare(ArrayList<PylosLocation> locations, PylosPlayer player) {
		return Collections.max(locations, new Comparator<PylosLocation>() {
			@Override
			public int compare(PylosLocation o1, PylosLocation o2) {
				return Integer.compare(o1.getMaxInSquare(player), o2.getMaxInSquare(player));
			}
		});
	}

	private PylosLocation getMaxZorMaxInSquare(ArrayList<PylosLocation> locations, PylosPlayer player) {
		return Collections.max(locations, new Comparator<PylosLocation>() {
			@Override
			public int compare(PylosLocation o1, PylosLocation o2) {
				int compZ = Integer.compare(o1.Z, o2.Z);
				if (compZ != 0) return compZ;
				return Integer.compare(o1.getMaxInSquare(player), o2.getMaxInSquare(player));
			}
		});
	}

	private void sortZorMaxInSquare(ArrayList<PylosLocation> locations, PylosPlayer player) {
		Collections.sort(locations, new Comparator<PylosLocation>() {
			@Override
			public int compare(PylosLocation o1, PylosLocation o2) {
				int compZ = -Integer.compare(o1.Z, o2.Z);
				if (compZ != 0) return compZ;
				return -Integer.compare(o1.getMaxInSquare(player), o2.getMaxInSquare(player));
			}
		});
	}

	private PylosSphere getMovableSphereMinInSquare(PylosLocation toLocation, PylosBoard board) {
		ArrayList<PylosSphere> movableSpheres = new ArrayList<>();
		for (PylosSphere sphere : board.getSpheres(this)) {
			if (!sphere.isReserve() && sphere.canMoveTo(toLocation)) {
				movableSpheres.add(sphere);
			}
		}
		if (!movableSpheres.isEmpty()) {
			/* pick the one with the minimum in square */
			PylosSphere sphere = Collections.min(movableSpheres, new Comparator<PylosSphere>() {
				@Override
				public int compare(PylosSphere o1, PylosSphere o2) {
					return Integer.compare(o1.getLocation().getMaxInSquare(PylosPlayerBestFit.this), o2.getLocation().getMaxInSquare(PylosPlayerBestFit.this));
				}
			});
			return sphere;
		} else {
			return null;
		}
	}

	private PylosSphere getMovableSphereOrReserve(PylosLocation toLocation, PylosBoard board) {
		PylosSphere usedSphere = getMovableSphereMinInSquare(toLocation, board);
		return usedSphere != null ? usedSphere : board.getReserve(this);
	}

	@Override
	public void doRemove(PylosGameIF game, PylosBoard board) {
		/* removeSphere a random sphere from the square */
		PylosSphere sphereToRemove = lastPylosLocation.getSphere();
		game.removeSphere(sphereToRemove);
	}

	@Override
	public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
		/* collect all removable spheres */
		ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
		for (PylosSphere ps : board.getSpheres(this)) {
			if (ps.canRemove()) {
				removableSpheres.add(ps);
			}
		}
		Collections.shuffle(removableSpheres, getRandom());

		/* if remove a sphere, remove the sphere with minimum in square
		 * otherwise, pass */
		if (!removableSpheres.isEmpty()) {
			PylosSphere sphere = Collections.min(removableSpheres, new Comparator<PylosSphere>() {
				@Override
				public int compare(PylosSphere o1, PylosSphere o2) {
					return Integer.compare(o1.getLocation().getMaxInSquare(PylosPlayerBestFit.this), o2.getLocation().getMaxInSquare(PylosPlayerBestFit.this));
				}
			});
			game.removeSphere(sphere);
		} else {
			game.pass();
		}
	}
}
