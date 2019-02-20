package be.kuleuven.pylos.game;

import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Jan on 13/02/2015.
 */
public class PylosBoard {

	public final int SIZE;
	public final int SPHERES_PER_PLAYER;

	private final ArrayList<ArrayList<ArrayList<PylosLocation>>> locations; // z,x,y
	private final PylosLocation[] allLocations;
	private final PylosLocation[][] symmetrics;
	private final PylosSquare[] allSquares;

	private final PylosSphere[] spheresLight;
	private final PylosSphere[] spheresDark;
	private final PylosSphere[] allSpheres;
	private final ArrayList<PylosSphere> reservesLight;
	private final ArrayList<PylosSphere> reservesDark;

	/* state of the board:
	 *
	 * bit=0: light, bit=1: dark
	 *
	 * ........ ........ ........ ........ xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx	layer z=0	(16 spheres)
	 * ........ ......xx xxxxxxxx xxxxxxxx ........ ........ ........ ........	layer z=1	(9 spheres)
	 * ......xx xxxxxx.. ........ ........ ........ ........ ........ ........	layer z=2	(4 spheres)
	 * ....xx.. ........ ........ ........ ........ ........ ........ ........	layer z=3	(1 sphere)
	 *
	 * ........ ........ ........ ........ ........ ........ ........ xxxxxxxx	layer z=0, x=0
	 * ........ ........ ........ ........ ........ ........ ........ ......xx	layer z=0, x=0, y=0
	 *
	 * ........ ........ ........ ........ ........ ........ ........ ......00	layer z=0, x=0, y=0, location not used
	 * ........ ........ ........ ........ ........ ........ ........ ......01	layer z=0, x=0, y=0, light
	 * ........ ........ ........ ........ ........ ........ ........ ......10	layer z=0, x=0, y=0, dark
	 * ........ ........ ........ ........ ........ ........ ........ ......11	layer z=0, x=0, y=0, error
	 *
	 * */

	static final long MASK_z0x0 = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_11111111L;
	static final long MASK_z0x1 = 0b00000000_00000000_00000000_00000000_00000000_00000000_11111111_00000000L;
	static final long MASK_z0x2 = 0b00000000_00000000_00000000_00000000_00000000_11111111_00000000_00000000L;
	static final long MASK_z0x3 = 0b00000000_00000000_00000000_00000000_11111111_00000000_00000000_00000000L;
	static final long MASK_z1x0 = 0b00000000_00000000_00000000_00111111_00000000_00000000_00000000_00000000L;
	static final long MASK_z1x1 = 0b00000000_00000000_00001111_11000000_00000000_00000000_00000000_00000000L;
	static final long MASK_z1x2 = 0b00000000_00000011_11110000_00000000_00000000_00000000_00000000_00000000L;
	static final long MASK_z2x0 = 0b00000000_00111100_00000000_00000000_00000000_00000000_00000000_00000000L;
	static final long MASK_z2x1 = 0b00000011_11000000_00000000_00000000_00000000_00000000_00000000_00000000L;
	static final long MASK_z3x0 = 0b00001100_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;

//	public long flip(long state) {
//		// along x axis
//		long z0x0 = state & MASK_z0x0;
//		long z0x1 = state & MASK_z0x1;
//		long z0x2 = state & MASK_z0x2;
//		long z0x3 = state & MASK_z0x3;
//		long z1x0 = state & MASK_z1x0;
//		long z1x1 = state & MASK_z1x1;
//		long z1x2 = state & MASK_z1x2;
//		long z2x0 = state & MASK_z2x0;
//		long z2x1 = state & MASK_z2x1;
//		long z3x0 = state & MASK_z3x0;
//
//		// flip around x axis
//
//		z0x0 <<= 24;
//		z0x1 <<= 8;
//		z0x2 >>= 8;
//		z0x3 >>= 24;
//
//		z1x0 <<= 12;
//		z1x2 >>= 12;
//
//		z2x0 <<= 4;
//		z2x1 >>= 4;
//
//		return z0x0 | z0x1 | z0x2 | z0x3 | z1x0 | z1x1 | z1x2 | z2x0 | z2x1 | z3x0;
//	}

	private long state = 0;
	private final static long[][][][] BIT_MASK_FOR_OR;    // [z][x][y][0/1 color]
	private final static long[][][][] BIT_MASK_FOR_AND;

	static {
		int index = 0;
		BIT_MASK_FOR_OR = new long[4][][][];
		BIT_MASK_FOR_AND = new long[4][][][];
		for (int z = 0; z < 4; z++) {
			BIT_MASK_FOR_OR[z] = new long[4 - z][][];
			BIT_MASK_FOR_AND[z] = new long[4 - z][][];
			for (int x = 0; x < BIT_MASK_FOR_OR[z].length; x++) {
				BIT_MASK_FOR_OR[z][x] = new long[4 - z][];
				BIT_MASK_FOR_AND[z][x] = new long[4 - z][];
				for (int y = 0; y < BIT_MASK_FOR_OR[z].length; y++) {
					BIT_MASK_FOR_OR[z][x][y] = new long[2];
					BIT_MASK_FOR_OR[z][x][y][0] = 1l << index;
					BIT_MASK_FOR_OR[z][x][y][1] = 1l << (index + 1);
					BIT_MASK_FOR_AND[z][x][y] = new long[2];
					BIT_MASK_FOR_AND[z][x][y][0] = ~BIT_MASK_FOR_OR[z][x][y][0];
					BIT_MASK_FOR_AND[z][x][y][1] = ~BIT_MASK_FOR_OR[z][x][y][1];
					index += 2;
				}
			}
		}
	}

	public PylosBoard() {
		this(4);

//		set(0, 0, 3, PylosPlayerColor.LIGHT);
//
//		for(int z=0; z<4; z++){
//			for(int x=0; x<BIT_MASK_FOR_OR[z].length; x++){
//				for(int y=0; y<BIT_MASK_FOR_OR[z].length; y++){
//					set(x, y, z, PylosPlayerColor.DARK);
//				}
//			}
//		}
//
//		System.exit(0);

	}

	private PylosBoard(int size) {
		SIZE = size;
		int totalSpheres = 0;
		for (int i = 1; i <= SIZE; i++) {
			totalSpheres += i * i;
		}
		SPHERES_PER_PLAYER = totalSpheres / 2;

		/* create PylosSpheres */
		spheresLight = new PylosSphere[SPHERES_PER_PLAYER];
		spheresDark = new PylosSphere[SPHERES_PER_PLAYER];
		allSpheres = new PylosSphere[SPHERES_PER_PLAYER * 2];
		reservesLight = new ArrayList<>();
		reservesDark = new ArrayList<>();
		for (int i = 0; i < SPHERES_PER_PLAYER; i++) {
			spheresLight[i] = new PylosSphere(PylosPlayerColor.LIGHT, i);
			spheresDark[i] = new PylosSphere(PylosPlayerColor.DARK, i);
			reservesLight.add(0, spheresLight[i]);
			reservesDark.add(0, spheresDark[i]);
			allSpheres[i] = spheresLight[i];
			allSpheres[SPHERES_PER_PLAYER + i] = spheresDark[i];
		}

		/* create locations */
		locations = new ArrayList<>();
		allLocations = new PylosLocation[SPHERES_PER_PLAYER * 2];
		int allLocId = 0;
		for (int z = 0; z < size; z++) {
			ArrayList<ArrayList<PylosLocation>> xList = new ArrayList<>();
			locations.add(xList);
			for (int x = 0; x < size - z; x++) {
				ArrayList<PylosLocation> yList = new ArrayList<>();
				xList.add(yList);
				for (int y = 0; y < size - z; y++) {
					PylosLocation pylosLocation = new PylosLocation(x, y, z);
					yList.add(pylosLocation);
					allLocations[allLocId++] = pylosLocation;
				}
			}
		}

		/* order locations for 8 symmetric */
		symmetrics = new PylosLocation[8][30];
		// xy(0,0) >
		int index = 0;
		for (int z = 0; z < 4; z++)
			for (int x = 0; x < 4 - z; x++)
				for (int y = 0; y < 4 - z; y++) symmetrics[0][index++] = getBoardLocation(x, y, z);
		// xy(0,0) v
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int y = 0; y < 4 - z; y++)
				for (int x = 0; x < 4 - z; x++) symmetrics[1][index++] = getBoardLocation(x, y, z);
		// xy(0,3) <
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int x = 3 - z; x >= 0; x--)
				for (int y = 0; y < 4 - z; y++) symmetrics[2][index++] = getBoardLocation(x, y, z);
		// xy(0,3) v
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int y = 0; y < 4 - z; y++)
				for (int x = 3 - z; x >= 0; x--) symmetrics[3][index++] = getBoardLocation(x, y, z);
		// xy(3,3) <
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int x = 3 - z; x >= 0; x--)
				for (int y = 3 - z; y >= 0; y--) symmetrics[4][index++] = getBoardLocation(x, y, z);
		// xy(3,3) ^
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int y = 3 - z; y >= 0; y--)
				for (int x = 3 - z; x >= 0; x--) symmetrics[5][index++] = getBoardLocation(x, y, z);
		// xy(0,3) >
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int x = 0; x < 4 - z; x++)
				for (int y = 3 - z; y >= 0; y--) symmetrics[6][index++] = getBoardLocation(x, y, z);
		// xy(0,3) ^
		index = 0;
		for (int z = 0; z < 4; z++)
			for (int y = 3 - z; y >= 0; y--)
				for (int x = 0; x < 4 - z; x++) symmetrics[7][index++] = getBoardLocation(x, y, z);

		/* create squares */
		ArrayList<PylosSquare> squares = new ArrayList<>();
		for (int z = 0; z < size - 1; z++) {
			for (int x = 0; x < size - z - 1; x++) {
				for (int y = 0; y < size - z - 1; y++) {
					PylosSquare square = new PylosSquare(
							getBoardLocation(x, y, z),
							getBoardLocation(x + 1, y, z),
							getBoardLocation(x, y + 1, z),
							getBoardLocation(x + 1, y + 1, z),
							getBoardLocation(x, y, z + 1)
					);
					squares.add(square);
				}
			}
		}
		allSquares = new PylosSquare[squares.size()];
		for (int i = 0; i < squares.size(); i++) allSquares[i] = squares.get(i);

		/* link locations */
		PylosLocation top = getBoardLocation(0, 0, SIZE - 1);
		link(top);
	}

	/* public methods --------------------------------------------------------------------------------------------- */

	private long toLowestLong() {

		ArrayList<PylosLocation[]> symSeqs = new ArrayList<>();
		symSeqs.addAll(Arrays.asList(symmetrics));

		for (int i = 0; i < 30; i++) {
			int smallestOrdinal = Integer.MAX_VALUE;
			for (int j = 0; j < symSeqs.size(); j++) {
				PylosLocation pl = symSeqs.get(j)[i];
				int ordinal;
				if(!pl.isUsed()) ordinal = 0;
				else if(pl.getSphere().PLAYER_COLOR==PylosPlayerColor.LIGHT) ordinal = 1;
				else ordinal = 2;
				smallestOrdinal = Math.min(smallestOrdinal, ordinal);
			}
			// remove larger ordinals
			for (int j = 0; j < symSeqs.size(); ) {
				PylosLocation pl = symSeqs.get(j)[i];
				int ordinal;
				if(!pl.isUsed()) ordinal = 0;
				else if(pl.getSphere().PLAYER_COLOR==PylosPlayerColor.LIGHT) ordinal = 1;
				else ordinal = 2;
				if (ordinal > smallestOrdinal) {
					symSeqs.remove(j);
				} else {
					j++;
				}
			}
			if (symSeqs.size() == 1) break;
		}

		PylosLocation[] sequence = symSeqs.get(0);

		long state = 0;
		for (int i = 0; i < sequence.length; i++) {
			PylosLocation pl = sequence[i];
			long ordinal;
			if(!pl.isUsed()) ordinal = 0;
			else if(pl.getSphere().PLAYER_COLOR==PylosPlayerColor.LIGHT) ordinal = 1;
			else ordinal = 2;
			state |= (ordinal << (i*2));
		}

		return state;
	}

	/**
	 * returns a long representation of this board state
	 *
	 * @return
	 */
	public long toLong() {
		return state;
	}

	/**
	 * returns the number of spheres on this board
	 *
	 * @return
	 */
	public int getNumberOfSpheresOnBoard() {
		return allSpheres.length - reservesLight.size() - reservesDark.size();
	}

	/**
	 * returns the location on level z (0=bottom), position x y
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public PylosLocation getBoardLocation(int x, int y, int z) {
		assert z >= 0 && z < SIZE : "z (=" + z + ") is out of bounds";
		assert x >= 0 && x < (SIZE - z) : "x (=" + x + ") is out of bounds";
		assert y >= 0 && y < (SIZE - z) : "y (=" + y + ") is out of bounds";
		return locations.get(z).get(x).get(y);
	}

	/**
	 * returns all 30 locations of the board
	 *
	 * @return
	 */
	public PylosLocation[] getLocations() {
		return allLocations;
	}

	/**
	 * returns all 14 squares of the board
	 *
	 * @return
	 */
	public PylosSquare[] getAllSquares() {
		return allSquares;
	}

	/**
	 * returns all 30 spheres
	 *
	 * @return
	 */
	public PylosSphere[] getSpheres() {
		return allSpheres;
	}

	/**
	 * returns all 15 spheres of 'player'
	 *
	 * @param player
	 * @return
	 */
	public PylosSphere[] getSpheres(PylosPlayer player) {
		return getSpheres(player.PLAYER_COLOR);
	}

	/**
	 * returns all 15 spheres of 'color'
	 *
	 * @param color
	 * @return
	 */
	public PylosSphere[] getSpheres(PylosPlayerColor color) {
		return color == PylosPlayerColor.LIGHT ? spheresLight : spheresDark;
	}

	/**
	 * returns a reserve sphere of 'player'
	 *
	 * @param player
	 * @return
	 */
	public PylosSphere getReserve(PylosPlayer player) {
		return getReserve(player.PLAYER_COLOR);
	}

	/**
	 * returns a reserve sphere of 'color'
	 *
	 * @param color
	 * @return
	 */
	public PylosSphere getReserve(PylosPlayerColor color) {
		ArrayList<PylosSphere> reserves = color == PylosPlayerColor.LIGHT ? reservesLight : reservesDark;
		assert !reserves.isEmpty() : "Player " + color + " has no reserve spheres, player " + color.other() + " has won the game";
		return reserves.get(reserves.size() - 1);
	}

	/**
	 * returns the number of reserve spheres of 'player'
	 *
	 * @param player
	 * @return
	 */
	public int getReservesSize(PylosPlayer player) {
		return getReservesSize(player.PLAYER_COLOR);
	}

	/**
	 * returns the number of reserve spheres of 'color'
	 *
	 * @param color
	 * @return
	 */
	public int getReservesSize(PylosPlayerColor color) {
		return (color == PylosPlayerColor.LIGHT ? reservesLight : reservesDark).size();
	}

	/* package accessible ----------------------------------------------------------------------------------------- */

	void reset() {
		state = 0;
		reservesLight.clear();
		reservesDark.clear();

		for (PylosLocation pl : allLocations) {
			if (pl.isUsed()) pl.remove();
		}
		for (int i = SPHERES_PER_PLAYER - 1; i >= 0; i--) {
			PylosSphere spLight = spheresLight[i];
			if (spLight.pylosLocation != null) spLight.pylosLocation.remove();
			reservesLight.add(spLight);
			PylosSphere spDark = spheresDark[i];
			if (spDark.pylosLocation != null) spDark.pylosLocation.remove();
			reservesDark.add(spDark);
		}
	}

	boolean hasReserves(PylosPlayerColor playerColor) {
		return !(playerColor == PylosPlayerColor.LIGHT ? reservesLight : reservesDark).isEmpty();
	}

	public void remove(PylosSphere sphere) {
		assert !sphere.isReserve() : "Can't remove " + sphere + ", it's not used";
		assert !sphere.pylosLocation.hasAbove() : "Can't remove " + sphere + ", at " + sphere.pylosLocation + ", it has other spheres above";

		clearBit(sphere.getLocation());
		sphere.pylosLocation.remove();
		(sphere.PLAYER_COLOR == PylosPlayerColor.LIGHT ? reservesLight : reservesDark).add(sphere);
	}

	public boolean add(PylosSphere reserveSphere, PylosLocation toLocation) {
		assert reserveSphere.isReserve() : reserveSphere + " is not a reserve sphere";
		assert toLocation.isUsable() : toLocation + " is not usable";

		setBit(toLocation, reserveSphere.PLAYER_COLOR);
		(reserveSphere.PLAYER_COLOR == PylosPlayerColor.LIGHT ? reservesLight : reservesDark).remove(reserveSphere);
		return toLocation.put(reserveSphere);
	}

	public boolean move(PylosSphere sphere, PylosLocation toLocation) {
		PylosLocation fromLocation = sphere.getLocation();
		assert !sphere.isReserve() : sphere + " is a reserve sphere";
		assert fromLocation != toLocation : "Can't move a sphere to the same location " + fromLocation.toStringCoords();
		assert !sphere.pylosLocation.hasAbove() : "Can't move " + sphere + ", at " + fromLocation + ", it has other spheres above";
		assert toLocation.isUsable() : toLocation + " is not usable";
		assert fromLocation.Z < toLocation.Z : "Can't move " + sphere + ", at " + fromLocation.toStringCoords() + ", to " + toLocation.toStringCoords() + ", should be moved to higher z-level";
		assert !fromLocation.isBelow(toLocation) : "Can't move, " + fromLocation.toStringCoords() + " is supporting " + toLocation.toStringCoords();

		clearBit(fromLocation);
		setBit(toLocation, sphere.PLAYER_COLOR);
		fromLocation.remove();
		return toLocation.put(sphere);
	}

	public boolean moveDown(PylosSphere sphere, PylosLocation toLocation) {
		PylosLocation fromLocation = sphere.getLocation();
		assert !sphere.isReserve() : sphere + " is a reserve sphere";
		assert fromLocation != toLocation : "Can't move a sphere to the same location " + fromLocation.toStringCoords();
		assert !sphere.pylosLocation.hasAbove() : "Can't move " + sphere + ", at " + fromLocation + ", it has other spheres above";
		assert toLocation.isUsable() : toLocation + " is not usable";
		assert fromLocation.Z > toLocation.Z : "Can't move down " + sphere + ", at " + fromLocation.toStringCoords() + ", to " + toLocation.toStringCoords() + ", should be moved to lower z-level";
		assert !fromLocation.isBelow(toLocation) : "Can't move, " + fromLocation.toStringCoords() + " is supporting " + toLocation.toStringCoords();

		clearBit(fromLocation);
		setBit(toLocation, sphere.PLAYER_COLOR);
		fromLocation.remove();
		return toLocation.put(sphere);
	}

	long toLongIfRemove(PylosSphere sphere) {
		return clearBit(state, sphere.getLocation());
	}

	long toLongIfAdd(PylosSphere reserveSphere, PylosLocation toLocation) {
		return setBit(state, toLocation, reserveSphere.PLAYER_COLOR);
	}

	long toLongIfMove(PylosSphere sphere, PylosLocation toLocation) {
		PylosLocation fromLocation = sphere.getLocation();
		long tmp = clearBit(state, fromLocation);
		return setBit(tmp, toLocation, sphere.PLAYER_COLOR);
	}

	PylosSphere getSphere(PylosPlayerColor playerColor, int id) {
		return playerColor == PylosPlayerColor.LIGHT ? spheresLight[id] : spheresDark[id];
	}

	/* internals -------------------------------------------------------------------------------------------------- */

	private void setBit(PylosLocation location, PylosPlayerColor color) {
		assert color != null;
		state |= BIT_MASK_FOR_OR[location.Z][location.X][location.Y][color.ordinal()];
//		System.out.println(Long.toString(state, 2) + "\t" + state);
	}

	private void clearBit(PylosLocation location) {
		state &= BIT_MASK_FOR_AND[location.Z][location.X][location.Y][0];
		state &= BIT_MASK_FOR_AND[location.Z][location.X][location.Y][1];
	}

	private long setBit(long state, PylosLocation location, PylosPlayerColor color) {
		assert color != null;
		state |= BIT_MASK_FOR_OR[location.Z][location.X][location.Y][color.ordinal()];
		return state;
	}

	private long clearBit(long state, PylosLocation location) {
		state &= BIT_MASK_FOR_AND[location.Z][location.X][location.Y][0];
		state &= BIT_MASK_FOR_AND[location.Z][location.X][location.Y][1];
		return state;
	}

	private void link(PylosLocation location) {
		if (location.Z > 0) {
			link(location, getBoardLocation(location.X, location.Y, location.Z - 1));
			link(location, getBoardLocation(location.X + 1, location.Y, location.Z - 1));
			link(location, getBoardLocation(location.X, location.Y + 1, location.Z - 1));
			link(location, getBoardLocation(location.X + 1, location.Y + 1, location.Z - 1));
			link(getBoardLocation(location.X, location.Y, location.Z - 1));
			link(getBoardLocation(location.X + 1, location.Y, location.Z - 1));
			link(getBoardLocation(location.X, location.Y + 1, location.Z - 1));
			link(getBoardLocation(location.X + 1, location.Y + 1, location.Z - 1));
		}
	}

	private void link(PylosLocation above, PylosLocation below) {
		if (!above.below.contains(below)) above.below.add(below);
		if (!below.above.contains(above)) below.above.add(above);
	}

}
