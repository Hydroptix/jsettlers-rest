package com.samuelfrazee.jsettlersrest;

import soc.baseclient.ServerConnectInfo;
import soc.disableDebug.D;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.message.*;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
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
     */
    private SOCFeatureSet cliFeats;

    /**
     * String identifier for the client to send to the server.
     * Will change later to be supplied by the rest client.
     */
    private static String nickname = "JSettlers REST Client";

    /**
     * Class to report to the server
     */
    private String rbclass = this.getClass().getName();

    public JSettlersClient(ServerConnectInfo sci) {
        super(sci, nickname, null);
        System.out.println(this + ": Hello");
    }

    public Hashtable<String, SOCGame> getGames() {
        System.out.println(this.games);
        System.out.println(this);
        return games;
    }

    public boolean joinGame(String gameId) {
        if (put(SOCJoinGame.toCmd(nickname, password, SOCMessage.EMPTYSTR, gameId)))
        {
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