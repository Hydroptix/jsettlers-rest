package com.samuelfrazee.jsettlersrest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.stereotype.Component;
import soc.game.*;

import java.io.IOException;

@Component
public class SOCPlayerSerializer extends StdSerializer<SOCPlayer> {

    public SOCPlayerSerializer() {
        super(SOCPlayer.class);
    }

    @Override
    public void serialize(SOCPlayer player, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeStringField("name", player.getName());
        jgen.writeNumberField("playerNumber", player.getPlayerNumber());
        jgen.writeStringField("gameId", player.getGame().getName());
        jgen.writeNumberField("numSettlements", player.getNumPieces(SOCPlayingPiece.SETTLEMENT));
        jgen.writeNumberField("numCities", player.getNumPieces(SOCPlayingPiece.CITY));
        jgen.writeNumberField("numRoads", player.getNumPieces(SOCPlayingPiece.ROAD));

        // Prevent infinite JSON recursion
        jgen.writeArrayFieldStart("pieces");
        for (SOCPlayingPiece piece : player.getPieces()) {
            jgen.writeStartObject();
            jgen.writeStringField("type", SOCPlayingPiece.getTypeName(piece.getType()));
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("roadsAndShips");
        for (SOCRoutePiece piece : player.getRoadsAndShips()) {
            jgen.writeStartObject();
            jgen.writeStringField("type", piece.getTypeName(piece.getType()));
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("settlements");
        for(SOCSettlement piece: player.getSettlements()) {
            jgen.writeStartObject();
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("cities");
        for(SOCCity piece: player.getCities()) {
            jgen.writeStartObject();
            jgen.writeStringField("player", piece.getPlayer().getName());
            jgen.writeNumberField("playerNumber", piece.getPlayerNumber());
            jgen.writeNumberField("coordinates", piece.getCoordinates());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeNumberField("lastSettlementCoord", player.getLastSettlementCoord());
        jgen.writeNumberField("lastRoadCoord", player.getLastRoadCoord());
        jgen.writeNumberField("longestRoadLength", player.getLongestRoadLength());
        jgen.writeObjectField("lrPaths", player.getLRPaths());
        jgen.writeObjectField("resources", player.getResources());
        jgen.writeObjectField("rolledResources", player.getRolledResources());
        jgen.writeObjectField("inventory", player.getInventory());
        jgen.writeNumberField("numKnights", player.getNumKnights());
        jgen.writeObjectField("devCardsPlayed", player.getDevCardsPlayed());
        jgen.writeBooleanField("needToDiscard", player.getNeedToDiscard());
        jgen.writeObjectField("roadNodes", player.getRoadNodes());
        jgen.writeObjectField("legalSettlements", player.getLegalSettlements());
        jgen.writeNumberField("addedLegalSettlement", player.getAddedLegalSettlement());
        jgen.writeObjectField("potentialSettlements", player.getPotentialSettlements());
        jgen.writeObjectField("ports", player.getPortFlags());
        jgen.writeObjectField("currentOffer", player.getCurrentOffer());
        jgen.writeNumberField("currentOfferTime", player.getCurrentOfferTime());
        jgen.writeBooleanField("hasPlayedDevCard", player.hasPlayedDevCard());
        jgen.writeBooleanField("hasAskedBoardReset", player.hasAskedBoardReset());
        jgen.writeBooleanField("isRobot", player.isRobot());
        jgen.writeBooleanField("isBuiltInRobot", player.isBuiltInRobot());
        jgen.writeObjectField("ourNumbers", player.getNumbers());
        jgen.writeEndObject();

    }

}
