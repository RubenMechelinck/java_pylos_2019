package be.kuleuven.pylos.game;

import be.kuleuven.pylos.player.PylosPlayer;

/**
 * Created by Jan on 13/02/2015.
 */
public interface PylosGameObserver {

	PylosGameObserver NONE = new PylosGameObserver() {
		@Override
		public void move(PylosSphere pylosSphere, PylosLocation prevLocation) {

		}

		@Override
		public void completed(PylosPlayer winningPlayer) {

		}

		@Override
		public void aborted() {

		}

		@Override
		public void draw() {

		}

		@Override
		public void println(String str) {

		}
	};

	PylosGameObserver CONSOLE_GAME_OBSERVER = new PylosGameObserver() {

		@Override
		public void move(PylosSphere pylosSphere, PylosLocation prevLocation) {

		}

		@Override
		public void completed(PylosPlayer winningPlayer) {
			System.out.println("Game completed, winner: " + winningPlayer);
		}

		@Override
		public void aborted() {
			System.out.println("Game aborted");
		}

		@Override
		public void draw() {
			System.out.println("Draw");
		}

		@Override
		public void println(String str) {
			System.out.println(str);
		}
	};

	default void aboutToCall(PylosGameState currentState, PylosPlayer player){};
	default void callPerformed(){};

	/**
	 * called whenever a player performed a move, remove or pass
	 *
	 * @param pylosSphere
	 * @param prevLocation
	 */
	void move(PylosSphere pylosSphere, PylosLocation prevLocation);

	/**
	 * called when the game finished with a winner
	 *
	 * @param winningPlayer
	 */
	void completed(PylosPlayer winningPlayer);

	/**
	 * called when the game is externally aborted
	 */
	void aborted();

	/**
	 * called when the game got into a loop state
	 */
	void draw();

	/**
	 * called to print a string somewhere
	 *
	 * @param str
	 */
	void println(String str);

}
