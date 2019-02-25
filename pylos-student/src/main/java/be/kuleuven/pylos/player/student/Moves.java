package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import javax.xml.stream.Location;
import java.util.*;
import java.util.stream.Collectors;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFit.testSphere;

/**
 * Created by ruben on 23/02/19.
 * Elke methode returned true als de move is uitgevoerd,
 * false als de move niet uitvoerbaar is
 */
public abstract class Moves {

    public static boolean makeCross(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){
        return false;
    }


    public static boolean buildFullZ(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){
        PylosSquare[] squares = board.getAllSquares(); //14 mogelijke squares in 4x4 bord

        //zoek driehoeken, return false als geen zijn
        List<PylosSquare>  triangles = Arrays.stream(squares)
                .filter(e -> e.getInSquare(player)==3 && e.getInSquare() == 3)
                .collect(Collectors.toList());


        return true;
    }



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
     * Maak een vierkant van een square met 3 ballen van jouw kleur
     * @param game
     * @param board
     * @param player De huidige speler aan de beurt
     * @return
     */
    public static boolean buildSquare(PylosGameIF game, PylosBoard board, PylosPlayer player){
        ArrayList<PylosLocation> allUsableLocations = new ArrayList<>();
        for (PylosLocation bl : board.getLocations()) {
            if (bl.isUsable()) {
                allUsableLocations.add(bl);
            }
        }

        //Zoeken van squares met 3 ballen van eigen kleur
        ArrayList<PylosSquare> allUsefullsquares = new ArrayList<>();
        for (PylosSquare square : board.getAllSquares()) {
            if(square.getInSquare(player)==3&&square.getInSquare(player.OTHER)==0){
                allUsefullsquares.add(square);
            }
        }

        //GEEN MOGELIJKHEID TOT MAKEN VAN VIERKANT
        if(allUsefullsquares.size()==0){
            return false;
        }


        else{
            Collections.shuffle(allUsefullsquares);     //Neem een random square
            PylosSquare pylosSquare = allUsefullsquares.get(0);
            PylosLocation pylosLocation = getLegeLocation(pylosSquare);

            //TODO NIET ALTIJD VAN RESERVE NEMEN
            PylosSphere pylosSphere = board.getReserve(player);

            game.moveSphere(pylosSphere,pylosLocation);

            return true;
        }



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

    public static boolean buildHalfZ(PylosGameIF game, PylosBoard board, PylosPlayer player,  PylosSphere sphere){
        //zoek squares met 2 eigen ballen en 1 tegenstander bal
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 2 && e.getInSquare() == 3)
                .collect(Collectors.toList());


        //Kijken of eigen ballen schuin tov elkaar staan
        for(PylosSquare square :squares){
            if(isHalfZSquare(player,square)){
                PylosLocation pylosLocation = getHalfZSquarePos(player,square);

            }
        }



/*
        //zoek alle vrije plaatsen in driehoeken
        List<PylosLocation> freeLocations = new ArrayList<>();
        squares.forEach(e -> Arrays.stream(e.isSquare())
                .filter(PylosLocation::isUsable)
                .forEach(freeLocations::add));

        freeLocations.get(0).
*/


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
     * Connecteer 2 ballen aan elkaar, deze zoekt ballen waarbij er slechts 1 bal aanwezig is van de speler
     * @param board
     * @param player De huidige speler aan de beurt
     * @return
     */
    public static boolean createConnection(PylosGameIF game, PylosBoard board, PylosPlayer player,PylosSphere sphere){

        //Zoek squares met 1 bal van player en nog lege plaatsen
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 1 && e.getInSquare() < 4)
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






    //TODO NIET ALTIJD EERSTE LOCATIE TERUGGEVEN
    public static PylosLocation getLegeLocation(PylosSquare pylosSquare){
        for(int i=0; i<4; i++){
            if(!pylosSquare.getLocations()[i].isUsed()){
                return pylosSquare.getLocations()[i];
            }
        }
        return null;
    }


    //Kijkt of square van type

    /*

    ************ Wit Black
    **** ******* Niets Wit

    is

     */
    public static boolean isHalfZSquare(PylosPlayer player, PylosSquare square){
       if(!square.getLocations()[0].isUsed() || square.getLocations()[0].getSphere().PLAYER_COLOR.equals(player.OTHER.PLAYER_COLOR)){
           if(square.getLocations()[1].getSphere().PLAYER_COLOR.equals(square.getLocations()[2].getSphere().PLAYER_COLOR)){
             return true;
           }
       }

       else{
           if (square.getLocations()[2].isUsed() && square.getLocations()[0].getSphere().PLAYER_COLOR.equals(square.getLocations()[2].getSphere().PLAYER_COLOR)){
               return true;
           }

       return false;
    }
    return false;
    }

    public static PylosLocation getHalfZSquarePos(PylosPlayer player, PylosSquare square){
        for(PylosLocation location :square.getLocations()){
            if(location.isUsed() && location.getSphere().PLAYER_COLOR.equals(player.OTHER.PLAYER_COLOR)){

                return location;
            }
        }

        return null;
    }

}
