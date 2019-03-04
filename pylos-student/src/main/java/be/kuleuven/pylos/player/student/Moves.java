package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFit.lastPlacedSphere;

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


        //2 situaties
        //TODO SITUATIE 1


        //zoek driehoeken, return false als geen zijn
        List<PylosSquare>  triangles = Arrays.stream(squares)
                .filter(e -> e.getInSquare(player)==3 && e.getInSquare() == 3)
                .collect(Collectors.toList());



        for(PylosSquare square :triangles){
            int xpos = getLegeSpotposTriangle(square);  //xpos is pos van lege plaats

            int [] richting = bepaalrichting(xpos); //Bepaalt richting van andere bal
            PylosLocation pylosLocation = square.getLocations()[xpos];        //Geeft locatie van lege plaats
            boolean[] exist = realLocation(richting,pylosLocation,board);       //Kijkt of bal bestaat

            //TODO IN METHODE STEKEN
            if(exist[0]){
                int pos2 = Integer.signum(richting[1]);
                System.out.println("xpos: " + xpos);
                System.out.println("geval1");
                PylosLocation locationbal = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y,pylosLocation.Z);
                PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y + pos2 ,pylosLocation.Z);
                if(locationbal.isUsable()&&locationbal2.isUsable()){
                    game.moveSphere(sphere,locationbal);
                    return true;
                }
            }

            if(exist[1]){
                int pos2 = Integer.signum(richting[0]);
                System.out.println("xpos: " + xpos);
                PylosLocation locationbal = board.getBoardLocation(pylosLocation.X,pylosLocation.Y+richting[1],pylosLocation.Z);
                PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+ pos2,pylosLocation.Y + richting[1],pylosLocation.Z);
                if(locationbal.isUsable() && locationbal2.isUsable()){
                    game.moveSphere(sphere,locationbal);
                    return true;
                }
            }




        }

        //TODO SITUATIE 2


        return false;
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
        lastPlacedSphere = sphere;

        System.out.println("blockTriangleOpponent");
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
            if(square.getInSquare(player) == 3 && square.getInSquare(player.OTHER)==0){
                allUsefullsquares.add(square);
            }
        }

        //GEEN MOGELIJKHEID TOT MAKEN VAN VIERKANT
        if(allUsefullsquares.size()==0){
            return false;
        }


        else{
            Collections.shuffle(allUsefullsquares);     //Neem een random square
            int i = 0;
            PylosSquare pylosSquare = allUsefullsquares.get(i);
            PylosLocation pylosLocation = null;
            while(i < allUsefullsquares.size() && (pylosLocation = getLegeLocation(pylosSquare)) == null){
                pylosSquare = allUsefullsquares.get(i++);
            }

            if(pylosLocation == null)
                return false;


            //TODO NIET ALTIJD VAN RESERVE NEMEN
            PylosSphere pylosSphere = board.getReserve(player);

            game.moveSphere(pylosSphere,pylosLocation);

            lastPlacedSphere = pylosSphere;

            System.out.println("buildSquare");
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
        lastPlacedSphere = sphere;
        System.out.println("buildUpWithLowerSphere");
        return true;
    }

    public static boolean buildHalfZ(PylosGameIF game, PylosBoard board, PylosPlayer player,  PylosSphere sphere) {
        //zoek squares met 3 eigen ballen en 1 tegenstander bal
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 3 && e.getInSquare() == 4)
                .collect(Collectors.toList());

        for (PylosSquare square : squares) {
            int xpos = findXPos(player, square); //Bepaalt pos van X
            int [] richting = bepaalrichting(xpos); //Bepaalt richting van andere bal
            PylosLocation pylosLocation = getHalfZSquarePos(player,square);         //Geeft locatie van X
            boolean[] exist = realLocation(richting,pylosLocation,board);       //Kijkt of bal bestaat

            if(exist[0]){
                int pos2 = Integer.signum(richting[1]);
                PylosLocation locationbal = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y,pylosLocation.Z);
                PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y + pos2 ,pylosLocation.Z);
                if(locationbal.isUsable() && locationbal2.isUsable()){
                    game.moveSphere(sphere,locationbal);
                    return true;
                }
            }



            if(exist[1]){
                int pos2 = Integer.signum(richting[0]);
                PylosLocation locationbal = board.getBoardLocation(pylosLocation.X,pylosLocation.Y+richting[1],pylosLocation.Z);
                PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+ pos2,pylosLocation.Y + +richting[1],pylosLocation.Z);
                if(locationbal.isUsable() && locationbal2.isUsable()){
                    game.moveSphere(sphere,locationbal);
                    return true;
                }
            }


        }

        return false;
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
            int r = Math.abs(location.X + location.Y - board.SIZE + location.Z); //board.size - location.Z is breedte van laag
            if(t > r) {
                t = r;
                toLocation = location;
            }
        }

        //verzet of plaats op bord
        game.moveSphere(sphere, toLocation);
        lastPlacedSphere = sphere;

        System.out.println("createTriangle");
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

        List<PylosLocation> bezetteLocations = new ArrayList<>();
        squares.forEach(e -> Arrays.stream(e.getLocations())
                .forEach(q -> {if(!q.isUsable()){bezetteLocations.add(q);}}));


        if(freeLocations.isEmpty())
            return false;

        //zoek plaats dichtst bij centrum (eerste gevonden dichtste plaats)
        int t = Integer.MAX_VALUE;
        PylosLocation toLocation = null;
        PylosLocation pylosLocationbal = bezetteLocations.get(0);
        for(PylosLocation location: freeLocations){
            if(pylosLocationbal!=null) {
                if ((pylosLocationbal.X + location.X + pylosLocationbal.Y + location.Y) % 2 == 1) {

                    int r = Math.abs(location.X + location.Y - board.SIZE + location.Z);
                    if (t > r) {
                        t = r;
                        toLocation = location;
                    }
                }
            }
        }

        //verzet of plaats op bord
        if(toLocation!=null){
            game.moveSphere(sphere, toLocation);
            lastPlacedSphere = sphere;
            return true;
        }

        
        return false;
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
        lastPlacedSphere = randomSphere;

        System.out.println("randomMove");
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
        if(!square.getLocations()[0].isUsed() || square.getLocations()[0].getSphere().PLAYER_COLOR.equals(player.OTHER.PLAYER_COLOR)) {           //is X of leeg
            if (square.getLocations()[1].isUsed() && square.getLocations()[2].isUsed()) {                                                    //TODO EFFICIENTER
                if (square.getLocations()[1].getSphere().PLAYER_COLOR.equals(square.getLocations()[2].getSphere().PLAYER_COLOR)) {
                    return true;
                }
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









    //Geeft Location van de X
    public static PylosLocation getHalfZSquarePos(PylosPlayer player, PylosSquare square){
        for(PylosLocation location :square.getLocations()){
            if(location.isUsed() && location.getSphere().PLAYER_COLOR.equals(player.OTHER.PLAYER_COLOR)){

                return location;
            }
        }

        return null;
    }

    private static int findXPos(PylosPlayer player,PylosSquare pylosSquare){
        for(int i=0; i<4; i++) {
            if (pylosSquare.getLocations()[i].isUsed()) {
                if (pylosSquare.getLocations()[i].getSphere().PLAYER_COLOR.equals(player.OTHER.PLAYER_COLOR)) {
                    return i;
                }
            }
        }
        return -1;
    }



    //TODO DOCUMENTEREN
    private static int[] bepaalrichting(int posX) {
        int[] richting = new int[2];
        richting[0] = 2;       //bit zetten op 0/1
        richting[1] = 2;


        if (posX % 2 != 0) {
            richting[0] = -richting[0];
        }

        if (posX % 4 - 1 > 0) {
            richting[1] = -richting[1];
        }

        return richting;
    }



    private static boolean[] realLocation(int [] richting ,PylosLocation pylosLocation, PylosBoard board){
        boolean [] exist = new boolean[2];
        //exist default op false

        System.out.println("x:" + pylosLocation.X );
        System.out.println("y:" + pylosLocation.Y);
        System.out.println("richtingx:" + richting[0]);
        System.out.println("richtingy:" + richting[1]);
        int positionx = pylosLocation.X + richting[0];
        int positiony = pylosLocation.Y + richting[1];

        if(pylosLocation.Z == 0){
            if(positionx>=0&&positionx<4){
                exist[0] = true;
            }

            if(positiony>=0&&positiony<4){
                exist[1] = true;
            }
        }

        else if(pylosLocation.Z == 1){
            if(positionx>=0&&positionx<3){
                exist[0] = true;
            }

            if(positiony>=0&&positiony<3){
                exist[1] = true;
            }
        }

        return exist;
    }




    public static int getLegeSpotposTriangle(PylosSquare square){
        for(int i=0; i<4; i++) {
            if (!square.getLocations()[i].isUsed()) {
                return i;
            }
        }
        return -1;
    }





}
