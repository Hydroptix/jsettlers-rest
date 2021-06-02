package com.samuelfrazee.jsettlersrest;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.web.bind.annotation.*;
import soc.game.*;

import java.util.*;

@RestController
@RequestMapping("/games")
public class GamesController {

    private boolean gameExists(String gameId) {
        return JsettlersrestApplication.jsettlersClient.getGame(gameId) != null;
    }

    @GetMapping("")
    public List<String> getGames() {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;
        return new ArrayList<String>(gameClient.getGames().keySet());
    }

    @DeleteMapping("/{gameId}/leave")
    public ResponseEntity<Void> leaveGame(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;

        gameClient.leaveGame(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<Void> joinGame(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;

        boolean joined = gameClient.joinGame(gameId);

        // TODO: better checking of whether the game was successfully joined or not
        if (joined) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{gameId}/board")
    public SOCBoard getGameBoard(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;

        // TODO: might be a better return format for this
        return gameClient.getGameBoard(gameId);
    }

    @GetMapping("/{gameId}/players")
    public SOCPlayer[] getGamePlayers(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;

        if (gameClient.getGame(gameId) == null) {
            throw new GameNotFoundException(gameId);
        }

        return gameClient.getGamePlayers(gameId);
    }

    @GetMapping("/{gameId}/currentPlayer")
    public String getCurrentPlayer(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;
        SOCGame game = gameClient.getGame(gameId);

        if (game == null) {
            throw new GameNotFoundException(gameId);
        }

        if (game.getCurrentPlayerNumber() <= 0) {
            return "";
        } else {
            return game.getPlayer(game.getCurrentPlayerNumber()).getName();
        }
    }

    @PostMapping("{gameId}/action")
    public ResponseEntity<String> postGameAction(@PathVariable String gameId, @RequestBody Map<String, Object> gameAction) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;
        SOCGame game = gameClient.getGame(gameId);
        String actionType;
        Map<String, Object> args;
        Map<String, Integer> validResources = new HashMap<>(){{
            put("clay", 1);
            put("ore", 2);
            put("sheep", 3);
            put("wheat", 4);
            put("wood", 5);
        }};

        if (game == null) {
            throw new GameNotFoundException(gameId);
        }

        if (gameAction.get("action") instanceof String) {
            actionType = (String) gameAction.get("action");
        } else {
            // TODO: make this an actual exception to fit with Spring
            return ResponseEntity.badRequest().body("value of \"action\" not a string");
        }

        // Actions that don't need args here
        if (actionType.equals("rejectOffer")) {
            gameClient.rejectOffer(game);
            //TODO: better checking if an offer was actually refused
            return ResponseEntity.ok().build();
        }

        if (actionType.equals("buyDevCard")) {
            System.out.println("Client trying to buy dev card");
            gameClient.buyDevCard(game);
            //TODO: better checking if we actually could buy a dev card
            return ResponseEntity.ok().build();
        }

        if (actionType.equals("endTurn")) {
            gameClient.endTurn(game);
            //TODO: better checking if it was actually our turn
            return ResponseEntity.ok().build();
        }

        if (actionType.equals("clearOffer")) {
            gameClient.clearOffer(game);
            return ResponseEntity.ok().build();
        }

        if (actionType.equals("rollDice")) {
            gameClient.rollDice(game);
            // TODO: better checking if it was actually the player's turn to roll the dice
            return ResponseEntity.ok().build();
        }

        // Actions that need args here
        try {
            // We don't know that this is going to be a map until we try to cast it because it came from the client.
            // We're going to try to cast, and if it fails we're going to tell the client they suck for sending us bad info.
            @SuppressWarnings("unchecked")
            Map<String, Object> maybeArgsMap = (Map<String, Object>) gameAction.get("args");

            args = maybeArgsMap;
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("\"args\" not a JSON object");
        }

        if (actionType.equals("putPiece")) {
            SOCPlayingPiece piece;
            String pieceType;
            int coord;

            if (args.get("coord") instanceof String) {
                try {
                    coord = Integer.parseInt((String) args.get("coord"));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body("\"coord\" not a number");
                }
            } else if (args.get("coord") instanceof Integer) {
                coord = (Integer) args.get("coord");
            } else {
                return ResponseEntity.badRequest().body("\"coord\" not a number");
            }

            if (args.get("pieceType") instanceof String) {
                pieceType = (String) args.get("pieceType");
            } else {
                return ResponseEntity.badRequest().body("\"pieceType\" not a JSON object");
            }

            switch (pieceType.toLowerCase()) {
                case ("road"):
                    piece = new SOCRoad(gameClient.getMyPlayer(game), coord, null);
                    break;
                case ("settlement"):
                    piece = new SOCSettlement(gameClient.getMyPlayer(game), coord, null);
                    break;
                case ("city"):
                    piece = new SOCCity(gameClient.getMyPlayer(game), coord, null);
                    break;
                default:
                    return ResponseEntity.badRequest().body("\"" + pieceType + "\" not a valid piece type");
            }

            System.out.println("putting piece" + piece);
            gameClient.putPiece(game, piece);

            // TODO: better checking to see if a piece was actually placed
            return ResponseEntity.ok().build();
        }

        if (actionType.equals("acceptOffer")) {
            int from;

            if (args.get("from") instanceof String) {
                try {
                    from = Integer.parseInt((String) args.get("from"));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body("\"from\" not a number");
                }
            } else if (args.get("from") instanceof Integer) {
                from = (Integer) args.get("from");
            } else {
                return ResponseEntity.badRequest().body("\"coord\" not a number");
            }
            gameClient.acceptOffer(game, from);

            // TODO: check if the offer was actually accepted
            return ResponseEntity.ok().build();
        }

        if (actionType.equals("bankTrade")) {
            SOCResourceSet give = new SOCResourceSet();
            SOCResourceSet get = new SOCResourceSet();
            Map<String, Object> giveMap;
            Map<String, Object> getMap;

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> maybeGiveMap = (Map<String, Object>)args.get("give");

                giveMap = maybeGiveMap;
            } catch (ClassCastException e) {
                return ResponseEntity.badRequest().body("\"give\" is not a valid JSON object");
            }

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> maybeGetMap = (Map<String, Object>)args.get("get");

                getMap = maybeGetMap;
            } catch (ClassCastException e) {
                return ResponseEntity.badRequest().body("\"get\" is not a valid JSON object");
            }

            for (Map.Entry<String, Object> item:giveMap.entrySet()) {
                String resourceType = item.getKey();
                int resourceCount;
                int rType;

                if (validResources.containsKey(resourceType)) {
                    rType = validResources.get(resourceType);
                } else {
                    return ResponseEntity.badRequest().body("\"" + resourceType + "\" not a valid resource type");
                }

                if (item.getValue() instanceof String) {
                    try {
                        resourceCount = Integer.parseInt((String)item.getValue());
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body("Resource count must be an integer");
                    }

                } else if (item.getValue() instanceof Integer) {
                    resourceCount = (Integer)item.getValue();

                } else {
                    return ResponseEntity.badRequest().body("Resource count must be an integer");
                }

                give.add(resourceCount, rType);
            }

            for (Map.Entry<String, Object> item:getMap.entrySet()) {
                String resourceType = item.getKey();
                int resourceCount;
                int rType;

                if (validResources.containsKey(resourceType)) {
                    rType = validResources.get(resourceType);
                } else {
                    return ResponseEntity.badRequest().body("\"" + resourceType + "\" not a valid resource type");
                }

                if (item.getValue() instanceof String) {
                    try {
                        resourceCount = Integer.parseInt((String)item.getValue());
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body("Resource count must be an integer");
                    }

                } else if (item.getValue() instanceof Integer) {
                    resourceCount = (Integer)item.getValue();

                } else {
                    return ResponseEntity.badRequest().body("Resource count must be an integer");
                }

                get.add(resourceCount, rType);
            }

            System.out.println("Got bank trade:");
            System.out.println(give);
            System.out.println(get);
            gameClient.bankTrade(game, give, get);

            return ResponseEntity.ok().build();

        } else if (actionType.equals("playDevCard")) {
            int dc;
            try {
                dc = (Integer) args.get("dc");
            } catch (ClassCastException e) {
                return ResponseEntity.badRequest().body("dc must be an integer");
            }

            gameClient.playDevCard(game, dc);

            return ResponseEntity.ok().build();


        } else if (actionType.equals("moveRobber")) {
            int coord;
            SOCPlayer targetPlayer = null;

            if (args.get("targetPlayer") instanceof String) {
                targetPlayer = game.getPlayer((String) args.get("targetPlayer"));
                if (targetPlayer == null) {
                    return ResponseEntity.badRequest().body("No player named \"" + args.get("targetPlayer") + "\" at this game");
                }
            } else if (args.get("targetPlayer") != null) {
                return ResponseEntity.badRequest().body("targetPlayer not a string");
            }

            try {
                coord = (Integer) args.get("coord");
            } catch (ClassCastException e) {
                return ResponseEntity.badRequest().body("coord must be an integer");
            }

            gameClient.moveRobber(game, gameClient.getMyPlayer(game), coord);

            if(targetPlayer != null) {
                gameClient.choosePlayer(game, targetPlayer.getPlayerNumber());
            }

            return ResponseEntity.ok().build();

        } else if (actionType.equals("discard")) {
            SOCResourceSet resourcesToDiscard = new SOCResourceSet();

            System.out.println(args);

            if (args.get("wheat") instanceof Integer) {
                resourcesToDiscard.add((Integer) args.get("wheat"), SOCResourceConstants.WHEAT);
            }
            if (args.get("wood") instanceof Integer) {
                resourcesToDiscard.add((Integer) args.get("wood"), SOCResourceConstants.WOOD);
            }
            if (args.get("ore") instanceof Integer) {
                resourcesToDiscard.add((Integer) args.get("ore"), SOCResourceConstants.ORE);
            }
            if (args.get("sheep") instanceof Integer) {
                resourcesToDiscard.add((Integer) args.get("sheep"), SOCResourceConstants.SHEEP);
            }
            if (args.get("clay") instanceof Integer) {
                resourcesToDiscard.add((Integer) args.get("clay"), SOCResourceConstants.CLAY);
            }
            System.out.println("Client trying to discard:");
            System.out.println(resourcesToDiscard);
            gameClient.discard(game, resourcesToDiscard);

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().body("\"" + actionType + "\" is not a valid action");
    }

    @GetMapping("{gameId}/lastRoll")
    public Integer getLastRoll(@PathVariable String gameId) {
        JSettlersClient gameClient = JsettlersrestApplication.jsettlersClient;
        SOCGame game = gameClient.getGame(gameId);

        if (game == null) {
            throw new GameNotFoundException(gameId);
        }

        return game.getCurrentDice();
    }

}
