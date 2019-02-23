package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFit.testSphere;

/**
 * Created by ruben on 23/02/19.
 * Elke methode returned true als de move is uitgevoerd,
 * false als de move niet uitvoerbaar is
 */
public abstract class Moves {

    /**
     * Blokeer een driehoek van de tegenstander door een eigen bal op de legen plek te leggen.
     * @param sphere
     * @param board
     * @param player De speler die de blokkade uitvoerd, niet de tegenstander!!
     * @return
     */
    public static boolean blockTriangleOpponent(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){
        PylosSquare[] squares = board.getAllSquares(); //14 mogelijke squares in 4x4 bord

        //zoek driehoeken, return false als geen zijn
        List<PylosSquare>  triangles = Arrays.stream(squares)
                .filter(e -> e.getInSquare(player.OTHER)==3 && e.getInSquare() == 3)
                .collect(Collectors.toList());

        //zoek alle vrije plaatsen in driehoeken
        List<PylosLocation> freeTrianglesLocations = new ArrayList<>();
        triangles.forEach(e -> Arrays.stream(e.getLocations())
                .filter(PylosLocation::isUsable)
                .forEach(freeTrianglesLocations::add));

        if (freeTrianglesLocations.isEmpty())
            return false;

        //zoek plaats dichtst bij centrum (eerste gevonden dichtste plaats)
        int t = Integer.MAX_VALUE;
        PylosLocation toLocation = null;
        for(PylosLocation location: freeTrianglesLocations){
            int r = Math.abs(location.X + location.Y - board.SIZE + location.Z);
            if(t > r) {
                t = r;
                toLocation = location;
            }
        }

        //verzet of plaats op bord
        game. moveSphere(sphere, toLocation);

        //for testing
        testSphere = sphere;

        return true;
    }

    /**
     * Plaats laagst neembare bal op de hoogst mogelijke plaats.
     * @param board
     * @param player De speler die aan zet is
     * @return
     */
    public static boolean buildUpWithLowerSphere(PylosGameIF game, PylosBoard board, PylosPlayer player){
        //vrije plaatsen zoeken
        //14 mogelijke squares in 4x4 bord
        List<PylosLocation> freeSquares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getTopLocation().isUsable())
                .map(PylosSquare::getTopLocation)
                .collect(Collectors.toList());

        //hoogste vrije plaats zoeken (eerste gevonden hoogste)
        int t = 0;
        PylosLocation toLocation = null;
        for(PylosLocation pylosLocation: freeSquares){
            if(pylosLocation.Z > t){
                t = pylosLocation.Z;
                toLocation = pylosLocation;
            }
        }

        if (toLocation == null)
            return false;

        //zoek vrije ballen
        // + zoek laagste niveau
        //check dat op hoger niveau ligt zit in PylosSphere::canMoveTo
        //30 locations op 4x4 bord
        final int[] laagsteNiveau = {Integer.MAX_VALUE};
        PylosLocation finalToLocation = toLocation;
        List<PylosSphere> moveableSpheres = Arrays.stream(board.getLocations())
                .filter(e -> e.isUsed()
                        && e.getSphere().PLAYER_COLOR.equals(player.PLAYER_COLOR)
                        && e.getSphere().canMoveTo(finalToLocation))
                .peek(e -> { if(e.Z < laagsteNiveau[0]){laagsteNiveau[0] = e.Z;}})
                .map(PylosLocation::getSphere)
                .collect(Collectors.toList());

        if(moveableSpheres.isEmpty())
            return false;

        //zoek bal verst van centrum gelegen
        int q = Integer.MIN_VALUE;
        PylosSphere sphere = null;
        List<PylosSphere> laagsteSpheres = moveableSpheres.stream()
                .filter(e -> e.getLocation().Z == laagsteNiveau[0])
                .collect(Collectors.toList());

        for(PylosSphere pylosSphere: laagsteSpheres){
            int r = Math.abs(pylosSphere.getLocation().X + pylosSphere.getLocation().Y - board.SIZE + pylosSphere.getLocation().Z);
            if(q < r) {
                q = r;
                sphere = pylosSphere;
            }
        }

        // do move
        game.moveSphere(sphere, toLocation);

        //for testing
        testSphere = sphere;

        return true;
    }

    /**
     * Zoekt vierkant met 2 eigen ballen en 2 lege plaatsen, zet par:sphere op plek dichtst bij centrum
     * @param board
     * @param player
     * @return
     */
    public static boolean createTriangle(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){

        //zoek squares met 2 eigen ballen en 2 legen plaatsen
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 2 && e.getInSquare() == 2)
                .collect(Collectors.toList());

        //lege plaatsen in halve squares
        List<PylosLocation> freeLocations = new ArrayList<>();
        squares.forEach(e -> Arrays.stream(e.getLocations())
                .forEach(q -> {if(q.isUsable()){freeLocations.add(q);}}));

        if(freeLocations.isEmpty())
            return false;

        //zoek plaats dichtst bij centrum (eerste gevonden dichtste plaats)
        int t = Integer.MAX_VALUE;
        PylosLocation toLocation = null;
        for(PylosLocation location: freeLocations){
            int r = Math.abs(location.X + location.Y - board.SIZE + location.Z);
            if(t > r) {
                t = r;
                toLocation = location;
            }
        }

        //verzet of plaats op bord
        game.moveSphere(sphere, toLocation);

        //for testing
        testSphere = sphere;

        return true;
    }

    /**
     * Zet een random bal (reserver of reeds op bord) naar random locatie
     * @param board
     * @param player
     * @return
     */
    public static boolean randomMove(PylosGameIF game, PylosBoard board, PylosPlayer player){
        Random random = new Random(0);

        //get random vrije locatie op bord
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(e -> e.isUsable()).collect(Collectors.toList());
        PylosLocation randomLocation = usableLocations.get(random.nextInt(usableLocations.size()));

        //get random bal (uit reserve of reeds op bord)
        List<PylosSphere> usableSpheres = Arrays.stream(board.getSpheres(player)).filter(e -> e.canMoveTo(randomLocation)).collect(Collectors.toList());
        PylosSphere randomSphere = usableSpheres.get(random.nextInt(usableSpheres.size()));

        //verzet of plaats op bord
        game.moveSphere(randomSphere, randomLocation);

        //for testing
        testSphere = randomSphere;

        return true;
    }
}
