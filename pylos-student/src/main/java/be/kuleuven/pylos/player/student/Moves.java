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

    /**
     * Maakt een volledige Z, dit is een Z waarbij er 2 vrije plaatsen aanwezig zijn.
     * @param sphere De sphere om te plaatsen.
     * @param board Het bord van het spel.
     * @param player De speler die de aan de beurt is.
     * @return
     */

    public static boolean buildFullZ(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){
        if(buildFullZ1(game,board,player,sphere)) {
            return true;
        }

        return buildFullZ2(game, board, player, sphere);
    }


    /**
     * Het eerste patroon van voor creatie van een Z, hierbij heeft het vierkant al een driehoek.
     * @param sphere De sphere om te plaatsen.
     * @param board Het bord van het spel.
     * @param player De speler die de aan de beurt is.
     * @return
     */

    private static boolean buildFullZ1(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){
        PylosSquare[] squares = board.getAllSquares(); //14 mogelijke squares in 4x4 bord

        //zoek driehoeken
        List<PylosSquare>  triangles = Arrays.stream(squares)
                .filter(e -> e.getInSquare(player)==3 && e.getInSquare() == 3)
                .collect(Collectors.toList());

        for(PylosSquare square :triangles){
            int xpos = getLegeSpotposTriangle(square);  //xpos is pos van lege plaats

            int [] richting = bepaalrichting(xpos); //Bepaalt richting van andere bal
            PylosLocation pylosLocation = square.getLocations()[xpos];        //Geeft locatie van lege plaats

            PylosLocation locationbal;
            if((locationbal = checkPlaceExists(board, richting, pylosLocation)) != null){
                game.moveSphere(sphere, locationbal);
                lastPlacedSphere = sphere;
                return true;
            }
        }

        return false;
    }




    /**
     * Het tweede patroon van voor creatie van een Z, hierbij heeft het vierkant een diagonaal.
     * @param sphere De sphere om te plaatsen.
     * @param board Het bord van het spel.
     * @param player De speler die de aan de beurt is.
     * @return
     */

    private static boolean buildFullZ2(PylosGameIF game, PylosBoard board, PylosPlayer player, PylosSphere sphere){
        //zoek squares met 2 eigen ballen en 1 tegenstander bal
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 2 && e.getInSquare() == 2)
                .collect(Collectors.toList());


        //Kijken of eigen ballen schuin tov elkaar staan
        for(PylosSquare square :squares){
            if(isFullZSquare(square)){
                List<Integer> lpos = getLegeBalpos(square); //Bepaalt lege plaatsen positie
                List<PylosSquare> locations = new ArrayList<>();
                locations.add(square);
                List<PylosLocation> legeplaatsen = getLegePlaatsenInSquares(locations);     //Bepaalt de lege plaatsen

                for(int i=0; i<legeplaatsen.size();i++) {
                    if(legeplaatsen.get(i).isUsable()){ //kijken of usable is

                        int [] richting = bepaalrichting((lpos.get((i+1)%2)));      //halen richting van andere lege plaats
                        PylosLocation pylosLocation = getTegenovergesteldeLeeg(square,legeplaatsen.get(i));
                        if(pylosLocation!=null){                //KAN ALS TEGENOVERGESTELDE ZWEEFT
                        if (isCheckPlaceExistsPattern(board, richting, pylosLocation,player)) {
                            game.moveSphere(sphere, legeplaatsen.get(i));
                            lastPlacedSphere = sphere;
                            return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }


    //Controlleerd of de plaats bestaat buiten het bord voor Pattern2.
    private static PylosLocation checkPlaceExists(PylosBoard board, int[] richting, PylosLocation pylosLocation){
        boolean[] exist = realLocation(richting,pylosLocation);       //Kijkt of bal bestaat

        if(exist[0]){
            int pos2 = Integer.signum(richting[1]);
            PylosLocation locationbal = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y,pylosLocation.Z);
            PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y + pos2 ,pylosLocation.Z);
            if(locationbal.isUsable()&&locationbal2.isUsable()){
                return locationbal;
            }
        }

        if(exist[1]){
            int pos2 = Integer.signum(richting[0]);
            PylosLocation locationbal = board.getBoardLocation(pylosLocation.X,pylosLocation.Y+richting[1],pylosLocation.Z);
            PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+ pos2,pylosLocation.Y + richting[1],pylosLocation.Z);
            if(locationbal.isUsable() && locationbal2.isUsable()){
                return locationbal;
            }
        }
        return null;
    }

    //controlleert of de plaats bestaat voor Pattern1.
    private static boolean isCheckPlaceExistsPattern(PylosBoard board, int[] richting, PylosLocation pylosLocation, PylosPlayer player){

        boolean[] exist = realLocation(richting,pylosLocation);

        if(exist[0]){
            int pos2 = Integer.signum(richting[1]);
            PylosLocation locationbal = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y,pylosLocation.Z);
            PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+richting[0],pylosLocation.Y + pos2 ,pylosLocation.Z);
            if(locationbal.isUsed()&&locationbal.getSphere().PLAYER_COLOR.equals(player.PLAYER_COLOR) && !locationbal2.isUsed()){
                return true;
            }
        }

        if(exist[1]){
            int pos2 = Integer.signum(richting[0]);
            PylosLocation locationbal = board.getBoardLocation(pylosLocation.X,pylosLocation.Y+richting[1],pylosLocation.Z);
            PylosLocation locationbal2 = board.getBoardLocation(pylosLocation.X+ pos2,pylosLocation.Y + +richting[1],pylosLocation.Z);
            if(locationbal.isUsed() && locationbal.getSphere().PLAYER_COLOR.equals(player.PLAYER_COLOR) && !locationbal2.isUsed()){
                return true;
            }
        }

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

        PylosLocation toLocation = getDichstBijCentrum(board, freeTrianglesLocations);

        //verzet of plaats op bord
        game. moveSphere(sphere, toLocation);
        lastPlacedSphere = sphere;
        //System.out.println("blockTriangleOpponent");
        return true;
    }

    private static PylosLocation getDichstBijCentrum(PylosBoard board, List<PylosLocation> freeTrianglesLocations){
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

        return toLocation;
    }

    /**
     * Zoek driehoek waarvan een viekant kan maken.
     * @param game
     * @param board
     * @param player De huidige speler aan de beurt.
     * @return
     */
    public static boolean buildSquare(PylosGameIF game, PylosBoard board, PylosPlayer player){
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 3 && e.getInSquare(player.OTHER) == 0)
                .collect(Collectors.toList());

        if(squares.isEmpty())
            return false;

        PylosLocation location = null;
        while (location == null && !squares.isEmpty())
            location = getLegeLocation(squares.remove(0));

        if(location != null) {
            PylosSphere pylosSphere = board.getReserve(player);
            game.moveSphere(pylosSphere, location);
            lastPlacedSphere = pylosSphere;
            //System.out.println("buildSquare");
            return true;
        }
        return false;
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
        //System.out.println("buildUpWithLowerSphere");
        return true;
    }


    /**
     * Maakt een halve Z, waarbij 1 van de vakjes geblokkeerd is door de tegenstander.
     * @param sphere De sphere om te plaatsen.
     * @param board Het bord van het spel.
     * @param player De speler die de aan de beurt is.
     * @return
     */
    public static boolean buildHalfZ(PylosGameIF game, PylosBoard board, PylosPlayer player,  PylosSphere sphere){
        if(buildHalfZ1(game,board,player,sphere)){
            return true;
        }

        return buildHalfZ2(game,board,player,sphere);
    }


    /**
     * Maakt een halve Z, waarbij 1 van de vakjes geblokkeerd is door de tegenstander
     * en vertrekt van een vierkant met 1 bal van tegenstander
     * @param sphere De sphere om te plaatsen.
     * @param board Het bord van het spel.
     * @param player De speler die de aan de beurt is.
     * @return
     */
    private static boolean buildHalfZ1(PylosGameIF game, PylosBoard board, PylosPlayer player,  PylosSphere sphere) {
        //zoek squares met 3 eigen ballen en 1 tegenstander bal
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 3 && e.getInSquare() == 4)
                .collect(Collectors.toList());

        for (PylosSquare square : squares) {
            int xpos = findXPos(player, square); //Bepaalt pos van X
            int [] richting = bepaalrichting(xpos); //Bepaalt richting van andere bal
            PylosLocation pylosLocation = getHalfZSquarePos(player,square);         //Geeft locatie van X

            PylosLocation locationbal;
            if((locationbal = checkPlaceExists(board, richting, pylosLocation)) != null){
                game.moveSphere(sphere, locationbal);
                lastPlacedSphere = sphere;
                return true;
            }
        }

        return false;
    }



    /**
     * Maakt een halve Z, waarbij 1 van de vakjes geblokkeerd is door de tegenstander
     * en vertrekt van een driehoek met 1 bal van tegenstander
     * @param sphere De sphere om te plaatsen.
     * @param board Het bord van het spel.
     * @param player De speler die de aan de beurt is.
     * @return
     */
    private static boolean buildHalfZ2(PylosGameIF game, PylosBoard board, PylosPlayer player,  PylosSphere sphere){


        //zoek squares met 2 eigen ballen en 1 tegenstander bal
        List<PylosSquare> squares = Arrays.stream(board.getAllSquares())
                .filter(e -> e.getInSquare(player) == 2 && e.getInSquare() == 3)
                .collect(Collectors.toList());


        //Kijken of eigen ballen schuin tov elkaar staan
        for(PylosSquare square :squares){
            if(isHalfZSquare(player,square)){
                int xpos = findXPos(player,square); //Bepaalt pos van X
                PylosLocation legeplaats = getLegeLocation(square);

                //lege plaats kan null zijn wanneer locatie onder plaats nog niet gebruikt is
                if(legeplaats!=null) {
                    int [] richting = bepaalrichting(xpos); //Bepaalt richting van andere bal
                    PylosLocation pylosLocation = getHalfZSquarePos(player,square);         //Geeft locatie van X


                    if(isCheckPlaceExistsPattern(board,richting,pylosLocation,player)){
                        game.moveSphere(sphere, legeplaats);
                        lastPlacedSphere = sphere;
                        return true;
                    }

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
        List<PylosLocation> freeLocations = getLegePlaatsenInSquares(squares);

        if(freeLocations.isEmpty())
            return false;

        //zoek plaats dichtst bij centrum (eerste gevonden dichtste plaats)
        PylosLocation toLocation = getDichstBijCentrum(board, freeLocations);

        //verzet of plaats op bord
        game.moveSphere(sphere, toLocation);
        lastPlacedSphere = sphere;
        //System.out.println("createTriangle");
        return true;
    }

    private static List<PylosLocation> getLegePlaatsenInSquares(List<PylosSquare> squares){
        List<PylosLocation> freeLocations = new ArrayList<>();
        squares.forEach(e -> Arrays.stream(e.getLocations())
                .forEach(q -> {if(q.isUsable()){freeLocations.add(q);}}));
        return freeLocations;
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
        List<PylosLocation> freeLocations = getLegePlaatsenInSquares(squares);

        List<PylosLocation> bezetteLocations = new ArrayList<>();
        squares.forEach(e -> Arrays.stream(e.getLocations())
                .forEach(q -> {if(!q.isUsable()){bezetteLocations.add(q);}}));

        if(freeLocations.isEmpty())
            return false;

        //zoek plaats dichtst bij centrum (eerste gevonden dichtste plaats)
        PylosLocation toLocation = null;
        PylosLocation pylosLocationbal = bezetteLocations.get(0);
        for(PylosLocation location: freeLocations){
            if(pylosLocationbal!=null) {
                if ((pylosLocationbal.X + location.X + pylosLocationbal.Y + location.Y) % 2 == 1) {
                    toLocation = getDichstBijCentrum(board, freeLocations);
                }
            }
        }

        //verzet of plaats op bord
        if(toLocation != null){
            game.moveSphere(sphere, toLocation);
            lastPlacedSphere = sphere;
            return true;
        }

        return false;
    }

    /**
     * Zet een random bal (reserve of reeds op bord) naar random locatie
     * @param board
     * @param player
     * @return
     */
    public static boolean randomMove(PylosGameIF game, PylosBoard board, PylosPlayer player){
        Random random = new Random();

        //get random vrije locatie op bord
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(e -> e.isUsable()).collect(Collectors.toList());
        PylosLocation randomLocation = usableLocations.get(random.nextInt(usableLocations.size()));

        //get random bal (uit reserve of reeds op bord)
        List<PylosSphere> usableSpheres = Arrays.stream(board.getSpheres(player)).filter(e -> e.canMoveTo(randomLocation)).collect(Collectors.toList());
        PylosSphere randomSphere = usableSpheres.get(random.nextInt(usableSpheres.size()));

        //verzet of plaats op bord
        game.moveSphere(randomSphere, randomLocation);
        lastPlacedSphere = randomSphere;

        //System.out.println("randomMove");
        return true;
    }

    //TODO DOCUMENTEREN
    //get eerste lege location
    private static PylosLocation getLegeLocation(PylosSquare pylosSquare){
        for(int i=0; i<4; i++)
            if(pylosSquare.getLocations()[i].isUsable())
                return pylosSquare.getLocations()[i];

        return null;
    }

    private static boolean isHalfZSquare(PylosPlayer player, PylosSquare square){
        if(!square.getLocations()[0].isUsed() || square.getLocations()[0].getSphere().PLAYER_COLOR.equals(player.OTHER.PLAYER_COLOR)) {           //is X of leeg
            if (square.getLocations()[1].isUsed() && square.getLocations()[2].isUsed()) {
                return square.getLocations()[1].getSphere().PLAYER_COLOR.equals(square.getLocations()[2].getSphere().PLAYER_COLOR);
            }
        }

        else{
            return square.getLocations()[2].isUsed() && square.getLocations()[0].getSphere().PLAYER_COLOR.equals(square.getLocations()[2].getSphere().PLAYER_COLOR);
        }
        return false;
    }

    //Geeft Location van de X
    private static PylosLocation getHalfZSquarePos(PylosPlayer player, PylosSquare square){
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



    private static boolean[] realLocation(int [] richting ,PylosLocation pylosLocation){
        boolean [] exist = new boolean[2];
        //exist default op false
        int positionx = pylosLocation.X + richting[0];
        int positiony = pylosLocation.Y + richting[1];

        if(pylosLocation.Z == 0){
            if(positionx >= 0 && positionx < 4){
                exist[0] = true;
            }

            if(positiony >= 0 && positiony < 4){
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

    private static int getLegeSpotposTriangle(PylosSquare square){
        for(int i=0; i<4; i++) {
            if (!square.getLocations()[i].isUsed()) {
                return i;
            }
        }
        return -1;
    }


    private static boolean isFullZSquare(PylosSquare square) {
        List<Integer> legePositions = getLegeBalpos(square);

        int legePosition1 = legePositions.remove(0);
        int legePosition2 = legePositions.remove(0);

        return legePosition1 + legePosition2 == 3;

    }


    private static List<Integer> getLegeBalpos(PylosSquare square){
        List<Integer> lijst = new ArrayList<>();
        for(int i=0; i<4; i++) {
            if (!square.getLocations()[i].isUsed()) {
                lijst.add(i);
            }
        }
        return lijst;
    }

    private static PylosLocation getTegenovergesteldeLeeg(PylosSquare square, PylosLocation location){
        for(int i=0; i<4; i++){
            if(square.getLocations()[i].isUsable() && !square.getLocations()[i].equals(location)){
                return square.getLocations()[i];
            }
        }

        return null;
    }


}
