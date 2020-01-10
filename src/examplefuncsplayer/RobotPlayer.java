package examplefuncsplayer;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {

    static class Crypto {
        static final int[] secrets = {256642705, 1566700715, 180962821, -564815468, -1501169840, 1817168436, 760065196};
        static final int[] secret_perm = {181, 69, 66, 52, 148, 157, 190, 134, 7, 177, 10, 51, 174, 192, 137, 120,
                212, 98, 115, 2, 4, 189, 71, 198, 159, 97, 60, 96, 58, 153, 47, 21, 164, 68, 216, 20, 25, 111, 129,
                182, 141, 78, 172, 122, 165, 144, 195, 40, 16, 197, 90, 83, 56, 54, 160, 203, 18, 13, 93, 162, 191,
                119, 45, 86, 17, 185, 150, 179, 130, 11, 57, 168, 43, 29, 112, 77, 76, 125, 70, 208, 75, 135, 215,
                154, 142, 64, 147, 42, 207, 87, 143, 213, 128, 34, 15, 41, 194, 67, 201, 218, 188, 156, 9, 46, 84, 91
                , 22, 173, 171, 200, 152, 183, 104, 30, 36, 133, 61, 80, 24, 101, 6, 124, 100, 81, 102, 210, 131, 108
                , 219, 27, 88, 99, 105, 136, 127, 184, 155, 49, 126, 32, 85, 92, 221, 95, 186, 109, 211, 28, 145, 89,
                167, 175, 161, 140, 166, 132, 214, 3, 158, 0, 59, 1, 82, 48, 39, 178, 138, 149, 63, 62, 74, 72, 116,
                73, 79, 204, 217, 170, 169, 176, 205, 117, 26, 14, 94, 23, 44, 37, 202, 50, 187, 8, 53, 65, 12, 139,
                222, 107, 103, 146, 110, 38, 33, 5, 106, 199, 206, 223, 114, 113, 123, 196, 31, 180, 151, 209, 35,
                193, 121, 55, 118, 19, 163, 220};


        private static BitSet msgToBits(final int[] msg) {
            assert (msg.length == 7);
            BitSet bits = new BitSet(7 * 32);
            for (int i = 0; i < 7; ++i) {
                int x = msg[i];
                for (int j = 0; j < 32; ++j)
                    bits.set(i * 32 + j, (((x >> j) & 1) != 0));
            }
            return bits;
        }

        static int[] bitsToMsg(BitSet bits) {
            int[] res = new int[7];
            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < 32; ++j) {
                    int bit = bits.get(i * 32 + j) ? 1 : 0;
                    res[i] ^= bit << j;
                }
            }
            return res;
        }

        static int[] Encrypt(final int[] msg) {
            int[] msgPlus = msg.clone();
            for (int i = 0; i < 7; ++i)
                msgPlus[i] += secrets[i];
            BitSet bits = msgToBits(msgPlus);
            BitSet bitsEncrypted = new BitSet(7 * 32);
            for (int i = 0; i < 7 * 32; ++i)
                bitsEncrypted.set(i, bits.get(secret_perm[i]));
            return bitsToMsg(bitsEncrypted);
        }

        static int[] Decrypt(final int[] msg) {
            BitSet bits = msgToBits(msg);
            BitSet bitsDecrypted = new BitSet(7 * 32);
            for (int i = 0; i < 7 * 32; ++i)
                bitsDecrypted.set(secret_perm[i], bits.get(i));
            int[] res = bitsToMsg(bitsDecrypted);
            for (int i = 0; i < 7; ++i)
                res[i] -= secrets[i];
            return res;
        }
    }

    static class KnownCell {
        int x;
        int y;
        int lastVisited;

        public KnownCell(int x, int y, int lastVisited) {
            this.x = x;
            this.y = y;
            this.lastVisited = lastVisited;
        }

        public MapLocation location() {
            return new MapLocation(x, y);
        }

        //        static final int UPD_PERIOD = 50;
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KnownCell knownCell = (KnownCell) o;

            if (x != knownCell.x) return false;
            return y == knownCell.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 33 * result + y;
            return result;
        }

    }

    static class SoupCell extends KnownCell {
        int soupLeft;

        public SoupCell(int x, int y, int lastVisited, int soupLeft) {
            super(x, y, lastVisited);
            this.soupLeft = soupLeft;
        }
    }

    static class RobotCell extends KnownCell {
        int id;
        RobotType type;
        boolean enemy;

        public RobotCell(int x, int y, int lastVisited, int id, RobotType type, boolean enemy) {
            super(x, y, lastVisited);
            this.id = id;
            this.type = type;
            this.enemy = enemy;
        }

        public RobotCell(int x, int y, int lastVisited, int id, int type_num, boolean enemy) {
            super(x, y, lastVisited);
            this.id = id;
            this.type = robotTypes[type_num];
            this.enemy = enemy;
        }
    }

    RobotController rc;

    static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    public static final RobotType[] robotTypes = {RobotType.HQ, RobotType.MINER, RobotType.REFINERY,
            RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.LANDSCAPER,
            RobotType.DELIVERY_DRONE, RobotType.NET_GUN, RobotType.COW};
    RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    int turnCount = 0;

    ArrayList<Integer> message_times = new ArrayList<>();
    ArrayList<Transaction> messages = new ArrayList<>();
    ArrayList<KnownCell> knownCells = new ArrayList<KnownCell>(Arrays.asList(new KnownCell[1]));
    ArrayList<Integer> next = new ArrayList<Integer>(Arrays.asList(new Integer[1]));
    int[][] starts;

    MapLocation HQLoc = null;
    MapLocation HQEnemyLoc = null;

    Random rnd = new Random();

    Direction getDir(MapLocation to) {
        int mn = 100000, mni = -1;
        Direction[] d1 = directions.clone();
        for (int i = 0; i < directions.length; ++i) {
            if (!rc.canMove(directions[i]))
                continue;
            int d = distance(rc.getLocation().add(directions[i]), to);
            if (d < mn) {
                mn = d;
                mni = i;
            }
        }
        if (mni == -1)
            return null;
        return directions[mni];
    }

    int sqr(int x) {
        return x * x;
    }

    int distance(MapLocation a, MapLocation b) {
        return sqr(a.x - b.x) + sqr(a.y - b.y);
    }

    static class MinerPlayer extends RobotPlayer {
        ArrayList<SoupCell> soupCells = new ArrayList<>();
        ArrayList<MapLocation> refineries = new ArrayList<>();

        @Override
        int addKnownCell(KnownCell cell) {
            if (cell instanceof SoupCell) {
                soupCells.add((SoupCell) cell);
            }
            if (cell instanceof RobotCell) {
                RobotCell robot = (RobotCell)cell;
                if (!robot.enemy && robot.type == RobotType.REFINERY)
                    refineries.add(new MapLocation(robot.x, robot.y));
            }
            return super.addKnownCell(cell);
        }

        @Override
        void runTypeDependent() throws GameActionException {
            for (Direction dir : directions)
                if (tryMine(dir)) {
                    System.out.println("I mined soup! " + rc.getSoupCarrying());
                    return;
                }
            for (Direction dir : directions)
                if (tryRefine(dir)) {
                    System.out.println("I refined soup! " + rc.getTeamSoup());
                    return;
                }
            if (rc.getSoupCarrying() < rc.getType().soupLimit) {
                if (soupCells.size() > 0) {
                    SoupCell soup = Collections.min(soupCells, Comparator.comparing((SoupCell x) -> distance(x.location(),
                            rc.getLocation())));
                    if (distance(soup.location(), rc.getLocation()) < 5 || rc.getSoupCarrying() < rc.getType().soupLimit / 2)
                        tryMove(getDir(soup.location()));
                }
            }
            if (rc.getSoupCarrying() >= rc.getType().soupLimit / 4) {
//                if (HQLoc != null) {
                    assert (HQLoc != null);
                    Direction dir = getDir(HQLoc);
                    if (dir != null)
                        tryMove(dir);
                    return;
 //               }
            }
            if (moveExists())
               moveSmart();
        }
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (rc.getType() == RobotType.MINER)
            new MinerPlayer().start(rc);
        else
            new RobotPlayer().start(rc);
    }

    public void start(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        this.rc = rc;

        starts = new int[rc.getMapWidth()][rc.getMapHeight()];

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        if (rc.getRoundNum() > 1)
            processMessages(1);
        int begturn = rc.getRoundNum();
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                runAll();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                if (rc.getRoundNum() > begturn) {
                    int x=228;
                }
                Clock.yield();
                begturn = rc.getRoundNum();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    void runTypeDependent() throws GameActionException {
        switch (rc.getType()) {
            case HQ:
                runHQ();
                break;
            case REFINERY:
                runRefinery();
                break;
            case VAPORATOR:
                runVaporator();
                break;
            case DESIGN_SCHOOL:
                runDesignSchool();
                break;
            case FULFILLMENT_CENTER:
                runFulfillmentCenter();
                break;
            case LANDSCAPER:
                runLandscaper();
                break;
            case DELIVERY_DRONE:
                runDeliveryDrone();
                break;
            case NET_GUN:
                runNetGun();
                break;
        }
    }

    void runAll() throws GameActionException {
        readBlockchain();
        System.out.println("read blockchain: " + Clock.getBytecodeNum());
        if (rc.getType().canMove())
            scanSoup();
        scanRobots();
        runTypeDependent();
    }

    private void scanSoupLoc(int x, int y) throws GameActionException {
        MapLocation loc = new MapLocation(x,y);
        if (rc.canSenseLocation(loc)) {
            int soupLeft = rc.senseSoup(loc);
            if (soupLeft > 0) {
                KnownCell cell = new SoupCell(x, y, rc.getRoundNum(), soupLeft);
                int last = addKnownCell(cell);
                if (soupLeft >= 50) {
                    if (last == -1 || rc.getRoundNum() - last >= 50)
                        sendKnownCell(cell);
                }
            }
        }
    }

    private void scanSoup() throws GameActionException {
        // TODO faster
        int rad = rc.getCurrentSensorRadiusSquared();
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        int sqrt = (int)Math.sqrt(rad);
        switch(sqrt) {
            case 5: {
                scanSoupLoc(x+5, y);
                scanSoupLoc(x+4, y+1);
                scanSoupLoc(x+3, y+2);
                scanSoupLoc(x+2, y+3);
                scanSoupLoc(x+1, y+4);
                scanSoupLoc(x, y+5);
                scanSoupLoc(x-5, y);
                scanSoupLoc(x-4, y+1);
                scanSoupLoc(x-3, y+2);
                scanSoupLoc(x-2, y+3);
                scanSoupLoc(x-1, y+4);
                scanSoupLoc(x-5, y);
                scanSoupLoc(x-4, y-1);
                scanSoupLoc(x-3, y-2);
                scanSoupLoc(x-2, y-3);
                scanSoupLoc(x-1, y-4);
                scanSoupLoc(x+5, y);
                scanSoupLoc(x+4, y-1);
                scanSoupLoc(x+3, y-2);
                scanSoupLoc(x+2, y-3);
                scanSoupLoc(x+1, y-4);
                scanSoupLoc(x, y-5);
            }
        }
        /*
        for(int x = myx - sqrt; x <= myx + sqrt; ++x)
            for(int y = ley; y <= riy; ++y) {
//                if (Math.hypot(x-myx,y-myy) <= Math.sqrt(rad)) { //
//                if ((x-myx)*(x-myx) + (y-myy)*(y-myy) <= rad) {
                MapLocation loc = new MapLocation(x,y);
                if (rc.canSenseLocation(loc)) {
                    int soupLeft = rc.senseSoup(loc);
                    if (soupLeft > 0) {
                        KnownCell cell = new SoupCell(x, y, rc.getRoundNum(), soupLeft);
                        int last = addKnownCell(cell);
                        if (soupLeft >= 50) {
                            if (last == -1 || rc.getRoundNum() - last >= 50)
                                sendKnownCell(cell);
                        }
                    }
                }
            }
        */
    }

    private void scanRobots() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            if (robot.type.isBuilding()) {
                KnownCell cell = new RobotCell(robot.location.x, robot.location.y, rc.getRoundNum(), robot.ID,
                        robot.type, true);
                int last = addKnownCell(cell);
                if (last == -1 || rc.getRoundNum() - last >= 50)
                    sendKnownCell(cell);
            }
        }
    }

    void processMessages(int round) throws GameActionException {
        for (Transaction trans : rc.getBlock(round)) {
//                int[] decr = Crypto.Decrypt(trans.getMessage());
            int[] decr = trans.getMessage();
            decr[6] -= 239239239 * rc.getTeam().name().hashCode();
            if (decr[6] > round || decr[6] < round - 5) { // Enemy's message
                continue;
            }
            int type = decr[0];
            KnownCell cell = null;
            if (type == 0) {
                cell = new SoupCell(decr[1], decr[2], decr[6], decr[3]);
            }
            if (type == 1) {
                cell = new RobotCell(decr[1], decr[2], decr[6], decr[3], decr[4], decr[5] > 0);
            }
            if (cell != null) {
                addKnownCell(cell);
            }
        }
    }

    private void readBlockchain() throws GameActionException {
        for (int round = Math.max(1, rc.getRoundNum() - 1); round < rc.getRoundNum(); ++round)
            processMessages(round);
    }

    int addKnownCell(KnownCell cell) {
        if (cell instanceof RobotCell && ((RobotCell)cell).type == RobotType.HQ) {
            if (((RobotCell)cell).enemy)
                HQEnemyLoc = new MapLocation(cell.x, cell.y);
            else {
                System.out.println("Found HQ");
                HQLoc = new MapLocation(cell.x, cell.y);
            }
        }
        if (starts[cell.x][cell.y] == 0) {
            starts[cell.x][cell.y] = knownCells.size();
            knownCells.add(cell);
            next.add(0);
            return -1;
        }
        for (int cur = starts[cell.x][cell.y]; cur != 0; cur = next.get(cur)) {
            if (knownCells.get(cur).equals(cell)) {
                int res = knownCells.get(cur).lastVisited;
                knownCells.set(cur, cell);
                return res;
            }
        }
        knownCells.add(cell);
        next.add(starts[cell.x][cell.y]);
        starts[cell.x][cell.y] = knownCells.size() - 1;
        return -1;
    }

    void sendKnownCell(KnownCell cell) throws GameActionException {
        int[] msg = new int[7];
        if (cell instanceof SoupCell) {
            SoupCell soup = (SoupCell) cell;
            msg[0] = 0;
            msg[1] = soup.x;
            msg[2] = soup.y;
            msg[3] = soup.soupLeft;
        }
        if (cell instanceof RobotCell) {
            RobotCell robot = (RobotCell) cell;
            msg[0] = 1;
            msg[1] = robot.x;
            msg[2] = robot.y;
            msg[3] = robot.id;
            msg[4] = Arrays.asList(robotTypes).indexOf(robot.type);
            msg[5] = robot.enemy ? 1 : 0;
        }
        sendMessage(msg);
    }

    void runHQ() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.MINER, dir);
        if (rc.getRoundNum() == 1) {
            sendKnownCell(new RobotCell(rc.getLocation().x, rc.getLocation().y, 0, rc.getID(), RobotType.HQ, false));
        }
    }

    void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    void runVaporator() throws GameActionException {

    }

    void runDesignSchool() throws GameActionException {

    }

    void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    void runLandscaper() throws GameActionException {

    }

    MapLocation droneFindFlooded() {
        //TODO faster
        int D = rc.getCurrentSensorRadiusSquared();
        for (int x = rc.getLocation().x - 5; x <= rc.getLocation().x + 5; ++x)
            for (int y = rc.getLocation().y - 5; y <= rc.getLocation().y + 5; ++y) {
                MapLocation loc = new MapLocation(x, y);
                if (distance(loc, rc.getLocation()) <= D)
                    return loc;
            }
        return null;
    }

    void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            robots = Arrays.stream(robots).filter((RobotInfo x) -> x.type != RobotType.DELIVERY_DRONE).toArray(RobotInfo[]::new);

            if (robots.length > 0) {
                assert (rc.canPickUpUnit(robots[0].getID()));
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        }
        if (rc.isCurrentlyHoldingUnit()) {
            MapLocation loc = droneFindFlooded();
            if (loc != null) {
                Direction dir = getDir(loc);
                if (dir != null)
                    rc.move(dir);
            }
        }
        // No close robots, so search for robots within sight radius
        if (moveExists())
            moveSmart();
    }

    void runNetGun() throws GameActionException {
        RobotInfo[] robots = Arrays.stream(rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED,
                rc.getTeam().opponent())).filter((RobotInfo x) -> x.type == RobotType.DELIVERY_DRONE).toArray(RobotInfo[]::new);
        if (robots.length > 0) {
            rc.shootUnit(Arrays.stream(robots).min(Comparator.comparing((RobotInfo x) -> distance(rc.getLocation(),
                    x.location))).get().getID());
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    boolean moveExists() {
        for (Direction direction : directions)
            if (rc.canMove(direction))
                return true;
        return false;
    }

    Direction[] moveOrder = null;

    void moveSmart() throws GameActionException {
        assert (moveExists());
        while (true) {
            if (moveOrder != null) {
                for (Direction dir : moveOrder)
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        return;
                    }
            }
            moveOrder = new Direction[3];
            int ind = rnd.nextInt(directions.length);
            int dir = rnd.nextInt(2) * 2 - 1;
            for (int i = 0; i < 3; ++i)
                moveOrder[i] = directions[(ind + dir * i + directions.length) % directions.length];
        }
    }

    boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir  The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    void sendMessage(int[] msg) throws GameActionException {
        if (true) {
            assert (msg[6] == 0);
            msg[6] = rc.getRoundNum() + rc.getTeam().name().hashCode()*239239239;
//            int[] enc = Crypto.Encrypt(msg);
            if (!rc.canSubmitTransaction(msg, 1)) {
                if (rc.getTeamSoup() >= 1)
                    System.out.println("Wtf why can't I submit a transaction");
                return;
            }
            rc.submitTransaction(msg, 1);
            // System.out.println(rc.getRoundMessages(turnCount-1));
        }
    }
}
