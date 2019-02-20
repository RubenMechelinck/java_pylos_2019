package be.kuleuven.pylos.main;

import be.kuleuven.pylos.battle.Battle;
import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGame;
import be.kuleuven.pylos.game.PylosGameObserver;
import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerObserver;
import be.kuleuven.pylos.player.codes.PylosPlayerRandomFit;
import be.kuleuven.pylos.player.student.StudentPlayerRandomFit;

import java.util.Random;

/**
 * Created by Jan on 15/11/2016.
 */
public class PylosMain {

	public PylosMain(){

	}

	public void startSingleGame(){
		Random random = new Random(0);

		PylosPlayer randomPlayerCodes = new PylosPlayerRandomFit();
		PylosPlayer randomPlayerStudent = new StudentPlayerRandomFit();

		PylosBoard pylosBoard = new PylosBoard();
		PylosGame pylosGame = new PylosGame(pylosBoard, randomPlayerCodes, randomPlayerStudent, random, PylosGameObserver.CONSOLE_GAME_OBSERVER, PylosPlayerObserver.NONE);

		pylosGame.play();
	}

	public void startBattle(){
		PylosPlayer playerLight = new PylosPlayerRandomFit();
		PylosPlayer playerDark = new PylosPlayerRandomFit();

		Battle.play(playerLight, playerDark, 100);
	}

	public static void main(String[] args){

		/* !!! vm argument !!! -ea */

		new PylosMain().startSingleGame();
//		new PylosMain().startBattle();

	}

}
