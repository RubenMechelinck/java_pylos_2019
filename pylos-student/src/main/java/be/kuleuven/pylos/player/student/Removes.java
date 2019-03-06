package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.game.PylosSquare;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFit.lastPlacedSphere;

/**
 * Created by ruben on 2/03/19.
 */
public class Removes {

    /**
     * Remove een bal uit het gemaakte vierkant, kies bal die verst van de rand ligt.
     * @param game
     * @param board
     */
    public static void removeFirstSphere(PylosGameIF game, PylosBoard board, PylosPlayer player){

        //get alle squares waar laatste gelegde sphere tot behoort
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(t -> t.getInSquare(player) == 4)
                .filter(e -> Arrays.stream(e.getLocations())
                        .anyMatch(v -> v == lastPlacedSphere.getLocation()))
                .collect(Collectors.toList());

        //get locatie met eigen sphere die dichts bij rand ligt
        PylosSphere[] sphere = new PylosSphere[1];
        int[] l = {Integer.MIN_VALUE};
        squares.forEach(e -> Arrays.stream(e.getLocations()).forEach(r -> {
            int v = Math.abs(r.X + r.Y - board.SIZE + r.Z);
            if(r.getSphere().canRemove() && v > l[0]) {
                l[0] = v;
                sphere[0] = r.getSphere();
            }
        }));

        //haal die sphere weg
        game.removeSphere(sphere[0]);
        //System.out.println("- removeFirstSphere");
    }

    public static void removeSecondSphere(PylosGameIF game, PylosBoard board, PylosPlayer player){
        //get alle vrije spheres die geen voordeel voor tegenstander geven
        List<PylosSphere> spheres = Arrays.stream(board.getSpheres(player))
                .filter(e -> e.canRemove() && !canOpponentBuildSquire(e, board, player))
                .collect(Collectors.toList());


        if(!spheres.isEmpty()) {
            game.removeSphere(spheres.get(0));
            //System.out.println("- removeSecondSphere");
        } else
            game.pass();
    }

    private static boolean canOpponentBuildSquire(PylosSphere sphere, PylosBoard board, PylosPlayer player){
        return sphere.getLocation().getMaxInSquare(player.OTHER) == 3;
    }
}
