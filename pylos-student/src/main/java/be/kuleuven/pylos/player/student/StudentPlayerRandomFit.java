package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer{

    private PylosSphere pylosSphere;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
		/* add a reserve sphere to a feasible random location */
        pylosSphere = board.getReserve(this);
        PylosLocation pylosLocation = null;

        Random random = new Random(1);
        PylosLocation[] locations = board.getLocations();

        while(true) {
            PylosLocation p = locations[random.nextInt(board.SIZE-1)];
            if (p.isUsable()){
                pylosLocation = p;
                break;
            }
        }

        game.moveSphere(pylosSphere, pylosLocation);

    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
		/* removeSphere a random sphere */

        board.remove(pylosSphere);


    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        game.pass();
		/* always pass */

    }
}
