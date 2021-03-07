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

            //resetThread = new SOCRobotResetThread(this);
            //resetThread.start();
            put(SOCVersion.toCmd
                    (Version.versionNumber(), Version.version(), Version.buildnum(), cliFeats.getEncodedList(), null));
            put(SOCImARobot.toCmd(nickname, serverConnectInfo.robotCookie, rbclass));
        } catch (Exception e) {
            ex = e;
            System.err.println("Could not connect to the server: " + ex);
        }
    }

    @Override
    protected SOCGame handleSITDOWN(final SOCSitDown mes)
    {
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
        try
        {
            ga.addPlayer(plName, pn);
            player = ga.getPlayer(pn);
            player.setRobotFlag(mes.isRobot(), false);
        }
        catch (Exception e)
        {
            System.out.println("Exception caught - " + e);
            e.printStackTrace();

            return null;
        }
        finally
        {
            ga.releaseMonitor();
        }

        if (nickname.equals(plName)
                && (ga.isPractice || (sVersion >= SOCDevCardAction.VERSION_FOR_SITDOWN_CLEARS_INVENTORY)))
        {
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

    public void disconnectFromServer() {
        disconnect();
    }

}