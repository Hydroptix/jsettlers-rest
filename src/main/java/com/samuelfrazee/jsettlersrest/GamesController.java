package com.samuelfrazee.jsettlersrest;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soc.game.SOCGame;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@RestController
public class GamesController {

    @GetMapping("/games")
    public List<String> getGames() {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;
        return new ArrayList<String>(gameClient.getGames().keySet());
    }

    @DeleteMapping("/games/{gameId}/leave")
    public ResponseEntity leaveGame(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;

        System.out.println("Trying to leave game \"" + gameId + "\"");
        gameClient.leaveGame(gameId);
        return ResponseEntity.ok().body("");
    }

    @PostMapping("/games/{gameId}/join")
    public ResponseEntity joinGame(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;

        gameClient.joinGame(gameId);

        return ResponseEntity.ok().body("");
    }

}
