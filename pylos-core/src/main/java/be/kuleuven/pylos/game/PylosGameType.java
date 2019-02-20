package be.kuleuven.pylos.game;

/**
 * Created by Jan on 16/02/2015.
 */
public enum PylosGameType {

	/* The square rule is neglected */
	CHILD,

	/* The classic game */
	CLASSIC,

	/* the square rule is extended with:
		- 4 of the same color in one line at level 0
		- 3 of the same color in one line at level 1
	*/
	EXPERT

}
