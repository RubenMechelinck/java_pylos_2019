package be.kuleuven.pylos.battle;

import be.kuleuven.pylos.player.codes.PylosPlayerBestFit;
import be.kuleuven.pylos.player.codes.PylosPlayerMiniMax;
import be.kuleuven.pylos.player.codes.PylosPlayerRandomFit;
import be.kuleuven.pylos.player.student.StudentPlayerBestFit;

/**
 * Created by Jan on 23/02/2015.
 */
public class BattleMain {

	public static void main(String[] args){
		Battle.play(new StudentPlayerBestFit(), new PylosPlayerMiniMax(4), 500);
	}

}
