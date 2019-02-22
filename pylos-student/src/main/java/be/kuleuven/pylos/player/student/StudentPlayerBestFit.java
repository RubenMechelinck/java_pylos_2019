package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer{
    private int boardEvaluation = 0;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {


    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

    }

    public void calculateBoardEvalutation(PylosGameIF game, PylosBoard board){
        int evaluationWhite = 0;
        int evaluationBlack = 0;

        PylosPlayer tegenstander = this.OTHER;

        evaluationWhite = evaluationWhite + board.getReservesSize(this);
        evaluationBlack = evaluationBlack + board.getReservesSize(tegenstander);

    }



    public void findPatern(){

    }
}
