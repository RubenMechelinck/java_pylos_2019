package be.kuleuven.pylos.game;

/**
 * Created by Jan on 13/02/2015.
 */
public class PylosSphere {

	public final PylosPlayerColor PLAYER_COLOR;
	public final int ID;
	PylosLocation pylosLocation;

	/* package constructor ---------------------------------------------------------------------------------------- */

	PylosSphere(PylosPlayerColor playerColor, int id) {
		PLAYER_COLOR = playerColor;
		ID = id;
	}

	/* public methods --------------------------------------------------------------------------------------------- */

	/**
	 * returns true if this sphere is not used
	 *
	 * @return
	 */
	public boolean isReserve() {
		return pylosLocation == null;
	}

	/**
	 * returns true if this sphere is not used or has no spheres above
	 *
	 * @return
	 */
	public boolean canMove() {
		return isReserve() || !pylosLocation.hasAbove();
	}

	/**
	 * returns true if:
	 * this sphere is not used and the location is usable
	 * or this sphere is used and the location is: (1) usable, (2) on a higher level, (3) not above the sphere
	 *
	 * @param location
	 * @return
	 */
	public boolean canMoveTo(PylosLocation location) {
		if (isReserve()) {
			/* reserve sphere can be moved to every usable location */
			return location.isUsable();
		} else {
			if (pylosLocation.hasAbove()) {
				/* trying to move a sphere which has other spheres above */
				return false;
			} else {
				/* trying to move a used sphere, the location should be on higher level and not above the sphere */
				return location.isUsable() && pylosLocation.Z < location.Z && !pylosLocation.isBelow(location);
			}
		}
	}

	/**
	 * returns true if the sphere used and has no spheres above
	 *
	 * @return
	 */
	public boolean canRemove() {
		return !(isReserve() || pylosLocation.hasAbove());
	}

	/**
	 * returns the locations of this sphere, or null if this sphere is not used
	 *
	 * @return
	 */
	public PylosLocation getLocation() {
		return pylosLocation;
	}

	public String toString() {
		return "PylosSphere[player=" + PLAYER_COLOR + ", id=" + ID + "]";
	}

}
