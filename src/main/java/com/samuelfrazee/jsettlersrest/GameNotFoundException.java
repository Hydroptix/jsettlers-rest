package com.samuelfrazee.jsettlersrest;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException() {
        super("Game not found");
    }

    public GameNotFoundException(String gameId) {
        super("Game \"" + gameId + "\" not found");
    }

}
