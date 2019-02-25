package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static be.kuleuven.pylos.player.student.Moves.*;


/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer{

    //for testing
    public static PylosSphere testSphere;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        // for testing ////////////////////////////////////////////////////////////////
        if(blockTriangleOpponent(game, board, this, board.getReserve(this)))
            return;

        if(buildSquare(game, board, this))
            return;

        if(buildUpWithLowerSphere(game, board, this))
            return;

        if (createTriangle(game, board, this, board.getReserve(this)))
            return;

        if(createConnection(game, board, this, board.getReserve(this)))
            return;

        randomMove(game, board, this);
        // end for testing ////////////////////////////////////////////////////////////
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        //for testing
        game.removeSphere(testSphere);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        //for testing
        game.pass();
    }

}
