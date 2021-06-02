package com.samuelfrazee.jsettlersrest;

import soc.baseclient.ServerConnectInfo;
import soc.disableDebug.D;
import soc.game.*;
import soc.message.*;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.robot.SOCRobotDM;
import soc.server.genericServer.StringServerSocket;
import soc.util.CappedQueue;
import soc.util.SOCFeatureSet;
import soc.util.Version;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Vector;

public class JSettlersClient extends SOCRobotClient {

    /**
     * Thread for reading socket input
     */
    private Thread socketListener;

    /**
     * Collection of features that a bot supports (to be removed later for different REST agents)
     * TODO: change to be supplied by REST client
     */
    private SOCFeatureSet cliFeats;

    /**
     * String identifier for the client to send to the server.
     * Will change later to be supplied by the rest client.
     * TODO: change to be supplied by REST client
     */
    private static final String nickname = "JSettlers REST Client";


    /**
     * Class to report to the server
     */
    private final String rbclass = this.getClass().getName();

    public JSettlersClient(ServerConnectInfo sci) {
        super(sci, nickname, null);
    }

    public Hashtable<String, SOCGame> getGames() {
        return games;
    }

    public SOCGame getGame(String gameId) {
        return this.games.get(gameId);
    }

    public SOCBoard getGameBoard(String gameId) {
        SOCGame game = this.games.get(gameId);
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }

        return game.getBoard();
    }

    public SOCPlayer[] getGamePlayers(String gameId) {
        return this.games.get(gameId).getPlayers();
    }

    public SOCPlayer getMyPlayer(SOCGame game) {
        return game.getPlayer(this.getNickname());
    }

    @Override
    public void init() {
        try {
            if (serverConnectInfo.stringSocketName == null) {
                sock = new Socket(serverConnectInfo.hostname, serverConnectInfo.port);
                sock.setSoTimeout(300000);
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());
            } else {
                System.out.println(this + " connecting to server");
                sLocal = StringServerSocket.connectTo(serverConnectInfo.stringSocketName);
            }
            connected = true;
            socketListener = new Thread(this);
            socketListener.start();

            if (cliFeats == null) {
                cliFeats = buildClientFeats();
                // subclass or third-party bot may override: must check result
                if (cliFeats == null)
                    throw new IllegalStateException("buildClientFeats() must not return null");
            }

            resetThread = new SOCRobotResetThread(this);
            resetThread.start();
            put(SOCVersion.toCmd
                    (Version.versionNumber(), Version.version(), Version.buildnum(), cliFeats.getEncodedList(), null));
            put(SOCImARobot.toCmd(nickname, serverConnectInfo.robotCookie, rbclass));
        } catch (Exception e) {
            ex = e;
            System.err.println("Could not connect to the server: " + ex);
        }
    }

    @Override
    protected SOCGame handleSITDOWN(final SOCSitDown mes) {
        /**
         * tell the game that a player is sitting
         */
        SOCGame ga = games.get(mes.getGame());
        if (ga == null)
            return null;

        final int pn = mes.getPlayerNumber();
        final String plName = mes.getNickname();
        SOCPlayer player = null;

        ga.takeMonitor();
        try {
            ga.addPlayer(plName, pn);
            player = ga.getPlayer(pn);
            player.setRobotFlag(mes.isRobot(), false);
        } catch (Exception e) {
            System.out.println("Exception caught - " + e);
            e.printStackTrace();

            return null;
        } finally {
            ga.releaseMonitor();
        }

        if (nickname.equals(plName)
                && (ga.isPractice || (sVersion >= SOCDevCardAction.VERSION_FOR_SITDOWN_CLEARS_INVENTORY))) {
            // server is about to send our dev-card inventory contents
            player.getInventory().clear();
        }

        /**
         * change our face to the robot face
         */
        put(new SOCChangeFace(ga.getName(), pn, 0).toCmd());

        return ga;
    }

    public boolean joinGame(String gameId) {

        if (put(SOCJoinGame.toCmd(nickname, password, SOCMessage.EMPTYSTR, gameId))) {
            D.ebugPrintlnINFO("**** sent SOCJoinGame ****");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void treat(SOCMessage mes) {
        if (mes == null)
            return;  // Message syntax error or unknown type

        // Using debugRandomPause?
        if (debugRandomPause
                && (mes instanceof SOCMessageForGame)
                && !(mes instanceof SOCGameTextMsg)
                && !(mes instanceof SOCGameServerText)
                && !(mes instanceof SOCTurn)) {
            final String ga = ((SOCMessageForGame) mes).getGame();
            if (ga != null) {
                if (!debugRandomPauseActive) {
                    // random chance of doing so
                    if ((Math.random() < DEBUGRANDOMPAUSE_FREQ)
                            && ((debugRandomPauseQueue == null)
                            || (debugRandomPauseQueue.isEmpty()))) {
                        SOCGame gm = games.get(ga);
                        final int cpn = gm.getCurrentPlayerNumber();
                        SOCPlayer rpl = gm.getPlayer(nickname);
                        if ((rpl != null) && (cpn == rpl.getPlayerNumber())
                                && (gm.getGameState() >= SOCGame.ROLL_OR_CARD)) {
                            // we're current player, pause us
                            debugRandomPauseActive = true;
                            debugRandomPauseUntil = System.currentTimeMillis() + (1000L * DEBUGRANDOMPAUSE_SECONDS);
                            if (debugRandomPauseQueue == null)
                                debugRandomPauseQueue = new Vector<SOCMessage>();
                            System.err.println("L379 -> do random pause: " + nickname);
                            sendText(gm,
                                    "debugRandomPauseActive for " + DEBUGRANDOMPAUSE_SECONDS + " seconds");
                        }
                    }
                }
            }
        }

        if (debugRandomPause && debugRandomPauseActive) {
            if ((System.currentTimeMillis() < debugRandomPauseUntil)
                    && !(mes instanceof SOCTurn)) {
                // time hasn't arrived yet, and still our turn:
                //   Add message to queue (even non-game and SOCGameTextMsg)
                debugRandomPauseQueue.addElement(mes);

                return;  // <--- Early return: debugRandomPauseActive ---
            }

            // time to resume the queue
            debugRandomPauseActive = false;
            while (!debugRandomPauseQueue.isEmpty()) {
                // calling ourself is safe, because
                //  ! queue.isEmpty; thus won't decide
                //  to set debugRandomPauseActive=true again.
                treat(debugRandomPauseQueue.firstElement());
                debugRandomPauseQueue.removeElementAt(0);
            }

            // Don't return from this method yet,
            // we still need to process mes.
        }

        if ((debugTraffic || D.ebugIsEnabled())
                && !((mes instanceof SOCServerPing) && (nextServerPingExpectedAt != 0)
                && (Math.abs(System.currentTimeMillis() - nextServerPingExpectedAt) <= 66000)))
        // within 66 seconds of the expected time; see displaylesscli.handleSERVERPING
        {
            soc.debug.D.ebugPrintlnINFO("IN - " + nickname + " - " + mes);
        }

        try {
            switch (mes.getType()) {
                /**
                 * status message
                 */
                case SOCMessage.STATUSMESSAGE:
                    handleSTATUSMESSAGE((SOCStatusMessage) mes);
                    break;

                /**
                 * admin ping
                 */
                case SOCMessage.ADMINPING:
                    handleADMINPING((SOCAdminPing) mes);
                    break;

                /**
                 * admin reset
                 */
                case SOCMessage.ADMINRESET:
                    handleADMINRESET((SOCAdminReset) mes);
                    break;

                /**
                 * update the current robot parameters
                 */
                case SOCMessage.UPDATEROBOTPARAMS:
                    handleUPDATEROBOTPARAMS((SOCUpdateRobotParams) mes);
                    break;

                /**
                 * join game authorization
                 */
                case SOCMessage.JOINGAMEAUTH:
                    handleJOINGAMEAUTH((SOCJoinGameAuth) mes, (sLocal != null));
                    break;

                /**
                 * game has been destroyed
                 */
                case SOCMessage.DELETEGAME:
                    handleDELETEGAME((SOCDeleteGame) mes);
                    break;

                /**
                 * list of game members
                 */
                case SOCMessage.GAMEMEMBERS:
                    handleGAMEMEMBERS((SOCGameMembers) mes);
                    break;

                /**
                 * game text message (bot debug commands)
                 */
                case SOCMessage.GAMETEXTMSG:
                    handleGAMETEXTMSG((SOCGameTextMsg) mes);
                    break;

                /**
                 * someone is sitting down
                 */
                case SOCMessage.SITDOWN:
                    handleSITDOWN((SOCSitDown) mes);
                    break;

                /**
                 * the server is requesting that we join a game
                 */
                case SOCMessage.BOTJOINGAMEREQUEST:
                    handleBOTJOINGAMEREQUEST((SOCBotJoinGameRequest) mes);
                    break;

                /**
                 * message that means the server wants us to leave the game
                 */
                case SOCMessage.ROBOTDISMISS:
                    handleROBOTDISMISS((SOCRobotDismiss) mes);
                    break;

                /**
                 * handle board reset (new game with same players, same game name, new layout).
                 */
                case SOCMessage.RESETBOARDAUTH:
                    handleRESETBOARDAUTH((SOCResetBoardAuth) mes);
                    break;

                /**
                 * generic "simple request" responses or announcements from the server.
                 * Message type added 2013-02-17 for v1.1.18,
                 * bot ignored these until 2015-10-10 for v2.0.00 SC_PIRI
                 * and for PROMPT_PICK_RESOURCES from gold hex.
                 */
                case SOCMessage.SIMPLEREQUEST:
                    handleSIMPLEREQUEST(games, (SOCSimpleRequest) mes);
                    //handlePutBrainQ((SOCSimpleRequest) mes);
                    break;

                /**
                 * generic "simple action" announcements from the server.
                 * Added 2013-09-04 for v1.1.19.
                 */
                case SOCMessage.SIMPLEACTION:
                    super.handleSIMPLEACTION(games, (SOCSimpleAction) mes);
                    //handlePutBrainQ((SOCSimpleAction) mes);
                    break;

                /**
                 * a special inventory item action: either add or remove,
                 * or we cannot play our requested item.
                 * Added 2013-11-26 for v2.0.00.
                 */
                case SOCMessage.INVENTORYITEMACTION: {
                    final boolean isReject = super.handleINVENTORYITEMACTION
                            (games, (SOCInventoryItemAction) mes);
                    //if (isReject)
                    //handlePutBrainQ((SOCInventoryItemAction) mes);
                }
                break;

                /**
                 * Special Item change announcements.
                 * Added 2014-04-16 for v2.0.00.
                 */
                case SOCMessage.SETSPECIALITEM:
                    super.handleSETSPECIALITEM(games, (SOCSetSpecialItem) mes);
                    //handlePutBrainQ((SOCSetSpecialItem) mes);
                    break;

                // These message types are ignored by the robot client;
                // don't send them to SOCDisplaylessClient.treat:

                case SOCMessage.BCASTTEXTMSG:
                case SOCMessage.CHANGEFACE:
                case SOCMessage.CHANNELMEMBERS:
                case SOCMessage.CHANNELS:        // If bot ever uses CHANNELS, update SOCChannels class javadoc
                case SOCMessage.CHANNELTEXTMSG:
                case SOCMessage.DELETECHANNEL:
                case SOCMessage.GAMES:
                case SOCMessage.GAMESERVERTEXT:  // SOCGameServerText contents are ignored by bots
                    // (but not SOCGameTextMsg, which is used solely for debug commands)
                case SOCMessage.GAMESTATS:
                case SOCMessage.JOINCHANNEL:
                case SOCMessage.JOINCHANNELAUTH:
                case SOCMessage.LEAVECHANNEL:
                case SOCMessage.NEWCHANNEL:
                case SOCMessage.NEWGAME:
                case SOCMessage.SETSEATLOCK:
                    break;  // ignore this message type

                /**
                 * Call SOCDisplaylessClient.treat for all other message types.
                 * For types relevant to robots, it will update data from the message contents.
                 * Other message types will be ignored.
                 */
                default:
                    super.treat(mes, true);
            }
        } catch (Throwable e) {
            System.err.println("SOCRobotClient treat ERROR - " + e + " " + e.getMessage());
            e.printStackTrace();
            while (e.getCause() != null) {
                e = e.getCause();
                System.err.println(" -> nested: " + e.getClass());
                e.printStackTrace();
            }
            System.err.println("-- end stacktrace --");
            System.out.println("  For message: " + mes);
        }
    }

    public void disconnectFromServer() {
        disconnect();
    }


    public void discard(SOCGame ga, SOCResourceSet rs) {
        String cmd = SOCDiscard.toCmd(ga.getName(), rs);
        System.out.println(cmd);

        put(cmd);
    }
}