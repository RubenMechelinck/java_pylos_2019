package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import static be.kuleuven.pylos.player.student.Moves.*;
import static be.kuleuven.pylos.player.student.Removes.removeFirstSphere;
import static be.kuleuven.pylos.player.student.Removes.removeSecondSphere;


/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer{

    //laatst gezette bal
    public static PylosSphere lastPlacedSphere;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        if(buildFullZ(game,board,this,board.getReserve(this)))
            return;

        if(blockTriangleOpponent(game, board, this, board.getReserve(this)))
            return;

        if(buildSquare(game, board, this))
            return;

        if(buildUpWithLowerSphere(game, board, this))
            return;

        if(buildHalfZ(game,board,this,board.getReserve(this)))
            return;

        if(createTriangle(game, board, this, board.getReserve(this)))
            return;

        if(createConnection(game, board, this, board.getReserve(this)))
            return;

        randomMove(game, board, this);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

        //sws 1 spere terug nemen
        removeFirstSphere(game, board, this);

        //enventueel 2e bal wegnemen
        removeSecondSphere(game, board,this);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        //for testing
        game.pass();
    }

}
