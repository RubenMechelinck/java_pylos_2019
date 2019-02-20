package be.kuleuven.pylos.game;

/**
 * Created by Jan on 16/02/2015.
 */
public enum PylosGameState {

	/* states when the game is running */
	MOVE, REMOVE_FIRST, REMOVE_SECOND,

	/* states when the game is finished */
	COMPLETED,		// there is a winner
	ABORTED,		// the game is externally aborted
	DRAW			// draw, no winner due to a game loop

}
